package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHCheckSuite
import gs.git.vps.data.github.model.GHPullFile
import gs.git.vps.data.github.model.GHPullMergeStatus
import gs.git.vps.data.github.model.GHPullRequest
import gs.git.vps.data.github.model.GHPullReview
import gs.git.vps.data.github.model.GHReviewComment
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен PullRequests+Reviews+Checks слоя GitHub API: pull requests, merge, ревью,
 * запрошенные ревьюеры, файлы PR, комментарии ревью, check-runs/check-suites.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Вся сеть — через ядро
 * `GitHubManager.request()`. Парсинг — чистые `parseGHX`-функции без IO. Сигнатуры публичных
 * вызовов (`GitHubManager.getPullRequests(...)` и т.п.) не изменились при выносе.
 *
 * НЕ здесь (сознательно):
 *  - реакции на review-комментарии (getPullRequestReviewCommentReaction*) — домен Reactions,
 *    режется целиком вместе с issue-реакциями (общий GHReaction и DELETE /reactions/{id});
 *  - getCommitComments/createCommitComment (тоже возвращают GHReviewComment) — домен Commits;
 *  - compareCommits — домен Commits.
 */

// ─── Pull requests ───────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getPullRequests(context: Context, owner: String, repo: String, state: String = "open", page: Int = 1): List<GHPullRequest> {
    val r = request(context, "/repos/$owner/$repo/pulls?state=$state&per_page=30&page=$page")
    if (!r.success) return emptyList()
    val prs = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHPullRequest(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return prs
    return prs + getPullRequests(context, owner, repo, state, nextPage)
}

