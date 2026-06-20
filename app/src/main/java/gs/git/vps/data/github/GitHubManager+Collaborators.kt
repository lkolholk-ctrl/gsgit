package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHCollaborator
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Collaborators слоя GitHub API: участники репозитория (список, добавление, удаление,
 * смена прав). Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро
 * `request()`, парсинг — чистая `parseGHCollaborator`. Сигнатуры вызовов не менялись.
 */

internal suspend fun GitHubManager.getCollaborators(context: Context, owner: String, repo: String): List<GHCollaborator> {
    val r = request(context, "/repos/$owner/$repo/collaborators?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHCollaborator(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addCollaborator(context: Context, owner: String, repo: String, username: String, permission: String = "push"): Boolean {
    val body = JSONObject().apply { put("permission", permission) }.toString()
    return request(context, "/repos/$owner/$repo/collaborators/${URLEncoder.encode(username, "UTF-8")}", "PUT", body).let { it.code == 201 || it.code == 204 || it.success }
}

internal suspend fun GitHubManager.removeCollaborator(context: Context, owner: String, repo: String, username: String): Boolean =
    request(context, "/repos/$owner/$repo/collaborators/${URLEncoder.encode(username, "UTF-8")}", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.updateCollaboratorPermission(context: Context, owner: String, repo: String, username: String, permission: String): Boolean {
    val body = JSONObject().apply { put("permission", permission) }.toString()
    return request(context, "/repos/$owner/$repo/collaborators/${URLEncoder.encode(username, "UTF-8")}", "PUT", body).success
}

// ─── Парсер (чистый, без IO) ─────────────────────────────────────────────────

private fun parseGHCollaborator(j: JSONObject): GHCollaborator {
    val perms = j.optJSONObject("permissions")
    return GHCollaborator(
        login = j.optString("login"),
        avatarUrl = j.optString("avatar_url", ""),
        role = perms?.let {
            when {
                it.optBoolean("admin", false) -> "admin"
                it.optBoolean("maintain", false) -> "maintain"
                it.optBoolean("push", false) -> "push"
                it.optBoolean("triage", false) -> "triage"
                it.optBoolean("pull", false) -> "pull"
                else -> "pull"
            }
        } ?: "pull"
    )
}
