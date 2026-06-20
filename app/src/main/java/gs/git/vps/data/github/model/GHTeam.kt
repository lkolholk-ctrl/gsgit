package gs.git.vps.data.github.model

/**
 * Модели домена Teams слоя GitHub API: команды репозитория/организации, обсуждения команд.
 * Вынесены из god-файла GitHubManager.kt в рамках декомпозиции data-слоя
 * (см. docs/decomposition-log.md).
 *
 * GHCollaborator (возвращает getTeamMembers) ОСТАВЛЕН в core — шарится с репо-доступом.
 */

/** Команда, имеющая доступ к репозиторию. */
data class GHRepoTeam(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String,
    val privacy: String,
    val permission: String,
    val membersCount: Int,
    val reposCount: Int,
    val htmlUrl: String,
    val organization: String
)

/** Команда организации. */
data class GHOrgTeam(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String,
    val privacy: String,
    val permission: String,
    val membersCount: Int,
    val reposCount: Int,
    val htmlUrl: String
)

/** Обсуждение внутри команды. */
data class GHTeamDiscussion(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String,
    val author: String,
    val avatarUrl: String,
    val createdAt: String,
    val commentsCount: Int,
    val htmlUrl: String
)
