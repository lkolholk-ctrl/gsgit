package gs.git.vps.data.github.model

/**
 * Модели домена Security слоя GitHub API: dependabot / code-scanning / secret-scanning alerts,
 * repository security advisories, community profile, security settings. Вынесены из god-файла
 * GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHDependabotAlert(
    val number: Int,
    val state: String,
    val severity: String,
    val summary: String,
    val description: String,
    val packageName: String,
    val createdAt: String,
    val ecosystem: String = "",
    val manifestPath: String = "",
    val vulnerableRequirements: String = "",
    val ghsaId: String = "",
    val cveId: String = "",
    val htmlUrl: String = "",
    val updatedAt: String = "",
    val fixedIn: List<String> = emptyList()
)

data class GHCodeScanningAlert(
    val number: Int,
    val state: String,
    val ruleId: String,
    val ruleName: String,
    val severity: String,
    val description: String,
    val toolName: String,
    val message: String,
    val path: String,
    val startLine: Int,
    val ref: String,
    val category: String,
    val createdAt: String,
    val fixedAt: String,
    val dismissedAt: String,
    val dismissedReason: String,
    val htmlUrl: String
)

data class GHSecretScanningAlert(
    val number: Int,
    val state: String,
    val resolution: String,
    val secretType: String,
    val secretTypeDisplayName: String,
    val secret: String,
    val validity: String,
    val public: Boolean,
    val pushProtectionBypassed: Boolean,
    val createdAt: String,
    val resolvedAt: String,
    val htmlUrl: String
)

data class GHRepositorySecurityAdvisory(
    val ghsaId: String,
    val cveId: String,
    val url: String,
    val htmlUrl: String,
    val summary: String,
    val description: String,
    val severity: String,
    val state: String,
    val publishedAt: String,
    val updatedAt: String,
    val withdrawnAt: String,
    val cvssScore: Double,
    val cweIds: List<String>,
    val vulnerabilities: List<GHAdvisoryVulnerability>
)

data class GHAdvisoryVulnerability(
    val ecosystem: String,
    val packageName: String,
    val vulnerableRange: String,
    val patchedVersions: String
)

data class GHRepositorySecuritySettings(
    val automatedSecurityFixes: Boolean,
    val automatedSecurityFixesPaused: Boolean,
    val vulnerabilityAlerts: Boolean,
    val privateVulnerabilityReporting: Boolean
)

data class GHCommunityProfile(
    val healthPercentage: Int,
    val description: String,
    val documentationUrl: String,
    val updatedAt: String,
    val files: List<GHCommunityProfileFile>
)

data class GHCommunityProfileFile(
    val key: String,
    val name: String,
    val htmlUrl: String,
    val present: Boolean
)
