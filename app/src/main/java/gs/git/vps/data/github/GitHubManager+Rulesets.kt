package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHRuleSuite
import gs.git.vps.data.github.model.GHRuleset
import gs.git.vps.data.github.model.GHRulesetBypassActor
import gs.git.vps.data.github.model.GHRulesetDetail
import gs.git.vps.data.github.model.GHRulesetRule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Rulesets слоя GitHub API: repository rulesets (CRUD) и rule-suites (read).
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`,
 * парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * `parseStringArray` — общий генерик-хелпер (та же реализация есть в core для parseAppInstallation
 * и в GitHubManager+Security.kt); продублирован как private, чтобы файл был самодостаточным.
 */

internal suspend fun GitHubManager.getRulesets(context: Context, owner: String, repo: String): List<GHRuleset> {
    val r = request(context, "/repos/$owner/$repo/rulesets?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHRuleset(
                id = j.optInt("id"),
                name = j.optString("name"),
                enforcement = j.optString("enforcement"),
                rulesCount = j.optJSONArray("rules")?.length() ?: 0,
                target = j.optString("target", ""),
                sourceType = j.optString("source_type", ""),
                createdAt = j.optString("created_at", ""),
                updatedAt = j.optString("updated_at", ""),
                htmlUrl = "https://github.com/$owner/$repo/settings/rules/${j.optInt("id")}"
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRulesetDetail(context: Context, owner: String, repo: String, rulesetId: Int): GHRulesetDetail? {
    val r = request(context, "/repos/$owner/$repo/rulesets/$rulesetId", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHRulesetDetail(
            id = j.optInt("id"),
            name = j.optString("name", ""),
            target = j.optString("target", ""),
            sourceType = j.optString("source_type", ""),
            source = j.optString("source", ""),
            enforcement = j.optString("enforcement", ""),
            createdAt = j.optString("created_at", ""),
            updatedAt = j.optString("updated_at", ""),
            rules = parseRulesetRules(j.optJSONArray("rules")),
            bypassActors = parseRulesetBypassActors(j.optJSONArray("bypass_actors")),
            refNameIncludes = parseStringArray(j.optJSONObject("conditions")?.optJSONObject("ref_name")?.optJSONArray("include")),
            refNameExcludes = parseStringArray(j.optJSONObject("conditions")?.optJSONObject("ref_name")?.optJSONArray("exclude")),
            htmlUrl = "https://github.com/$owner/$repo/settings/rules/${j.optInt("id")}"
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getRuleSuites(context: Context, owner: String, repo: String): List<GHRuleSuite> {
    val r = request(context, "/repos/$owner/$repo/rule-suites?per_page=100", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("rule_suites") ?: JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(::parseRuleSuite)
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRuleSuite(context: Context, owner: String, repo: String, suiteId: Long): GHRuleSuite? {
    val r = request(context, "/repos/$owner/$repo/rule-suites/$suiteId", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRuleSuite(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createRuleset(
    context: Context,
    owner: String,
    repo: String,
    name: String,
    target: String,
    enforcement: String,
    includeRefs: List<String>,
    excludeRefs: List<String>,
    rulesJson: String
): GHRulesetDetail? {
    val body = buildRulesetPayload(name, target, enforcement, includeRefs, excludeRefs, rulesJson) ?: return null
    val r = request(context, "/repos/$owner/$repo/rulesets", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRulesetDetail(JSONObject(r.body), owner, repo) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateRuleset(
    context: Context,
    owner: String,
    repo: String,
    rulesetId: Int,
    name: String,
    target: String,
    enforcement: String,
    includeRefs: List<String>,
    excludeRefs: List<String>,
    rulesJson: String
): GHRulesetDetail? {
    val body = buildRulesetPayload(name, target, enforcement, includeRefs, excludeRefs, rulesJson) ?: return null
    val r = request(context, "/repos/$owner/$repo/rulesets/$rulesetId", "PUT", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseRulesetDetail(JSONObject(r.body), owner, repo) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deleteRuleset(context: Context, owner: String, repo: String, rulesetId: Int): Boolean =
    request(context, "/repos/$owner/$repo/rulesets/$rulesetId", "DELETE", extraHeaders = mapOf("Accept" to "application/vnd.github+json")).let { it.code == 204 || it.success }

// ─── Хелперы / парсеры (чистые, без IO) ──────────────────────────────────────

private fun buildRulesetPayload(
    name: String,
    target: String,
    enforcement: String,
    includeRefs: List<String>,
    excludeRefs: List<String>,
    rulesJson: String
): String? {
    val rules = try { JSONArray(rulesJson.ifBlank { "[]" }) } catch (e: Exception) { return null }
    return JSONObject().apply {
        put("name", name)
        put("target", target)
        put("enforcement", enforcement)
        put("conditions", JSONObject().apply {
            put("ref_name", JSONObject().apply {
                put("include", JSONArray(includeRefs.ifEmpty { listOf("~DEFAULT_BRANCH") }))
                put("exclude", JSONArray(excludeRefs))
            })
        })
        put("rules", rules)
    }.toString()
}

private fun parseRulesetDetail(j: JSONObject, owner: String, repo: String): GHRulesetDetail =
    GHRulesetDetail(
        id = j.optInt("id"),
        name = j.optString("name", ""),
        target = j.optString("target", ""),
        sourceType = j.optString("source_type", ""),
        source = j.optString("source", ""),
        enforcement = j.optString("enforcement", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        rules = parseRulesetRules(j.optJSONArray("rules")),
        bypassActors = parseRulesetBypassActors(j.optJSONArray("bypass_actors")),
        refNameIncludes = parseStringArray(j.optJSONObject("conditions")?.optJSONObject("ref_name")?.optJSONArray("include")),
        refNameExcludes = parseStringArray(j.optJSONObject("conditions")?.optJSONObject("ref_name")?.optJSONArray("exclude")),
        htmlUrl = j.optJSONObject("_links")?.optJSONObject("html")?.optString("href")
            ?: "https://github.com/$owner/$repo/settings/rules/${j.optInt("id")}"
    )

private fun parseRuleSuite(j: JSONObject): GHRuleSuite =
    GHRuleSuite(
        id = j.optLong("id"),
        actor = j.optJSONObject("actor")?.optString("login") ?: "",
        beforeSha = j.optString("before_sha", ""),
        afterSha = j.optString("after_sha", ""),
        ref = j.optString("ref", ""),
        status = j.optString("status", ""),
        result = j.optString("result", ""),
        evaluationResult = j.optString("evaluation_result", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", "")
    )

private fun parseRulesetRules(arr: JSONArray?): List<GHRulesetRule> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val j = arr.optJSONObject(i) ?: return@mapNotNull null
        val parameters = j.optJSONObject("parameters")
        GHRulesetRule(
            type = j.optString("type", ""),
            parameters = parameters?.keys()?.asSequence()?.map { key ->
                key to parameters.opt(key).toString()
            }?.toList().orEmpty()
        )
    }
}

private fun parseRulesetBypassActors(arr: JSONArray?): List<GHRulesetBypassActor> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val j = arr.optJSONObject(i) ?: return@mapNotNull null
        GHRulesetBypassActor(
            actorId = j.optLong("actor_id"),
            actorType = j.optString("actor_type", ""),
            bypassMode = j.optString("bypass_mode", "")
        )
    }
}

private fun parseStringArray(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() && it != "null" } }
}
