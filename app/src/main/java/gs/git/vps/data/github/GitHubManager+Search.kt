package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHCodeResult
import gs.git.vps.data.github.model.GHLabelSearchResult
import gs.git.vps.data.github.model.GHSearchCommitResult
import gs.git.vps.data.github.model.GHSearchIssueResult
import gs.git.vps.data.github.model.GHTopicSearchResult
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Search слоя GitHub API (advanced search: code/issues/commits/topics/labels).
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Вся сеть — через ядро
 * `GitHubManager.request()`; парсинг — чистые `parseGH*`. Сигнатуры публичных вызовов не изменились.
 *
 * Прим.: `searchRepos`/`searchUsers` оставлены в god-файле — они возвращают GHRepo/GHUser и
 * переедут в домены Repos/Users (используют их parse-функции).
 */

internal suspend fun GitHubManager.searchCode(context: Context, query: String, owner: String, repo: String): List<GHCodeResult> {
    val q = URLEncoder.encode("$query repo:$owner/$repo", "UTF-8")
    val r = request(context, "/search/code?q=$q&per_page=20")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).getJSONArray("items")
        (0 until arr.length()).map { i -> parseGHCodeResult(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.searchIssuesAdvanced(context: Context, query: String, page: Int = 1): List<GHSearchIssueResult> {
    val q = URLEncoder.encode(query, "UTF-8")
    val r = request(context, "/search/issues?q=$q&sort=updated&order=desc&per_page=30&page=$page", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseGHSearchIssueResult) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.searchCommitsAdvanced(context: Context, query: String, page: Int = 1): List<GHSearchCommitResult> {
    val q = URLEncoder.encode(query, "UTF-8")
    val r = request(context, "/search/commits?q=$q&sort=author-date&order=desc&per_page=30&page=$page", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseGHSearchCommitResult) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.searchTopics(context: Context, query: String, page: Int = 1): List<GHTopicSearchResult> {
    val q = URLEncoder.encode(query, "UTF-8")
    val r = request(context, "/search/topics?q=$q&per_page=30&page=$page", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseGHTopicSearchResult) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.searchLabels(context: Context, repositoryFullName: String, query: String, page: Int = 1): List<GHLabelSearchResult> {
    val parts = repositoryFullName.trim().split("/")
    if (parts.size != 2) return emptyList()
    val repositoryId = getRepositoryId(context, parts[0], parts[1])
    if (repositoryId <= 0L) return emptyList()
    val q = URLEncoder.encode(query, "UTF-8")
    val r = request(context, "/search/labels?q=$q&repository_id=$repositoryId&per_page=30&page=$page", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { j -> parseGHLabelSearchResult(j, repositoryFullName) }
        }
    } catch (e: Exception) { emptyList() }
}

private suspend fun GitHubManager.getRepositoryId(context: Context, owner: String, repo: String): Long {
    val r = request(context, "/repos/${URLEncoder.encode(owner, "UTF-8")}/${URLEncoder.encode(repo, "UTF-8")}", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return 0L
    return try { JSONObject(r.body).optLong("id", 0L) } catch (e: Exception) { 0L }
}

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHCodeResult(j: JSONObject): GHCodeResult = GHCodeResult(
    name = j.optString("name"),
    path = j.optString("path"),
    sha = j.optString("sha"),
    htmlUrl = j.optString("html_url", ""),
    score = j.optDouble("score", 0.0)
)

internal fun parseGHSearchIssueResult(j: JSONObject): GHSearchIssueResult {
    val user = j.optJSONObject("user")
    val repo = repoNameFromIssueSearch(j)
    return GHSearchIssueResult(
        id = j.optLong("id"),
        number = j.optInt("number"),
        title = j.optString("title", ""),
        body = j.optString("body", ""),
        state = j.optString("state", ""),
        author = user?.optString("login") ?: "",
        avatarUrl = user?.optString("avatar_url") ?: "",
        comments = j.optInt("comments", 0),
        labels = parseLabelNames(j.optJSONArray("labels")),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        htmlUrl = j.optString("html_url", ""),
        repository = repo,
        isPullRequest = j.has("pull_request"),
        score = j.optDouble("score", 0.0)
    )
}

internal fun parseGHSearchCommitResult(j: JSONObject): GHSearchCommitResult {
    val commit = j.optJSONObject("commit")
    val author = commit?.optJSONObject("author")
    val repo = j.optJSONObject("repository")
    val user = j.optJSONObject("author") ?: j.optJSONObject("committer")
    return GHSearchCommitResult(
        sha = j.optString("sha", ""),
        message = commit?.optString("message", "") ?: "",
        author = user?.optString("login") ?: author?.optString("name") ?: "",
        avatarUrl = user?.optString("avatar_url") ?: "",
        date = author?.optString("date", "") ?: "",
        repository = repo?.optString("full_name", "") ?: "",
        htmlUrl = j.optString("html_url", ""),
        score = j.optDouble("score", 0.0)
    )
}

internal fun parseGHTopicSearchResult(j: JSONObject): GHTopicSearchResult =
    GHTopicSearchResult(
        name = j.optString("name", ""),
        displayName = j.optString("display_name", ""),
        shortDescription = j.optString("short_description", ""),
        description = j.optString("description", ""),
        createdBy = j.optString("created_by", ""),
        released = j.optString("released", ""),
        updatedAt = j.optString("updated_at", ""),
        featured = j.optBoolean("featured", false),
        curated = j.optBoolean("curated", false),
        score = j.optDouble("score", 0.0),
        aliases = parseTopicNameArray(j.optJSONArray("aliases")),
        related = parseTopicNameArray(j.optJSONArray("related"))
    )

internal fun parseGHLabelSearchResult(j: JSONObject, repository: String): GHLabelSearchResult =
    GHLabelSearchResult(
        name = j.optString("name", ""),
        color = j.optString("color", ""),
        description = j.optString("description", ""),
        repository = repository,
        score = j.optDouble("score", 0.0)
    )

// --- Приватные хелперы парсинга (используются только в Search) ---

private fun repoNameFromIssueSearch(j: JSONObject): String {
    val repositoryUrl = j.optString("repository_url", "")
    if (repositoryUrl.contains("/repos/")) return repositoryUrl.substringAfter("/repos/")
    val html = j.optString("html_url", "").removePrefix("https://github.com/")
    val parts = html.split("/")
    return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else ""
}

private fun parseLabelNames(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
    }
}

private fun parseTopicNameArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val item = arr.opt(i)
        when (item) {
            is JSONObject -> item.optString("name").takeIf { it.isNotBlank() }
            else -> item?.toString()?.takeIf { it.isNotBlank() && it != "null" }
        }
    }
}
