package gs.git.vps.data.github.model

/** Элемент содержимого репозитория (файл/директория). Домен Repos. */
data class GHContent(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val downloadUrl: String,
    val sha: String
)
