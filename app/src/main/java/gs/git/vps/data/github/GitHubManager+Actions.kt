package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHActionRunner
import gs.git.vps.data.github.model.GHActionRunnerGroup
import gs.git.vps.data.github.model.GHActionsCacheEntry
import gs.git.vps.data.github.model.GHActionsCacheUsage
import gs.git.vps.data.github.model.GHActionsPermissions
import gs.git.vps.data.github.model.GHActionsRetention
import gs.git.vps.data.github.model.GHDeployment
import gs.git.vps.data.github.model.GHDeploymentBranchPolicy
import gs.git.vps.data.github.model.GHEnvironment
import gs.git.vps.data.github.model.GHEnvironmentProtectionRule
import gs.git.vps.data.github.model.GHPendingDeployment
import gs.git.vps.data.github.model.GHRunnerToken
import gs.git.vps.data.github.model.GHWorkflowPermissions
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Actions-инфраструктуры — deployments, environments, caches, self-hosted runners,
 * actions-permissions/retention. Нарезан по эталону (см. docs/decomposition-log.md).
 * Сеть — через ядро GitHubManager.request(); парсинг — чистые parse*-функции.
 */

private const val ACTIONS_TAG = "GH"

private fun parseActionRunner(j: JSONObject): GHActionRunner {
    val labels = j.optJSONArray("labels")?.let { labelArr ->
        (0 until labelArr.length()).mapNotNull { idx -> labelArr.optJSONObject(idx)?.optString("name") }
    } ?: emptyList()
    return GHActionRunner(
        id = j.optLong("id"),
        name = j.optString("name"),
        os = j.optString("os"),
        status = j.optString("status"),
        busy = j.optBoolean("busy", false),
        labels = labels
    )
}

