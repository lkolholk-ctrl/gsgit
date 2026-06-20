package gs.git.vps.data.github.model

/**
 * Модели домена Discussions слоя GitHub API (GraphQL). Вынесены из god-файла GitHubManager.kt
 * (см. docs/decomposition-log.md). Комментарии обсуждений переиспользуют GHComment (домен Issues).
 */

data class GHDiscussion(
    val id: String = "",
    val number: Int,
    val title: String,
    val body: String,
    val author: String,
    val avatarUrl: String = "",
    val state: String,
    val comments: Int,
    val createdAt: String,
    val updatedAt: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val categoryEmoji: String = "",
    val isAnswerable: Boolean = false,
    val isAnswered: Boolean = false,
    val locked: Boolean = false,
    val upvotes: Int = 0,
    val viewerHasUpvoted: Boolean = false,
    val htmlUrl: String = ""
)

data class GHDiscussionCategory(
    val id: String,
    val name: String,
    val slug: String,
    val emoji: String,
    val description: String,
    val isAnswerable: Boolean
)
