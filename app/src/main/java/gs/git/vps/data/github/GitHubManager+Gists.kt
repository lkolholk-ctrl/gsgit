package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHGist
import gs.git.vps.data.github.model.GHGistComment
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Gists слоя GitHub API. Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Вся сеть — через ядро `GitHubManager.request()`. Парсинг — чистые `parseGHGist`/`parseGHGistComment`.
 * Сигнатуры публичных вызовов (`GitHubManager.getGists(...)` и т.п.) не изменились при выносе.
 */

internal suspend fun GitHubManager.getGists(context: Context): List<GHGist> {
    val r = request(context, "/gists?per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHGist(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createGist(context: Context, description: String, isPublic: Boolean, files: Map<String, String>): Boolean {
    val filesObj = JSONObject()
    files.forEach { (name, content) -> filesObj.put(name, JSONObject().apply { put("content", content) }) }
    val body = JSONObject().apply {
        put("description", description); put("public", isPublic); put("files", filesObj)
    }.toString()
    return request(context, "/gists", "POST", body).success
}

internal suspend fun GitHubManager.getGistContent(context: Context, gistId: String): Map<String, String> {
    val r = request(context, "/gists/$gistId")
    if (!r.success) return emptyMap()
    return try {
        val filesObj = JSONObject(r.body).optJSONObject("files") ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        filesObj.keys().forEach { key ->
            result[key] = filesObj.getJSONObject(key).optString("content", "")
        }
        result
    } catch (e: Exception) { emptyMap() }
}

internal suspend fun GitHubManager.deleteGist(context: Context, gistId: String): Boolean =
    request(context, "/gists/$gistId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.starGist(context: Context, gistId: String): Boolean =
    request(context, "/gists/$gistId/star", "PUT", "").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.unstarGist(context: Context, gistId: String): Boolean =
    request(context, "/gists/$gistId/star", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.isGistStarred(context: Context, gistId: String): Boolean {
    val r = request(context, "/gists/$gistId/star", trackErrors = false)
    return r.code == 204
}

internal suspend fun GitHubManager.forkGist(context: Context, gistId: String): Boolean =
    request(context, "/gists/$gistId/forks", "POST", "{}").success

internal suspend fun GitHubManager.updateGist(context: Context, gistId: String, description: String? = null, files: Map<String, String?>? = null): Boolean {
    val body = JSONObject()
    if (description != null) body.put("description", description)
    if (files != null) {
        val filesObj = JSONObject()
        files.forEach { (name, content) ->
            if (content != null) filesObj.put(name, JSONObject().apply { put("content", content) })
            else filesObj.put(name, JSONObject.NULL)
        }
        body.put("files", filesObj)
    }
    return request(context, "/gists/$gistId", "PATCH", body.toString()).success
}

internal suspend fun GitHubManager.getGistComments(context: Context, gistId: String): List<GHGistComment> {
    val r = request(context, "/gists/$gistId/comments?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHGistComment(arr.getJSONObject(i)) }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addGistComment(context: Context, gistId: String, body: String): Boolean {
    val payload = JSONObject().apply { put("body", body) }.toString()
    return request(context, "/gists/$gistId/comments", "POST", payload).success
}

internal suspend fun GitHubManager.deleteGistComment(context: Context, gistId: String, commentId: Long): Boolean =
    request(context, "/gists/$gistId/comments/$commentId", "DELETE").let { it.code == 204 || it.success }

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHGist(j: JSONObject): GHGist {
    val filesObj = j.optJSONObject("files")
    val files = mutableListOf<String>()
    filesObj?.keys()?.forEach { files.add(it) }
    return GHGist(
        id = j.optString("id"),
        description = j.optString("description", ""),
        isPublic = j.optBoolean("public", true),
        files = files,
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        owner = j.optJSONObject("owner")?.optString("login", "").orEmpty(),
        comments = j.optInt("comments", 0),
        htmlUrl = j.optString("html_url", ""),
    )
}

internal fun parseGHGistComment(j: JSONObject): GHGistComment =
    GHGistComment(
        id = j.optLong("id"),
        body = j.optString("body"),
        user = j.optJSONObject("user")?.optString("login", "").orEmpty(),
        createdAt = j.optString("created_at", ""),
    )
