package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHNotification
import gs.git.vps.data.github.model.GHNotificationActionResult
import gs.git.vps.data.github.model.GHNotificationThreadResult
import gs.git.vps.data.github.model.GHNotificationsPage
import gs.git.vps.data.github.model.GHRepositorySubscription
import gs.git.vps.data.github.model.GHRepositorySubscriptionResult
import gs.git.vps.data.github.model.GHThreadSubscription
import gs.git.vps.data.github.model.GHThreadSubscriptionResult
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
): List<GHNotification> = getNotificationsPage(
    context = context,
    all = all,
    participating = participating,
    since = since,
).notifications

internal suspend fun GitHubManager.listNotifications(
    context: Context,
    all: Boolean = false,
    participating: Boolean = false,
    since: String? = null
): List<GHNotification> {
    val result = getNotificationsPage(
        context = context,
        all = all,
        participating = participating,
        since = since,
    )
    if (result.error.isNotBlank()) {
        val rateLimited = result.code == 403 && result.error.contains("rate limit", ignoreCase = true)
        if (!rateLimited) throw IOException("GitHub notifications request failed: HTTP ${result.code}: ${result.error}")
    }
    return result.notifications
}

internal suspend fun GitHubManager.getNotificationsPage(
    context: Context,
    all: Boolean = false,
    participating: Boolean = false,
    since: String? = null,
    before: String? = null,
    page: Int = 1,
    perPage: Int = 50,
    repositoryFullName: String? = null,
): GHNotificationsPage {
    val normalizedRepo = repositoryFullName.orEmpty().trim().trim('/')
    val endpointBase = if (normalizedRepo.isBlank()) {
        "/notifications"
    } else {
        val parts = normalizedRepo.split('/')
        if (parts.size != 2 || parts.any { it.isBlank() }) {
            return GHNotificationsPage(page = page, error = "Repository must be owner/name", code = -1)
        }
        repoPath(parts[0], parts[1], "/notifications")
    }
    val query = buildList {
        add("all=$all")
        add("participating=$participating")
        add("per_page=${perPage.coerceIn(1, 50)}")
        add("page=${page.coerceAtLeast(1)}")
        since?.takeIf { it.isNotBlank() }?.let { add("since=${URLEncoder.encode(it, "UTF-8")}") }
        before?.takeIf { it.isNotBlank() }?.let { add("before=${URLEncoder.encode(it, "UTF-8")}") }
    }.joinToString("&")
    val r = request(
        context,
        "$endpointBase?$query",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        trackErrors = false,
    )
    if (!r.success && r.code != 304) {
        return GHNotificationsPage(
            page = page,
            code = r.code,
            error = apiErrorMessage(r),
            pollIntervalSeconds = r.headers["x-poll-interval"]?.toIntOrNull()?.coerceAtLeast(1) ?: 60,
            lastModified = r.headers["last-modified"].orEmpty(),
            repositoryFullName = normalizedRepo,
        )
    }
    return try {
        val arr = JSONArray(r.body)
        val nextPage = parseNextPage(r.headers)
        GHNotificationsPage(
            notifications = (0 until arr.length()).map { i -> parseGHNotification(arr.getJSONObject(i)) },
            page = page,
            hasNextPage = nextPage != null,
            nextPage = nextPage,
            code = r.code,
            pollIntervalSeconds = r.headers["x-poll-interval"]?.toIntOrNull()?.coerceAtLeast(1) ?: 60,
            lastModified = r.headers["last-modified"].orEmpty(),
            notModified = r.code == 304,
            repositoryFullName = normalizedRepo,
        )
    } catch (e: Exception) {
        GHNotificationsPage(
            page = page,
            code = r.code,
            error = "Failed to parse GitHub notifications: ${e.message ?: "invalid response"}",
            pollIntervalSeconds = r.headers["x-poll-interval"]?.toIntOrNull()?.coerceAtLeast(1) ?: 60,
            lastModified = r.headers["last-modified"].orEmpty(),
            repositoryFullName = normalizedRepo,
        )
    }
}

