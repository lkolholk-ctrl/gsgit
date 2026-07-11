package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHDeployKey
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHLicenseDetail
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoCreateResult
import gs.git.vps.data.github.model.GHRepoEvent
import gs.git.vps.data.github.model.GHRepoInvitation
import gs.git.vps.data.github.model.GHRepoPerson
import gs.git.vps.data.github.model.GHRepoSettings
import gs.git.vps.data.github.model.GHTag
import gs.git.vps.data.github.model.GHTrafficPath
import gs.git.vps.data.github.model.GHTrafficPoint
import gs.git.vps.data.github.model.GHTrafficReferrer
import gs.git.vps.data.github.model.GHTrafficSeries
import gs.git.vps.data.github.model.GHUserRepositoryInvitation
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Repos слоя GitHub API — чистое ядро репозиториев: CRUD, контент, звёзды/форки,
 * трафик, stargazers/watchers/events, настройки, топики, теги, deploy-ключи, лимиты
 * взаимодействий, transfer и приглашения. Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Вся сеть — через ядро `GitHubManager.request()`.
 * Шаренные хелперы ядра (`parseRepo`, `repoPath`, `refQuery`, `parseNextPage`) остаются
 * `internal` в `GitHubManager.kt` — их используют и другие домены (searchRepos/getFileContent).
 * Сигнатуры публичных вызовов не изменились при выносе.
 */

private const val REPOS_TAG = "GH"

// --- Список / получение / создание / удаление ---

internal suspend fun GitHubManager.getRepos(context: Context, page: Int = 1, perPage: Int = 30): List<GHRepo> {
    val r = request(context, "/user/repos?sort=updated&per_page=$perPage&page=$page&type=all")
    if (!r.success) return emptyList()
    val repos = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { Log.e(REPOS_TAG, "Parse repositories failed"); return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return repos
    return repos + getRepos(context, nextPage, perPage)
}

internal suspend fun GitHubManager.getRepo(context: Context, owner: String, repo: String): GHRepo? {
    val r = request(context, "/repos/$owner/$repo")
    if (!r.success) return null
    return try {
        parseRepo(JSONObject(r.body))
    } catch (e: Exception) {
        Log.e(REPOS_TAG, "Parse repository failed")
        null
    }
}

internal suspend fun GitHubManager.createRepo(
    context: Context, name: String, description: String, isPrivate: Boolean,
    autoInit: Boolean = true, gitignoreTemplate: String = "", licenseTemplate: String = "",
    hasIssues: Boolean = true, hasProjects: Boolean = true, hasWiki: Boolean = true
): Boolean = createRepoWithResult(context, name, description, isPrivate, autoInit, gitignoreTemplate, licenseTemplate, hasIssues, hasProjects, hasWiki).success

internal suspend fun GitHubManager.createRepoWithResult(
    context: Context, name: String, description: String, isPrivate: Boolean,
    autoInit: Boolean = true, gitignoreTemplate: String = "", licenseTemplate: String = "",
    hasIssues: Boolean = true, hasProjects: Boolean = true, hasWiki: Boolean = true
): GHRepoCreateResult {
    val body = JSONObject().apply {
        put("name", name); put("description", description); put("private", isPrivate)
        put("auto_init", autoInit); put("has_issues", hasIssues)
        put("has_projects", hasProjects); put("has_wiki", hasWiki)
        if (gitignoreTemplate.isNotBlank()) put("gitignore_template", gitignoreTemplate)
        if (licenseTemplate.isNotBlank()) put("license_template", licenseTemplate)
    }.toString()
    val r = request(context, "/user/repos", "POST", body)
    if (!r.success) return GHRepoCreateResult(success = false, repo = null)
    return try {
        val j = JSONObject(r.body)
        val repo = GHRepo(
            name = j.optString("name", ""), fullName = j.optString("full_name", ""),
            description = j.optString("description", ""), language = j.optString("language", ""),
            stars = j.optInt("stargazers_count", 0), forks = j.optInt("forks_count", 0),
            isPrivate = j.optBoolean("private", false), isFork = j.optBoolean("fork", false),
            defaultBranch = j.optString("default_branch", "main"), updatedAt = j.optString("updated_at", ""),
            owner = j.optJSONObject("owner")?.optString("login") ?: "",
            htmlUrl = j.optString("html_url", ""), id = j.optLong("id", 0L)
        )
        GHRepoCreateResult(success = true, repo = repo, cloneUrl = j.optString("clone_url", ""), sshUrl = j.optString("ssh_url", ""), autoInit = autoInit)
    } catch (e: Exception) { GHRepoCreateResult(success = false, repo = null) }
}

internal suspend fun GitHubManager.createRepoFromTemplateWithResult(
    context: Context, templateOwner: String, templateRepo: String,
    name: String, description: String, isPrivate: Boolean,
    includeAllBranches: Boolean = false
): GHRepoCreateResult {
    val body = JSONObject().apply {
        put("name", name); put("description", description); put("private", isPrivate)
        put("owner", templateOwner)
        put("include_all_branches", includeAllBranches)
    }.toString()
    val r = request(context, "/repos/$templateOwner/$templateRepo/generate", "POST", body)
    if (!r.success) return GHRepoCreateResult(success = false, repo = null)
    return try {
        val j = JSONObject(r.body)
        val repo = GHRepo(
            name = j.optString("name", ""), fullName = j.optString("full_name", ""),
            description = j.optString("description", ""), language = j.optString("language", ""),
            stars = j.optInt("stargazers_count", 0), forks = j.optInt("forks_count", 0),
            isPrivate = j.optBoolean("private", false), isFork = j.optBoolean("fork", false),
            defaultBranch = j.optString("default_branch", "main"), updatedAt = j.optString("updated_at", ""),
            owner = j.optJSONObject("owner")?.optString("login") ?: "",
            htmlUrl = j.optString("html_url", ""), id = j.optLong("id", 0L)
        )
        GHRepoCreateResult(success = true, repo = repo, cloneUrl = j.optString("clone_url", ""), sshUrl = j.optString("ssh_url", ""), autoInit = false)
    } catch (e: Exception) { GHRepoCreateResult(success = false, repo = null) }
}

internal suspend fun GitHubManager.deleteRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo", "DELETE").success

internal suspend fun GitHubManager.getRepoContents(context: Context, owner: String, repo: String, path: String = "", branch: String? = null): List<GHContent> {
    val r = request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}${refQuery(branch)}")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHContent(j.optString("name"), j.optString("path"), j.optString("type"),
                j.optLong("size", 0), j.optString("download_url", ""), j.optString("sha", ""))
        }.sortedWith(compareBy<GHContent> { it.type != "dir" }.thenBy { it.name.lowercase() })
    } catch (e: Exception) { emptyList() }
}

