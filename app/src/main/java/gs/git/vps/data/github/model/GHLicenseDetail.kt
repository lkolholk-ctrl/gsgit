package gs.git.vps.data.github.model

/** Детальная лицензия (тело + метаданные). Используется getRepoLicense (Repos) и getLicense (Explore). */
data class GHLicenseDetail(
    val key: String,
    val name: String,
    val spdxId: String,
    val description: String,
    val body: String,
    val htmlUrl: String,
    val featured: Boolean
)
