package gs.git.vps.data.github.model

/** Модели Actions-инфраструктуры: deployments/environments/caches/runners/permissions. */

data class GHPendingDeployment(val environmentId: Long, val environmentName: String,
    val currentUserCanApprove: Boolean, val waitTimer: Int, val waitTimerStartedAt: String,
    val reviewers: List<String>)

data class GHDeployment(
    val id: Long, val sha: String, val ref: String, val task: String,
    val environment: String, val description: String, val createdAt: String,
    val updatedAt: String, val creator: String
)

data class GHActionsCacheUsage(val fullName: String, val activeCachesSizeInBytes: Long,
    val activeCachesCount: Int)

data class GHActionsCacheEntry(val id: Long, val ref: String, val key: String, val version: String,
    val lastAccessedAt: String, val createdAt: String, val sizeInBytes: Long)

data class GHActionRunner(val id: Long, val name: String, val os: String, val status: String,
    val busy: Boolean, val labels: List<String>)

data class GHActionRunnerGroup(
    val id: Long,
    val name: String,
    val visibility: String,
    val isDefault: Boolean,
    val inherited: Boolean,
    val allowsPublicRepositories: Boolean,
    val restrictedToWorkflows: Boolean,
    val selectedWorkflows: List<String>,
    val runnersUrl: String,
    val selectedRepositoriesUrl: String
)

data class GHRunnerToken(val token: String, val expiresAt: String)

data class GHActionsPermissions(val enabled: Boolean, val allowedActions: String,
    val selectedActionsUrl: String)

data class GHWorkflowPermissions(val defaultWorkflowPermissions: String,
    val canApprovePullRequestReviews: Boolean)

data class GHActionsRetention(val days: Int)

enum class GHActionsCapabilityState { AVAILABLE, DENIED, ERROR }

data class GHActionsCapability(
    val key: String,
    val label: String,
    val requiredPermission: String,
    val state: GHActionsCapabilityState,
    val statusCode: Int,
    val detail: String,
    val requestId: String = "",
)

data class GHEnvironment(
    val id: Long,
    val name: String,
    val url: String,
    val htmlUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val protectionRules: List<GHEnvironmentProtectionRule>,
    val deploymentBranchPolicy: GHDeploymentBranchPolicy?
)

data class GHEnvironmentProtectionRule(
    val id: Long,
    val type: String,
    val enabled: Boolean
)

data class GHDeploymentBranchPolicy(
    val protectedBranches: Boolean,
    val customBranchPolicies: Boolean
)
