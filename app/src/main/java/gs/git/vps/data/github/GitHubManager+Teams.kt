package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHOrgTeam
import gs.git.vps.data.github.model.GHRepoTeam
import gs.git.vps.data.github.model.GHTeamDiscussion
import gs.git.vps.data.github.model.GHCollaborator
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Teams слоя GitHub API: команды репозитория/организации, доступ команд к репо,
 * участники команд, обсуждения команд. Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Сеть — через ядро `request()`, парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * GHCollaborator (возвращает getTeamMembers) — домен Collaborators, импортируется из `.model`.
 */

// ─── Команды репо / организации ──────────────────────────────────────────────

internal suspend fun GitHubManager.getRepoTeams(context: Context, owner: String, repo: String): List<GHRepoTeam> {
    val r = request(context, "/repos/$owner/$repo/teams?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHRepoTeam(arr.getJSONObject(i)) }
    } catch (e: Exception) {
        emptyList()
    }
}

internal suspend fun GitHubManager.getOrgTeams(context: Context, org: String): List<GHOrgTeam> {
    val r = request(context, "/orgs/${URLEncoder.encode(org, "UTF-8")}/teams?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHOrgTeam(
                id = j.optLong("id"),
                name = j.optString("name"),
                slug = j.optString("slug"),
                description = j.optString("description", ""),
                privacy = j.optString("privacy", ""),
                permission = normalizeRepoTeamPermission(j.optString("permission", "")),
                membersCount = j.optInt("members_count", 0),
                reposCount = j.optInt("repos_count", 0),
                htmlUrl = j.optString("html_url", "")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

internal suspend fun GitHubManager.addRepoTeam(context: Context, org: String, teamSlug: String, owner: String, repo: String, permission: String): Boolean {
    val body = JSONObject().apply { put("permission", normalizeRepoTeamPermission(permission)) }.toString()
    val encodedTeam = URLEncoder.encode(teamSlug, "UTF-8")
    return request(context, "/orgs/${URLEncoder.encode(org, "UTF-8")}/teams/$encodedTeam/repos/$owner/$repo", "PUT", body).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.updateRepoTeamPermission(context: Context, org: String, teamSlug: String, owner: String, repo: String, permission: String): Boolean =
    addRepoTeam(context, org, teamSlug, owner, repo, permission)

internal suspend fun GitHubManager.removeRepoTeam(context: Context, org: String, teamSlug: String, owner: String, repo: String): Boolean {
    val encodedTeam = URLEncoder.encode(teamSlug, "UTF-8")
    return request(context, "/orgs/${URLEncoder.encode(org, "UTF-8")}/teams/$encodedTeam/repos/$owner/$repo", "DELETE").let { it.code == 204 || it.success }
}

// ─── Участники команд ────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getTeamMembers(context: Context, org: String, teamSlug: String): List<GHCollaborator> {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    val r = request(context, "/orgs/$encodedOrg/teams/$encodedTeam/members?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHCollaborator(
                login = j.optString("login"),
                avatarUrl = j.optString("avatar_url", ""),
                role = "member"
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addTeamMember(context: Context, org: String, teamSlug: String, username: String, role: String = "member"): Boolean {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
    val body = JSONObject().apply { put("role", role) }.toString()
    return request(context, "/orgs/$encodedOrg/teams/$encodedTeam/memberships/$encodedUser", "PUT", body).success
}

internal suspend fun GitHubManager.removeTeamMember(context: Context, org: String, teamSlug: String, username: String): Boolean {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
    return request(context, "/orgs/$encodedOrg/teams/$encodedTeam/memberships/$encodedUser", "DELETE").success
}

// ─── Обсуждения команд ───────────────────────────────────────────────────────

internal suspend fun GitHubManager.getTeamDiscussions(context: Context, org: String, teamSlug: String): List<GHTeamDiscussion> {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    val r = request(context, "/orgs/$encodedOrg/teams/$encodedTeam/discussions?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val author = j.optJSONObject("author")
            GHTeamDiscussion(
                id = j.optLong("id"),
                number = j.optInt("number"),
                title = j.optString("title"),
                body = j.optString("body", ""),
                author = author?.optString("login") ?: j.optString("author_login"),
                avatarUrl = author?.optString("avatar_url") ?: "",
                createdAt = j.optString("created_at", ""),
                commentsCount = j.optInt("comments_count", 0),
                htmlUrl = j.optString("html_url", "")
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createTeamDiscussion(context: Context, org: String, teamSlug: String, title: String, body: String): Boolean {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    val reqBody = JSONObject().apply {
        put("title", title)
        put("body", body)
    }.toString()
    return request(context, "/orgs/$encodedOrg/teams/$encodedTeam/discussions", "POST", reqBody).success
}

internal suspend fun GitHubManager.deleteTeamDiscussion(context: Context, org: String, teamSlug: String, number: Int): Boolean {
    val encodedOrg = java.net.URLEncoder.encode(org, "UTF-8")
    val encodedTeam = java.net.URLEncoder.encode(teamSlug, "UTF-8")
    return request(context, "/orgs/$encodedOrg/teams/$encodedTeam/discussions/$number", "DELETE").success
}

// ─── Парсеры / хелперы (чистые, без IO) ──────────────────────────────────────

private fun parseGHRepoTeam(j: JSONObject): GHRepoTeam {
    val permissions = j.optJSONObject("permissions")
    val permission = when {
        permissions?.optBoolean("admin", false) == true -> "admin"
        permissions?.optBoolean("maintain", false) == true -> "maintain"
        permissions?.optBoolean("push", false) == true -> "push"
        permissions?.optBoolean("triage", false) == true -> "triage"
        permissions?.optBoolean("pull", false) == true -> "pull"
        else -> normalizeRepoTeamPermission(j.optString("permission", "pull"))
    }
    val org = j.optJSONObject("organization")
    return GHRepoTeam(
        id = j.optLong("id"),
        name = j.optString("name"),
        slug = j.optString("slug"),
        description = j.optString("description", ""),
        privacy = j.optString("privacy", ""),
        permission = permission,
        membersCount = j.optInt("members_count", 0),
        reposCount = j.optInt("repos_count", 0),
        htmlUrl = j.optString("html_url", ""),
        organization = org?.optString("login", "") ?: ""
    )
}

private fun normalizeRepoTeamPermission(permission: String): String = when (permission.lowercase()) {
    "read", "pull" -> "pull"
    "write", "push" -> "push"
    "triage", "maintain", "admin" -> permission.lowercase()
    else -> "pull"
}
