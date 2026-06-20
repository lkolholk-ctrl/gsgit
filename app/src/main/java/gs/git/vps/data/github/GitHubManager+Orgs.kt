package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHAuditLogEntry
import gs.git.vps.data.github.model.GHOrg
import gs.git.vps.data.github.model.GHOrgMembership
import gs.git.vps.data.github.model.GHSamlAuthorization
import gs.git.vps.data.github.model.GHScimUser
import gs.git.vps.data.github.model.GHScimUsersPage
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Orgs слоя GitHub API: организации пользователя, членство, участники, обновление орг-настроек,
 * а также org-админ-фичи (audit-log, SCIM-провизионинг, SAML credential-authorizations).
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`,
 * парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * GHUserLite (возвращает getOrgMembers) — в core (шарится с доменом Issues), импорт не нужен:
 * этот файл в том же пакете `gs.git.vps.data.github`.
 */

// ─── Организации и членство ──────────────────────────────────────────────────

internal suspend fun GitHubManager.getOrganizations(context: Context): List<GHOrg> {
    val r = request(context, "/user/orgs?per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHOrg(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getOrg(context: Context, org: String): GHOrg? {
    val r = request(context, "/orgs/${encPath(org)}")
    if (!r.success) return null
    return try { parseGHOrg(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getOrgMembership(context: Context, org: String): GHOrgMembership? {
    val r = request(context, "/user/memberships/orgs/${encPath(org)}")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHOrgMembership(
            org = j.optJSONObject("organization")?.optString("login") ?: org,
            state = j.optString("state", ""),
            role = j.optString("role", ""),
            url = j.optString("url", "")
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateOrgMembership(context: Context, org: String, state: String = "active"): Boolean {
    val body = JSONObject().apply { put("state", state) }.toString()
    val r = request(context, "/user/memberships/orgs/${encPath(org)}", "PATCH", body)
    return r.success
}

internal suspend fun GitHubManager.getOrgMembers(context: Context, org: String, role: String = "all", page: Int = 1): List<GHUserLite> {
    val r = request(context, "/orgs/${encPath(org)}/members?role=$role&per_page=100&page=$page")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHUserLite(login = j.optString("login", ""), avatarUrl = j.optString("avatar_url", ""))
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.removeOrgMember(context: Context, org: String, username: String): Boolean {
    return request(context, "/orgs/${encPath(org)}/members/${encPath(username)}", "DELETE").let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.updateOrg(context: Context, org: String, description: String? = null, defaultRepoPermission: String? = null, hasOrgProjects: Boolean? = null, hasRepoProjects: Boolean? = null): Boolean {
    val body = JSONObject().apply {
        description?.let { put("description", it) }
        defaultRepoPermission?.let { put("default_repository_permission", it) }
        hasOrgProjects?.let { put("has_organization_projects", it) }
        hasRepoProjects?.let { put("has_repository_projects", it) }
    }.toString()
    val r = request(context, "/orgs/${encPath(org)}", "PATCH", body)
    return r.success
}

// ─── Org-админ: audit-log / SCIM / SAML ──────────────────────────────────────

internal suspend fun GitHubManager.getOrgAuditLog(context: Context, org: String, phrase: String = "", page: Int = 1): List<GHAuditLogEntry> {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank()) return emptyList()
    val query = buildString {
        append("?per_page=100&page=$page")
        if (phrase.isNotBlank()) append("&phrase=${URLEncoder.encode(phrase.trim(), "UTF-8")}")
    }
    val r = request(context, "/orgs/$cleanOrg/audit-log$query")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHAuditLogEntry(arr.getJSONObject(i), cleanOrg) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getOrgScimUsers(context: Context, org: String, startIndex: Int = 1, count: Int = 50): GHScimUsersPage {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank()) return GHScimUsersPage()
    val r = request(context, "/scim/v2/organizations/$cleanOrg/Users?startIndex=$startIndex&count=$count")
    if (!r.success) return GHScimUsersPage(error = r.body.ifBlank { "HTTP ${r.code}" })
    return try {
        val j = JSONObject(r.body)
        val resources = j.optJSONArray("Resources") ?: JSONArray()
        GHScimUsersPage(
            totalResults = j.optInt("totalResults", 0),
            startIndex = j.optInt("startIndex", startIndex),
            itemsPerPage = j.optInt("itemsPerPage", count),
            users = (0 until resources.length()).map { i -> parseGHScimUser(resources.getJSONObject(i)) }
        )
    } catch (e: Exception) { GHScimUsersPage(error = e.message ?: "parse error") }
}

internal suspend fun GitHubManager.getOrgSamlAuthorizations(context: Context, org: String, login: String = "", page: Int = 1): List<GHSamlAuthorization> {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank()) return emptyList()
    val query = buildString {
        append("?per_page=100&page=$page")
        if (login.isNotBlank()) append("&login=${URLEncoder.encode(login.trim(), "UTF-8")}")
    }
    val r = request(context, "/orgs/$cleanOrg/credential-authorizations$query")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHSamlAuthorization(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.removeOrgSamlAuthorization(context: Context, org: String, credentialId: Long): Boolean {
    val cleanOrg = org.trim()
    if (cleanOrg.isBlank() || credentialId <= 0L) return false
    return request(context, "/orgs/$cleanOrg/credential-authorizations/$credentialId", "DELETE").let { it.code == 204 || it.success }
}

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun parseGHOrg(j: JSONObject): GHOrg =
    GHOrg(login = j.optString("login"), avatarUrl = j.optString("avatar_url", ""), description = j.optString("description", ""))

private fun parseGHAuditLogEntry(j: JSONObject, fallbackOrg: String): GHAuditLogEntry =
    GHAuditLogEntry(
        id = j.optString("_document_id", j.optString("id", "")),
        action = j.optString("action", ""),
        actor = j.optString("actor", ""),
        createdAt = j.optString("@timestamp", j.optString("created_at", "")),
        org = j.optString("org", fallbackOrg),
        repo = j.optString("repo", ""),
        user = j.optString("user", ""),
        operationType = j.optString("operation_type", ""),
        transportProtocol = j.optString("transport_protocol", "")
    )

private fun parseGHScimUser(user: JSONObject): GHScimUser {
    val name = user.optJSONObject("name")
    val emails = user.optJSONArray("emails")?.let { arr ->
        (0 until arr.length()).mapNotNull { idx ->
            arr.optJSONObject(idx)?.optString("value")?.takeIf { it.isNotBlank() }
        }
    } ?: emptyList()
    return GHScimUser(
        id = user.optString("id", ""),
        userName = user.optString("userName", ""),
        displayName = user.optString("displayName", ""),
        givenName = name?.optString("givenName", "") ?: "",
        familyName = name?.optString("familyName", "") ?: "",
        active = user.optBoolean("active", false),
        externalId = user.optString("externalId", ""),
        emails = emails
    )
}

private fun parseGHSamlAuthorization(j: JSONObject): GHSamlAuthorization {
    val scopes = j.optJSONArray("scopes")?.let { scopeArr ->
        (0 until scopeArr.length()).mapNotNull { idx -> scopeArr.optString(idx).takeIf { it.isNotBlank() } }
    } ?: emptyList()
    return GHSamlAuthorization(
        login = j.optString("login", ""),
        credentialId = j.optLong("credential_id"),
        credentialType = j.optString("credential_type", ""),
        tokenLastEight = j.optString("token_last_eight", ""),
        authorizedAt = j.optString("credential_authorized_at", ""),
        accessedAt = j.optString("credential_accessed_at", ""),
        expiresAt = j.optString("authorized_credential_expires_at", ""),
        scopes = scopes
    )
}
