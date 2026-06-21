package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHAutolink
import gs.git.vps.data.github.model.GHCodespace
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен RepoFeatures слоя GitHub API: autolinks репозитория (CRUD), codespaces пользователя
 * (list/delete), Git LFS репозитория (enable/disable). Мелкие фиче-эндпоинты, объединённые в один
 * файл. Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`.
 * Сигнатуры вызовов не менялись.
 */

// ─── Autolinks ───────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getAutolinks(context: Context, owner: String, repo: String): List<GHAutolink> {
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

internal suspend fun GitHubManager.createAutolink(context: Context, owner: String, repo: String, keyPrefix: String, urlTemplate: String, isAlphanumeric: Boolean = true): Boolean {
    val body = JSONObject().apply {
        put("key_prefix", keyPrefix)
        put("url_template", urlTemplate)
        put("is_alphanumeric", isAlphanumeric)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/autolinks", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.success
}

internal suspend fun GitHubManager.deleteAutolink(context: Context, owner: String, repo: String, autolinkId: Long): Boolean {
    return request(context, "/repos/$owner/$repo/autolinks/$autolinkId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }
}

// ─── Codespaces ──────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getCodespaces(context: Context, page: Int = 1): List<GHCodespace> {
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

internal suspend fun GitHubManager.deleteCodespace(context: Context, codespaceName: String): Boolean {
    return request(context, "/user/codespaces/${URLEncoder.encode(codespaceName, "UTF-8")}", "DELETE").let { it.code == 202 || it.code == 204 || it.success }
}

// ─── Git LFS ─────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.enableRepoLfs(context: Context, owner: String, repo: String): Boolean {
    val body = JSONObject().apply { put("enabled", true) }.toString()
    val r = request(context, "/repos/$owner/$repo/lfs", "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.success
}

internal suspend fun GitHubManager.disableRepoLfs(context: Context, owner: String, repo: String): Boolean {
    return request(context, "/repos/$owner/$repo/lfs", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }
}