internal suspend fun GitHubManager.getPendingDeployments(context: Context, owner: String, repo: String, runId: Long): List<GHPendingDeployment> {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/pending_deployments")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val env = j.optJSONObject("environment")
            GHPendingDeployment(
                environmentId = env?.optLong("id") ?: 0L,
                environmentName = env?.optString("name") ?: "",
                currentUserCanApprove = j.optBoolean("current_user_can_approve", false),
                waitTimer = j.optInt("wait_timer", 0),
                waitTimerStartedAt = j.optString("wait_timer_started_at", ""),
                reviewers = j.optJSONArray("reviewers")?.let { reviewers ->
                    (0 until reviewers.length()).mapNotNull { idx ->
                        reviewers.optJSONObject(idx)?.optJSONObject("reviewer")?.optString("login")
                    }
                } ?: emptyList()
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.reviewPendingDeployments(context: Context, owner: String, repo: String, runId: Long, environmentIds: List<Long>, approve: Boolean, comment: String): Boolean {
    val body = JSONObject().apply {
        put("environment_ids", JSONArray(environmentIds))
        put("state", if (approve) "approved" else "rejected")
        put("comment", comment)
    }.toString()
    return request(context, "/repos/$owner/$repo/actions/runs/$runId/pending_deployments", "POST", body).success
}

internal suspend fun GitHubManager.getDeployments(context: Context, owner: String, repo: String, environment: String? = null, ref: String? = null, page: Int = 1): List<GHDeployment> {
    val params = mutableListOf("per_page=30", "page=$page")
    environment?.let { params.add("environment=${URLEncoder.encode(it, "UTF-8")}") }
    ref?.let { params.add("ref=${URLEncoder.encode(it, "UTF-8")}") }
    val r = request(context, "${repoPath(owner, repo, "/deployments")}?${params.joinToString("&")}")
    if (!r.success) return emptyList()
    val deployments = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHDeployment(
                id = j.optLong("id"), sha = j.optString("sha"), ref = j.optString("ref"),
                task = j.optString("task"), environment = j.optString("environment"),
                description = j.optString("description", ""), createdAt = j.optString("created_at", ""),
                updatedAt = j.optString("updated_at", ""), creator = j.optJSONObject("creator")?.optString("login") ?: ""
            )
        }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return deployments
    return deployments + getDeployments(context, owner, repo, environment, ref, nextPage)
}

internal suspend fun GitHubManager.createDeployment(context: Context, owner: String, repo: String, ref: String, environment: String = "production", description: String = "", payload: String = ""): GHDeployment? {
    val body = JSONObject().apply {
        put("ref", ref)
        put("environment", environment)
        if (description.isNotBlank()) put("description", description)
        if (payload.isNotBlank()) put("payload", JSONObject(payload))
        put("auto_merge", false)
    }.toString()
    val r = request(context, "${repoPath(owner, repo, "/deployments")}", "POST", body)
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHDeployment(j.optLong("id"), j.optString("sha"), j.optString("ref"), j.optString("task"),
            j.optString("environment"), j.optString("description", ""), j.optString("created_at", ""),
            j.optString("updated_at", ""), j.optJSONObject("creator")?.optString("login") ?: "")
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createDeploymentStatus(context: Context, owner: String, repo: String, deploymentId: Long, state: String, description: String = "", environmentUrl: String = ""): Boolean {
    val body = JSONObject().apply {
        put("state", state)
        if (description.isNotBlank()) put("description", description)
        if (environmentUrl.isNotBlank()) put("environment_url", environmentUrl)
    }.toString()
    val r = request(context, "${repoPath(owner, repo, "/deployments/$deploymentId/statuses")}", "POST", body)
    return r.success
}

internal suspend fun GitHubManager.getActionsCacheUsage(context: Context, owner: String, repo: String): GHActionsCacheUsage? {
    val r = request(context, "/repos/$owner/$repo/actions/cache/usage")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHActionsCacheUsage(j.optString("full_name", ""), j.optLong("active_caches_size_in_bytes", 0), j.optInt("active_caches_count", 0))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getActionsCaches(context: Context, owner: String, repo: String, page: Int = 1, key: String? = null, ref: String? = null): List<GHActionsCacheEntry> {
    val params = mutableListOf("per_page=100", "page=$page")
    key?.takeIf { it.isNotBlank() }?.let { params += "key=${URLEncoder.encode(it, "UTF-8")}" }
    ref?.takeIf { it.isNotBlank() }?.let { params += "ref=${URLEncoder.encode(it, "UTF-8")}" }
    val r = request(context, "/repos/$owner/$repo/actions/caches?${params.joinToString("&")}")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("actions_caches") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHActionsCacheEntry(
                id = j.optLong("id"),
                ref = j.optString("ref"),
                key = j.optString("key"),
                version = j.optString("version"),
                lastAccessedAt = j.optString("last_accessed_at"),
                createdAt = j.optString("created_at"),
                sizeInBytes = j.optLong("size_in_bytes", 0)
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.deleteActionsCache(context: Context, owner: String, repo: String, cacheId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/caches/$cacheId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.deleteActionsCacheByKey(context: Context, owner: String, repo: String, key: String): Boolean {
    val r = request(context, "/repos/$owner/$repo/actions/caches?key=${URLEncoder.encode(key, "UTF-8")}", "DELETE")
    return r.success || r.code == 204
}

// ═══════════════════════════════════
// Environments

internal suspend fun GitHubManager.getEnvironments(context: Context, owner: String, repo: String): List<GHEnvironment> {
    val r = request(context, "/repos/$owner/$repo/environments?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("environments") ?: JSONArray()
        (0 until arr.length()).map { i -> parseEnvironment(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getEnvironment(context: Context, owner: String, repo: String, envName: String): GHEnvironment? {
    val encoded = URLEncoder.encode(envName, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/environments/$encoded")
    if (!r.success) return null
    return try { parseEnvironment(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createOrUpdateEnvironment(context: Context, owner: String, repo: String, envName: String, waitTimer: Int = 0, reviewers: List<Long> = emptyList(), deploymentBranchPolicy: String? = null): GHEnvironment? {
    val encoded = URLEncoder.encode(envName, "UTF-8")
    val body = JSONObject().apply {
        if (waitTimer > 0) put("wait_timer", waitTimer)
        if (reviewers.isNotEmpty()) {
            val arr = JSONArray()
            reviewers.forEach { arr.put(JSONObject().put("id", it).put("type", "User")) }
            put("reviewers", arr)
        }
        if (deploymentBranchPolicy != null) {
            put("deployment_branch_policy", JSONObject().put("protected_branches", deploymentBranchPolicy == "protected").put("custom_branch_policies", deploymentBranchPolicy == "custom"))
        }
    }.toString()
    val r = request(context, "/repos/$owner/$repo/environments/$encoded", "PUT", body)
    if (!r.success) return null
    return try { parseEnvironment(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deleteEnvironment(context: Context, owner: String, repo: String, envName: String): Boolean {
    val encoded = URLEncoder.encode(envName, "UTF-8")
    return request(context, "/repos/$owner/$repo/environments/$encoded", "DELETE").let { it.code == 204 || it.success }
}

private fun parseEnvironment(j: JSONObject): GHEnvironment {
    val rules = j.optJSONArray("protection_rules") ?: JSONArray()
    val protectionRules = (0 until rules.length()).mapNotNull { i ->
        try {
            val r = rules.getJSONObject(i)
            GHEnvironmentProtectionRule(r.optLong("id", 0), r.optString("type", ""), r.optBoolean("enabled", false))
        } catch (e: Exception) { null }
    }
    val dbp = j.optJSONObject("deployment_branch_policy")
    val deploymentBranchPolicy = if (dbp != null) GHDeploymentBranchPolicy(dbp.optBoolean("protected_branches", false), dbp.optBoolean("custom_branch_policies", false)) else null
    return GHEnvironment(
        id = j.optLong("id", 0),
        name = j.optString("name", ""),
        url = j.optString("url", ""),
        htmlUrl = j.optString("html_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        protectionRules = protectionRules,
        deploymentBranchPolicy = deploymentBranchPolicy
    )
}

internal suspend fun GitHubManager.getRepoSelfHostedRunners(context: Context, owner: String, repo: String): List<GHActionRunner> {
    val r = request(context, "/repos/$owner/$repo/actions/runners?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("runners") ?: JSONArray()
        (0 until arr.length()).map { i -> parseActionRunner(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getOrgRunnerGroups(context: Context, org: String): List<GHActionRunnerGroup> {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank()) return emptyList()
    val r = request(context, "/orgs/$cleanOrg/actions/runner-groups?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("runner_groups") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val workflows = j.optJSONArray("selected_workflows")?.let { wf ->
                (0 until wf.length()).mapNotNull { idx -> wf.optString(idx).takeIf { it.isNotBlank() } }
            } ?: emptyList()
            GHActionRunnerGroup(
                id = j.optLong("id"),
                name = j.optString("name"),
                visibility = j.optString("visibility"),
                isDefault = j.optBoolean("default", false),
                inherited = j.optBoolean("inherited", false),
                allowsPublicRepositories = j.optBoolean("allows_public_repositories", false),
                restrictedToWorkflows = j.optBoolean("restricted_to_workflows", false),
                selectedWorkflows = workflows,
                runnersUrl = j.optString("runners_url", ""),
                selectedRepositoriesUrl = j.optString("selected_repositories_url", "")
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getOrgRunnerGroupRunners(context: Context, org: String, groupId: Long): List<GHActionRunner> {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank() || groupId <= 0L) return emptyList()
    val r = request(context, "/orgs/$cleanOrg/actions/runner-groups/$groupId/runners?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("runners") ?: JSONArray()
        (0 until arr.length()).map { i -> parseActionRunner(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getEnterpriseRunners(context: Context, enterprise: String): List<GHActionRunner> {
    val cleanEnterprise = enterprise.trim()
    if (cleanEnterprise.isBlank()) return emptyList()
    val r = request(context, "/enterprises/$cleanEnterprise/actions/runners?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("runners") ?: JSONArray()
        (0 until arr.length()).map { i -> parseActionRunner(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.deleteRepoSelfHostedRunner(context: Context, owner: String, repo: String, runnerId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/runners/$runnerId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.createRepoRunnerRegistrationToken(context: Context, owner: String, repo: String): GHRunnerToken? {
    val r = request(context, "/repos/$owner/$repo/actions/runners/registration-token", "POST", "{}")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHRunnerToken(j.optString("token"), j.optString("expires_at", ""))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createRepoRunnerRemoveToken(context: Context, owner: String, repo: String): GHRunnerToken? {
    val r = request(context, "/repos/$owner/$repo/actions/runners/remove-token", "POST", "{}")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHRunnerToken(j.optString("token"), j.optString("expires_at", ""))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getRepoActionsPermissions(context: Context, owner: String, repo: String): GHActionsPermissions? {
    val r = request(context, "/repos/$owner/$repo/actions/permissions")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHActionsPermissions(
            enabled = j.optBoolean("enabled", false),
            allowedActions = j.optString("allowed_actions", ""),
            selectedActionsUrl = j.optString("selected_actions_url", "")
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.setRepoActionsPermissions(context: Context, owner: String, repo: String, enabled: Boolean, allowedActions: String): Boolean {
    val body = JSONObject().apply {
        put("enabled", enabled)
        if (allowedActions.isNotBlank()) put("allowed_actions", allowedActions)
    }.toString()
    return request(context, "/repos/$owner/$repo/actions/permissions", "PUT", body).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.getRepoActionsWorkflowPermissions(context: Context, owner: String, repo: String): GHWorkflowPermissions? {
    val r = request(context, "/repos/$owner/$repo/actions/permissions/workflow")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHWorkflowPermissions(j.optString("default_workflow_permissions", ""), j.optBoolean("can_approve_pull_request_reviews", false))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.setRepoActionsWorkflowPermissions(context: Context, owner: String, repo: String, defaultWorkflowPermissions: String, canApprovePullRequestReviews: Boolean): Boolean {
    val body = JSONObject().apply {
        put("default_workflow_permissions", defaultWorkflowPermissions)
        put("can_approve_pull_request_reviews", canApprovePullRequestReviews)
    }.toString()
    return request(context, "/repos/$owner/$repo/actions/permissions/workflow", "PUT", body).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.getRepoActionsRetention(context: Context, owner: String, repo: String): GHActionsRetention? {
    val r = request(context, "/repos/$owner/$repo/actions/permissions/artifact-and-log-retention")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHActionsRetention(j.optInt("days", 0))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.setRepoActionsRetention(context: Context, owner: String, repo: String, days: Int): Boolean {
    val body = JSONObject().apply { put("days", days) }.toString()
    return request(context, "/repos/$owner/$repo/actions/permissions/artifact-and-log-retention", "PUT", body).let { it.code == 204 || it.success }
}
