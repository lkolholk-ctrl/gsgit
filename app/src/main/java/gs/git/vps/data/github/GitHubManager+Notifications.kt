package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHNotification
import gs.git.vps.data.github.model.GHThreadSubscription
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Домен Notifications слоя GitHub API (уведомления, подписки на треды, watch репозитория).
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Вся сеть — через ядро
 * `GitHubManager.request()`; парсинг — чистые `parseGHNotification`/`parseGHThreadSubscription`.
 * Сигнатуры публичных вызовов не изменились при выносе.
 */

internal suspend fun GitHubManager.getNotifications(
    context: Context,
    all: Boolean = false,
    participating: Boolean = false,
    since: String? = null
): List<GHNotification> = fetchNotifications(context, all, participating, since, strictErrors = false)

internal suspend fun GitHubManager.listNotifications(
    context: Context,
    all: Boolean = false,
    participating: Boolean = false,
    since: String? = null
): List<GHNotification> = fetchNotifications(context, all, participating, since, strictErrors = true)

private suspend fun GitHubManager.fetchNotifications(
    context: Context,
    all: Boolean,
    participating: Boolean,
    since: String?,
    strictErrors: Boolean
): List<GHNotification> {
    val sinceQ = if (!since.isNullOrBlank()) "&since=${URLEncoder.encode(since, "UTF-8")}" else ""
    val r = request(context, "/notifications?all=$all&participating=$participating&per_page=50$sinceQ")
    if (!r.success) {
        val rateLimited = r.code == 403 &&
            (r.body.contains("rate limit", ignoreCase = true) ||
                r.body.contains("API rate limit exceeded", ignoreCase = true))
        if (strictErrors && !rateLimited) {
            throw IOException("GitHub notifications request failed: HTTP ${r.code}")
        }
        return emptyList()
    }
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHNotification(arr.getJSONObject(i)) }
    } catch (e: Exception) {
        if (strictErrors) throw IOException("Failed to parse GitHub notifications", e)
        emptyList()
    }
}

internal suspend fun GitHubManager.markNotificationRead(context: Context, threadId: String): Boolean =
    request(context, "/notifications/threads/$threadId", "PATCH").let { it.code == 205 || it.success }

/** Alias matching the notifications spec naming. */
internal suspend fun GitHubManager.markThreadRead(context: Context, threadId: String): Boolean =
    markNotificationRead(context, threadId)

internal suspend fun GitHubManager.markAllNotificationsRead(context: Context): Boolean =
    request(context, "/notifications", "PUT", "{\"read\":true}").let { it.code == 205 || it.success }

internal suspend fun GitHubManager.markRepoNotificationsRead(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo/notifications", "PUT", "{\"read\":true}").let { it.code == 205 || it.success }

internal suspend fun GitHubManager.getThreadSubscription(context: Context, threadId: String): GHThreadSubscription {
    val r = request(context, "/notifications/threads/$threadId/subscription", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return GHThreadSubscription(subscribed = false, ignored = false, reason = "", createdAt = "", url = "")
    return try { parseGHThreadSubscription(JSONObject(r.body)) } catch (e: Exception) {
        GHThreadSubscription(subscribed = false, ignored = false, reason = "", createdAt = "", url = "")
    }
}

internal suspend fun GitHubManager.setThreadSubscription(context: Context, threadId: String, subscribed: Boolean, ignored: Boolean): Boolean {
    val body = JSONObject().apply {
        put("subscribed", subscribed)
        put("ignored", ignored)
    }.toString()
    return request(context, "/notifications/threads/$threadId/subscription", "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json")).success
}

internal suspend fun GitHubManager.deleteThreadSubscription(context: Context, threadId: String): Boolean =
    request(context, "/notifications/threads/$threadId/subscription", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }

internal suspend fun GitHubManager.markThreadDone(context: Context, threadId: String): Boolean =
    request(context, "/notifications/threads/$threadId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.getNotification(context: Context, threadId: String): GHNotification? {
    val r = request(context, "/notifications/threads/$threadId")
    if (!r.success) return null
    return try { parseGHNotification(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.isWatching(context: Context, owner: String, repo: String): Boolean {
    val r = request(context, "/repos/$owner/$repo/subscription")
    return r.success && JSONObject(r.body).optBoolean("subscribed", false)
}

internal suspend fun GitHubManager.isWatchingRepo(context: Context, owner: String, repo: String): Boolean {
    val r = request(context, "/repos/$owner/$repo/subscription")
    if (!r.success) return false
    return try { JSONObject(r.body).optString("subscribed", "false") == "true" } catch (_: Exception) { false }
}

internal suspend fun GitHubManager.watchRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo/subscription", "PUT", "{\"subscribed\":true}").success

internal suspend fun GitHubManager.unwatchRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo/subscription", "DELETE").let { it.code == 204 || it.success }

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHNotification(j: JSONObject): GHNotification {
    val sub = j.optJSONObject("subject")
    val repo = j.optJSONObject("repository")
    val subjectUrl = sub?.optString("url") ?: ""
    val repoHtmlUrl = repo?.optString("html_url") ?: ""
    val htmlUrl = githubApiUrlToWebUrl(subjectUrl).ifBlank { repoHtmlUrl }
    return GHNotification(
        id = j.optString("id"),
        unread = j.optBoolean("unread", false),
        reason = j.optString("reason", ""),
        title = sub?.optString("title") ?: "",
        type = sub?.optString("type") ?: "",
        repoName = repo?.optString("full_name") ?: "",
        updatedAt = j.optString("updated_at", ""),
        url = subjectUrl,
        lastReadAt = j.optString("last_read_at", "").takeIf { it.isNotBlank() && it != "null" },
        subjectUrl = subjectUrl,
        repositoryUrl = repo?.optString("url") ?: "",
        htmlUrl = htmlUrl
    )
}

internal fun parseGHThreadSubscription(j: JSONObject): GHThreadSubscription =
    GHThreadSubscription(
        subscribed = j.optBoolean("subscribed", false),
        ignored = j.optBoolean("ignored", false),
        reason = j.optString("reason", ""),
        createdAt = j.optString("created_at", ""),
        url = j.optString("url", "")
    )

private fun githubApiUrlToWebUrl(apiUrl: String): String {
    if (apiUrl.isBlank()) return ""
    if (apiUrl.startsWith("https://github.com/")) return apiUrl
    return apiUrl
        .replace("https://api.github.com/repos/", "https://github.com/")
        .replace("/pulls/", "/pull/")
}
