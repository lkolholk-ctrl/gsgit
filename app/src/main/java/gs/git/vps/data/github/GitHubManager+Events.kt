package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHRepoEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Events слоя GitHub API: лента событий пользователя (received / public). Нарезан по эталону
 * Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`, парсинг — чистая
 * `parseGHRepoEvent`. Сигнатуры вызовов не менялись. GHRepoEvent — модель домена Repos.
 */

internal suspend fun GitHubManager.getUserReceivedEvents(context: Context, username: String, page: Int = 1): List<GHRepoEvent> {
    val r = request(context, "/users/$username/received_events?per_page=30&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHRepoEvent(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getUserPublicEvents(context: Context, username: String, page: Int = 1): List<GHRepoEvent> {
    val r = request(context, "/users/$username/events/public?per_page=100&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHRepoEvent(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

// ─── Парсер (чистый, без IO) ─────────────────────────────────────────────────

private fun parseGHRepoEvent(j: JSONObject): GHRepoEvent {
    val payload = j.optJSONObject("payload")
    return GHRepoEvent(
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