// --- Звёзды / форк ---

internal suspend fun GitHubManager.starRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/user/starred/$owner/$repo", "PUT").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.unstarRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/user/starred/$owner/$repo", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.forkRepo(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo/forks", "POST", "{}").success

// --- Лицензия репозитория ---

internal suspend fun GitHubManager.getRepoLicense(context: Context, owner: String, repo: String): GHLicenseDetail? {
    val r = request(context, "${repoPath(owner, repo, "/license")}", trackErrors = false)
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body).optJSONObject("license") ?: return null
        GHLicenseDetail(j.optString("key"), j.optString("name"), j.optString("spdx_id"),
            j.optString("description", ""), "", j.optString("html_url", ""), j.optBoolean("featured"))
    } catch (e: Exception) { null }
}

// --- Трафик ---

internal suspend fun GitHubManager.getRepoTrafficViews(context: Context, owner: String, repo: String): GHTrafficSeries? {
    val r = request(context, "/repos/$owner/$repo/traffic/views?per=day")
    if (!r.success) return null
    return try { parseTrafficSeries(JSONObject(r.body), "views") } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getRepoTrafficClones(context: Context, owner: String, repo: String): GHTrafficSeries? {
    val r = request(context, "/repos/$owner/$repo/traffic/clones?per=day")
    if (!r.success) return null
    return try { parseTrafficSeries(JSONObject(r.body), "clones") } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getRepoTrafficReferrers(context: Context, owner: String, repo: String): List<GHTrafficReferrer> {
    val r = request(context, "/repos/$owner/$repo/traffic/popular/referrers")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHTrafficReferrer(
                referrer = j.optString("referrer", ""),
                count = j.optInt("count", 0),
                uniques = j.optInt("uniques", 0)
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRepoTrafficPaths(context: Context, owner: String, repo: String): List<GHTrafficPath> {
    val r = request(context, "/repos/$owner/$repo/traffic/popular/paths")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHTrafficPath(
                path = j.optString("path", ""),
                title = j.optString("title", ""),
                count = j.optInt("count", 0),
                uniques = j.optInt("uniques", 0)
            )
        }
    } catch (e: Exception) { emptyList() }
}

// --- Stargazers / Watchers / Events ---

internal suspend fun GitHubManager.getRepoStargazers(context: Context, owner: String, repo: String, page: Int = 1): List<GHRepoPerson> {
    val r = request(
        context,
        "/repos/$owner/$repo/stargazers?per_page=50&page=$page",
        extraHeaders = mapOf("Accept" to "application/vnd.github.star+json")
    )
    if (!r.success) return emptyList()
    val result = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> parseRepoPerson(arr.getJSONObject(i), starred = true) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return result
    return result + getRepoStargazers(context, owner, repo, nextPage)
}

internal suspend fun GitHubManager.getRepoWatchers(context: Context, owner: String, repo: String, page: Int = 1): List<GHRepoPerson> {
    val r = request(context, "/repos/$owner/$repo/subscribers?per_page=50&page=$page")
    if (!r.success) return emptyList()
    val result = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> parseRepoPerson(arr.getJSONObject(i), starred = false) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return result
    return result + getRepoWatchers(context, owner, repo, nextPage)
}

internal suspend fun GitHubManager.getRepoEvents(context: Context, owner: String, repo: String, page: Int = 1): List<GHRepoEvent> {
    val r = request(context, "/repos/$owner/$repo/events?per_page=50&page=$page")
    if (!r.success) return emptyList()
    val events = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val payload = j.optJSONObject("payload")
            GHRepoEvent(
                id = j.optString("id", ""),
                type = j.optString("type", ""),
                actor = j.optJSONObject("actor")?.optString("login") ?: "",
                createdAt = j.optString("created_at", ""),
                action = payload?.optString("action", "") ?: "",
                ref = payload?.optString("ref", "") ?: "",
                refType = payload?.optString("ref_type", "") ?: "",
                size = payload?.optInt("size", 0) ?: 0,
                repoName = "$owner/$repo"
            )
        }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return events
    return events + getRepoEvents(context, owner, repo, nextPage)
}

// --- Репозитории пользователя / организации ---

internal suspend fun GitHubManager.getUserRepos(context: Context, username: String): List<GHRepo> {
    val r = request(context, "/users/$username/repos?sort=updated&per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getStarredRepos(context: Context, page: Int = 1): List<GHRepo> {
    val r = request(context, "/user/starred?sort=created&per_page=30&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getWatchedRepos(context: Context, page: Int = 1): List<GHRepo> {
    val r = request(context, "/user/subscriptions?per_page=30&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getOrgRepos(context: Context, org: String): List<GHRepo> {
    val r = request(context, "/orgs/$org/repos?sort=updated&per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }
}

// --- Приглашения текущего пользователя в чужие репозитории ---

internal suspend fun GitHubManager.getUserRepositoryInvitations(context: Context): List<GHUserRepositoryInvitation> {
    val r = request(context, "/user/repository_invitations?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val repoJson = j.optJSONObject("repository")
            val repo = repoJson?.let { parseRepo(it) }
            val inviter = j.optJSONObject("inviter")
            GHUserRepositoryInvitation(
                id = j.optLong("id"),
                repository = repo,
                repoFullName = repo?.fullName ?: repoJson?.optString("full_name", "").orEmpty(),
                inviter = inviter?.optString("login", "").orEmpty(),
                inviterAvatarUrl = inviter?.optString("avatar_url", "").orEmpty(),
                permissions = j.optString("permissions", ""),
                createdAt = j.optString("created_at", ""),
                expired = j.optBoolean("expired", false)
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.acceptUserRepositoryInvitation(context: Context, invitationId: Long): Boolean =
    request(context, "/user/repository_invitations/$invitationId", "PATCH").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.declineUserRepositoryInvitation(context: Context, invitationId: Long): Boolean =
    request(context, "/user/repository_invitations/$invitationId", "DELETE").let { it.code == 204 || it.success }

// --- Лимиты взаимодействий ---

internal suspend fun GitHubManager.getRepoInteractionLimit(context: Context, owner: String, repo: String): GHInteractionLimitEntry? {
    val r = request(context, "/repos/$owner/$repo/interaction-limits", trackErrors = false)
    if (!r.success || r.body.isBlank()) return null
    return try {
        val j = JSONObject(r.body)
        GHInteractionLimitEntry(j.optString("limit"), j.optString("expires_at", "").ifBlank { null })
    } catch (_: Exception) { null }
}

internal suspend fun GitHubManager.setRepoInteractionLimit(context: Context, owner: String, repo: String, limit: String, expiry: String): Boolean {
    val body = JSONObject().apply { put("limit", limit); put("expiry", expiry) }.toString()
    return request(context, "/repos/$owner/$repo/interaction-limits", "PUT", body).success
}

internal suspend fun GitHubManager.removeRepoInteractionLimit(context: Context, owner: String, repo: String): Boolean =
    request(context, "/repos/$owner/$repo/interaction-limits", "DELETE").let { it.code == 204 || it.success }

// --- Настройки / топики / теги ---

internal suspend fun GitHubManager.getRepoSettings(context: Context, owner: String, repo: String): GHRepoSettings? {
    val r = request(context, "/repos/$owner/$repo")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHRepoSettings(
            name = j.optString("name"),
            description = j.optString("description", ""),
            homepage = j.optString("homepage", ""),
            isPrivate = j.optBoolean("private", false),
            hasIssues = j.optBoolean("has_issues", true),
            hasProjects = j.optBoolean("has_projects", true),
            hasWiki = j.optBoolean("has_wiki", true),
            hasDiscussions = j.optBoolean("has_discussions", false),
            allowForking = j.optBoolean("allow_forking", true),
            isTemplate = j.optBoolean("is_template", false),
            archived = j.optBoolean("archived", false),
            disabled = j.optBoolean("disabled", false),
            defaultBranch = j.optString("default_branch", "main"),
            topics = mutableListOf<String>().apply {
                val arr = j.optJSONArray("topics")
                if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
            },
            allowMergeCommit = j.optBoolean("allow_merge_commit", true),
            allowSquashMerge = j.optBoolean("allow_squash_merge", true),
            allowRebaseMerge = j.optBoolean("allow_rebase_merge", true),
            deleteBranchOnMerge = j.optBoolean("delete_branch_on_merge", false)
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateRepoSettings(
    context: Context, owner: String, repo: String,
    name: String? = null,
    description: String? = null,
    homepage: String? = null,
    isPrivate: Boolean? = null,
    hasIssues: Boolean? = null,
    hasProjects: Boolean? = null,
    hasWiki: Boolean? = null,
    hasDiscussions: Boolean? = null,
    allowForking: Boolean? = null,
    isTemplate: Boolean? = null,
    archived: Boolean? = null,
    topics: List<String>? = null,
    allowMergeCommit: Boolean? = null,
    allowSquashMerge: Boolean? = null,
    allowRebaseMerge: Boolean? = null,
    deleteBranchOnMerge: Boolean? = null
): Boolean {
    val body = JSONObject().apply {
        if (name != null) put("name", name)
        if (description != null) put("description", description)
        if (homepage != null) put("homepage", homepage)
        if (isPrivate != null) put("private", isPrivate)
        if (hasIssues != null) put("has_issues", hasIssues)
        if (hasProjects != null) put("has_projects", hasProjects)
        if (hasWiki != null) put("has_wiki", hasWiki)
        if (hasDiscussions != null) put("has_discussions", hasDiscussions)
        if (allowForking != null) put("allow_forking", allowForking)
        if (isTemplate != null) put("is_template", isTemplate)
        if (archived != null) put("archived", archived)
        if (topics != null) put("topics", JSONArray(topics))
        if (allowMergeCommit != null) put("allow_merge_commit", allowMergeCommit)
        if (allowSquashMerge != null) put("allow_squash_merge", allowSquashMerge)
        if (allowRebaseMerge != null) put("allow_rebase_merge", allowRebaseMerge)
        if (deleteBranchOnMerge != null) put("delete_branch_on_merge", deleteBranchOnMerge)
    }.toString()
    return request(context, "/repos/$owner/$repo", "PATCH", body).success
}

internal suspend fun GitHubManager.getRepoTopics(context: Context, owner: String, repo: String): List<String> {
    val r = request(context, "/repos/$owner/$repo/topics")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("names") ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRepoTags(context: Context, owner: String, repo: String, page: Int = 1): List<GHTag> {
    val r = request(context, "/repos/$owner/$repo/tags?per_page=50&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val commit = j.optJSONObject("commit")
            GHTag(
                name = j.optString("name"),
                zipballUrl = j.optString("zipball_url", ""),
                tarballUrl = j.optString("tarball_url", ""),
                commitSha = commit?.optString("sha", "") ?: "",
                commitUrl = commit?.optString("url", "") ?: ""
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.replaceRepoTopics(context: Context, owner: String, repo: String, topics: List<String>): Boolean {
    val body = JSONObject().apply { put("names", JSONArray(topics)) }.toString()
    return request(context, "/repos/$owner/$repo/topics", "PUT", body).success
}

// --- Deploy-ключи ---

internal suspend fun GitHubManager.getRepoDeployKeys(context: Context, owner: String, repo: String): List<GHDeployKey> {
    val r = request(context, "/repos/$owner/$repo/keys?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHDeployKey(
                id = j.optLong("id", 0L),
                title = j.optString("title", ""),
                key = j.optString("key", ""),
                verified = j.optBoolean("verified", false),
                readOnly = j.optBoolean("read_only", true),
                createdAt = j.optString("created_at", ""),
                addedBy = j.optString("added_by", ""),
                lastUsed = j.optString("last_used", ""),
                enabled = j.optBoolean("enabled", true),
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

internal suspend fun GitHubManager.createRepoDeployKey(
    context: Context,
    owner: String,
    repo: String,
    title: String,
    key: String,
    readOnly: Boolean,
): Boolean {
    val body = JSONObject().apply {
        put("title", title)
        put("key", key)
        put("read_only", readOnly)
    }.toString()
    return request(context, "/repos/$owner/$repo/keys", "POST", body).code == 201
}

internal suspend fun GitHubManager.deleteRepoDeployKey(context: Context, owner: String, repo: String, keyId: Long): Boolean =
    request(context, "/repos/$owner/$repo/keys/$keyId", "DELETE").let { it.code == 204 || it.success }

// --- Transfer ---

internal suspend fun GitHubManager.transferRepo(context: Context, owner: String, repo: String, newOwner: String, newName: String? = null): Boolean {
    val body = JSONObject().apply {
        put("new_owner", newOwner)
        if (newName != null) put("new_name", newName)
    }.toString()
    return request(context, "/repos/$owner/$repo/transfer", "POST", body).success
}

// --- Приглашения коллабораторов в репозиторий ---

internal suspend fun GitHubManager.getRepoInvitations(context: Context, owner: String, repo: String): List<GHRepoInvitation> {
    val r = request(context, "/repos/$owner/$repo/invitations?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHRepoInvitation(
                id = j.optLong("id"),
                invitee = j.optJSONObject("invitee")?.optString("login") ?: "",
                inviter = j.optJSONObject("inviter")?.optString("login") ?: "",
                permissions = j.optString("permissions", ""),
                createdAt = j.optString("created_at", ""),
                expired = j.optBoolean("expired", false),
                htmlUrl = j.optString("html_url", "")
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.updateRepoInvitation(context: Context, owner: String, repo: String, invitationId: Long, permission: String): Boolean {
    val body = JSONObject().apply { put("permissions", permission) }.toString()
    return request(context, "/repos/$owner/$repo/invitations/$invitationId", "PATCH", body).success
}

internal suspend fun GitHubManager.deleteRepoInvitation(context: Context, owner: String, repo: String, invitationId: Long): Boolean =
    request(context, "/repos/$owner/$repo/invitations/$invitationId", "DELETE").let { it.code == 204 || it.success }

// --- Парсинг: чистые функции JSON→модель, без IO (перенесены из ядра) ---

private fun parseTrafficSeries(j: JSONObject, itemKey: String): GHTrafficSeries {
    val items = j.optJSONArray(itemKey) ?: JSONArray()
    return GHTrafficSeries(
        count = j.optInt("count", 0),
        uniques = j.optInt("uniques", 0),
        items = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            GHTrafficPoint(
                timestamp = item.optString("timestamp", ""),
                count = item.optInt("count", 0),
                uniques = item.optInt("uniques", 0)
            )
        }
    )
}

private fun parseRepoPerson(j: JSONObject, starred: Boolean): GHRepoPerson? {
    val user = if (starred && j.has("user")) j.optJSONObject("user") else j
    val login = user?.optString("login", "").orEmpty()
    if (login.isBlank()) return null
    return GHRepoPerson(
        login = login,
        avatarUrl = user?.optString("avatar_url", "").orEmpty(),
        htmlUrl = user?.optString("html_url", "").orEmpty(),
        starredAt = if (starred) j.optString("starred_at", "") else ""
    )
}

// ─── Поиск репозиториев / star-статус (базовые Repos-эндпоинты) ───────────────

internal suspend fun GitHubManager.searchRepos(context: Context, query: String): List<GHRepo> {
    val r = request(context, "/search/repositories?q=$query&sort=stars&per_page=20")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).getJSONArray("items")
        (0 until arr.length()).map { parseRepo(arr.getJSONObject(it)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.isStarred(context: Context, owner: String, repo: String): Boolean =
    request(context, "/user/starred/$owner/$repo").code == 204