internal suspend fun GitHubManager.markNotificationReadResult(context: Context, threadId: String): GHNotificationActionResult {
    val path = notificationThreadPath(threadId)
        ?: return GHNotificationActionResult(false, -1, "Invalid notification thread id")
    val r = request(context, path, "PATCH", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val success = r.code == 205 || r.code == 304
    return GHNotificationActionResult(success, r.code, if (success) "Thread marked read" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.markNotificationRead(context: Context, threadId: String): Boolean =
    markNotificationReadResult(context, threadId).success

/** Alias matching the notifications spec naming. */
internal suspend fun GitHubManager.markThreadRead(context: Context, threadId: String): Boolean =
    markNotificationRead(context, threadId)

internal suspend fun GitHubManager.markAllNotificationsReadResult(
    context: Context,
    lastReadAt: String? = null,
    read: Boolean = true,
): GHNotificationActionResult {
    val body = JSONObject().apply {
        put("read", read)
        lastReadAt?.takeIf { it.isNotBlank() }?.let { put("last_read_at", it) }
    }.toString()
    val r = request(context, "/notifications", "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val success = r.code == 202 || r.code == 205 || r.code == 304
    val message = when {
        !success -> apiErrorMessage(r)
        r.code == 202 -> "GitHub accepted background mark-read processing"
        else -> "Notifications marked read"
    }
    return GHNotificationActionResult(success, r.code, message)
}

internal suspend fun GitHubManager.markAllNotificationsRead(context: Context): Boolean =
    markAllNotificationsReadResult(context).success

internal suspend fun GitHubManager.markRepoNotificationsReadResult(
    context: Context,
    owner: String,
    repo: String,
    lastReadAt: String? = null,
): GHNotificationActionResult {
    val body = JSONObject().apply {
        put("read", true)
        lastReadAt?.takeIf { it.isNotBlank() }?.let { put("last_read_at", it) }
    }.toString()
    val r = request(
        context,
        repoPath(owner, repo, "/notifications"),
        "PUT",
        body,
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
    )
    val success = r.code == 202 || r.code == 205 || r.code == 304
    return GHNotificationActionResult(success, r.code, if (success) "Repository notifications marked read" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.markRepoNotificationsRead(context: Context, owner: String, repo: String): Boolean =
    markRepoNotificationsReadResult(context, owner, repo).success

internal suspend fun GitHubManager.getThreadSubscriptionResult(context: Context, threadId: String): GHThreadSubscriptionResult {
    val path = notificationThreadPath(threadId, "/subscription")
        ?: return GHThreadSubscriptionResult(code = -1, error = "Invalid notification thread id")
    val r = request(context, path, extraHeaders = mapOf("Accept" to "application/vnd.github+json"), trackErrors = false)
    if (!r.success) return GHThreadSubscriptionResult(code = r.code, error = apiErrorMessage(r))
    return try {
        GHThreadSubscriptionResult(subscription = parseGHThreadSubscription(JSONObject(r.body)), code = r.code)
    } catch (e: Exception) {
        GHThreadSubscriptionResult(code = r.code, error = e.message ?: "Invalid subscription response")
    }
}

internal suspend fun GitHubManager.getThreadSubscription(context: Context, threadId: String): GHThreadSubscription {
    val result = getThreadSubscriptionResult(context, threadId)
    val value = result.subscription
    if (value != null) return value
    return GHThreadSubscription(subscribed = false, ignored = false, reason = "", createdAt = "", url = "")
}

internal suspend fun GitHubManager.setThreadSubscriptionResult(
    context: Context,
    threadId: String,
    ignored: Boolean,
): GHNotificationActionResult {
    val path = notificationThreadPath(threadId, "/subscription")
        ?: return GHNotificationActionResult(false, -1, "Invalid notification thread id")
    val body = JSONObject().put("ignored", ignored).toString()
    val r = request(context, path, "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return GHNotificationActionResult(r.success, r.code, if (r.success) if (ignored) "Thread ignored" else "Thread subscribed" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.setThreadSubscription(context: Context, threadId: String, subscribed: Boolean, ignored: Boolean): Boolean =
    if (!subscribed && !ignored) deleteThreadSubscription(context, threadId)
    else setThreadSubscriptionResult(context, threadId, ignored).success

internal suspend fun GitHubManager.deleteThreadSubscriptionResult(context: Context, threadId: String): GHNotificationActionResult {
    val path = notificationThreadPath(threadId, "/subscription")
        ?: return GHNotificationActionResult(false, -1, "Invalid notification thread id")
    val r = request(context, path, "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val success = r.code == 204 || r.code == 304
    return GHNotificationActionResult(success, r.code, if (success) "Thread subscription removed" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.deleteThreadSubscription(context: Context, threadId: String): Boolean =
    deleteThreadSubscriptionResult(context, threadId).success

internal suspend fun GitHubManager.markThreadDoneResult(context: Context, threadId: String): GHNotificationActionResult {
    val path = notificationThreadPath(threadId)
        ?: return GHNotificationActionResult(false, -1, "Invalid notification thread id")
    val r = request(context, path, "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val success = r.code == 204
    return GHNotificationActionResult(success, r.code, if (success) "Thread marked done" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.markThreadDone(context: Context, threadId: String): Boolean =
    markThreadDoneResult(context, threadId).success

internal suspend fun GitHubManager.getNotification(context: Context, threadId: String): GHNotification? {
    return getNotificationResult(context, threadId).notification
}

internal suspend fun GitHubManager.getNotificationResult(context: Context, threadId: String): GHNotificationThreadResult {
    val path = notificationThreadPath(threadId)
        ?: return GHNotificationThreadResult(code = -1, error = "Invalid notification thread id")
    val r = request(
        context,
        path,
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        trackErrors = false,
    )
    if (!r.success) return GHNotificationThreadResult(code = r.code, error = apiErrorMessage(r))
    return try {
        GHNotificationThreadResult(notification = parseGHNotification(JSONObject(r.body)), code = r.code)
    } catch (e: Exception) {
        GHNotificationThreadResult(code = r.code, error = e.message ?: "Invalid thread response")
    }
}

internal suspend fun GitHubManager.isWatching(context: Context, owner: String, repo: String): Boolean {
    return getRepositorySubscription(context, owner, repo).subscription?.subscribed == true
}

internal suspend fun GitHubManager.isWatchingRepo(context: Context, owner: String, repo: String): Boolean {
    return getRepositorySubscription(context, owner, repo).subscription?.subscribed == true
}

internal suspend fun GitHubManager.getRepositorySubscription(
    context: Context,
    owner: String,
    repo: String,
): GHRepositorySubscriptionResult {
    val r = request(
        context,
        repoPath(owner, repo, "/subscription"),
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        trackErrors = false,
    )
    if (!r.success) return GHRepositorySubscriptionResult(code = r.code, error = apiErrorMessage(r))
    return try {
        GHRepositorySubscriptionResult(subscription = parseGHRepositorySubscription(JSONObject(r.body)), code = r.code)
    } catch (e: Exception) {
        GHRepositorySubscriptionResult(code = r.code, error = e.message ?: "Invalid repository subscription response")
    }
}

internal suspend fun GitHubManager.setRepositorySubscription(
    context: Context,
    owner: String,
    repo: String,
    subscribed: Boolean,
    ignored: Boolean,
): GHNotificationActionResult {
    val body = JSONObject().apply {
        put("subscribed", subscribed)
        put("ignored", ignored)
    }.toString()
    val r = request(
        context,
        repoPath(owner, repo, "/subscription"),
        "PUT",
        body,
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
    )
    return GHNotificationActionResult(r.success, r.code, if (r.success) "Repository notification preference updated" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.deleteRepositorySubscription(
    context: Context,
    owner: String,
    repo: String,
): GHNotificationActionResult {
    val r = request(
        context,
        repoPath(owner, repo, "/subscription"),
        "DELETE",
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
    )
    val success = r.code == 204
    return GHNotificationActionResult(success, r.code, if (success) "Stopped watching repository" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.watchRepo(context: Context, owner: String, repo: String): Boolean =
    setRepositorySubscription(context, owner, repo, subscribed = true, ignored = false).success

internal suspend fun GitHubManager.unwatchRepo(context: Context, owner: String, repo: String): Boolean =
    deleteRepositorySubscription(context, owner, repo).success

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHNotification(j: JSONObject): GHNotification {
    val sub = j.optJSONObject("subject")
    val repo = j.optJSONObject("repository")
    val owner = repo?.optJSONObject("owner")
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
        url = j.optString("url", ""),
        lastReadAt = j.optString("last_read_at", "").takeIf { it.isNotBlank() && it != "null" },
        subjectUrl = subjectUrl,
        repositoryUrl = repo?.optString("url") ?: "",
        htmlUrl = htmlUrl,
        latestCommentUrl = sub?.optString("latest_comment_url", "").orEmpty().takeUnless { it == "null" }.orEmpty(),
        threadUrl = j.optString("url", ""),
        subscriptionUrl = j.optString("subscription_url", ""),
        repositoryHtmlUrl = repoHtmlUrl,
        repositoryId = repo?.optLong("id") ?: 0L,
        repositoryPrivate = repo?.optBoolean("private", false) ?: false,
        repositoryOwner = owner?.optString("login", "").orEmpty(),
        repositoryOwnerAvatarUrl = owner?.optString("avatar_url", "").orEmpty(),
        subjectNumber = notificationSubjectNumber(subjectUrl),
    )
}

internal fun parseGHThreadSubscription(j: JSONObject): GHThreadSubscription =
    GHThreadSubscription(
        subscribed = j.optBoolean("subscribed", false),
        ignored = j.optBoolean("ignored", false),
        reason = j.optString("reason", "").takeUnless { it == "null" }.orEmpty(),
        createdAt = j.optString("created_at", ""),
        url = j.optString("url", ""),
        threadUrl = j.optString("thread_url", ""),
    )

internal fun parseGHRepositorySubscription(j: JSONObject): GHRepositorySubscription =
    GHRepositorySubscription(
        subscribed = j.optBoolean("subscribed", false),
        ignored = j.optBoolean("ignored", false),
        reason = j.optString("reason", "").takeUnless { it == "null" }.orEmpty(),
        createdAt = j.optString("created_at", ""),
        url = j.optString("url", ""),
        repositoryUrl = j.optString("repository_url", ""),
    )

private fun notificationThreadPath(threadId: String, suffix: String = ""): String? {
    val normalized = threadId.trim()
    if (!normalized.matches(Regex("[0-9]+"))) return null
    return "/notifications/threads/$normalized$suffix"
}

private fun notificationSubjectNumber(subjectUrl: String): Int? =
    subjectUrl.substringAfterLast('/', "").toIntOrNull()

private fun githubApiUrlToWebUrl(apiUrl: String): String {
    if (apiUrl.isBlank()) return ""
    if (apiUrl.startsWith("https://github.com/")) return apiUrl
    return apiUrl
        .replace("https://api.github.com/repos/", "https://github.com/")
        .replace("/pulls/", "/pull/")
}
