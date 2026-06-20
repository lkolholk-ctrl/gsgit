package gs.git.vps.data.github.model

/**
 * Модель репозитория GitHub. Вынесена из god-файла `GitHubManager.kt` при нарезке домена
 * Repos (см. docs/decomposition-log.md). `permissions` присутствует только в ответах,
 * аутентифицированных пользователем; для поиска/анонимных эндпоинтов — null.
 */
data class GHRepo(
    val name: String,
    val fullName: String,
    val description: String,
    val language: String,
    val stars: Int,
    val forks: Int,
    val isPrivate: Boolean,
    val isFork: Boolean,
    val defaultBranch: String,
    val updatedAt: String,
    val owner: String,
    val htmlUrl: String = "",
    val isArchived: Boolean = false,
    val isTemplate: Boolean = false,
    val id: Long = 0L,
    val permissions: GHPermissions? = null
)

/**
 * Зеркало объекта `permissions` из GitHub REST API для репозитория.
 * Присутствует только в ответах, аутентифицированных пользователем; null для анонимных /
 * search-результатов / старых эндпоинтов.
 */
data class GHPermissions(
    val admin: Boolean = false,
    val maintain: Boolean = false,
    val push: Boolean = false,
    val triage: Boolean = false,
    val pull: Boolean = false
)

data class GHRepoCreateResult(
    val success: Boolean,
    val repo: GHRepo?,
    val cloneUrl: String = "",
    val sshUrl: String = "",
    val autoInit: Boolean = true
)
