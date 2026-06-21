package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHActionResult
import gs.git.vps.data.github.model.GHAppInstallation
import gs.git.vps.data.github.model.GHAppInstallationReposPage
import gs.git.vps.data.github.model.GHAppInstallationsPage
import org.json.JSONArray
import org.json.JSONObject

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

internal suspend fun GitHubManager.getAppInstallationRepositories(context: Context, installationId: Long, page: Int = 1, perPage: Int = 30): GHAppInstallationReposPage {
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

internal suspend fun GitHubManager.addRepositoryToAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
    val r = request(
        context,
        "/user/installations/$installationId/repositories/$repositoryId",
        "PUT",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json")
    )
    return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository added" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.removeRepositoryFromAppInstallation(context: Context, installationId: Long, repositoryId: Long): GHActionResult {
    val r = request(
        context,
        "/user/installations/$installationId/repositories/$repositoryId",
        "DELETE",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json")
    )
    return GHActionResult(r.code == 204 || r.success, r.code, if (r.success || r.code == 204) "Repository removed" else apiErrorMessage(r))
}

// ─── Парсеры / хелперы (чистые, без IO) ──────────────────────────────────────

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
