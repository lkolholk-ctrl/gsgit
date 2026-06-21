package gs.git.vps.data.github.model

/**
 * Модели домена Apps слоя GitHub API (GitHub App installations). Вынесены из god-файла
 * GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHAppInstallationsPage(
    val installations: List<GHAppInstallation> = emptyList(),
    val totalCount: Int = 0,
    val error: String = "",
    val code: Int = 0
)

data class GHAppInstallationReposPage(
    val repositories: List<GHRepo> = emptyList(),
    val totalCount: Int = 0,
    val error: String = "",
    val code: Int = 0
)

data class GHAppInstallation(
    val id: Long,
    val appId: Long,
    val appSlug: String,
    val targetId: Long,
    val targetType: String,
    val targetLogin: String,
    val targetAvatarUrl: String,
    val repositorySelection: String,
    val permissions: List<Pair<String, String>>,
    val events: List<String>,
    val singleFileName: String,
    val singleFilePaths: List<String>,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val suspendedAt: String,
    val suspendedBy: String
)
