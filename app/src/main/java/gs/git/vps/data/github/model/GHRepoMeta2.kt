package gs.git.vps.data.github.model

/**
 * Модели домена RepoMeta слоя GitHub API: метаданные github.com (/meta), лицензии-каталог,
 * контрибьюторы репозитория. Вынесены из god-файла GitHubManager.kt
 * (см. docs/decomposition-log.md). Детали лицензии — в model/GHLicenseDetail.kt (домен Repos).
 */

data class GHMeta(
    val verifiablePasswordAuthentication: Boolean,
    val sshKeys: List<String>,
    val sshKeyFingerprints: List<String>,
    val hooks: List<String>,
    val web: List<String>,
    val api: List<String>,
    val git: List<String>,
    val packages: List<String>,
    val pages: List<String>,
    val importer: List<String>,
)

data class GHLicense(
    val key: String,
    val name: String,
    val spdxId: String,
    val url: String,
    val featured: Boolean
)

data class GHContributor(val login: String, val avatarUrl: String, val contributions: Int)
