package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.App
import gs.git.vps.data.github.model.GHActionResult
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHLicenseDetail
import gs.git.vps.data.github.model.GHPermissions
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoEvent
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

    private suspend fun requestBasic(endpoint: String, method: String, body: String?, username: String, password: String): ApiResult =
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

    private fun parseOAuthTokenInfo(j: JSONObject): GHOAuthTokenInfo {
        val app = j.optJSONObject("app")
        val scopes = j.optJSONArray("scopes")?.let { arr ->
            (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
        } ?: emptyList()
        return GHOAuthTokenInfo(
            id = j.optLong("id"),
            url = j.optString("url", ""),
            appName = app?.optString("name", "") ?: "",
            appUrl = app?.optString("url", "") ?: "",
            clientId = app?.optString("client_id", "") ?: "",
            tokenLastEight = j.optString("token_last_eight", ""),
            note = j.optString("note", ""),
            noteUrl = j.optString("note_url", ""),
            createdAt = j.optString("created_at", ""),
            updatedAt = j.optString("updated_at", ""),
            scopes = scopes,
            fingerprint = j.optString("fingerprint", ""),
            token = j.optString("token", "")
        )
    }

    suspend fun isStarred(context: Context, owner: String, repo: String): Boolean =
        request(context, "/user/starred/$owner/$repo").code == 204

    suspend fun getReadme(context: Context, owner: String, repo: String): String {
        val r = request(context, "/repos/$owner/$repo/readme")
        if (!r.success) return ""
        return try {
            val j = JSONObject(r.body)
            val content = j.optString("content", "")
            String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
        } catch (e: Exception) { "" }
    }

    suspend fun getGitHubMeta(context: Context): GHMeta? {
        val r = request(context, "/meta", trackErrors = false)
        if (!r.success) return null
        return try {
            val j = JSONObject(r.body)
            GHMeta(
                verifiablePasswordAuthentication = j.optBoolean("verifiable_password_authentication"),
                sshKeys = j.optJSONArray("ssh_keys")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                sshKeyFingerprints = j.optJSONArray("ssh_key_fingerprints")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                hooks = j.optJSONArray("hooks")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                web = j.optJSONArray("web")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                api = j.optJSONArray("api")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                git = j.optJSONArray("git")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                packages = j.optJSONArray("packages")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                pages = j.optJSONArray("pages")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                importer = j.optJSONArray("importer")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            )
        } catch (e: Exception) { null }
    }

    suspend fun renderMarkdown(context: Context, text: String, mode: String = "markdown", contextRepo: String = ""): String {
        val body = JSONObject().apply {
            put("text", text)
            put("mode", mode)
            if (contextRepo.isNotBlank()) put("context", contextRepo)
        }.toString()
        val r = request(context, "/markdown", "POST", body, trackErrors = false)
        return if (r.success) r.body else ""
    }

    suspend fun getLanguages(context: Context, owner: String, repo: String): Map<String, Long> {
        val r = request(context, "/repos/$owner/$repo/languages")
        if (!r.success) return emptyMap()
        return try {
            val j = JSONObject(r.body)
            val map = mutableMapOf<String, Long>()
            j.keys().forEach { key -> map[key] = j.optLong(key) }
            map
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun getEmojis(context: Context): Map<String, String> {
        val r = request(context, "/emojis", trackErrors = false)
        if (!r.success) return emptyMap()
        return try {
            val j = JSONObject(r.body)
            val map = mutableMapOf<String, String>()
            j.keys().forEach { key -> map[key] = j.optString(key) }
            map
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun getGitignoreTemplates(context: Context): List<String> {
        val r = request(context, "/gitignore/templates", trackErrors = false)
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).mapNotNull { arr.optString(it)?.takeIf { s -> s.isNotBlank() } }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getGitignoreTemplate(context: Context, name: String): String? {
        val r = request(context, "/gitignore/templates/${encPath(name)}", trackErrors = false)
        if (!r.success) return null
        return try { JSONObject(r.body).optString("source") } catch (e: Exception) { null }
    }

    suspend fun getLicenses(context: Context): List<GHLicense> {
        val r = request(context, "/licenses", trackErrors = false)
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                GHLicense(j.optString("key"), j.optString("name"), j.optString("spdx_id"), j.optString("url"), j.optBoolean("featured"))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getLicense(context: Context, key: String): GHLicenseDetail? {
        val r = request(context, "/licenses/${encPath(key)}", trackErrors = false)
        if (!r.success) return null
        return try {
            val j = JSONObject(r.body)
            GHLicenseDetail(j.optString("key"), j.optString("name"), j.optString("spdx_id"),
                j.optString("description", ""), j.optString("body", ""), j.optString("html_url", ""),
                j.optBoolean("featured"))
        } catch (e: Exception) { null }
    }

    suspend fun getContributors(context: Context, owner: String, repo: String): List<GHContributor> {
        val r = request(context, "/repos/$owner/$repo/contributors?per_page=30")
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                GHContributor(j.optString("login"), j.optString("avatar_url", ""), j.optInt("contributions", 0))
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUserReceivedEvents(context: Context, username: String, page: Int = 1): List<GHRepoEvent> {
        val r = request(context, "/users/$username/received_events?per_page=30&page=$page")
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                val payload = j.optJSONObject("payload")
                GHRepoEvent(
                    id = j.optString("id", ""),
                    type = j.optString("type", ""),
                    actor = j.optJSONObject("actor")?.optString("login") ?: "",
                    createdAt = j.optString("created_at", ""),
                    action = payload?.optString("action", "") ?: "",
                    ref = payload?.optString("ref", "") ?: "",
                    refType = payload?.optString("ref_type", "") ?: "",
                    size = payload?.optInt("size", 0) ?: 0,
                    repoName = j.optJSONObject("repo")?.optString("name") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUserPublicEvents(context: Context, username: String, page: Int = 1): List<GHRepoEvent> {
        val r = request(context, "/users/$username/events/public?per_page=100&page=$page")
        if (!r.success) return emptyList()
        return try {
            val arr = JSONArray(r.body)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                val payload = j.optJSONObject("payload")
                GHRepoEvent(
                    id = j.optString("id", ""),
                    type = j.optString("type", ""),
                    actor = j.optJSONObject("actor")?.optString("login") ?: "",
                    createdAt = j.optString("created_at", ""),
                    action = payload?.optString("action", "") ?: "",
                    ref = payload?.optString("ref", "") ?: "",
                    refType = payload?.optString("ref_type", "") ?: "",
                    size = payload?.optInt("size", 0) ?: 0,
                    repoName = j.optJSONObject("repo")?.optString("name") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

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

    suspend fun checkOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): GHOAuthTokenInfo? {
        val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
        val r = requestBasic("/applications/${clientId.trim()}/token", "POST", body, clientId.trim(), clientSecret)
        if (!r.success) return null
        return try { parseOAuthTokenInfo(JSONObject(r.body)) } catch (e: Exception) { null }
    }

    suspend fun resetOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): GHOAuthTokenInfo? {
        val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
        val r = requestBasic("/applications/${clientId.trim()}/token", "PATCH", body, clientId.trim(), clientSecret)
        if (!r.success) return null
        return try { parseOAuthTokenInfo(JSONObject(r.body)) } catch (e: Exception) { null }
    }

    suspend fun deleteOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): Boolean {
        val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
        return requestBasic("/applications/${clientId.trim()}/token", "DELETE", body, clientId.trim(), clientSecret).let { it.code == 204 || it.success }
    }

    suspend fun deleteOAuthAppGrant(clientId: String, clientSecret: String, accessToken: String): Boolean {
        val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
        return requestBasic("/applications/${clientId.trim()}/grant", "DELETE", body, clientId.trim(), clientSecret).let { it.code == 204 || it.success }
    }

    suspend fun initiateDeviceFlow(clientId: String): GHDeviceCode? {
        val body = JSONObject().apply {
            put("client_id", clientId)
            put("scope", "read:user,repo,write:repo_hook,admin:repo_hook,copilot")
        }.toString()
        val webUrl = getWebUrl()
        val r = requestBasic("$webUrl/login/device/code", "POST", body, clientId, "")
        if (!r.success) return null
        return try {
            val j = JSONObject(r.body)
            GHDeviceCode(
                deviceCode = j.optString("device_code"),
                userCode = j.optString("user_code"),
                verificationUri = j.optString("verification_uri"),
                expiresIn = j.optInt("expires_in"),
                interval = j.optInt("interval", 5)
            )
        } catch (e: Exception) { null }
    }

    suspend fun pollDeviceToken(clientId: String, deviceCode: String): GHDeviceTokenResult {
        val body = JSONObject().apply {
            put("client_id", clientId)
            put("device_code", deviceCode)
            put("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
        }.toString()
        val webUrl = getWebUrl()
        val r = requestBasic("$webUrl/login/oauth/access_token", "POST", body, clientId, "")
        return try {
            val j = JSONObject(r.body)
            val error = j.optString("error", "")
            if (error.isBlank()) {
                GHDeviceTokenResult(token = j.optString("access_token"), error = null)
            } else {
                GHDeviceTokenResult(token = null, error = error)
            }
        } catch (e: Exception) {
            GHDeviceTokenResult(token = null, error = "parse_error")
        }
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

    // ═══════════════════════════════════
    // GitHub Apps / Installations
    // ═══════════════════════════════════

    suspend fun getAppInstallations(context: Context, page: Int = 1, perPage: Int = 30): GHAppInstallationsPage {
        val r = request(
            context,
            "/user/installations?per_page=$perPage&page=$page",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json")
        )
        if (!r.success) return GHAppInstallationsPage(error = apiErrorMessage(r), code = r.code)
        return try {
            val root = JSONObject(r.body)
            val arr = root.optJSONArray("installations") ?: JSONArray()
            GHAppInstallationsPage(
                installations = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseAppInstallation) },
                totalCount = root.optInt("total_count", arr.length()),
                code = r.code
            )
        } catch (e: Exception) {
            GHAppInstallationsPage(error = e.message ?: "Parse error", code = r.code)
        }
    }

    suspend fun getAppInstallationRepositories(context: Context, installationId: Long, page: Int = 1, perPage: Int = 30): GHAppInstallationReposPage {
        val r = request(
            context,
            "/user/installations/$installationId/repositories?per_page=$perPage&page=$page",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json")
        )
        if (!r.success) return GHAppInstallationReposPage(error = apiErrorMessage(r), code = r.code)
        return try {
            val root = JSONObject(r.body)
            val arr = root.optJSONArray("repositories") ?: JSONArray()
            GHAppInstallationReposPage(
                repositories = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseRepo) },
                totalCount = root.optInt("total_count", arr.length()),
                code = r.code
            )
        } catch (e: Exception) {
            GHAppInstallationReposPage(error = e.message ?: "Parse error", code = r.code)
        }
    }

    suspend fun addRepositoryToAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
        val r = request(
            context,
            "/user/installations/$installationId/repositories/$repositoryId",
            "PUT",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json")
        )
        return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository added" else apiErrorMessage(r))
    }

    suspend fun removeRepositoryFromAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
        val r = request(
            context,
            "/user/installations/$installationId/repositories/$repositoryId",
            "DELETE",
            extraHeaders = mapOf("Accept" to "application/vnd.github+json")
        )
        return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository removed" else apiErrorMessage(r))
    }


    suspend fun getRateLimitSummaryNative(context: Context): String {
        val r = request(context, "/rate_limit")
        if (!r.success || r.body.isBlank()) return "Unavailable"
        return try {
            val core = JSONObject(r.body).getJSONObject("resources").getJSONObject("core")
            val remaining = core.optInt("remaining")
            val limit = core.optInt("limit")
            val reset = core.optLong("reset")
            "$remaining / $limit · reset $reset"
        } catch (_: Exception) { "Unavailable" }
    }

    suspend fun getRateLimitGraphQL(context: Context): GHRateLimitGraphQL? {
        val data = graphql(context, "{ rateLimit { limit cost remaining resetAt nodeCount } }")
        if (data == null) return null
        return try {
            val rl = data.getJSONObject("rateLimit")
            GHRateLimitGraphQL(
                limit = rl.optInt("limit"),
                cost = rl.optInt("cost"),
                remaining = rl.optInt("remaining"),
                resetAt = rl.optString("resetAt", ""),
                nodeCount = rl.optInt("nodeCount")
            )
        } catch (e: Exception) { null }
    }

    suspend fun getGitHubStatus(context: Context): GHStatusSummary? =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL("https://www.githubstatus.com/api/v2/summary.json").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "GlassFiles")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    return@withContext null
                }
                val bodyStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                
                val j = JSONObject(bodyStr)
                val statusObj = j.getJSONObject("status")
                val desc = statusObj.getString("description")
                val indicator = statusObj.getString("indicator")
                
                val componentsArr = j.getJSONArray("components")
                val componentsList = mutableListOf<GHStatusComponent>()
                for (i in 0 until componentsArr.length()) {
                    val compObj = componentsArr.getJSONObject(i)
                    val name = compObj.getString("name")
                    val status = compObj.getString("status")
                    if (name in listOf("Git Operations", "API Requests", "GitHub Actions", "GitHub Pages", "Issues", "Pull Requests", "Copilot")) {
                        componentsList.add(GHStatusComponent(name, status))
                    }
                }
                
                GHStatusSummary(desc, indicator, componentsList)
            } catch (e: Exception) {
                Log.e(TAG, "Get GitHub status: ${e.message}")
                null
            }
        }

    suspend fun runApiDiagnostics(
        context: Context,
        owner: String = "",
        repo: String = "",
        org: String = "",
        enterprise: String = "",
    ): GHApiDiagnostics {
        val checks = mutableListOf<GHApiDiagnosticCheck>()
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        val cleanOrg = org.trim()
        val cleanEnterprise = enterprise.trim()

        fun addResult(
            title: String,
            endpoint: String,
            result: ApiResult,
            successMessage: String,
            hint: String,
        ) {
            checks += GHApiDiagnosticCheck(
                title = title,
                endpoint = endpoint,
                statusCode = result.code,
                status = diagnosticStatus(result),
                message = if (result.success) successMessage else apiErrorMessage(result),
                hint = hint,
            )
        }

        fun addSkip(title: String, endpoint: String, hint: String) {
            checks += GHApiDiagnosticCheck(
                title = title,
                endpoint = endpoint,
                statusCode = 0,
                status = "skip",
                message = "not checked",
                hint = hint,
            )
        }

        val userResult = request(context, "/user", trackErrors = false)
        val login = parseLogin(userResult.body)
        addResult(
            title = "Token identity",
            endpoint = "/user",
            result = userResult,
            successMessage = if (login.isBlank()) "authenticated" else "authenticated as @$login",
            hint = "401 means token is missing, expired, or revoked.",
        )

        val rateResult = request(context, "/rate_limit", trackErrors = false)
        val rate = parseRateLimitSummary(rateResult.body)
        addResult(
            title = "Rate limit",
            endpoint = "/rate_limit",
            result = rateResult,
            successMessage = "core ${rate.coreRemaining}/${rate.coreLimit}, search ${rate.searchRemaining}/${rate.searchLimit}",
            hint = "403 with zero remaining means the token has to wait for reset.",
        )

        addResult(
            title = "Repository list",
            endpoint = "/user/repos?type=all&per_page=1",
            result = request(context, "/user/repos?type=all&per_page=1", trackErrors = false),
            successMessage = "repository list is readable",
            hint = "Requires repository access on fine-grained tokens.",
        )
        addResult(
            title = "Organizations",
            endpoint = "/user/orgs?per_page=1",
            result = request(context, "/user/orgs?per_page=1", trackErrors = false),
            successMessage = "organization list is readable",
            hint = "Some organizations may be hidden by SSO or token restrictions.",
        )

        if (cleanOwner.isNotBlank() && cleanRepo.isNotBlank()) {
            val encodedOwner = URLEncoder.encode(cleanOwner, "UTF-8")
            val encodedRepo = URLEncoder.encode(cleanRepo, "UTF-8")
            addResult(
                title = "Repository access",
                endpoint = "/repos/$cleanOwner/$cleanRepo",
                result = request(context, "/repos/$encodedOwner/$encodedRepo", trackErrors = false),
                successMessage = "repository metadata is readable",
                hint = "404 can mean the repo is private or the token has no repository permission.",
            )
            addResult(
                title = "Actions workflows",
                endpoint = "/repos/$cleanOwner/$cleanRepo/actions/workflows?per_page=1",
                result = request(context, "/repos/$encodedOwner/$encodedRepo/actions/workflows?per_page=1", trackErrors = false),
                successMessage = "Actions workflows are readable",
                hint = "Requires Actions access for the selected repository.",
            )
            addResult(
                title = "Branches",
                endpoint = "/repos/$cleanOwner/$cleanRepo/branches?per_page=1",
                result = request(context, "/repos/$encodedOwner/$encodedRepo/branches?per_page=1", trackErrors = false),
                successMessage = "branches are readable",
                hint = "Branch reads should work for any visible repository.",
            )
            if (login.isNotBlank()) {
                addResult(
                    title = "Your repo permission",
                    endpoint = "/repos/$cleanOwner/$cleanRepo/collaborators/$login/permission",
                    result = request(context, "/repos/$encodedOwner/$encodedRepo/collaborators/${URLEncoder.encode(login, "UTF-8")}/permission", trackErrors = false),
                    successMessage = "permission endpoint is readable",
                    hint = "This helps explain why write/admin buttons are disabled.",
                )
            } else {
                addSkip("Your repo permission", "/repos/$cleanOwner/$cleanRepo/collaborators/{login}/permission", "Login was not available from /user.")
            }
        } else {
            addSkip("Repository access", "/repos/{owner}/{repo}", "Enter owner and repo to check repo-specific permissions.")
            addSkip("Actions workflows", "/repos/{owner}/{repo}/actions/workflows", "Enter owner and repo to check Actions visibility.")
            addSkip("Your repo permission", "/repos/{owner}/{repo}/collaborators/{login}/permission", "Enter owner and repo to check the current token permission.")
        }

        if (cleanOrg.isNotBlank()) {
            val encodedOrg = URLEncoder.encode(cleanOrg, "UTF-8")
            addResult(
                title = "Organization access",
                endpoint = "/orgs/$cleanOrg",
                result = request(context, "/orgs/$encodedOrg", trackErrors = false),
                successMessage = "organization metadata is readable",
                hint = "404/403 can mean private org membership, SSO, or missing org permission.",
            )
            addResult(
                title = "Organization audit log",
                endpoint = "/orgs/$cleanOrg/audit-log?per_page=1",
                result = request(context, "/orgs/$encodedOrg/audit-log?per_page=1", trackErrors = false),
                successMessage = "audit log is readable",
                hint = "Usually requires org owner/admin permissions.",
            )
        } else {
            addSkip("Organization access", "/orgs/{org}", "Enter org login to check organization visibility.")
            addSkip("Organization audit log", "/orgs/{org}/audit-log", "Enter org login to check admin-only audit log access.")
        }

        if (cleanEnterprise.isNotBlank()) {
            val encodedEnterprise = URLEncoder.encode(cleanEnterprise, "UTF-8")
            addResult(
                title = "Enterprise runners",
                endpoint = "/enterprises/$cleanEnterprise/actions/runners?per_page=1",
                result = request(context, "/enterprises/$encodedEnterprise/actions/runners?per_page=1", trackErrors = false),
                successMessage = "enterprise runners are readable",
                hint = "Requires enterprise owner/admin permissions.",
            )
        } else {
            addSkip("Enterprise runners", "/enterprises/{enterprise}/actions/runners", "Enter enterprise slug to check enterprise admin access.")
        }

        return GHApiDiagnostics(
            generatedAt = System.currentTimeMillis(),
            scopes = userResult.headers["x-oauth-scopes"].orEmpty(),
            acceptedScopes = userResult.headers["x-accepted-oauth-scopes"].orEmpty(),
            rate = rate,
            checks = checks,
        )
    }

    private fun parseLogin(body: String): String =
        try {
            JSONObject(body).optString("login", "")
        } catch (_: Exception) {
            ""
        }

    private fun diagnosticStatus(result: ApiResult): String =
        when {
            result.success -> "ok"
            result.code == 403 -> "warn"
            result.code == 404 -> "warn"
            result.code == 0 -> "skip"
            else -> "fail"
        }

    private fun parseRateLimitSummary(body: String): GHApiRateSummary =
        try {
            val root = JSONObject(body)
            val resources = root.optJSONObject("resources")
            val core = resources?.optJSONObject("core")
            val search = resources?.optJSONObject("search")
            val graphql = resources?.optJSONObject("graphql")
            GHApiRateSummary(
                coreLimit = core?.optInt("limit") ?: 0,
                coreRemaining = core?.optInt("remaining") ?: 0,
                searchLimit = search?.optInt("limit") ?: 0,
                searchRemaining = search?.optInt("remaining") ?: 0,
                graphqlLimit = graphql?.optInt("limit") ?: 0,
                graphqlRemaining = graphql?.optInt("remaining") ?: 0,
                resetEpoch = core?.optLong("reset") ?: 0L,
            )
        } catch (_: Exception) {
            GHApiRateSummary()
        }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() && it != "null" } }
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

    private fun parseAppInstallation(j: JSONObject): GHAppInstallation {
        val account = j.optJSONObject("account")
        val suspendedBy = j.optJSONObject("suspended_by")
        return GHAppInstallation(
            id = j.optLong("id"),
            appId = j.optLong("app_id"),
            appSlug = j.optString("app_slug", ""),
            targetId = j.optLong("target_id"),
            targetType = j.optString("target_type", ""),
            targetLogin = account?.optString("login") ?: "",
            targetAvatarUrl = account?.optString("avatar_url") ?: "",
            repositorySelection = j.optString("repository_selection", ""),
            permissions = parseStringMap(j.optJSONObject("permissions")),
            events = parseStringArray(j.optJSONArray("events")),
            singleFileName = j.optString("single_file_name", ""),
            singleFilePaths = parseStringArray(j.optJSONArray("single_file_paths")),
            htmlUrl = j.optString("html_url", ""),
            createdAt = j.optString("created_at", ""),
            updatedAt = j.optString("updated_at", ""),
            suspendedAt = j.optString("suspended_at", ""),
            suspendedBy = suspendedBy?.optString("login") ?: ""
        )
    }

    private fun parseStringMap(j: JSONObject?): List<Pair<String, String>> {
        if (j == null) return emptyList()
        return j.keys().asSequence()
            .mapNotNull { key -> key.takeIf { it.isNotBlank() }?.let { it to j.optString(it, "") } }
            .sortedBy { it.first }
            .toList()
    }

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

