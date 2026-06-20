package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHComment
import gs.git.vps.data.github.model.GHIssue
import gs.git.vps.data.github.model.GHIssueDetail
import gs.git.vps.data.github.model.GHIssueEvent
import gs.git.vps.data.github.model.GHLabel
import gs.git.vps.data.github.model.GHMilestone
import gs.git.vps.data.github.model.GHTimelineEvent
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Issues слоя GitHub API: issues, комментарии, события, метки, вехи, assignees, timeline.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Вся сеть — через ядро
 * `GitHubManager.request()`. Парсинг — чистые `parseGHX`-функции без IO. Сигнатуры публичных
 * вызовов (`GitHubManager.getIssues(...)` и т.п.) не изменились при выносе.
 *
 * Реакции (issue/comment reactions) НЕ здесь — они переедут в отдельный домен Reactions.
 */

// ─────────────────────────── parse-функции (чистые, без IO) ───────────────────────────

internal fun parseGHIssue(j: JSONObject): GHIssue = GHIssue(
    j.optInt("number"), j.optString("title"), j.optString("state"),
    j.optJSONObject("user")?.optString("login") ?: "", j.optString("created_at"),
    j.optInt("comments", 0), j.has("pull_request")
)

internal fun parseGHComment(j: JSONObject): GHComment = GHComment(
    id = j.optLong("id"), body = j.optString("body"),
    author = j.optJSONObject("user")?.optString("login") ?: "",
    avatarUrl = j.optJSONObject("user")?.optString("avatar_url") ?: "",
    createdAt = j.optString("created_at")
)

internal fun parseGHIssueEvent(j: JSONObject, fallbackIssueNumber: Int = 0): GHIssueEvent {
    val issue = j.optJSONObject("issue")
    val rename = j.optJSONObject("rename")
    val app = j.optJSONObject("performed_via_github_app")
    return GHIssueEvent(
        id = j.optLong("id"),
        event = j.optString("event", ""),
        actor = j.optJSONObject("actor")?.optString("login") ?: "",
        createdAt = j.optString("created_at", ""),
        issueNumber = issue?.optInt("number", fallbackIssueNumber) ?: fallbackIssueNumber,
        issueTitle = issue?.optString("title", "") ?: "",
        label = j.optJSONObject("label")?.optString("name") ?: "",
        assignee = j.optJSONObject("assignee")?.optString("login") ?: "",
        milestone = j.optJSONObject("milestone")?.optString("title") ?: "",
        renameFrom = rename?.optString("from", "") ?: "",
        renameTo = rename?.optString("to", "") ?: "",
        commitId = j.optString("commit_id", ""),
        url = j.optString("url", ""),
        commitUrl = j.optString("commit_url", ""),
        authorAssociation = issue?.optString("author_association", "") ?: "",
        stateReason = issue?.optString("state_reason", "") ?: "",
        performedViaGithubApp = app?.optString("name", "") ?: ""
    )
}

internal fun parseGHIssueDetail(j: JSONObject): GHIssueDetail {
    val labels = mutableListOf<String>()
    val labelsArr = j.optJSONArray("labels")
    if (labelsArr != null) for (i in 0 until labelsArr.length()) labels.add(labelsArr.getJSONObject(i).optString("name"))
    return GHIssueDetail(
        number = j.optInt("number"), title = j.optString("title"),
        body = j.optString("body", ""), state = j.optString("state"),
        author = j.optJSONObject("user")?.optString("login") ?: "",
        avatarUrl = j.optJSONObject("user")?.optString("avatar_url") ?: "",
        createdAt = j.optString("created_at"), comments = j.optInt("comments", 0),
        labels = labels, isPR = j.has("pull_request"),
        assignee = j.optJSONObject("assignee")?.optString("login") ?: "",
        milestoneTitle = j.optJSONObject("milestone")?.optString("title") ?: "",
        locked = j.optBoolean("locked", false),
        activeLockReason = j.optString("active_lock_reason", "")
    )
}

internal fun parseGHLabel(j: JSONObject): GHLabel =
    GHLabel(name = j.optString("name"), color = j.optString("color", ""), description = j.optString("description", ""))

