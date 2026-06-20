package gs.git.vps.data.github.model

/**
 * Модели домена Branches слоя GitHub API: защита веток и её составляющие. Вынесены из god-файла
 * GitHubManager.kt в рамках декомпозиции data-слоя (см. docs/decomposition-log.md).
 */

data class GHBranchProtection(
    val enabled: Boolean,
    val requiredStatusChecks: GHRequiredStatusChecks?,
    val requiredPRReviews: GHRequiredPRReviews?,
    val restrictions: GHBranchRestrictions?,
    val allowForcePushes: Boolean,
    val allowDeletions: Boolean,
    val requiredConversationResolution: Boolean,
    val enforceAdmins: Boolean,
    val requiredSignatures: Boolean = false,
    val requiredLinearHistory: Boolean = false,
    val blockCreations: Boolean = false,
    val lockBranch: Boolean = false,
    val requiredDeployments: List<String> = emptyList()
)

data class GHRequiredStatusChecks(
    val strict: Boolean,
    val contexts: List<String>
)

data class GHRequiredPRReviews(
    val requiredApprovingReviewCount: Int,
    val dismissStaleReviews: Boolean,
    val requireCodeOwnerReviews: Boolean
)

data class GHBranchRestrictions(
    val users: List<String>,
    val teams: List<String>
)
