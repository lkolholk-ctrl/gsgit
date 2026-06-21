package gs.git.vps.data.github.model

/**
 * Модели домена RepoFeatures слоя GitHub API: autolinks репозитория и codespaces пользователя.
 * Вынесены из god-файла GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHAutolink(
    val id: Long,
    val keyPrefix: String,
    val urlTemplate: String,
    val isAlphanumeric: Boolean
)

data class GHCodespace(
    val name: String,
    val displayName: String,
    val state: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val machine: String,
    val createdAt: String,
    val lastUsedAt: String,
    val idleTimeoutMinutes: Int,
    val url: String
)