internal suspend fun GitHubManager.getPullRequestDetail(context: Context, owner: String, repo: String, number: Int): GHPullRequest? {
    val r = request(context, "/repos/$owner/$repo/pulls/$number")
    if (!r.success) return null
    return try { parseGHPullRequest(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createPullRequest(
    context: Context, owner: String, repo: String,
    title: String, body: String, head: String, base: String
): Boolean {
    val json = JSONObject().apply {
        put("title", title); put("body", body); put("head", head); put("base", base)
    }.toString()
    return request(context, "/repos/$owner/$repo/pulls", "POST", json).success
}

internal suspend fun GitHubManager.updatePullRequest(context: Context, owner: String, repo: String, number: Int, title: String? = null, body: String? = null, base: String? = null, state: String? = null): Boolean {
    val json = JSONObject().apply {
        title?.let { put("title", it) }
        body?.let { put("body", it) }
        base?.takeIf { it.isNotBlank() }?.let { put("base", it) }
        state?.takeIf { it in listOf("open", "closed") }?.let { put("state", it) }
    }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$number", "PATCH", json).success
}

internal suspend fun GitHubManager.mergePullRequest(context: Context, owner: String, repo: String, number: Int, message: String = "", method: String = "merge", title: String = ""): Boolean {
    val body = JSONObject().apply {
        if (title.isNotBlank()) put("commit_title", title)
        if (message.isNotBlank()) put("commit_message", message)
        put("merge_method", method)
    }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$number/merge", "PUT", body).success
}

internal suspend fun GitHubManager.updatePullRequestBranch(context: Context, owner: String, repo: String, number: Int, expectedHeadSha: String? = null): Boolean {
    val body = JSONObject().apply {
        if (expectedHeadSha != null) put("expected_head_sha", expectedHeadSha)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/pulls/$number/update-branch", "PUT", body)
    return r.success || r.code == 204
}

internal suspend fun GitHubManager.getPullRequestMergedStatus(context: Context, owner: String, repo: String, number: Int): GHPullMergeStatus {
    val r = request(context, "/repos/$owner/$repo/pulls/$number/merge")
    return when (r.code) {
        204 -> GHPullMergeStatus(merged = true, checked = true, code = r.code, message = "merged")
        404 -> GHPullMergeStatus(merged = false, checked = true, code = r.code, message = "not merged")
        else -> GHPullMergeStatus(merged = false, checked = false, code = r.code, message = r.body.take(180))
    }
}

internal suspend fun GitHubManager.getPullRequestFiles(context: Context, owner: String, repo: String, number: Int): List<GHPullFile> {
    val r = request(context, "/repos/$owner/$repo/pulls/$number/files?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHPullFile(arr.getJSONObject(i)) }
    } catch (_: Exception) { emptyList() }
}

// ─── Reviews ─────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getPullRequestReviews(context: Context, owner: String, repo: String, number: Int): List<GHPullReview> {
    val r = request(context, "/repos/$owner/$repo/pulls/$number/reviews?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHPullReview(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getPullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long): GHPullReview? {
    val r = request(context, "/repos/$owner/$repo/pulls/$number/reviews/$reviewId")
    if (!r.success) return null
    return try { parseGHPullReview(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updatePullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long, body: String): GHPullReview? {
    val json = JSONObject().apply { put("body", body) }.toString()
    val r = request(context, "/repos/$owner/$repo/pulls/$number/reviews/$reviewId", "PUT", json)
    if (!r.success) return null
    return try { parseGHPullReview(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deletePullRequestReview(context: Context, owner: String, repo: String, number: Int, reviewId: Long): Boolean =
    request(context, "/repos/$owner/$repo/pulls/$number/reviews/$reviewId", "DELETE").let { it.code == 200 || it.code == 204 || it.success }

internal suspend fun GitHubManager.submitPullRequestReview(context: Context, owner: String, repo: String, number: Int, event: String, body: String = ""): Boolean {
    val json = JSONObject().apply { put("event", event); if (body.isNotBlank()) put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$number/reviews", "POST", json).success
}

internal suspend fun GitHubManager.requestPullRequestReviewers(context: Context, owner: String, repo: String, number: Int, reviewers: List<String>): Boolean {
    val clean = reviewers.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (clean.isEmpty()) return false
    val body = JSONObject().apply { put("reviewers", JSONArray(clean)) }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$number/requested_reviewers", "POST", body).success
}

internal suspend fun GitHubManager.removePullRequestReviewers(context: Context, owner: String, repo: String, number: Int, reviewers: List<String>): Boolean {
    val clean = reviewers.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    if (clean.isEmpty()) return false
    val body = JSONObject().apply { put("reviewers", JSONArray(clean)) }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$number/requested_reviewers", "DELETE", body).success
}

// ─── Review comments ─────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getPullRequestReviewComments(context: Context, owner: String, repo: String, pullNumber: Int, page: Int = 1): List<GHReviewComment> {
    val r = request(context, "/repos/$owner/$repo/pulls/$pullNumber/comments?per_page=100&page=$page")
    if (!r.success) return emptyList()
    val comments = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHReviewComment(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return comments
    return comments + getPullRequestReviewComments(context, owner, repo, pullNumber, nextPage)
}

internal suspend fun GitHubManager.createPullRequestReviewComment(
    context: Context, owner: String, repo: String, pullNumber: Int,
    body: String, path: String, line: Int, side: String = "RIGHT",
    inReplyToId: Long? = null
): Boolean {
    val json = JSONObject().apply {
        put("body", body)
        put("path", path)
        put("line", line)
        put("side", side)
        if (inReplyToId != null) put("in_reply_to", inReplyToId)
    }.toString()
    return request(context, "/repos/$owner/$repo/pulls/$pullNumber/comments", "POST", json).success
}

internal suspend fun GitHubManager.updatePullRequestReviewComment(context: Context, owner: String, repo: String, commentId: Long, body: String): Boolean {
    val json = JSONObject().apply { put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/pulls/comments/$commentId", "PATCH", json).success
}

internal suspend fun GitHubManager.deletePullRequestReviewComment(context: Context, owner: String, repo: String, commentId: Long): Boolean =
    request(context, "/repos/$owner/$repo/pulls/comments/$commentId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.getPullRequestReviewComment(context: Context, owner: String, repo: String, commentId: Long): GHReviewComment? {
    val r = request(context, "/repos/$owner/$repo/pulls/comments/$commentId")
    if (!r.success) return null
    return try { parseGHReviewComment(JSONObject(r.body)) } catch (e: Exception) { null }
}

// ─── Checks ──────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getPullRequestCheckRuns(context: Context, owner: String, repo: String, ref: String): List<GHCheckRun> {
    val encodedRef = encPath(ref)
    val r = request(context, "/repos/$owner/$repo/commits/$encodedRef/check-runs?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).getJSONArray("check_runs")
        (0 until arr.length()).map { i -> parseGHCheckRun(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getPullRequestCheckSuites(context: Context, owner: String, repo: String, ref: String): List<GHCheckSuite> {
    if (ref.isBlank()) return emptyList()
    val encodedRef = encPath(ref)
    val r = request(context, "/repos/$owner/$repo/commits/$encodedRef/check-suites?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("check_suites") ?: JSONArray()
        (0 until arr.length()).map { i -> parseGHCheckSuite(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

/** Логины из массива user-объектов (requested_reviewers). */
private fun parseUsers(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.optString("login")?.takeIf { it.isNotBlank() && it != "null" }
    }
}

/** Единый парсер PR: list-ответ не содержит detail-полей → opt-дефолты дают тот же результат. */
private fun parseGHPullRequest(j: JSONObject): GHPullRequest =
    GHPullRequest(
        number = j.optInt("number"),
        title = j.optString("title"),
        state = j.optString("state"),
        author = j.optJSONObject("user")?.optString("login") ?: "",
        createdAt = j.optString("created_at"),
        head = j.optJSONObject("head")?.optString("ref") ?: "",
        base = j.optJSONObject("base")?.optString("ref") ?: "",
        comments = j.optInt("comments", 0),
        merged = j.optBoolean("merged", false),
        body = j.optString("body", ""),
        draft = j.optBoolean("draft", false),
        htmlUrl = j.optString("html_url", ""),
        headSha = j.optJSONObject("head")?.optString("sha") ?: "",
        mergeable = if (j.isNull("mergeable")) null else j.optBoolean("mergeable"),
        mergeableState = j.optString("mergeable_state", ""),
        reviewComments = j.optInt("review_comments", 0),
        commits = j.optInt("commits", 0),
        additions = j.optInt("additions", 0),
        deletions = j.optInt("deletions", 0),
        changedFiles = j.optInt("changed_files", 0),
        requestedReviewers = parseUsers(j.optJSONArray("requested_reviewers"))
    )

private fun parseGHPullReview(j: JSONObject): GHPullReview =
    GHPullReview(
        id = j.optLong("id"),
        user = j.optJSONObject("user")?.optString("login") ?: "",
        state = j.optString("state", ""),
        body = j.optString("body", ""),
        submittedAt = j.optString("submitted_at", ""),
        commitId = j.optString("commit_id", ""),
        htmlUrl = j.optString("html_url", "")
    )

private fun parseGHPullFile(j: JSONObject): GHPullFile =
    GHPullFile(j.optString("filename"), j.optString("status"), j.optInt("additions", 0), j.optInt("deletions", 0), j.optString("patch", ""))

private fun parseGHReviewComment(j: JSONObject): GHReviewComment =
    GHReviewComment(
        id = j.optLong("id"),
        body = j.optString("body"),
        path = j.optString("path"),
        line = j.optInt("line", 0),
        originalLine = j.optInt("original_line", 0),
        diffHunk = j.optString("diff_hunk", ""),
        author = j.optJSONObject("user")?.optString("login") ?: "",
        avatarUrl = j.optJSONObject("user")?.optString("avatar_url") ?: "",
        createdAt = j.optString("created_at", ""),
        inReplyToId = j.optLong("in_reply_to_id", 0L).takeIf { it > 0 },
        side = j.optString("side", "").takeUnless { it.equals("null", ignoreCase = true) }.orEmpty(),
        originalSide = j.optString("original_side", "").takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
    )

private fun parseGHCheckRun(j: JSONObject): GHCheckRun =
    GHCheckRun(
        id = j.optLong("id"),
        name = j.optString("name"),
        status = j.optString("status"),
        conclusion = j.optString("conclusion", ""),
        detailsUrl = j.optString("details_url", ""),
        startedAt = j.optString("started_at", ""),
        completedAt = j.optString("completed_at", ""),
        outputTitle = j.optJSONObject("output")?.optString("title") ?: "",
        outputSummary = j.optJSONObject("output")?.optString("summary") ?: ""
    )

private fun parseGHCheckSuite(j: JSONObject): GHCheckSuite =
    GHCheckSuite(
        id = j.optLong("id"),
        status = j.optString("status"),
        conclusion = j.optString("conclusion", ""),
        app = j.optJSONObject("app")?.optString("name") ?: "",
        headBranch = j.optString("head_branch", ""),
        headSha = j.optString("head_sha", ""),
        before = j.optString("before", ""),
        after = j.optString("after", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        latestCheckRunsCount = j.optInt("latest_check_runs_count", 0)
    )
