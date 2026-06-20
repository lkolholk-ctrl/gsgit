package gs.git.vps.data.github.model

/**
 * Модель домена Collaborators слоя GitHub API (участник с доступом к репозиторию;
 * также возвращается getTeamMembers домена Teams). Вынесена из god-файла GitHubManager.kt
 * (см. docs/decomposition-log.md).
 */
data class GHCollaborator(
    val login: String,
    val avatarUrl: String,
    val role: String
)
