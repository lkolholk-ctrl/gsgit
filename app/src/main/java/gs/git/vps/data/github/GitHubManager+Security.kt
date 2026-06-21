package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHAdvisoryVulnerability
import gs.git.vps.data.github.model.GHCodeScanningAlert
import gs.git.vps.data.github.model.GHCommunityProfile
import gs.git.vps.data.github.model.GHCommunityProfileFile
import gs.git.vps.data.github.model.GHDependabotAlert
import gs.git.vps.data.github.model.GHRepositorySecurityAdvisory
import gs.git.vps.data.github.model.GHRepositorySecuritySettings
import gs.git.vps.data.github.model.GHSecretScanningAlert
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Security слоя GitHub API: dependabot / code-scanning / secret-scanning alerts,
 * repository security advisories (CRUD), community profile, security settings (automated fixes,
 * vulnerability alerts, private vulnerability reporting). Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Сеть — через ядро `request()`, парсинг — чистые `parseGHX`.
 * Сигнатуры вызовов не менялись. Rulesets/rule-suites — в GitHubManager+Rulesets.kt.
 *
 * `parseStringArray` — общий генерик-хелпер (та же реализация в core и в +Rulesets.kt);
 * продублирован как private, чтобы файл был самодостаточным.
 */

// ─── Alerts: dependabot / code-scanning / secret-scanning ────────────────────

