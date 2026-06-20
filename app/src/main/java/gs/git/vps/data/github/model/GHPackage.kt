package gs.git.vps.data.github.model

/**
 * Модели домена Packages слоя GitHub API (GitHub Packages). Вынесены из god-файла
 * GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHPackage(
    val id: Long,
    val name: String,
    val packageType: String,
    val visibility: String,
    val versionCount: Int,
    val url: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val ownerLogin: String,
    val repositoryName: String,
    val repositoryUrl: String
)

data class GHPackageVersion(
    val id: Long,
    val name: String,
    val url: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<String>,
    val packageType: String,
    val downloadCount: Int
)
