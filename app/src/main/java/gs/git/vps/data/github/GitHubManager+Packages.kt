package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHPackage
import gs.git.vps.data.github.model.GHPackageVersion
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Packages слоя GitHub API (GitHub Packages): пакеты пользователя/организации, их версии,
 * удаление и восстановление пакетов/версий. Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Сеть — через ядро `request()`, парсинг — чистые `parseGHX`.
 * Сигнатуры вызовов не менялись.
 */

private val githubPackageTypes = listOf("container", "docker", "npm", "maven", "nuget", "rubygems")

internal suspend fun GitHubManager.getUserPackages(context: Context, username: String, packageType: String = "all"): List<GHPackage> {
    if (packageType == "all") {
        return githubPackageTypes.flatMap { getUserPackages(context, username, it) }
            .distinctBy { "${it.packageType}/${it.name}/${it.ownerLogin}" }
    }
    val typeQuery = packageType.takeIf { it.isNotBlank() && it != "all" }?.let { "?package_type=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
    val r = request(context, "/users/${URLEncoder.encode(username, "UTF-8")}/packages$typeQuery", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return parsePackages(r.body)
}

internal suspend fun GitHubManager.getOrgPackages(context: Context, org: String, packageType: String = "all"): List<GHPackage> {
    if (packageType == "all") {
        return githubPackageTypes.flatMap { getOrgPackages(context, org, it) }
            .distinctBy { "${it.packageType}/${it.name}/${it.ownerLogin}" }
    }
    val typeQuery = packageType.takeIf { it.isNotBlank() && it != "all" }?.let { "?package_type=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
    val r = request(context, "/orgs/${URLEncoder.encode(org, "UTF-8")}/packages$typeQuery", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return parsePackages(r.body)
}

internal suspend fun GitHubManager.getPackage(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): GHPackage? {
    val r = request(context, "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parsePackage(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deletePackage(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): Boolean {
    val r = request(context, "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.getPackageVersions(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): List<GHPackageVersion> {
    val r = request(context, "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}/versions?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parsePackageVersion) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.deletePackageVersion(context: Context, ownerType: String, owner: String, packageType: String, packageName: String, versionId: Long): Boolean {
    val r = request(context, "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}/versions/$versionId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.restorePackage(context: Context, ownerType: String, owner: String, packageType: String, packageName: String): Boolean {
    val path = "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}/restore"
    val r = request(context, path, "POST", "{}", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.success
}

internal suspend fun GitHubManager.restorePackageVersion(context: Context, ownerType: String, owner: String, packageType: String, packageName: String, versionId: Long): Boolean {
    val path = "${packageOwnerPath(ownerType, owner)}/packages/${URLEncoder.encode(packageType, "UTF-8")}/${URLEncoder.encode(packageName, "UTF-8")}/versions/$versionId/restore"
    val r = request(context, path, "POST", "{}", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.success
}

// ─── Хелперы / парсеры (чистые, без IO) ──────────────────────────────────────

private fun packageOwnerPath(ownerType: String, owner: String): String {
    val encodedOwner = URLEncoder.encode(owner, "UTF-8")
    return if (ownerType == "org") "/orgs/$encodedOwner" else "/users/$encodedOwner"
}

private fun parsePackages(body: String): List<GHPackage> =
    try {
        val arr = JSONArray(body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parsePackage) }
    } catch (e: Exception) { emptyList() }

private fun parsePackage(j: JSONObject): GHPackage {
    val owner = j.optJSONObject("owner")
    val repository = j.optJSONObject("repository")
    return GHPackage(
        id = j.optLong("id"),
        name = j.optString("name", ""),
        packageType = j.optString("package_type", ""),
        visibility = j.optString("visibility", ""),
        versionCount = j.optInt("version_count", 0),
        url = j.optString("url", ""),
        htmlUrl = j.optString("html_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        ownerLogin = owner?.optString("login") ?: "",
        repositoryName = repository?.optString("full_name") ?: repository?.optString("name") ?: "",
        repositoryUrl = repository?.optString("html_url") ?: ""
    )
}

private fun parsePackageVersion(j: JSONObject): GHPackageVersion {
    val metadata = j.optJSONObject("metadata")
    val container = metadata?.optJSONObject("container")
    val tags = container?.optJSONArray("tags")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
    }.orEmpty()
    return GHPackageVersion(
        id = j.optLong("id"),
        name = j.optString("name", ""),
        url = j.optString("url", ""),
        htmlUrl = j.optString("html_url", j.optString("package_html_url", "")),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        tags = tags,
        packageType = metadata?.optString("package_type", "") ?: "",
        downloadCount = j.optInt("download_count", 0)
    )
}
