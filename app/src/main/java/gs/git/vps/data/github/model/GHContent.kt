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

/** Диапазон blame (одна строка/группа строк → коммит). Домен Contents. */
data class GHBlameRange(
    val startLine: Int,
    val endLine: Int,
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val avatarUrl: String
)

/** Результат сохранения файла через Contents API. Домен Contents. */
data class GHFileSaveResult(val success: Boolean, val sha: String, val error: String)
