package gs.git.vps.data.github.model

/**
 * Релиз GitHub. Модель вынесена из god-файла GitHubManager.kt в рамках декомпозиции
 * data-слоя (домен Releases — эталон конвенции, см. docs/decomposition-log.md).
 */
data class GHRelease(
    val tag: String,
    val name: String,
    val body: String,
    val prerelease: Boolean,
    val createdAt: String,
    val assets: List<GHAsset>,
    val id: Long = 0L,
    val draft: Boolean = false,
    val htmlUrl: String = "",
    val uploadUrl: String = ""
)

/** Бинарный ассет релиза (вложение). */
data class GHAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val downloadCount: Int,
    val id: Long = 0L,
    val contentType: String = "",
    val state: String = ""
)
