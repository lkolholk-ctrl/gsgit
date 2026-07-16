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

/** Public metadata returned by GET /apps/{app_slug}. This proves app identity,
 * owner and declared capabilities; it does not prove an installation. */
data class GHAppMetadata(
    val id: Long,
    val slug: String,
    val name: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String,
    val externalUrl: String,
    val htmlUrl: String,
    val permissions: List<Pair<String, String>>,
    val events: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

data class GHAppMetadataResult(
    val app: GHAppMetadata? = null,
    val error: String = "",
    val code: Int = 0,
)

/** A GitHub App observed in real check-runs on recent repository commits. */
data class GHObservedApp(
    val app: GHAppMetadata,
    val checkRunCount: Int,
    val checkNames: List<String>,
    val lastStatus: String,
    val lastConclusion: String,
    val lastSeenAt: String,
    val lastCommitSha: String,
)

data class GHWorkflowAppEvidence(
    val workflowId: Long,
    val name: String,
    val path: String,
    val state: String,
    val actionReferences: List<String>,
    val providerIds: List<String>,
)

/** Evidence is deliberately split by endpoint so an empty result is never
 * confused with a denied request. */
data class GHRepoAppsEvidence(
    val repoFullName: String,
    val branch: String,
    val commitsScanned: Int,
    val observedApps: List<GHObservedApp>,
    val workflowsTotal: Int,
    val workflowEvidence: List<GHWorkflowAppEvidence>,
    val providerSecretNames: List<String>,
    val checksCode: Int,
    val workflowsCode: Int,
    val secretsCode: Int,
    val checksError: String = "",
    val workflowsError: String = "",
    val secretsError: String = "",
)
