package gs.git.vps.data.github.model

/**
 * Модели домена Issues слоя GitHub API. Вынесены из god-файла GitHubManager.kt
 * в рамках декомпозиции data-слоя (домен Issues, см. docs/decomposition-log.md).
 */

/** Краткая запись issue в списке. */
data class GHIssue(val number: Int, val title: String, val state: String, val author: String,
    val createdAt: String, val comments: Int, val isPR: Boolean)

/** Событие в ленте событий issue (labeled/assigned/renamed/…). */
data class GHIssueEvent(
    val id: Long,
    val event: String,
    val actor: String,
    val createdAt: String,
    val issueNumber: Int,
    val issueTitle: String,
    val label: String,
    val assignee: String,
    val milestone: String,
    val renameFrom: String,
    val renameTo: String,
    val commitId: String,
    val url: String = "",
    val commitUrl: String = "",
    val authorAssociation: String = "",
    val stateReason: String = "",
    val performedViaGithubApp: String = ""
)

/** Полная карточка issue. */
data class GHIssueDetail(val number: Int, val title: String, val body: String, val state: String,
    val author: String, val avatarUrl: String, val createdAt: String, val comments: Int,
    val labels: List<String>, val isPR: Boolean, val assignee: String, val milestoneTitle: String = "",
    val locked: Boolean = false, val activeLockReason: String = "")

/** Комментарий к issue (также переиспользуется доменом Discussions). */
data class GHComment(val id: Long, val body: String, val author: String, val avatarUrl: String, val createdAt: String, val nodeId: String = "")

/** Метка (label) репозитория. */
data class GHLabel(val name: String, val color: String, val description: String)

/** Веха (milestone) репозитория. */
data class GHMilestone(val number: Int, val title: String, val description: String, val state: String,
    val openIssues: Int, val closedIssues: Int, val dueOn: String)

/** Событие из timeline-ленты issue. */
data class GHTimelineEvent(
    val id: Long,
    val event: String,
    val actor: String,
    val createdAt: String,
    val label: String,
    val milestone: String,
    val assignee: String,
    val source: String
)
