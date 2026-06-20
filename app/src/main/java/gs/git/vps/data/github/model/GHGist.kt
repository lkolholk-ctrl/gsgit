package gs.git.vps.data.github.model

/**
 * Gist GitHub. Модель вынесена из god-файла GitHubManager.kt в рамках декомпозиции
 * data-слоя (домен Gists, см. docs/decomposition-log.md).
 */
data class GHGist(
    val id: String,
    val description: String,
    val isPublic: Boolean,
    val files: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val owner: String = "",
    val comments: Int = 0,
    val htmlUrl: String = ""
)

/** Комментарий к gist. */
data class GHGistComment(val id: Long, val body: String, val user: String, val createdAt: String)
