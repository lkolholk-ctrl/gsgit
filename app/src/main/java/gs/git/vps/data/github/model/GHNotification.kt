package gs.git.vps.data.github.model

/**
 * Уведомление GitHub (notification thread). Модели вынесены из god-файла GitHubManager.kt
 * в рамках декомпозиции data-слоя (домен Notifications, см. docs/decomposition-log.md).
 */
data class GHNotification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    val title: String,
    val type: String,
    val repoName: String,
    val updatedAt: String,
    val url: String,
    val lastReadAt: String? = null,
    val subjectUrl: String = "",
    val repositoryUrl: String = "",
    val htmlUrl: String = "",
    val latestCommentUrl: String = "",
    val threadUrl: String = "",
    val subscriptionUrl: String = "",
    val repositoryHtmlUrl: String = "",
    val repositoryId: Long = 0L,
    val repositoryPrivate: Boolean = false,
    val repositoryOwner: String = "",
    val repositoryOwnerAvatarUrl: String = "",
    val subjectNumber: Int? = null,
) {
    /** Alias for spec parity. */
    val subjectTitle: String get() = title
    val subjectType: String get() = type
    val repositoryFullName: String get() = repoName
}

/** Подписка на тред уведомлений. */
data class GHThreadSubscription(
    val subscribed: Boolean,
    val ignored: Boolean,
    val reason: String,
    val createdAt: String,
    val url: String,
    val threadUrl: String = "",
)

/** One page returned by the global or repository Notifications endpoint. */
data class GHNotificationsPage(
    val notifications: List<GHNotification> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val nextPage: Int? = null,
    val code: Int = 0,
    val error: String = "",
    val pollIntervalSeconds: Int = 60,
    val lastModified: String = "",
    val notModified: Boolean = false,
    val repositoryFullName: String = "",
)

data class GHNotificationActionResult(
    val success: Boolean,
    val code: Int,
    val message: String,
)

data class GHNotificationThreadResult(
    val notification: GHNotification? = null,
    val code: Int = 0,
    val error: String = "",
)

data class GHThreadSubscriptionResult(
    val subscription: GHThreadSubscription? = null,
    val code: Int = 0,
    val error: String = "",
)

data class GHRepositorySubscription(
    val subscribed: Boolean,
    val ignored: Boolean,
    val reason: String,
    val createdAt: String,
    val url: String,
    val repositoryUrl: String,
)

data class GHRepositorySubscriptionResult(
    val subscription: GHRepositorySubscription? = null,
    val code: Int = 0,
    val error: String = "",
)
