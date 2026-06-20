package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHCommit
import gs.git.vps.data.github.model.GHCommitDetail
import gs.git.vps.data.github.model.GHCommitStatus
import gs.git.vps.data.github.model.GHCompareResult
import gs.git.vps.data.github.model.GHDiffFile
import gs.git.vps.data.github.model.GHReviewComment
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Commits слоя GitHub API: список коммитов (репо/файл), детали коммита (diff),
 * сравнение base...head, commit-статусы, commit-комментарии.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`,
 * парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * НЕ здесь: blame (getFileBlame) и git-data объекты (refs/trees/blobs/git-commit) — оставлены в core
 * для домена Contents/GitData. Ветки и защита веток — в GitHubManager+Branches.kt.
 *
 * ВНИМАНИЕ (предсуществующий баг, сохранён как есть — это рефактор, не фикс): в createCommitStatus
 * тело пишет `put("context", context)`, где `context` — это android `Context`, а не `statusContext`.
 * Параметр statusContext по факту игнорируется. Поведение не менял; помечено для отдельного фикса.
 */

// ─── Коммиты: список / детали / сравнение ────────────────────────────────────

internal suspend fun GitHubManager.getCommits(context: Context, owner: String, repo: String, page: Int = 1): List<GHCommit> {
    val r = request(context, "/repos/$owner/$repo/commits?per_page=30&page=$page")
    if (!r.success) return emptyList()
    val commits = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHCommit(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return commits
    return commits + getCommits(context, owner, repo, nextPage)
}

internal suspend fun GitHubManager.getFileCommits(context: Context, owner: String, repo: String, path: String, branch: String? = null): List<GHCommit> {
    val ref = branch?.takeIf { it.isNotBlank() }
    val refParam = if (ref != null) "&sha=${URLEncoder.encode(ref, "UTF-8")}" else ""
    val r = request(context, "/repos/$owner/$repo/commits?path=${URLEncoder.encode(path, "UTF-8")}&per_page=20$refParam")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHCommit(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getCommitDiff(context: Context, owner: String, repo: String, sha: String): GHCommitDetail? {
    val r = request(context, "/repos/$owner/$repo/commits/$sha")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        val files = parseDiffFiles(j.optJSONArray("files"))
        GHCommitDetail(
            sha = j.optString("sha"), message = j.getJSONObject("commit").optString("message"),
            author = j.getJSONObject("commit").optJSONObject("author")?.optString("name") ?: "",
            date = j.getJSONObject("commit").optJSONObject("author")?.optString("date") ?: "",
            files = files, totalAdditions = j.optJSONObject("stats")?.optInt("additions") ?: 0,
            totalDeletions = j.optJSONObject("stats")?.optInt("deletions") ?: 0
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.compareCommits(context: Context, owner: String, repo: String, base: String, head: String): GHCompareResult? {
    val encodedBase = URLEncoder.encode(base, "UTF-8")
    val encodedHead = URLEncoder.encode(head, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/compare/$encodedBase...$encodedHead")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        val files = parseDiffFiles(j.optJSONArray("files"))
        val commitsArr = j.optJSONArray("commits")
        val commits = mutableListOf<GHCommit>()
        if (commitsArr != null) for (i in 0 until commitsArr.length()) {
            val cj = commitsArr.getJSONObject(i)
            val commit = cj.optJSONObject("commit")
            val author = commit?.optJSONObject("author")
            val user = cj.optJSONObject("author")
            val parentsArr = cj.optJSONArray("parents")
            val parentsList = if (parentsArr != null) {
                (0 until parentsArr.length()).map { pIdx ->
                    parentsArr.getJSONObject(pIdx).optString("sha").take(7)
                }
            } else emptyList()
            commits.add(GHCommit(
                sha = cj.optString("sha"),
                message = commit?.optString("message", "") ?: "",
                author = user?.optString("login", "")?.ifBlank { author?.optString("name", "") ?: "" } ?: "",
                date = author?.optString("date", "") ?: "",
                avatarUrl = user?.optString("avatar_url", "") ?: "",
                parents = parentsList
            ))
        }
        GHCompareResult(
            status = j.optString("status"),
            aheadBy = j.optInt("ahead_by"),
            behindBy = j.optInt("behind_by"),
            totalCommits = j.optInt("total_commits"),
            files = files,
            commits = commits,
            htmlUrl = j.optString("html_url", "")
        )
    } catch (e: Exception) { null }
}

// ─── Commit-статусы ──────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getCommitStatuses(context: Context, owner: String, repo: String, ref: String): List<GHCommitStatus> {
    val encodedRef = URLEncoder.encode(ref, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/commits/$encodedRef/statuses?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHCommitStatus(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createCommitStatus(context: Context, owner: String, repo: String, sha: String, state: String, statusContext: String, description: String = "", targetUrl: String = ""): Boolean {
    val body = JSONObject().apply {
        put("state", state)
        put("context", context)
        if (description.isNotBlank()) put("description", description)
        if (targetUrl.isNotBlank()) put("target_url", targetUrl)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/statuses/$sha", "POST", body)
    return r.success
}

// ─── Commit-комментарии ──────────────────────────────────────────────────────

internal suspend fun GitHubManager.getCommitComments(context: Context, owner: String, repo: String, sha: String, page: Int = 1): List<GHReviewComment> {
    val r = request(context, "/repos/$owner/$repo/commits/$sha/comments?per_page=100&page=$page")
    if (!r.success) return emptyList()
    val comments = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHReviewComment(
                id = j.optLong("id"),
                body = j.optString("body"),
                path = j.optString("path", ""),
                line = j.optInt("line", 0),
                originalLine = j.optInt("position", 0),
                diffHunk = j.optString("diff_hunk", ""),
                author = j.optJSONObject("user")?.optString("login") ?: "",
                avatarUrl = j.optJSONObject("user")?.optString("avatar_url") ?: "",
                createdAt = j.optString("created_at", ""),
                inReplyToId = null
            )
        }
    } catch (e: Exception) { emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return comments
    return comments + getCommitComments(context, owner, repo, sha, nextPage)
}

internal suspend fun GitHubManager.createCommitComment(
    context: Context, owner: String, repo: String, sha: String,
    body: String, path: String, line: Int
): Boolean {
    val json = JSONObject().apply {
        put("body", body)
        put("path", path)
        put("line", line)
    }.toString()
    return request(context, "/repos/$owner/$repo/commits/$sha/comments", "POST", json).success
}

internal suspend fun GitHubManager.updateCommitComment(context: Context, owner: String, repo: String, commentId: Long, body: String): Boolean {
    val json = JSONObject().apply { put("body", body) }.toString()
    return request(context, "/repos/$owner/$repo/comments/$commentId", "PATCH", json).success
}

internal suspend fun GitHubManager.deleteCommitComment(context: Context, owner: String, repo: String, commentId: Long): Boolean =
    request(context, "/repos/$owner/$repo/comments/$commentId", "DELETE").let { it.code == 204 || it.success }

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun parseGHCommit(j: JSONObject): GHCommit {
    val commit = j.getJSONObject("commit")
    val author = commit.optJSONObject("author")
    val parentsArr = j.optJSONArray("parents")
    val parentsList = if (parentsArr != null) {
        (0 until parentsArr.length()).map { pIdx ->
            parentsArr.getJSONObject(pIdx).optString("sha").take(7)
        }
    } else emptyList()
    return GHCommit(
        sha = j.optString("sha").take(7),
        message = commit.optString("message"),
        author = author?.optString("name") ?: "?",
        date = author?.optString("date") ?: "",
        avatarUrl = j.optJSONObject("author")?.optString("avatar_url") ?: "",
        parents = parentsList,
        verified = commit.optJSONObject("verification")?.optBoolean("verified", false) ?: false
    )
}

private fun parseDiffFiles(filesArr: JSONArray?): List<GHDiffFile> {
    val files = mutableListOf<GHDiffFile>()
    if (filesArr != null) for (i in 0 until filesArr.length()) {
        val fj = filesArr.getJSONObject(i)
        files.add(GHDiffFile(
            filename = fj.optString("filename"),
            status = fj.optString("status"),
            additions = fj.optInt("additions"),
            deletions = fj.optInt("deletions"),
            patch = fj.optString("patch", "")
        ))
    }
    return files
}

private fun parseGHCommitStatus(j: JSONObject): GHCommitStatus =
    GHCommitStatus(
        id = j.optLong("id", 0),
        state = j.optString("state", ""),
        context = j.optString("context", ""),
        description = j.optString("description", ""),
        targetUrl = j.optString("target_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        creator = j.optJSONObject("creator")?.optString("login") ?: ""
    )
