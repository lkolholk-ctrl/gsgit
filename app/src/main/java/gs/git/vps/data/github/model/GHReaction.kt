package gs.git.vps.data.github.model

/**
 * Модель домена Reactions слоя GitHub API: реакция на issue / комментарий issue /
 * комментарий ревью PR. Вынесена из god-файла GitHubManager.kt
 * (см. docs/decomposition-log.md).
 */
data class GHReaction(
    val id: Long,
    val content: String,
    val user: String
)
