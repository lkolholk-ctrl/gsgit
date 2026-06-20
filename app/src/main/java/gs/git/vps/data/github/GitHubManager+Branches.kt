package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHBranchProtection
import gs.git.vps.data.github.model.GHBranchRestrictions
import gs.git.vps.data.github.model.GHRequiredPRReviews
import gs.git.vps.data.github.model.GHRequiredStatusChecks
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Branches слоя GitHub API: список веток, head-sha, создание/удаление, merge/rename,
 * защита веток (protection, required signatures). Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Сеть — через ядро `request()`, парсинг — чистые `parseGHX`.
 * Сигнатуры вызовов не менялись.
 *
 * Низкоуровневые git-refs (getGitRef/createGitRef/…) оставлены в core — домен GitData.
 */

// ─── Ветки: список / создание / удаление / merge / rename ────────────────────

internal suspend fun GitHubManager.getBranches(context: Context, owner: String, repo: String): List<String> {
    val branches = mutableListOf<String>()
    var page = 1
    while (true) {
        val r = request(context, "/repos/$owner/$repo/branches?per_page=100&page=$page")
        if (!r.success) break
        val count = try {
            val arr = JSONArray(r.body)
            for (i in 0 until arr.length()) {
                arr.getJSONObject(i).optString("name").takeIf { it.isNotBlank() }?.let { branches += it }
            }
            arr.length()
        } catch (e: Exception) {
            0
        }
        if (count < 100) break
        page++
    }
    return branches.distinct()
}

internal suspend fun GitHubManager.getBranchHeadSha(context: Context, owner: String, repo: String, branch: String): String? {
    if (branch.isBlank()) return null
    val r = request(context, "/repos/$owner/$repo/git/ref/heads/$branch")
    if (!r.success) return null
    return try {
        JSONObject(r.body).optJSONObject("object")?.optString("sha")?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

internal suspend fun GitHubManager.createBranch(context: Context, owner: String, repo: String, branchName: String, fromBranch: String): Boolean {
    val refR = request(context, "/repos/$owner/$repo/git/ref/heads/$fromBranch")
    if (!refR.success) return false
    val sha = JSONObject(refR.body).getJSONObject("object").getString("sha")
    val body = JSONObject().apply { put("ref", "refs/heads/$branchName"); put("sha", sha) }.toString()
    return request(context, "/repos/$owner/$repo/git/refs", "POST", body).success
}

internal suspend fun GitHubManager.deleteBranch(context: Context, owner: String, repo: String, branch: String): Boolean =
    request(context, "/repos/$owner/$repo/git/refs/heads/$branch", "DELETE").success

internal suspend fun GitHubManager.mergeBranch(
    context: Context,
    owner: String,
    repo: String,
    base: String,
    head: String,
    commitMessage: String? = null
): Boolean {
    val body = JSONObject().apply {
        put("base", base)
        put("head", head)
        if (!commitMessage.isNullOrBlank()) put("commit_message", commitMessage)
    }.toString()
    return request(context, "/repos/$owner/$repo/merges", "POST", body).success
}

internal suspend fun GitHubManager.renameBranch(context: Context, owner: String, repo: String, branch: String, newName: String): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    val body = JSONObject().apply { put("new_name", newName) }.toString()
    return request(context, "/repos/$owner/$repo/branches/$encodedBranch/rename", "POST", body).success
}

// ─── Защита веток ────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getBranchProtection(context: Context, owner: String, repo: String, branch: String): GHBranchProtection? {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection")
    if (!r.success) return null
    return try { parseGHBranchProtection(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getBranchRequiredSignatures(context: Context, owner: String, repo: String, branch: String): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection/required_signatures")
    if (!r.success) return false
    return try { JSONObject(r.body).optBoolean("enabled", true) } catch (e: Exception) { true }
}

internal suspend fun GitHubManager.enableBranchRequiredSignatures(context: Context, owner: String, repo: String, branch: String): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    return request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection/required_signatures", "POST", "{}").success
}

internal suspend fun GitHubManager.disableBranchRequiredSignatures(context: Context, owner: String, repo: String, branch: String): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    return request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection/required_signatures", "DELETE").let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.updateBranchProtection(
    context: Context, owner: String, repo: String, branch: String,
    requiredStatusChecks: GHRequiredStatusChecks? = null,
    requiredPRReviews: GHRequiredPRReviews? = null,
    restrictions: GHBranchRestrictions? = null,
    allowForcePushes: Boolean? = null,
    allowDeletions: Boolean? = null,
    requiredConversationResolution: Boolean? = null,
    enforceAdmins: Boolean? = null,
    requiredLinearHistory: Boolean? = null,
    blockCreations: Boolean? = null,
    lockBranch: Boolean? = null,
    requiredDeployments: List<String>? = null
): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    val body = JSONObject().apply {
        if (requiredStatusChecks != null) {
            put("required_status_checks", JSONObject().apply {
                put("strict", requiredStatusChecks.strict)
                put("contexts", JSONArray(requiredStatusChecks.contexts))
            })
        } else {
            put("required_status_checks", JSONObject.NULL)
        }
        if (requiredPRReviews != null) {
            put("required_pull_request_reviews", JSONObject().apply {
                put("required_approving_review_count", requiredPRReviews.requiredApprovingReviewCount)
                put("dismiss_stale_reviews", requiredPRReviews.dismissStaleReviews)
                put("require_code_owner_reviews", requiredPRReviews.requireCodeOwnerReviews)
            })
        } else {
            put("required_pull_request_reviews", JSONObject.NULL)
        }
        if (restrictions != null) {
            put("restrictions", JSONObject().apply {
                put("users", JSONArray(restrictions.users))
                put("teams", JSONArray(restrictions.teams))
            })
        } else {
            put("restrictions", JSONObject.NULL)
        }
        if (allowForcePushes != null) put("allow_force_pushes", JSONObject().apply { put("enabled", allowForcePushes) })
        if (allowDeletions != null) put("allow_deletions", JSONObject().apply { put("enabled", allowDeletions) })
        if (requiredConversationResolution != null) put("required_conversation_resolution", JSONObject().apply { put("enabled", requiredConversationResolution) })
        if (enforceAdmins != null) put("enforce_admins", JSONObject().apply { put("enabled", enforceAdmins) })
        if (requiredLinearHistory != null) put("required_linear_history", JSONObject().apply { put("enabled", requiredLinearHistory) })
        if (blockCreations != null) put("block_creations", JSONObject().apply { put("enabled", blockCreations) })
        if (lockBranch != null) put("lock_branch", JSONObject().apply { put("enabled", lockBranch) })
        if (requiredDeployments != null) put("required_deployments", JSONObject().apply { put("environments", JSONArray(requiredDeployments)) })
    }.toString()
    return request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection", "PUT", body).success
}

