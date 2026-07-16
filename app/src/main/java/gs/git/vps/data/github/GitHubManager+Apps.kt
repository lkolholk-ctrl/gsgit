package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHActionResult
import gs.git.vps.data.github.model.GHAppInstallation
import gs.git.vps.data.github.model.GHAppInstallationReposPage
import gs.git.vps.data.github.model.GHAppInstallationsPage
import gs.git.vps.data.github.model.GHAppMetadata
import gs.git.vps.data.github.model.GHAppMetadataResult
import gs.git.vps.data.github.model.GHObservedApp
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoAppsEvidence
import gs.git.vps.data.github.model.GHWorkflowAppEvidence
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Apps слоя GitHub API: установки GitHub App пользователя и управление их репозиториями.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`,
 * парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * Использует ядровые `apiErrorMessage()` и `parseRepo()` (internal, остаются в core, шарятся).
 * Приватные `parseStringArray`/`parseStringMap` переехали сюда вместе с `parseAppInstallation`
 * (их единственным потребителем) — в core больше не нужны.
 */

internal suspend fun GitHubManager.getAppInstallations(context: Context, page: Int = 1, perPage: Int = 30): GHAppInstallationsPage {
    val appToken = GitHubAuth.getValidGitHubAppUserToken(context)
    if (appToken.isBlank()) {
        return GHAppInstallationsPage(
            error = "Connect GsGit App to read its real installation state",
            code = 401,
        )
    }
    val r = request(
        context,
        "/user/installations?per_page=$perPage&page=$page",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        authToken = appToken,
    )
    if (r.code == 401) {
        GitHubAuth.disconnectGitHubApp(context)
        return GHAppInstallationsPage(error = "Reconnect GsGit App: GitHub rejected its user token", code = 401)
    }
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

internal suspend fun GitHubManager.getAppInstallationRepositories(context: Context, installationId: Long, page: Int = 1, perPage: Int = 30): GHAppInstallationReposPage {
    val appToken = GitHubAuth.getValidGitHubAppUserToken(context)
    if (appToken.isBlank()) {
        return GHAppInstallationReposPage(error = "GsGit App connection expired", code = 401)
    }
    val r = request(
        context,
        "/user/installations/$installationId/repositories?per_page=$perPage&page=$page",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        authToken = appToken,
    )
    if (r.code == 401) {
        GitHubAuth.disconnectGitHubApp(context)
        return GHAppInstallationReposPage(error = "Reconnect GsGit App: authorization was rejected", code = 401)
    }
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

internal suspend fun GitHubManager.addRepositoryToAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
    val appToken = GitHubAuth.getValidGitHubAppUserToken(context)
    if (appToken.isBlank()) return GHActionResult(false, 401, "GsGit App connection expired")
    val r = request(
        context,
        "/user/installations/$installationId/repositories/$repositoryId",
        "PUT",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        authToken = appToken,
    )
    if (r.code == 401) GitHubAuth.disconnectGitHubApp(context)
    return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository added" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.removeRepositoryFromAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
    val appToken = GitHubAuth.getValidGitHubAppUserToken(context)
    if (appToken.isBlank()) return GHActionResult(false, 401, "GsGit App connection expired")
    val r = request(
        context,
        "/user/installations/$installationId/repositories/$repositoryId",
        "DELETE",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        authToken = appToken,
    )
    if (r.code == 401) GitHubAuth.disconnectGitHubApp(context)
    return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository removed" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.getGitHubAppMetadata(context: Context, appSlug: String): GHAppMetadataResult {
    val slug = appSlug.trim().lowercase()
    if (!slug.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,98}[a-z0-9])?"))) {
        return GHAppMetadataResult(error = "Invalid GitHub App slug", code = -1)
    }
    val r = request(
        context,
        "/apps/$slug",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        trackErrors = false,
    )
    if (!r.success) return GHAppMetadataResult(error = apiErrorMessage(r), code = r.code)
    return try {
        GHAppMetadataResult(app = parseAppMetadata(JSONObject(r.body)), code = r.code)
    } catch (e: Exception) {
        GHAppMetadataResult(error = e.message ?: "Parse error", code = r.code)
    }
}

/**
 * Builds the strongest repository-scoped picture GitHub exposes to a PAT:
 * apps attached to check-runs on five recent commits, workflow action references,
 * and provider-related Actions secret names. None of these signals is promoted
 * to a claim that an app is installed.
 */
internal suspend fun GitHubManager.getRepoAppsEvidence(
    context: Context,
    repo: GHRepo,
): GHRepoAppsEvidence {
    val owner = repo.owner
    val name = repo.name
    val branch = repo.defaultBranch.ifBlank { "main" }
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")

    var checksCode = 0
    var checksError = ""
    val observed = linkedMapOf<String, MutableObservedApp>()
    var commitsScanned = 0

    val commitsResponse = request(
        context,
        "/repos/$owner/$name/commits?sha=$encodedBranch&per_page=5",
        trackErrors = false,
    )
    if (commitsResponse.success) {
        checksCode = commitsResponse.code
        val commits = runCatching { JSONArray(commitsResponse.body) }.getOrNull()
        if (commits == null) {
            checksError = "Commit response could not be parsed"
        } else {
            commitsScanned = commits.length()
            for (index in 0 until commits.length()) {
                val sha = commits.optJSONObject(index)?.optString("sha").orEmpty()
                if (sha.isBlank()) continue
                val checksResponse = request(
                    context,
                    "/repos/$owner/$name/commits/$sha/check-runs?filter=all&per_page=100",
                    extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
                    trackErrors = false,
                )
                if (!checksResponse.success) {
                    if (checksError.isBlank()) checksError = apiErrorMessage(checksResponse)
                    checksCode = checksResponse.code
                    continue
                }
                checksCode = checksResponse.code
                val runs = runCatching {
                    JSONObject(checksResponse.body).optJSONArray("check_runs") ?: JSONArray()
                }.getOrElse {
                    if (checksError.isBlank()) checksError = "Check-runs response could not be parsed"
                    JSONArray()
                }
                for (runIndex in 0 until runs.length()) {
                    val run = runs.optJSONObject(runIndex) ?: continue
                    val appJson = run.optJSONObject("app") ?: continue
                    val app = runCatching { parseAppMetadata(appJson) }.getOrNull() ?: continue
                    val key = app.slug.ifBlank { "id:${app.id}" }
                    val item = observed.getOrPut(key) {
                        MutableObservedApp(
                            app = app,
                            lastStatus = run.optString("status", ""),
                            lastConclusion = run.optString("conclusion", ""),
                            lastSeenAt = run.optString("completed_at", "")
                                .ifBlank { run.optString("started_at", "") },
                            lastCommitSha = sha,
                        )
                    }
                    item.checkRunCount++
                    run.optString("name", "").takeIf { it.isNotBlank() }?.let { item.checkNames += it }
                }
            }
        }
    } else {
        checksCode = commitsResponse.code
        checksError = apiErrorMessage(commitsResponse)
    }

    var workflowsCode = 0
    var workflowsError = ""
    var workflowsTotal = 0
    val workflowEvidence = mutableListOf<GHWorkflowAppEvidence>()
    val workflowsResponse = request(
        context,
        "/repos/$owner/$name/actions/workflows?per_page=100",
        trackErrors = false,
    )
    if (workflowsResponse.success) {
        workflowsCode = workflowsResponse.code
        val root = runCatching { JSONObject(workflowsResponse.body) }.getOrNull()
        val workflows = root?.optJSONArray("workflows") ?: JSONArray()
        workflowsTotal = root?.optInt("total_count", workflows.length()) ?: workflows.length()
        var unreadableDefinitions = 0
        for (index in 0 until minOf(workflows.length(), 50)) {
            val workflow = workflows.optJSONObject(index) ?: continue
            val path = workflow.optString("path", "")
            if (path.isBlank()) continue
            val contentResponse = request(
                context,
                "/repos/$owner/$name/contents/${encPath(path)}?ref=$encodedBranch",
                trackErrors = false,
            )
            val yaml = if (contentResponse.success) {
                runCatching {
                    val encoded = JSONObject(contentResponse.body).optString("content", "").replace("\n", "")
                    String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
                }.getOrDefault("")
            } else {
                unreadableDefinitions++
                ""
            }
            val actionRefs = WORKFLOW_USES_REGEX.findAll(yaml)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
            val providers = buildList {
                if (actionRefs.any { it.lowercase().startsWith("anthropics/claude") }) add("claude-code")
                if (actionRefs.any {
                        val ref = it.lowercase()
                        ref.startsWith("openai/") && ref.contains("codex")
                    }
                ) add("codex")
            }
            if (actionRefs.isNotEmpty() || providers.isNotEmpty()) {
                workflowEvidence += GHWorkflowAppEvidence(
                    workflowId = workflow.optLong("id"),
                    name = workflow.optString("name", path.substringAfterLast('/')),
                    path = path,
                    state = workflow.optString("state", ""),
                    actionReferences = actionRefs,
                    providerIds = providers,
                )
            }
        }
        if (workflows.length() > 50) {
            workflowsError = "Scanned the first 50 of ${workflows.length()} workflow definitions"
        } else if (unreadableDefinitions > 0) {
            workflowsError = "$unreadableDefinitions workflow definitions were not readable with this token"
        }
    } else {
        workflowsCode = workflowsResponse.code
        workflowsError = apiErrorMessage(workflowsResponse)
    }

    var secretsCode = 0
    var secretsError = ""
    val providerSecrets = mutableListOf<String>()
    val secretsResponse = request(
        context,
        "/repos/$owner/$name/actions/secrets?per_page=100",
        trackErrors = false,
    )
    if (secretsResponse.success) {
        secretsCode = secretsResponse.code
        val secrets = runCatching {
            JSONObject(secretsResponse.body).optJSONArray("secrets") ?: JSONArray()
        }.getOrElse { JSONArray() }
        for (index in 0 until secrets.length()) {
            val secret = secrets.optJSONObject(index)?.optString("name").orEmpty()
            if (PROVIDER_SECRET_MARKERS.any { secret.contains(it, ignoreCase = true) }) {
                providerSecrets += secret
            }
        }
    } else {
        secretsCode = secretsResponse.code
        secretsError = apiErrorMessage(secretsResponse)
    }

    return GHRepoAppsEvidence(
        repoFullName = repo.fullName,
        branch = branch,
        commitsScanned = commitsScanned,
        observedApps = observed.values.map { it.toResult() }
            .sortedByDescending { it.lastSeenAt },
        workflowsTotal = workflowsTotal,
        workflowEvidence = workflowEvidence,
        providerSecretNames = providerSecrets.distinct().sorted(),
        checksCode = checksCode,
        workflowsCode = workflowsCode,
        secretsCode = secretsCode,
        checksError = checksError,
        workflowsError = workflowsError,
        secretsError = secretsError,
    )
}

// ─── Парсеры / хелперы (чистые, без IO) ──────────────────────────────────────

private fun parseAppInstallation(j: JSONObject): GHAppInstallation {
    val account = j.optJSONObject("account")
    val suspendedBy = j.optJSONObject("suspended_by")
    return GHAppInstallation(
        id = j.optLong("id"),
        appId = j.optLong("app_id"),
        appSlug = j.optNullableString("app_slug"),
        targetId = j.optLong("target_id"),
        targetType = j.optNullableString("target_type"),
        targetLogin = account?.optString("login") ?: "",
        targetAvatarUrl = account?.optString("avatar_url") ?: "",
        repositorySelection = j.optNullableString("repository_selection"),
        permissions = parseStringMap(j.optJSONObject("permissions")),
        events = parseStringArray(j.optJSONArray("events")),
        singleFileName = j.optNullableString("single_file_name"),
        singleFilePaths = parseStringArray(j.optJSONArray("single_file_paths")),
        htmlUrl = j.optNullableString("html_url"),
        createdAt = j.optNullableString("created_at"),
        updatedAt = j.optNullableString("updated_at"),
        suspendedAt = j.optNullableString("suspended_at"),
        suspendedBy = suspendedBy?.optString("login") ?: ""
    )
}

private fun JSONObject.optNullableString(key: String): String =
    if (isNull(key)) "" else optString(key, "").takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

private fun parseAppMetadata(j: JSONObject): GHAppMetadata {
    val owner = j.optJSONObject("owner")
    return GHAppMetadata(
        id = j.optLong("id"),
        slug = j.optString("slug", ""),
        name = j.optString("name", ""),
        ownerLogin = owner?.optString("login", "").orEmpty(),
        ownerAvatarUrl = owner?.optString("avatar_url", "").orEmpty(),
        description = j.optString("description", "").trim(),
        externalUrl = j.optString("external_url", ""),
        htmlUrl = j.optString("html_url", ""),
        permissions = parseStringMap(j.optJSONObject("permissions")),
        events = parseStringArray(j.optJSONArray("events")),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
    )
}

private fun parseStringArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() && it != "null" } }
}

private fun parseStringMap(j: JSONObject?): List<Pair<String, String>> {
    if (j == null) return emptyList()
    return j.keys().asSequence()
        .mapNotNull { key -> key.takeIf { it.isNotBlank() }?.let { it to j.optString(it, "") } }
        .sortedBy { it.first }
        .toList()
}

private class MutableObservedApp(
    val app: GHAppMetadata,
    var checkRunCount: Int = 0,
    val checkNames: MutableSet<String> = linkedSetOf(),
    val lastStatus: String,
    val lastConclusion: String,
    val lastSeenAt: String,
    val lastCommitSha: String,
) {
    fun toResult() = GHObservedApp(
        app = app,
        checkRunCount = checkRunCount,
        checkNames = checkNames.toList(),
        lastStatus = lastStatus,
        lastConclusion = lastConclusion,
        lastSeenAt = lastSeenAt,
        lastCommitSha = lastCommitSha,
    )
}

private val WORKFLOW_USES_REGEX = Regex("""(?m)^\s*-?\s*uses:\s*['\"]?([^'\"\s#]+)""")
private val PROVIDER_SECRET_MARKERS = listOf("ANTHROPIC", "CLAUDE", "OPENAI", "CODEX")
