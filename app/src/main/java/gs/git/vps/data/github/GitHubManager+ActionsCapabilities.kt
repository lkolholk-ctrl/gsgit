package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHActionsCapability
import gs.git.vps.data.github.model.GHActionsCapabilityState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private data class CapabilityProbe(
    val key: String,
    val label: String,
    val requiredPermission: String,
    val endpoint: String,
)

/**
 * Read-only token capability probe. GitHub does not expose PAT permissions via
 * introspection, so write access must still be confirmed by the requested
 * mutation itself. These probes never create, update, rerun, or delete data.
 */
internal suspend fun GitHubManager.getActionsCapabilities(
    context: Context,
    owner: String,
    repo: String,
): List<GHActionsCapability> = coroutineScope {
    val base = "/repos/$owner/$repo"
    listOf(
        CapabilityProbe("actions", "Actions", "Actions: read", "$base/actions/workflows?per_page=1"),
        CapabilityProbe("secrets", "Secrets", "Secrets: read", "$base/actions/secrets?per_page=1"),
        CapabilityProbe("variables", "Variables", "Variables: read", "$base/actions/variables?per_page=1"),
        CapabilityProbe("environments", "Environments", "Environments: read", "$base/environments?per_page=1"),
        CapabilityProbe("runners", "Runners", "Administration: read", "$base/actions/runners?per_page=1"),
        CapabilityProbe("settings", "Actions settings", "Administration: read", "$base/actions/permissions"),
    ).map { probe ->
        async {
            val result = request(
                context = context,
                endpoint = probe.endpoint,
                method = "GET",
                trackErrors = false,
                rateLimitRetries = 0,
                backoffRetries = 0,
            )
            val state = when {
                result.success -> GHActionsCapabilityState.AVAILABLE
                result.code in setOf(401, 403, 404) -> GHActionsCapabilityState.DENIED
                else -> GHActionsCapabilityState.ERROR
            }
            val detail = when (state) {
                GHActionsCapabilityState.AVAILABLE -> "Read access confirmed"
                GHActionsCapabilityState.DENIED -> when (result.code) {
                    401 -> "Token missing, expired, or revoked"
                    403 -> "Required permission is missing"
                    404 -> "Resource hidden or unavailable to this token"
                    else -> "Access denied"
                }
                GHActionsCapabilityState.ERROR -> apiErrorMessage(result)
            }
            GHActionsCapability(
                key = probe.key,
                label = probe.label,
                requiredPermission = probe.requiredPermission,
                state = state,
                statusCode = result.code,
                detail = detail,
                requestId = result.headers["x-github-request-id"].orEmpty(),
            )
        }
    }.awaitAll()
}
