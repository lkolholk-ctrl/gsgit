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
    val htmlUrl: String = ""
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
    val url: String
)