internal suspend fun GitHubManager.deleteBranchProtection(context: Context, owner: String, repo: String, branch: String): Boolean {
    val encodedBranch = URLEncoder.encode(branch, "UTF-8")
    return request(context, "/repos/$owner/$repo/branches/$encodedBranch/protection", "DELETE").let { it.code == 204 || it.success }
}

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun parseGHBranchProtection(j: JSONObject): GHBranchProtection {
    val requiredStatusChecks = j.optJSONObject("required_status_checks")
    val requiredPRReviews = j.optJSONObject("required_pull_request_reviews")
    val restrictions = j.optJSONObject("restrictions")
    return GHBranchProtection(
        enabled = true,
        requiredStatusChecks = if (requiredStatusChecks != null) GHRequiredStatusChecks(
            strict = requiredStatusChecks.optBoolean("strict", false),
            contexts = mutableListOf<String>().apply {
                val arr = requiredStatusChecks.optJSONArray("contexts")
                if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
            }
        ) else null,
        requiredPRReviews = if (requiredPRReviews != null) GHRequiredPRReviews(
            requiredApprovingReviewCount = requiredPRReviews.optInt("required_approving_review_count", 1),
            dismissStaleReviews = requiredPRReviews.optBoolean("dismiss_stale_reviews", false),
            requireCodeOwnerReviews = requiredPRReviews.optBoolean("require_code_owner_reviews", false)
        ) else null,
        restrictions = if (restrictions != null) GHBranchRestrictions(
            users = mutableListOf<String>().apply {
                val arr = restrictions.optJSONArray("users")
                if (arr != null) for (i in 0 until arr.length()) add(arr.getJSONObject(i).optString("login"))
            },
            teams = mutableListOf<String>().apply {
                val arr = restrictions.optJSONArray("teams")
                if (arr != null) for (i in 0 until arr.length()) add(arr.getJSONObject(i).optString("slug"))
            }
        ) else null,
        allowForcePushes = j.optJSONObject("allow_force_pushes")?.optBoolean("enabled") ?: true,
        allowDeletions = j.optJSONObject("allow_deletions")?.optBoolean("enabled") ?: true,
        requiredConversationResolution = j.optJSONObject("required_conversation_resolution")?.optBoolean("enabled") ?: false,
        enforceAdmins = j.optJSONObject("enforce_admins")?.optBoolean("enabled") ?: false,
        requiredSignatures = j.optJSONObject("required_signatures")?.optBoolean("enabled") ?: false,
        requiredLinearHistory = j.optJSONObject("required_linear_history")?.optBoolean("enabled") ?: false,
        blockCreations = j.optJSONObject("block_creations")?.optBoolean("enabled") ?: false,
        lockBranch = j.optJSONObject("lock_branch")?.optBoolean("enabled") ?: false,
        requiredDeployments = mutableListOf<String>().apply {
            val rd = j.optJSONObject("required_deployments")
            val arr = rd?.optJSONArray("environments")
            if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
        }
    )
}
