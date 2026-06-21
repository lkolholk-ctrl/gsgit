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
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object GitHubManager {

    private const val TAG = "GH"
    @Volatile private var cachedApiUrl: String = "https://api.github.com"
    fun getApiUrl(): String = cachedApiUrl
    fun getWebUrl(): String {
        val api = getApiUrl()
        return if (api == "https://api.github.com") {
            "https://github.com"
        } else {
            api.replace("/api/v3", "").replace("/api", "")
        }
    }
    fun setApiUrl(context: Context, url: String) {
        val clean = url.trim().trimEnd('/')
        val resolved = clean.ifBlank { "https://api.github.com" }
        cachedApiUrl = resolved
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("custom_api_url", resolved)
            .apply()
    }
    internal fun updateApiUrl(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val custom = prefs.getString("custom_api_url", "") ?: ""
        cachedApiUrl = custom.ifBlank { "https://api.github.com" }
    }
    internal const val PREFS = "github_prefs"
    internal const val KEY_USER = "user_json"
    private const val CODE_NOT_MODIFIED = 304

    private val etagCache = mutableMapOf<String, Pair<String, Map<String, String>>>()
    @Volatile private var lastRateRemaining: Int = Int.MAX_VALUE
    @Volatile private var lastRateReset: Long = 0L

    fun getRateLimitRemaining(): Int = lastRateRemaining
    fun getRateLimitResetEpoch(): Long = lastRateReset
    fun isRateLimitLow(): Boolean = lastRateRemaining < 10
    fun clearEtagCache() { etagCache.clear() }

    data class TokenValidation(val valid: Boolean, val scopes: String, val login: String, val error: String)

    suspend fun validateToken(context: Context): TokenValidation {
        val token = getToken(context)
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
        var res = request(
            context = context,
            endpoint = "https://api.github.com/copilot_internal/v2/token",
            method = "GET",
            extraHeaders = mapOf(
                "User-Agent" to "GitHubCopilotChat/0.11.0",
                "Accept" to "application/json"
            ),
            trackErrors = false
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
                trackErrors = false
            )
        }
        if (res.success) {
            return JSONObject(res.body).optString("token", "")
        } else {
            throw java.io.IOException("Failed to get Copilot token: HTTP ${res.code}: ${res.body.trim()}")
        }
    }

    fun saveToken(context: Context, token: String) = GitHubAuth.saveToken(context, token)
    fun getToken(context: Context): String = GitHubAuth.getToken(context)
    fun isLoggedIn(context: Context): Boolean = GitHubAuth.isLoggedIn(context)
    fun logout(context: Context) = GitHubAuth.logout(context)
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
    ): ApiResult =
        withContext(Dispatchers.IO) {
            updateApiUrl(context)
            var conn: HttpURLConnection? = null
            try {
                val token = getToken(context)
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
                val cacheKey = "$method:$url"
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
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "GlassFiles")
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
                    return@withContext request(context, endpoint, method, body, extraHeaders, trackErrors, rateLimitRetries - 1)
                }

                if (code in 500..599 && backoffRetries > 0) {
                    val delayMs = (1000L * (4 - backoffRetries)).coerceIn(1000, 3000)
                    Log.w(TAG, "Server error $code, retrying in ${delayMs}ms ($backoffRetries left)")
                    kotlinx.coroutines.delay(delayMs)
                    return@withContext request(context, endpoint, method, body, extraHeaders, trackErrors, rateLimitRetries, backoffRetries - 1)
                }

                val result = if (code in 200..299) ApiResult(true, text, code, headers) else ApiResult(false, text, code, headers)
                if (!result.success && trackErrors) recordApiError(context, endpoint, method, result)
                result
            } catch (e: Exception) {
                if (backoffRetries > 0) {
                    val delayMs = (1000L * (4 - backoffRetries)).coerceIn(1000, 3000)
                    Log.w(TAG, "Network error: ${e.message}, retrying in ${delayMs}ms ($backoffRetries left)")
                    kotlinx.coroutines.delay(delayMs)
                    return@withContext request(context, endpoint, method, body, extraHeaders, trackErrors, rateLimitRetries, backoffRetries - 1)
                }
                Log.e(TAG, "Request error: ${e.message}")
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
                    if (url.contains("/login/")) {
                        setRequestProperty("Accept", "application/json")
                    } else {
                        setRequestProperty("Accept", "application/vnd.github+json")
                    }
                    setRequestProperty("User-Agent", "GlassFiles")
                    setRequestProperty("Authorization", "Basic $auth")
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
                Log.e(TAG, "Basic request error: ${e.message}")
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
                val errs = root.optJSONArray("errors")
                val msg = (0 until (errs?.length() ?: 0)).mapNotNull { errs?.optJSONObject(it)?.optString("message") }.joinToString("; ")
                Log.e(TAG, "GraphQL errors: $msg")
                null
            } else root.optJSONObject("data")
        } catch (e: Exception) {
            Log.e(TAG, "GraphQL parse error: ${e.message}")
            null
        }
    }

    suspend fun searchRepos(context: Context, query: String): List<GHRepo> {
        val r = request(context, "/search/repositories?q=$query&sort=stars&per_page=20")
        if (!r.success) return emptyList()
        return try {
            val arr = JSONObject(r.body).getJSONArray("items")
            (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    internal fun refQuery(branch: String?): String {
        val ref = branch?.takeIf { it.isNotBlank() } ?: return ""
        return "?ref=${URLEncoder.encode(ref, "UTF-8")}"
    }

    suspend fun isStarred(context: Context, owner: String, repo: String): Boolean =
        request(context, "/user/starred/$owner/$repo").code == 204

    suspend fun getQuickGlanceStats(context: Context): QuickGlanceStats {
        var prsCount = 0
        var issuesCount = 0
        val issuesRes = request(context, "/issues?filter=assigned&state=open")
        if (issuesRes.success) {
            try {
                val arr = JSONArray(issuesRes.body)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    if (item.has("pull_request")) {
                        prsCount++
                    } else {
                        issuesCount++
                    }
                }
            } catch (e: Exception) {}
        }

        var failedRuns = 0
        val reposRes = request(context, "/user/repos?sort=updated&per_page=5")
        if (reposRes.success) {
            try {
                val reposArr = JSONArray(reposRes.body)
                for (i in 0 until reposArr.length()) {
                    val repo = reposArr.getJSONObject(i)
                    val ownerObj = repo.optJSONObject("owner")
                    val owner = ownerObj?.optString("login") ?: ""
                    val name = repo.optString("name", "")
                    if (owner.isNotBlank() && name.isNotBlank()) {
                        val runsRes = request(context, "/repos/$owner/$name/actions/runs?per_page=5")
                        if (runsRes.success) {
                            val runsObj = JSONObject(runsRes.body)
                            val runsArr = runsObj.optJSONArray("workflow_runs")
                            if (runsArr != null) {
                                for (j in 0 until runsArr.length()) {
                                    val run = runsArr.getJSONObject(j)
                                    val conclusion = run.optString("conclusion", "")
                                    val createdAtStr = run.optString("created_at", "")
                                    if (conclusion == "failure" && createdAtStr.isNotBlank()) {
                                        try {
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                            val date = sdf.parse(createdAtStr)
                                            if (date != null && System.currentTimeMillis() - date.time < 24 * 60 * 60 * 1000) {
                                                failedRuns++
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        return QuickGlanceStats(
            assignedPrsCount = prsCount,
            openIssuesCount = issuesCount,
            failedBuildsCount = failedRuns,
            loading = false
        )
    }

    // ═══════════════════════════════════
    // Autolinks
    // ═══════════════════════════════════

    suspend fun getAutolinks(context: Context, owner: String, repo: String): List<GHAutolink> {
        val r = request(context, "/repos/$owner/$repo/autolinks", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                GHAutolink(j.optLong("id", 0), j.optString("key_prefix", ""), j.optString("url_template", ""), j.optBoolean("is_alphanumeric", true))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun createAutolink(context: Context, owner: String, repo: String, keyPrefix: String, urlTemplate: String, isAlphanumeric: Boolean = true): Boolean {
        val body = JSONObject().apply {
            put("key_prefix", keyPrefix)
            put("url_template", urlTemplate)
            put("is_alphanumeric", isAlphanumeric)
        }.toString()
        val r = request(context, "/repos/$owner/$repo/autolinks", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
        return r.success
    }

    suspend fun deleteAutolink(context: Context, owner: String, repo: String, autolinkId: Long): Boolean {
        return request(context, "/repos/$owner/$repo/autolinks/$autolinkId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }
    }

    // ═══════════════════════════════════
    // Codespaces
    // ═══════════════════════════════════

    suspend fun getCodespaces(context: Context, page: Int = 1): List<GHCodespace> {
        val r = request(context, "/user/codespaces?per_page=30&page=$page")
        if (!r.success) return emptyList()
        return try {
            val arr = JSONObject(r.body).optJSONArray("codespaces") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                GHCodespace(
                    name = j.optString("name", ""),
                    displayName = j.optString("display_name", ""),
                    state = j.optString("state", ""),
                    owner = j.optJSONObject("owner")?.optString("login") ?: "",
                    repo = j.optJSONObject("repository")?.optString("full_name") ?: "",
                    branch = j.optJSONObject("git_status")?.optString("ref", "") ?: "",
                    machine = j.optString("machine_display_name", ""),
                    createdAt = j.optString("created_at", ""),
                    lastUsedAt = j.optString("last_used_at", ""),
                    idleTimeoutMinutes = j.optInt("idle_timeout_minutes", 30),
                    url = j.optString("web_url", "")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteCodespace(context: Context, codespaceName: String): Boolean {
        return request(context, "/user/codespaces/${URLEncoder.encode(codespaceName, "UTF-8")}", "DELETE").let { it.code == 202 || it.code == 204 || it.success }
    }

    // ═══════════════════════════════════
    // Repo LFS
    // ═══════════════════════════════════

    suspend fun enableRepoLfs(context: Context, owner: String, repo: String): Boolean {
        val body = JSONObject().apply { put("enabled", true) }.toString()
        val r = request(context, "/repos/$owner/$repo/lfs", "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
        return r.success
    }

    suspend fun disableRepoLfs(context: Context, owner: String, repo: String): Boolean {
        return request(context, "/repos/$owner/$repo/lfs", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }
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


data class QuickGlanceStats(
    val assignedPrsCount: Int = 0,
    val openIssuesCount: Int = 0,
    val failedBuildsCount: Int = 0,
    val loading: Boolean = true
)

data class GHUserLite(val login: String, val avatarUrl: String = "")

data class GHAutolink(
    val id: Long,
    val keyPrefix: String,
    val urlTemplate: String,
    val isAlphanumeric: Boolean
)

data class GHCodespace(
    val name: String,
    val displayName: String,
    val state: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val machine: String,
    val createdAt: String,
    val lastUsedAt: String,
    val idleTimeoutMinutes: Int,
    val url: String
)

data class GHRepoCreateParams(
    val name: String, val description: String, val isPrivate: Boolean,
    val autoInit: Boolean = false, val gitignoreTemplate: String = "",
    val licenseTemplate: String = "", val hasIssues: Boolean = true,
    val hasProjects: Boolean = true, val hasWiki: Boolean = true,
    val templateOwner: String = "", val templateRepo: String = "",
    val includeAllBranches: Boolean = false
)

