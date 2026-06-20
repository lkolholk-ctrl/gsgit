package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHReaction
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Reactions слоя GitHub API: реакции на issues, на комментарии issues и на комментарии
 * ревью PR. Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро
 * `request()`, парсинг — чистая `parseGHReaction`. Сигнатуры вызовов не менялись.
 *
 * Удаление реакции у всех видов — общий эндпоинт DELETE /repos/{o}/{r}/reactions/{id}.
 */

// ─── Issue reactions ─────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getIssueReactions(context: Context, owner: String, repo: String, issueNumber: Int): List<GHReaction> {
    val r = request(context, "/repos/$owner/$repo/issues/$issueNumber/reactions?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHReaction(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addIssueReaction(context: Context, owner: String, repo: String, issueNumber: Int, content: String): Boolean {
    val body = JSONObject().apply { put("content", content) }.toString()
    val r = request(context, "/repos/$owner/$repo/issues/$issueNumber/reactions", "POST", body)
    return r.success
}

internal suspend fun GitHubManager.deleteIssueReaction(context: Context, owner: String, repo: String, reactionId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/reactions/$reactionId", "DELETE")
    return r.code == 204 || r.success
}

// ─── Issue comment reactions ─────────────────────────────────────────────────

internal suspend fun GitHubManager.getIssueCommentReactions(context: Context, owner: String, repo: String, commentId: Long): List<GHReaction> {
    val r = request(context, "/repos/$owner/$repo/issues/comments/$commentId/reactions?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHReaction(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addIssueCommentReaction(context: Context, owner: String, repo: String, commentId: Long, content: String): Boolean {
    val body = JSONObject().apply { put("content", content) }.toString()
    return request(context, "/repos/$owner/$repo/issues/comments/$commentId/reactions", "POST", body).success
}

internal suspend fun GitHubManager.deleteIssueCommentReaction(context: Context, owner: String, repo: String, reactionId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/reactions/$reactionId", "DELETE")
    return r.code == 204 || r.success
}

// ─── PR review comment reactions ─────────────────────────────────────────────

internal suspend fun GitHubManager.getPullRequestReviewCommentReactions(context: Context, owner: String, repo: String, commentId: Long): List<GHReaction> {
    val r = request(context, "/repos/$owner/$repo/pulls/comments/$commentId/reactions?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHReaction(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addPullRequestReviewCommentReaction(context: Context, owner: String, repo: String, commentId: Long, content: String): Boolean {
    val body = JSONObject().apply { put("content", content) }.toString()
    return request(context, "/repos/$owner/$repo/pulls/comments/$commentId/reactions", "POST", body).success
}

internal suspend fun GitHubManager.deletePullRequestReviewCommentReaction(context: Context, owner: String, repo: String, reactionId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/reactions/$reactionId", "DELETE")
    return r.code == 204 || r.success
}

// ─── Парсер (чистый, без IO) ─────────────────────────────────────────────────

private fun parseGHReaction(j: JSONObject): GHReaction =
    GHReaction(
        id = j.optLong("id"),
        content = j.optString("content"),
        user = j.optJSONObject("user")?.optString("login") ?: ""
    )
