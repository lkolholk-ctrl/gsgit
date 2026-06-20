package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHProject
import gs.git.vps.data.github.model.GHProjectCard
import gs.git.vps.data.github.model.GHProjectColumn
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Projects (classic) слоя GitHub API — REST-проекты репозитория/организации,
 * колонки и карточки. Нарезан по эталону (см. docs/decomposition-log.md).
 * Projects V2 (GraphQL) — в GitHubManager+ProjectsV2.kt. Сеть — через ядро request();
 * парсинг — чистые parse*-функции без IO.
 */

// ─── Classic Projects (REST) ───

internal suspend fun GitHubManager.getRepoProjects(context: Context, owner: String, repo: String, state: String = "all"): List<GHProject> {
    val r = request(context, "/repos/$owner/$repo/projects?state=$state&per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProject) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getProject(context: Context, projectId: Long): GHProject? {
    val r = request(context, "/projects/$projectId", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseProject(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createRepoProject(context: Context, owner: String, repo: String, name: String, body: String): GHProject? {
    val payload = JSONObject().apply {
        put("name", name)
        put("body", body)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/projects", "POST", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseProject(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getOrgProjects(context: Context, org: String, state: String = "all"): List<GHProject> {
    val r = request(context, "/orgs/${encPath(org)}/projects?state=$state&per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProject) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createOrgProject(context: Context, org: String, name: String, body: String): GHProject? {
    val payload = JSONObject().apply {
        put("name", name)
        put("body", body)
    }.toString()
    val r = request(context, "/orgs/${encPath(org)}/projects", "POST", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseProject(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateProject(context: Context, projectId: Long, name: String, body: String, state: String): Boolean {
    val payload = JSONObject().apply {
        put("name", name)
        put("body", body)
        put("state", state)
    }.toString()
    return request(context, "/projects/$projectId", "PATCH", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json")).success
}

internal suspend fun GitHubManager.deleteProject(context: Context, projectId: Long): Boolean =
    request(context, "/projects/$projectId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }

internal suspend fun GitHubManager.getProjectColumns(context: Context, projectId: Long): List<GHProjectColumn> {
    val r = request(context, "/projects/$projectId/columns?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectColumn) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createProjectColumn(context: Context, projectId: Long, name: String): GHProjectColumn? {
    val payload = JSONObject().apply { put("name", name) }.toString()
    val r = request(context, "/projects/$projectId/columns", "POST", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseProjectColumn(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getProjectCards(context: Context, columnId: Long): List<GHProjectCard> {
    val r = request(context, "/projects/columns/$columnId/cards?archived_state=all&per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectCard) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createProjectCard(context: Context, columnId: Long, note: String): GHProjectCard? {
    val payload = JSONObject().apply { put("note", note) }.toString()
    val r = request(context, "/projects/columns/$columnId/cards", "POST", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseProjectCard(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.moveProjectCard(context: Context, cardId: Long, position: String, columnId: Long? = null): Boolean {
    val payload = JSONObject().apply {
        put("position", position)
        if (columnId != null) put("column_id", columnId)
    }.toString()
    return request(context, "/projects/columns/cards/$cardId/moves", "POST", payload, extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 201 || it.success }
}

internal suspend fun GitHubManager.deleteProjectCard(context: Context, cardId: Long): Boolean =
    request(context, "/projects/columns/cards/$cardId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }

// ─── Парсеры (чистые, без IO) ───

private fun parseProject(j: JSONObject): GHProject =
    GHProject(
        id = j.optLong("id"),
        nodeId = j.optString("node_id", ""),
        name = j.optString("name"),
        body = j.optString("body", ""),
        state = j.optString("state", "open"),
        number = j.optInt("number", 0),
        columnsUrl = j.optString("columns_url", ""),
        htmlUrl = j.optString("html_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        creator = j.optJSONObject("creator")?.optString("login") ?: ""
    )

private fun parseProjectColumn(j: JSONObject): GHProjectColumn =
    GHProjectColumn(
        id = j.optLong("id"),
        nodeId = j.optString("node_id", ""),
        name = j.optString("name"),
        cardsUrl = j.optString("cards_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", "")
    )

private fun parseProjectCard(j: JSONObject): GHProjectCard =
    GHProjectCard(
        id = j.optLong("id"),
        nodeId = j.optString("node_id", ""),
        note = j.optString("note", ""),
        creator = j.optJSONObject("creator")?.optString("login") ?: "",
        contentUrl = j.optString("content_url", ""),
        archived = j.optBoolean("archived", false),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        columnUrl = j.optString("column_url", "")
    )
