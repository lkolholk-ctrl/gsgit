package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHContributor
import gs.git.vps.data.github.model.GHLicense
import gs.git.vps.data.github.model.GHLicenseDetail
import gs.git.vps.data.github.model.GHMeta
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен RepoMeta слоя GitHub API: README, метаданные github.com (/meta), рендер Markdown, языки репо,
 * emojis, gitignore-шаблоны, каталог лицензий, контрибьюторы. Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Сеть — через ядро `request()`. Сигнатуры вызовов не менялись.
 */

internal suspend fun GitHubManager.getReadme(context: Context, owner: String, repo: String): String {
    val r = request(context, "/repos/$owner/$repo/readme")
    if (!r.success) return ""
    return try {
        val j = JSONObject(r.body)
        val content = j.optString("content", "")
        String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
    } catch (e: Exception) { "" }
}

internal suspend fun GitHubManager.getGitHubMeta(context: Context): GHMeta? {
    val r = request(context, "/meta", trackErrors = false)
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHMeta(
            verifiablePasswordAuthentication = j.optBoolean("verifiable_password_authentication"),
            sshKeys = j.optJSONArray("ssh_keys")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            sshKeyFingerprints = j.optJSONArray("ssh_key_fingerprints")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            hooks = j.optJSONArray("hooks")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            web = j.optJSONArray("web")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            api = j.optJSONArray("api")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            git = j.optJSONArray("git")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            packages = j.optJSONArray("packages")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            pages = j.optJSONArray("pages")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
            importer = j.optJSONArray("importer")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.renderMarkdown(context: Context, text: String, mode: String = "markdown", contextRepo: String = ""): String {
    val body = JSONObject().apply {
        put("text", text)
        put("mode", mode)
        if (contextRepo.isNotBlank()) put("context", contextRepo)
    }.toString()
    val r = request(context, "/markdown", "POST", body, trackErrors = false)
    return if (r.success) r.body else ""
}

internal suspend fun GitHubManager.getLanguages(context: Context, owner: String, repo: String): Map<String, Long> {
    val r = request(context, "/repos/$owner/$repo/languages")
    if (!r.success) return emptyMap()
    return try {
        val j = JSONObject(r.body)
        val map = mutableMapOf<String, Long>()
        j.keys().forEach { key -> map[key] = j.optLong(key) }
        map
    } catch (e: Exception) { emptyMap() }
}

internal suspend fun GitHubManager.getEmojis(context: Context): Map<String, String> {
    val r = request(context, "/emojis", trackErrors = false)
    if (!r.success) return emptyMap()
    return try {
        val j = JSONObject(r.body)
        val map = mutableMapOf<String, String>()
        j.keys().forEach { key -> map[key] = j.optString(key) }
        map
    } catch (e: Exception) { emptyMap() }
}

internal suspend fun GitHubManager.getGitignoreTemplates(context: Context): List<String> {
    val r = request(context, "/gitignore/templates", trackErrors = false)
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { arr.optString(it)?.takeIf { s -> s.isNotBlank() } }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getGitignoreTemplate(context: Context, name: String): String? {
    val r = request(context, "/gitignore/templates/${encPath(name)}", trackErrors = false)
    if (!r.success) return null
    return try { JSONObject(r.body).optString("source") } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getLicenses(context: Context): List<GHLicense> {
    val r = request(context, "/licenses", trackErrors = false)
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHLicense(j.optString("key"), j.optString("name"), j.optString("spdx_id"), j.optString("url"), j.optBoolean("featured"))
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getLicense(context: Context, key: String): GHLicenseDetail? {
    val r = request(context, "/licenses/${encPath(key)}", trackErrors = false)
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHLicenseDetail(j.optString("key"), j.optString("name"), j.optString("spdx_id"),
            j.optString("description", ""), j.optString("body", ""), j.optString("html_url", ""),
            j.optBoolean("featured"))
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getContributors(context: Context, owner: String, repo: String): List<GHContributor> {
    val r = request(context, "/repos/$owner/$repo/contributors?per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHContributor(j.optString("login"), j.optString("avatar_url", ""), j.optInt("contributions", 0))
        }
    } catch (e: Exception) { emptyList() }
}