internal suspend fun GitHubManager.getDependabotAlerts(context: Context, owner: String, repo: String): List<GHDependabotAlert> {
    val r = request(context, "/repos/$owner/$repo/dependabot/alerts?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseDependabotAlert(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getDependabotAlert(context: Context, owner: String, repo: String, number: Int): GHDependabotAlert? {
    val r = request(context, "/repos/$owner/$repo/dependabot/alerts/$number", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseDependabotAlert(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getCodeScanningAlerts(context: Context, owner: String, repo: String): List<GHCodeScanningAlert> {
    val r = request(context, "/repos/$owner/$repo/code-scanning/alerts?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseCodeScanningAlert(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getCodeScanningAlert(context: Context, owner: String, repo: String, number: Int): GHCodeScanningAlert? {
    val r = request(context, "/repos/$owner/$repo/code-scanning/alerts/$number", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseCodeScanningAlert(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getSecretScanningAlerts(context: Context, owner: String, repo: String): List<GHSecretScanningAlert> {
    val r = request(context, "/repos/$owner/$repo/secret-scanning/alerts?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseSecretScanningAlert(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getSecretScanningAlert(context: Context, owner: String, repo: String, number: Int): GHSecretScanningAlert? {
    val r = request(context, "/repos/$owner/$repo/secret-scanning/alerts/$number", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseSecretScanningAlert(JSONObject(r.body)) } catch (e: Exception) { null }
}

// ─── Repository security advisories ──────────────────────────────────────────

internal suspend fun GitHubManager.getRepositorySecurityAdvisories(context: Context, owner: String, repo: String): List<GHRepositorySecurityAdvisory> {
    val r = request(context, "/repos/$owner/$repo/security-advisories?per_page=100&sort=updated&direction=desc", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseRepositorySecurityAdvisory) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRepositorySecurityAdvisory(context: Context, owner: String, repo: String, ghsaId: String): GHRepositorySecurityAdvisory? {
    val encoded = URLEncoder.encode(ghsaId, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/security-advisories/$encoded", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRepositorySecurityAdvisory(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createRepositorySecurityAdvisory(context: Context, owner: String, repo: String, summary: String, severity: String, cveId: String = "", description: String = "", vulnerabilities: String = "[]"): GHRepositorySecurityAdvisory? {
    val body = JSONObject().apply {
        put("summary", summary)
        put("severity", severity)
        if (cveId.isNotBlank()) put("cve_id", cveId)
        if (description.isNotBlank()) put("description", description)
        put("vulnerabilities", JSONArray(vulnerabilities))
    }.toString()
    val r = request(context, "${repoPath(owner, repo, "/security-advisories")}", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRepositorySecurityAdvisory(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateRepositorySecurityAdvisory(context: Context, owner: String, repo: String, ghsaId: String, severity: String? = null, summary: String? = null, state: String? = null): GHRepositorySecurityAdvisory? {
    val body = JSONObject().apply {
        severity?.let { put("severity", it) }
        summary?.let { put("summary", it) }
        state?.let { put("state", it) }
    }.toString()
    val encoded = URLEncoder.encode(ghsaId, "UTF-8")
    val r = request(context, "${repoPath(owner, repo, "/security-advisories/$encoded")}", "PATCH", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRepositorySecurityAdvisory(JSONObject(r.body)) } catch (e: Exception) { null }
}

// ─── Community profile / security settings ───────────────────────────────────

internal suspend fun GitHubManager.getCommunityProfile(context: Context, owner: String, repo: String): GHCommunityProfile? {
    val r = request(context, "/repos/$owner/$repo/community/profile", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        val files = j.optJSONObject("files")
        GHCommunityProfile(
            healthPercentage = j.optInt("health_percentage", 0),
            description = j.optString("description", ""),
            documentationUrl = j.optString("documentation_url", ""),
            updatedAt = j.optString("updated_at", ""),
            files = parseCommunityFiles(files)
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getRepositorySecuritySettings(context: Context, owner: String, repo: String): GHRepositorySecuritySettings {
    val automated = request(context, "/repos/$owner/$repo/automated-security-fixes", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val vulnerability = request(context, "/repos/$owner/$repo/vulnerability-alerts", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    val privateReporting = request(context, "/repos/$owner/$repo/private-vulnerability-reporting", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return GHRepositorySecuritySettings(
        automatedSecurityFixes = automated.success && parseEnabledFlag(automated.body, default = true),
        automatedSecurityFixesPaused = parsePausedFlag(automated.body),
        vulnerabilityAlerts = vulnerability.success || vulnerability.code == 204,
        privateVulnerabilityReporting = privateReporting.success && parseEnabledFlag(privateReporting.body, default = true)
    )
}

internal suspend fun GitHubManager.setAutomatedSecurityFixes(context: Context, owner: String, repo: String, enabled: Boolean): Boolean {
    val r = request(context, "/repos/$owner/$repo/automated-security-fixes", if (enabled) "PUT" else "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.setVulnerabilityAlerts(context: Context, owner: String, repo: String, enabled: Boolean): Boolean {
    val r = request(context, "/repos/$owner/$repo/vulnerability-alerts", if (enabled) "PUT" else "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.setPrivateVulnerabilityReporting(context: Context, owner: String, repo: String, enabled: Boolean): Boolean {
    val r = request(context, "/repos/$owner/$repo/private-vulnerability-reporting", if (enabled) "PUT" else "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    return r.code == 204 || r.success
}

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun parseDependabotAlert(j: JSONObject): GHDependabotAlert {
    val adv = j.optJSONObject("security_advisory")
    return GHDependabotAlert(
        number = j.optInt("number"),
        state = j.optString("state"),
        severity = adv?.optString("severity") ?: "",
        summary = adv?.optString("summary") ?: "",
        description = adv?.optString("description") ?: "",
        packageName = j.optJSONObject("dependency")?.optJSONObject("package")?.optString("name") ?: "",
        ecosystem = j.optJSONObject("dependency")?.optJSONObject("package")?.optString("ecosystem") ?: "",
        manifestPath = j.optJSONObject("dependency")?.optString("manifest_path") ?: "",
        vulnerableRequirements = j.optJSONObject("dependency")?.optString("vulnerable_requirements") ?: "",
        ghsaId = adv?.optString("ghsa_id") ?: "",
        cveId = adv?.optString("cve_id") ?: "",
        htmlUrl = j.optString("html_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        fixedIn = adv?.optJSONArray("vulnerabilities")?.let { vulns ->
            (0 until vulns.length()).mapNotNull { index ->
                vulns.optJSONObject(index)?.optJSONObject("first_patched_version")?.optString("identifier")?.takeIf { it.isNotBlank() }
            }.distinct()
        } ?: emptyList()
    )
}

private fun parseCodeScanningAlert(j: JSONObject): GHCodeScanningAlert {
    val rule = j.optJSONObject("rule")
    val tool = j.optJSONObject("tool")
    val instance = j.optJSONObject("most_recent_instance")
    val message = instance?.optJSONObject("message")
    val location = instance?.optJSONObject("location")
    return GHCodeScanningAlert(
        number = j.optInt("number"),
        state = j.optString("state", ""),
        ruleId = rule?.optString("id") ?: "",
        ruleName = rule?.optString("name") ?: "",
        severity = rule?.optString("security_severity_level")?.takeIf { it.isNotBlank() && it != "null" }
            ?: rule?.optString("severity") ?: "",
        description = rule?.optString("description") ?: "",
        toolName = tool?.optString("name") ?: "",
        message = message?.optString("text") ?: "",
        path = location?.optString("path") ?: "",
        startLine = location?.optInt("start_line", 0) ?: 0,
        ref = instance?.optString("ref") ?: "",
        category = instance?.optString("category") ?: "",
        createdAt = j.optString("created_at", ""),
        fixedAt = j.optString("fixed_at", ""),
        dismissedAt = j.optString("dismissed_at", ""),
        dismissedReason = j.optString("dismissed_reason", ""),
        htmlUrl = j.optString("html_url", "")
    )
}

private fun parseSecretScanningAlert(j: JSONObject): GHSecretScanningAlert =
    GHSecretScanningAlert(
        number = j.optInt("number"),
        state = j.optString("state", ""),
        resolution = j.optString("resolution", ""),
        secretType = j.optString("secret_type", ""),
        secretTypeDisplayName = j.optString("secret_type_display_name", ""),
        secret = j.optString("secret", ""),
        validity = j.optString("validity", ""),
        public = j.optBoolean("public", false),
        pushProtectionBypassed = j.optBoolean("push_protection_bypassed", false),
        createdAt = j.optString("created_at", ""),
        resolvedAt = j.optString("resolved_at", ""),
        htmlUrl = j.optString("html_url", "")
    )

private fun parseRepositorySecurityAdvisory(j: JSONObject): GHRepositorySecurityAdvisory =
    GHRepositorySecurityAdvisory(
        ghsaId = j.optString("ghsa_id", ""),
        cveId = j.optString("cve_id", ""),
        url = j.optString("url", ""),
        htmlUrl = j.optString("html_url", ""),
        summary = j.optString("summary", ""),
        description = j.optString("description", ""),
        severity = j.optString("severity", ""),
        state = j.optString("state", ""),
        publishedAt = j.optString("published_at", ""),
        updatedAt = j.optString("updated_at", ""),
        withdrawnAt = j.optString("withdrawn_at", ""),
        cvssScore = j.optJSONObject("cvss")?.optDouble("score", 0.0) ?: 0.0,
        cweIds = parseStringArray(j.optJSONArray("cwe_ids")),
        vulnerabilities = parseSecurityAdvisoryVulnerabilities(j.optJSONArray("vulnerabilities"))
    )

private fun parseCommunityFiles(files: JSONObject?): List<GHCommunityProfileFile> {
    if (files == null) return emptyList()
    return files.keys().asSequence().map { key ->
        val value = files.opt(key)
        val objectValue = value as? JSONObject
        GHCommunityProfileFile(
            key = key,
            name = objectValue?.optString("name")?.takeIf { it.isNotBlank() } ?: key.replace('_', ' '),
            htmlUrl = objectValue?.optString("html_url") ?: "",
            present = objectValue != null && value.toString() != "null"
        )
    }.toList()
}

private fun parseSecurityAdvisoryVulnerabilities(arr: JSONArray?): List<GHAdvisoryVulnerability> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.let { j ->
            val pkg = j.optJSONObject("package")
            GHAdvisoryVulnerability(
                ecosystem = pkg?.optString("ecosystem", "") ?: "",
                packageName = pkg?.optString("name", "") ?: "",
                vulnerableRange = j.optString("vulnerable_version_range", ""),
                patchedVersions = j.optString("patched_versions", "")
            )
        }
    }
}

private fun parseEnabledFlag(body: String, default: Boolean): Boolean =
    try {
        if (body.isBlank()) default else JSONObject(body).optBoolean("enabled", default)
    } catch (_: Exception) {
        default
    }

private fun parsePausedFlag(body: String): Boolean =
    try {
        if (body.isBlank()) false else JSONObject(body).optBoolean("paused", false)
    } catch (_: Exception) {
        false
    }

private fun parseStringArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() && it != "null" } }
}