data class GHApiDiagnostics(
    val generatedAt: Long,
    val scopes: String,
    val acceptedScopes: String,
    val rate: GHApiRateSummary,
    val checks: List<GHApiDiagnosticCheck>,
)

data class GHApiRateSummary(
    val coreLimit: Int = 0,
    val coreRemaining: Int = 0,
    val searchLimit: Int = 0,
    val searchRemaining: Int = 0,
    val graphqlLimit: Int = 0,
    val graphqlRemaining: Int = 0,
    val resetEpoch: Long = 0L,
)

data class GHRateLimitGraphQL(
    val limit: Int,
    val cost: Int,
    val remaining: Int,
    val resetAt: String,
    val nodeCount: Int
)

data class GHApiDiagnosticCheck(
    val title: String,
    val endpoint: String,
    val statusCode: Int,
    val status: String,
    val message: String,
    val hint: String,
)

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


data class GHAppInstallationsPage(
    val installations: List<GHAppInstallation> = emptyList(),
    val totalCount: Int = 0,
    val error: String = "",
    val code: Int = 0
)

data class GHAppInstallationReposPage(
    val repositories: List<GHRepo> = emptyList(),
    val totalCount: Int = 0,
    val error: String = "",
    val code: Int = 0
)

data class GHAppInstallation(
    val id: Long,
    val appId: Long,
    val appSlug: String,
    val targetId: Long,
    val targetType: String,
    val targetLogin: String,
    val targetAvatarUrl: String,
    val repositorySelection: String,
    val permissions: List<Pair<String, String>>,
    val events: List<String>,
    val singleFileName: String,
    val singleFilePaths: List<String>,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val suspendedAt: String,
    val suspendedBy: String
)