internal fun parseGHMilestone(j: JSONObject): GHMilestone = GHMilestone(
    number = j.optInt("number"), title = j.optString("title"),
    description = j.optString("description", ""), state = j.optString("state"),
    openIssues = j.optInt("open_issues"), closedIssues = j.optInt("closed_issues"),
    dueOn = j.optString("due_on", "")
)

internal fun parseGHTimelineEvent(j: JSONObject): GHTimelineEvent = GHTimelineEvent(
    id = j.optLong("id"),
    event = j.optString("event"),
    actor = j.optJSONObject("actor")?.optString("login") ?: "",
    createdAt = j.optString("created_at", ""),
    label = j.optJSONObject("label")?.optString("name") ?: "",
    milestone = j.optJSONObject("milestone")?.optString("title") ?: "",
    assignee = j.optJSONObject("assignee")?.optString("login") ?: "",
    source = j.optJSONObject("source")?.optString("issue") ?: ""
)

// ─────────────────────────── Issues ───────────────────────────

internal suspend fun GitHubManager.getIssues(context: Context, owner: String, repo: String, state: String = "open", page: Int = 1): List<GHIssue> {
    val r = request(context, "/repos/$owner/$repo/issues?state=$state&per_page=30&page=$page")
    if (!r.success) return emptyList()
    val issues = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHIssue(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return issues
    return issues + getIssues(context, owner, repo, state, nextPage)
}

internal suspend fun GitHubManager.createIssue(context: Context, owner: String, repo: String, title: String, body: String): Boolean {
    val json = JSONObject().apply { put("title", title); put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/issues", "POST", json).success
}

internal suspend fun GitHubManager.getIssueDetail(context: Context, owner: String, repo: String, number: Int): GHIssueDetail? {
    val r = request(context, "/repos/$owner/$repo/issues/$number")
    if (!r.success) return null
    return try { parseGHIssueDetail(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.closeIssue(context: Context, owner: String, repo: String, number: Int): Boolean {
    val json = JSONObject().apply { put("state", "closed") }.toString()
    return request(context, "/repos/$owner/$repo/issues/$number", "PATCH", json).success
}

internal suspend fun GitHubManager.reopenIssue(context: Context, owner: String, repo: String, number: Int): Boolean {
    val json = JSONObject().apply { put("state", "open") }.toString()
    return request(context, "/repos/$owner/$repo/issues/$number", "PATCH", json).success
}

internal suspend fun GitHubManager.lockIssue(context: Context, owner: String, repo: String, number: Int, reason: String = ""): Boolean {
    val json = JSONObject().apply {
        if (reason.isNotBlank()) put("lock_reason", reason)
    }.toString()
    return request(context, "/repos/$owner/$repo/issues/$number/lock", "PUT", json).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.unlockIssue(context: Context, owner: String, repo: String, number: Int): Boolean =
    request(context, "/repos/$owner/$repo/issues/$number/lock", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.updateIssueMeta(context: Context, owner: String, repo: String, issueNumber: Int, labels: List<String>, assignees: List<String>, milestoneNumber: Int?, clearMilestone: Boolean = false): Boolean {
    val body = JSONObject().apply {
        put("labels", JSONArray(labels))
        put("assignees", JSONArray(assignees))
        if (clearMilestone) put("milestone", JSONObject.NULL)
        else if (milestoneNumber != null) put("milestone", milestoneNumber)
    }.toString()
    return request(context, "/repos/$owner/$repo/issues/$issueNumber", "PATCH", body).success
}

// ─────────────────────────── Комментарии ───────────────────────────

internal suspend fun GitHubManager.getIssueComments(context: Context, owner: String, repo: String, number: Int): List<GHComment> {
    val r = request(context, "/repos/$owner/$repo/issues/$number/comments?per_page=50")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHComment(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addComment(context: Context, owner: String, repo: String, number: Int, body: String): Boolean {
    val json = JSONObject().apply { put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/issues/$number/comments", "POST", json).success
}

internal suspend fun GitHubManager.updateIssueComment(context: Context, owner: String, repo: String, commentId: Long, body: String): Boolean {
    val json = JSONObject().apply { put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/issues/comments/$commentId", "PATCH", json).success
}

internal suspend fun GitHubManager.deleteIssueComment(context: Context, owner: String, repo: String, commentId: Long): Boolean =
    request(context, "/repos/$owner/$repo/issues/comments/$commentId", "DELETE").let { it.code == 204 || it.success }

// ─────────────────────────── События issue ───────────────────────────

internal suspend fun GitHubManager.getIssueEvents(context: Context, owner: String, repo: String, page: Int = 1): List<GHIssueEvent> {
    val r = request(context, "/repos/$owner/$repo/issues/events?per_page=100&page=$page")
    if (!r.success) return emptyList()
    val events = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHIssueEvent(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return events
    return events + getIssueEvents(context, owner, repo, nextPage)
}

internal suspend fun GitHubManager.getIssueEventsForIssue(context: Context, owner: String, repo: String, issueNumber: Int, page: Int = 1): List<GHIssueEvent> {
    val r = request(context, "/repos/$owner/$repo/issues/$issueNumber/events?per_page=100&page=$page")
    if (!r.success) return emptyList()
    val events = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHIssueEvent(arr.getJSONObject(i), fallbackIssueNumber = issueNumber) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return events
    return events + getIssueEventsForIssue(context, owner, repo, issueNumber, nextPage)
}

internal suspend fun GitHubManager.getIssueEvent(context: Context, owner: String, repo: String, eventId: Long): GHIssueEvent? {
    if (eventId <= 0L) return null
    val r = request(context, "/repos/$owner/$repo/issues/events/$eventId")
    if (!r.success) return null
    return try { parseGHIssueEvent(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getIssueTimeline(context: Context, owner: String, repo: String, issueNumber: Int): List<GHTimelineEvent> {
    val r = request(context, "/repos/$owner/$repo/issues/$issueNumber/timeline?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github.mockingbird-preview+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHTimelineEvent(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

// ─────────────────────────── Метки, вехи, assignees ───────────────────────────

internal suspend fun GitHubManager.getLabels(context: Context, owner: String, repo: String): List<GHLabel> {
    val r = request(context, "/repos/$owner/$repo/labels?per_page=50")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHLabel(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createLabel(context: Context, owner: String, repo: String, name: String, color: String, description: String = ""): Boolean {
    val body = JSONObject().apply { put("name", name); put("color", color.removePrefix("#")); put("description", description) }.toString()
    return request(context, "/repos/$owner/$repo/labels", "POST", body).success
}

internal suspend fun GitHubManager.deleteLabel(context: Context, owner: String, repo: String, name: String): Boolean =
    request(context, "/repos/$owner/$repo/labels/${URLEncoder.encode(name, "UTF-8")}", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.addLabelsToIssue(context: Context, owner: String, repo: String, issueNumber: Int, labels: List<String>): Boolean {
    val body = JSONObject().apply { put("labels", JSONArray(labels)) }.toString()
    return request(context, "/repos/$owner/$repo/issues/$issueNumber/labels", "POST", body).success
}

internal suspend fun GitHubManager.getMilestones(context: Context, owner: String, repo: String): List<GHMilestone> {
    val r = request(context, "/repos/$owner/$repo/milestones?per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHMilestone(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createMilestone(context: Context, owner: String, repo: String, title: String, description: String = "", dueOn: String? = null): Boolean {
    val body = JSONObject().apply {
        put("title", title); put("description", description)
        if (dueOn != null) put("due_on", dueOn)
    }.toString()
    return request(context, "/repos/$owner/$repo/milestones", "POST", body).success
}

internal suspend fun GitHubManager.getAssignees(context: Context, owner: String, repo: String): List<GHUserLite> {
    val r = request(context, "/repos/$owner/$repo/assignees")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> val j = arr.getJSONObject(i)
            GHUserLite(j.optString("login"), j.optString("avatar_url", ""))
        }
    } catch (_: Exception) { emptyList() }
}
