package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.App
import gs.git.vps.data.github.model.GHActionResult
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHLicenseDetail
import gs.git.vps.data.github.model.GHPermissions
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHReviewComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale

object GitHubManager {

    private const val TAG = "GH"
    @Volatile private var cachedApiUrl: String = "https://api.github.com"
    fun getApiUrl(): String = cachedApiUrl
    fun getWebUrl(): String {
        val api = getApiUrl()
        if (api == "https://api.github.com") {
            return "https://github.com"
        }
        return try {
            val uri = URI(api)
            URI(uri.scheme, null, uri.host, uri.port, null, null, null).toString()
        } catch (_: Exception) {
            "https://github.com"
        }
    }
    fun setApiUrl(context: Context, url: String): Boolean {
        val resolved = normaliseApiUrl(url) ?: return false
        cachedApiUrl = resolved
        clearEtagCache()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("custom_api_url", resolved)
            .apply()
        return true
    }
    internal fun updateApiUrl(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val custom = prefs.getString("custom_api_url", "") ?: ""
        val resolved = normaliseApiUrl(custom) ?: "https://api.github.com"
        if (cachedApiUrl != resolved) clearEtagCache()
        cachedApiUrl = resolved
    }

    private fun normaliseApiUrl(url: String): String? {
        val candidate = url.trim().trimEnd('/').ifBlank { "https://api.github.com" }
        return try {
            val uri = URI(candidate)
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
                !uri.userInfo.isNullOrBlank() || uri.rawQuery != null || uri.rawFragment != null
            ) return null
            URI("https", null, uri.host, uri.port, uri.path?.trimEnd('/'), null, null).toString()
                .trimEnd('/')
        } catch (_: Exception) {
            null
        }
    }
    internal const val PREFS = "github_prefs"
    internal const val KEY_USER = "user_json"
    private const val CODE_NOT_MODIFIED = 304

    private val etagCache = ConcurrentHashMap<String, Pair<String, Map<String, String>>>()
    @Volatile private var lastRateRemaining: Int = Int.MAX_VALUE
    @Volatile private var lastRateReset: Long = 0L

    fun getRateLimitRemaining(): Int = lastRateRemaining
    fun getRateLimitResetEpoch(): Long = lastRateReset
    fun isRateLimitLow(): Boolean = lastRateRemaining < 10
    fun clearEtagCache() { etagCache.clear() }

    data class TokenValidation(val valid: Boolean, val scopes: String, val login: String, val error: String)

    suspend fun validateToken(context: Context): TokenValidation {
        val token = GitHubAuth.resolveApiToken(context)
        if (token.isBlank()) return TokenValidation(false, "", "", "no token stored")
        val r = request(context, "/user", trackErrors = false)
        if (!r.success) return TokenValidation(false, "", r.body.take(100), "HTTP ${r.code}: ${r.body.take(200)}")
        return try {
            val j = JSONObject(r.body)
            val scopesHeader = r.headers["x-oauth-scopes"] ?: ""
            TokenValidation(true, scopesHeader, j.optString("login", ""), "")
        } catch (e: Exception) { TokenValidation(false, "", "", e.message ?: "parse error") }
    }

    suspend fun getCopilotToken(context: Context): String {
        // Copilot-эндпоинты принимают только PAT/OAuth-токен с copilot-scope;
        // user-token GitHub App сюда не подходит — всегда шлём PAT.
        val patToken = getToken(context)
        var res = request(
            context = context,
            endpoint = "https://api.github.com/copilot_internal/v2/token",
            method = "GET",
            extraHeaders = mapOf(
                "User-Agent" to "GitHubCopilotChat/0.11.0",
                "Accept" to "application/json"
            ),
            trackErrors = false,
            authToken = patToken
        )
        if (!res.success) {
            res = request(
                context = context,
                endpoint = "https://api.github.com/copilot_user/token",
                method = "GET",
                extraHeaders = mapOf(
                    "User-Agent" to "GitHubCopilotChat/0.11.0",
                    "Accept" to "application/json"
                ),
                trackErrors = false,
                authToken = patToken
            )
        }
        if (res.success) {
            return JSONObject(res.body).optString("token", "")
        } else {
            throw java.io.IOException("Failed to get Copilot token: HTTP ${res.code}: ${res.body.trim()}")
        }
    }

    fun saveToken(context: Context, token: String): Boolean {
        return GitHubAuth.saveToken(context, token).also { saved ->
            if (saved) clearEtagCache()
        }
    }
    fun getToken(context: Context): String = GitHubAuth.getToken(context)
    fun isLoggedIn(context: Context): Boolean = GitHubAuth.isLoggedIn(context)
    fun logout(context: Context) {
        GitHubAuth.logout(context)
        clearEtagCache()
        lastRateRemaining = Int.MAX_VALUE
        lastRateReset = 0L
    }
    fun getApiErrorLog(context: Context): List<GHApiErrorLogEntry> = GitHubAuth.getApiErrorLog(context)
    fun clearApiErrorLog(context: Context) = GitHubAuth.clearApiErrorLog(context)

    internal suspend fun request(
        context: Context,
        endpoint: String,
        method: String = "GET",
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        trackErrors: Boolean = true,
        rateLimitRetries: Int = 1,
        backoffRetries: Int = 3,
        authToken: String? = null,
    ): ApiResult =
        withContext(Dispatchers.IO) {
            updateApiUrl(context)
            var conn: HttpURLConnection? = null
            try {
                val token = authToken ?: GitHubAuth.resolveApiToken(context)
                val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                
                // Diff whitespace ignore
                var finalEndpoint = endpoint
                if (method == "GET" && 
                    (endpoint.contains("/commits/") || endpoint.contains("/compare/") || (endpoint.contains("/pulls/") && endpoint.contains("/files"))) && 
                    !endpoint.contains("w=")
                ) {
                    if (prefs.getBoolean("editor_ignore_whitespace", false)) {
                        finalEndpoint = if (endpoint.contains("?")) "$endpoint&w=1" else "$endpoint?w=1"
                    }
                }
                
                val url = if (finalEndpoint.startsWith("http")) finalEndpoint else "${getApiUrl()}$finalEndpoint"
                if (!url.startsWith("https://", ignoreCase = true)) {
                    return@withContext ApiResult(false, "Only HTTPS API endpoints are allowed", -1)
                }
                // GitHub returns different representations of the same URL
                // depending on Accept (for example README JSON vs rendered HTML).
                // ETags are representation-specific, so the header variant is
                // part of the cache key as well.
                val cacheVariant = extraHeaders.entries
                    .sortedBy { it.key.lowercase(Locale.US) }
                    .joinToString("&") { "${it.key.lowercase(Locale.US)}=${it.value}" }
                val cacheKey = "$method:${token.hashCode()}:$url:$cacheVariant"
                val cachedEtag = if (method == "GET") etagCache[cacheKey]?.let { (_, h) -> h["etag"] } else null
                
                val proxyEnabled = prefs.getBoolean("network_proxy_enabled", false)
                val proxyHost = prefs.getString("network_proxy_host", "") ?: ""
                val proxyPort = prefs.getInt("network_proxy_port", 8080)
                
                val connectionUrl = URL(url)
                val connRaw = if (proxyEnabled && proxyHost.isNotBlank()) {
                    val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(proxyHost, proxyPort))
                    connectionUrl.openConnection(proxy)
                } else {
                    connectionUrl.openConnection()
                }
                
                conn = (connRaw as HttpURLConnection).apply {
                    requestMethod = method
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "GsGit")
                    extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
                    if (cachedEtag != null) setRequestProperty("If-None-Match", cachedEtag)
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        OutputStreamWriter(outputStream).use { it.write(body) }
                    }
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val code = conn.responseCode
                val headers = responseHeaders(conn)

                if (code == CODE_NOT_MODIFIED && etagCache.containsKey(cacheKey)) {
                    val (cachedBody, cachedHeaders) = etagCache[cacheKey]!!
                    return@withContext ApiResult(true, cachedBody, CODE_NOT_MODIFIED, cachedHeaders)
                }

                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

                headers["x-ratelimit-remaining"]?.toIntOrNull()?.let { lastRateRemaining = it }
                headers["x-ratelimit-reset"]?.toLongOrNull()?.let { lastRateReset = it }

                if (code in 200..299 && method == "GET" && headers.containsKey("etag")) {
                    etagCache[cacheKey] = text to headers
                }

                if (code == 403 && rateLimitRetries > 0 && headers["x-ratelimit-remaining"] == "0") {
                    val resetEpoch = headers["x-ratelimit-reset"]?.toLongOrNull() ?: 0L
                    val waitSec = (resetEpoch * 1000 - System.currentTimeMillis()).coerceIn(1000, 60_000) / 1000
                    Log.w(TAG, "Rate limited, waiting ${waitSec}s for reset")
                    kotlinx.coroutines.delay(waitSec * 1000)
                    return@withContext request(
                        context, endpoint, method, body, extraHeaders, trackErrors,
                        rateLimitRetries - 1, backoffRetries, authToken,
                    )
                }

                if (code in 500..599 && backoffRetries > 0) {
                    val delayMs = (1000L * (4 - backoffRetries)).coerceIn(1000, 3000)
                    Log.w(TAG, "Server error $code, retrying in ${delayMs}ms ($backoffRetries left)")
                    kotlinx.coroutines.delay(delayMs)
                    return@withContext request(
                        context, endpoint, method, body, extraHeaders, trackErrors,
                        rateLimitRetries, backoffRetries - 1, authToken,
                    )
                }

                val result = if (code in 200..299) ApiResult(true, text, code, headers) else ApiResult(false, text, code, headers)
                if (!result.success && trackErrors) recordApiError(context, endpoint, method, result)
                result
            } catch (e: Exception) {
                if (backoffRetries > 0) {
                    val delayMs = (1000L * (4 - backoffRetries)).coerceIn(1000, 3000)
                    Log.w(TAG, "Network error; retrying in ${delayMs}ms ($backoffRetries left)")
                    kotlinx.coroutines.delay(delayMs)
                    return@withContext request(
                        context, endpoint, method, body, extraHeaders, trackErrors,
                        rateLimitRetries, backoffRetries - 1, authToken,
                    )
                }
                Log.e(TAG, "Request failed")
                val result = ApiResult(false, e.message ?: "Network error", -1)
                if (trackErrors) recordApiError(context, endpoint, method, result)
                result
            } finally {
                conn?.disconnect()
            }
        }

    private fun recordApiError(context: Context, endpoint: String, method: String, result: ApiResult) {
        GitHubAuth.recordApiError(context, endpoint, method, result)
    }

    internal fun repoPath(owner: String, repo: String, suffix: String = ""): String {
        val o = URLEncoder.encode(owner, "UTF-8")
        val r = URLEncoder.encode(repo, "UTF-8")
        return "/repos/$o/$r$suffix"
    }

    internal fun encPath(segment: String): String = URLEncoder.encode(segment, "UTF-8")

    private fun responseHeaders(conn: HttpURLConnection): Map<String, String> =
        conn.headerFields
            .mapNotNull { (key, values) ->
                val name = key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                name.lowercase(Locale.US) to values.orEmpty().joinToString(",")
            }
            .toMap()

    internal fun parseNextPage(headers: Map<String, String>): Int? {
        val link = headers["link"] ?: return null
        val match = Regex("""<[^>]*[?&]page=(\d+)[^>]*>;\s*rel="next"""").find(link)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    internal suspend fun requestBasic(endpoint: String, method: String, body: String?, username: String, password: String): ApiResult =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                updateApiUrl(App.instance)
                val prefs = App.instance.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                val proxyEnabled = prefs.getBoolean("network_proxy_enabled", false)
                val proxyHost = prefs.getString("network_proxy_host", "") ?: ""
                val proxyPort = prefs.getInt("network_proxy_port", 8080)
                
                val url = if (endpoint.startsWith("http")) endpoint else "${getApiUrl()}$endpoint"
                val auth = android.util.Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
                
                val connectionUrl = URL(url)
                val connRaw = if (proxyEnabled && proxyHost.isNotBlank()) {
                    val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(proxyHost, proxyPort))
                    connectionUrl.openConnection(proxy)
                } else {
                    connectionUrl.openConnection()
                }
                
                conn = (connRaw as HttpURLConnection).apply {
                    requestMethod = method
                    instanceFollowRedirects = false
                    if (url.contains("/login/")) {
                        setRequestProperty("Accept", "application/json")
                    } else {
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                    setRequestProperty("User-Agent", "GsGit")
                    if (password.isNotBlank()) setRequestProperty("Authorization", "Basic $auth")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        OutputStreamWriter(outputStream).use { it.write(body) }
                    }
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code in 200..299) ApiResult(true, text, code) else ApiResult(false, text, code)
            } catch (e: Exception) {
                Log.e(TAG, "Basic request failed")
                ApiResult(false, e.message ?: "Network error", -1)
            } finally {
                conn?.disconnect()
            }
        }

    internal suspend fun graphql(context: Context, query: String, variables: JSONObject = JSONObject()): JSONObject? {
        val body = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }.toString()
        val r = request(context, "/graphql", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
        if (!r.success) return null
        return try {
            val root = JSONObject(r.body)
            if (root.has("errors")) {
                Log.e(TAG, "GraphQL response has errors")
                null
            } else root.optJSONObject("data")
        } catch (_: Exception) {
            Log.e(TAG, "GraphQL response parsing failed")
            null
        }
    }

    internal fun refQuery(branch: String?): String {
        val ref = branch?.takeIf { it.isNotBlank() } ?: return ""
        return "?ref=${URLEncoder.encode(ref, "UTF-8")}"
    }

    internal fun parseRepo(j: JSONObject) = GHRepo(
        name = j.optString("name"),
        fullName = j.optString("full_name"),
        description = j.optString("description", ""),
        language = j.optString("language", ""),
        stars = j.optInt("stargazers_count", 0),
        forks = j.optInt("forks_count", 0),
        isPrivate = j.optBoolean("private", false),
        isFork = j.optBoolean("fork", false),
        defaultBranch = j.optString("default_branch", "main"),
        updatedAt = j.optString("updated_at", ""),
        owner = j.optJSONObject("owner")?.optString("login") ?: "",
        htmlUrl = j.optString("html_url", ""),
        isArchived = j.optBoolean("archived", false),
        isTemplate = j.optBoolean("is_template", false),
        id = j.optLong("id", 0L),
        openIssues = j.optInt("open_issues_count", 0),
        permissions = j.optJSONObject("permissions")?.let { p ->
            GHPermissions(
                admin = p.optBoolean("admin", false),
                maintain = p.optBoolean("maintain", false),
                push = p.optBoolean("push", false),
                triage = p.optBoolean("triage", false),
                pull = p.optBoolean("pull", false)
            )
        }
    )

    data class ApiResult(
        val success: Boolean,
        val body: String,
        val code: Int,
        val headers: Map<String, String> = emptyMap(),
    )

    internal fun apiErrorMessage(result: ApiResult): String {
        val fallback = if (result.code > 0) "HTTP ${result.code}" else "Network error"
        if (result.body.isBlank()) return fallback
        return try {
            val json = JSONObject(result.body)
            val message = json.optString("message").takeIf { it.isNotBlank() }
            val errors = json.optJSONArray("errors")
            val details = if (errors != null) {
                (0 until errors.length()).mapNotNull { index ->
                    val item = errors.opt(index)
                    when (item) {
                        is JSONObject -> listOf(
                            item.optString("field"),
                            item.optString("code"),
                            item.optString("message")
                        ).filter { it.isNotBlank() && it != "null" }.joinToString(" ")
                        else -> item?.toString()
                    }?.takeIf { it.isNotBlank() && it != "null" }
                }.take(3).joinToString("; ")
            } else ""
            listOfNotNull(message, details.takeIf { it.isNotBlank() }).joinToString(": ").ifBlank { fallback }
        } catch (_: Exception) {
            result.body.trim().take(220).ifBlank { fallback }
        }
    }

}

data class GHApiErrorLogEntry(
    val timestamp: Long,
    val method: String,
    val endpoint: String,
    val statusCode: Int,
    val message: String,
    val body: String,
    val requestId: String,
    val rateRemaining: String,
)

/** True if the current user can push commits / edit content / run workflows. */
fun GHRepo.canWrite(): Boolean = permissions?.let { it.push || it.maintain || it.admin } == true

/** True if the current user has full admin rights (settings, webhooks, collaborators). */
fun GHRepo.canAdmin(): Boolean = permissions?.admin == true


data class GHUserLite(val login: String, val avatarUrl: String = "")

data class GHRepoCreateParams(
    val name: String, val description: String, val isPrivate: Boolean,
    val autoInit: Boolean = false, val gitignoreTemplate: String = "",
    val licenseTemplate: String = "", val hasIssues: Boolean = true,
    val hasProjects: Boolean = true, val hasWiki: Boolean = true,
    val templateOwner: String = "", val templateRepo: String = "",
    val includeAllBranches: Boolean = false
)