data class GHContributor(val login: String, val avatarUrl: String, val contributions: Int)

data class QuickGlanceStats(
    val assignedPrsCount: Int = 0,
    val openIssuesCount: Int = 0,
    val failedBuildsCount: Int = 0,
    val loading: Boolean = true
)

data class GHOAuthTokenInfo(
    val id: Long,
    val url: String,
    val appName: String,
    val appUrl: String,
    val clientId: String,
    val tokenLastEight: String,
    val note: String,
    val noteUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val scopes: List<String>,
    val fingerprint: String,
    val token: String = ""
)


data class GHDeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int
)

data class GHDeviceTokenResult(
    val token: String?,
    val error: String?
)


data class GHUserLite(val login: String, val avatarUrl: String = "")

data class GHMeta(
    val verifiablePasswordAuthentication: Boolean,
    val sshKeys: List<String>,
    val sshKeyFingerprints: List<String>,
    val hooks: List<String>,
    val web: List<String>,
    val api: List<String>,
    val git: List<String>,
    val packages: List<String>,
    val pages: List<String>,
    val importer: List<String>,
)

data class GHLicense(
    val key: String,
    val name: String,
    val spdxId: String,
    val url: String,
    val featured: Boolean
)


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

data class GHStatusComponent(
    val name: String,
    val status: String
)

data class GHStatusSummary(
    val description: String,
    val indicator: String,
    val components: List<GHStatusComponent>
)
