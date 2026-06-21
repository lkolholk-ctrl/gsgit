package gs.git.vps.data.github.model

/**
 * Модели домена Diagnostics слоя GitHub API: сводка API-диагностики, rate-limit, статус github.com.
 * Вынесены из god-файла GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHApiDiagnostics(
    val generatedAt: Long,
    val scopes: String,
    val acceptedScopes: String,
    val rate: GHApiRateSummary,
    val checks: List<GHApiDiagnosticCheck>,
)

data class GHApiRateSummary(
    val coreLimit: Int = 0,
    val coreRemaining: Int = 0,
    val searchLimit: Int = 0,
    val searchRemaining: Int = 0,
    val graphqlLimit: Int = 0,
    val graphqlRemaining: Int = 0,
    val resetEpoch: Long = 0L,
)

data class GHRateLimitGraphQL(
    val limit: Int,
    val cost: Int,
    val remaining: Int,
    val resetAt: String,
    val nodeCount: Int
)

data class GHApiDiagnosticCheck(
    val title: String,
    val endpoint: String,
    val statusCode: Int,
    val status: String,
    val message: String,
    val hint: String,
)

data class GHStatusComponent(
    val name: String,
    val status: String
)

data class GHStatusSummary(
    val description: String,
    val indicator: String,
    val components: List<GHStatusComponent>
)
