package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.GitHubManager.ApiResult
import gs.git.vps.data.github.model.GHApiDiagnosticCheck
import gs.git.vps.data.github.model.GHApiDiagnostics
import gs.git.vps.data.github.model.GHApiRateSummary
import gs.git.vps.data.github.model.GHRateLimitGraphQL
import gs.git.vps.data.github.model.GHStatusComponent
import gs.git.vps.data.github.model.GHStatusSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Домен Diagnostics слоя GitHub API: rate-limit (REST + GraphQL), статус github.com и комплексная
 * API-диагностика токена/доступов. Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Сеть — через ядро `request()`/`graphql()`. Сигнатуры вызовов не менялись.
 *
 * Прямой `openConnection()` в getGitHubStatus — законное исключение: githubstatus.com это внешний
 * сервис (не GitHub API), текстовое ядро `request()` его не обслуживает.
 */

private const val DIAG_TAG = "GH"

internal suspend fun GitHubManager.pingApi(context: Context): Boolean =
    request(context, "/zen", trackErrors = false, backoffRetries = 0).success

internal suspend fun GitHubManager.getRateLimitSummaryNative(context: Context): String {
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

internal suspend fun GitHubManager.getRateLimitGraphQL(context: Context): GHRateLimitGraphQL? {
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

internal suspend fun GitHubManager.getGitHubStatus(context: Context): GHStatusSummary? =
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
            Log.e(DIAG_TAG, "Get GitHub status: ${e.message}")
            null
        }
    }

internal suspend fun GitHubManager.runApiDiagnostics(
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

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

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
