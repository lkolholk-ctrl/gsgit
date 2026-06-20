package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHWebhook
import gs.git.vps.data.github.model.GHWebhookConfig
import gs.git.vps.data.github.model.GHWebhookDelivery
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Webhooks слоя GitHub API (вебхуки репозитория и организации). Нарезан по эталону Releases
 * (см. docs/decomposition-log.md). Вся сеть — через ядро `GitHubManager.request()`; парсинг — чистые
 * `parseGHWebhook*`. Сигнатуры публичных вызовов не изменились при выносе.
 */

// --- Org hooks ---

internal suspend fun GitHubManager.getOrgHooks(context: Context, org: String): List<GHWebhook> {
    val r = request(context, "/orgs/${encPath(org)}/hooks?per_page=30", extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHWebhook(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createOrgHook(context: Context, org: String, url: String, events: List<String> = listOf("push"), active: Boolean = true, secret: String = ""): GHWebhook? {
    val body = JSONObject().apply {
        put("url", url)
        put("events", JSONArray(events))
        put("active", active)
        if (secret.isNotBlank()) put("secret", secret)
    }.toString()
    val r = request(context, "/orgs/${encPath(org)}/hooks", "POST", body, extraHeaders = mapOf("Accept" to "application/vnd.github+json"))
    if (!r.success) return null
    return try { parseGHWebhook(JSONObject(r.body)) } catch (e: Exception) { null }
}

// --- Repo webhooks ---

internal suspend fun GitHubManager.getWebhooks(context: Context, owner: String, repo: String): List<GHWebhook> {
    val r = request(context, "/repos/$owner/$repo/hooks?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHWebhook(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getWebhook(context: Context, owner: String, repo: String, hookId: Long): GHWebhook? {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId")
    if (!r.success) return null
    return try { parseGHWebhook(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createWebhook(context: Context, owner: String, repo: String, config: Map<String, String>, events: List<String>, active: Boolean = true): Boolean {
    val configJson = JSONObject().apply { config.forEach { (k, v) -> put(k, v) } }
    val body = JSONObject().apply {
        put("name", "web")
        put("config", configJson)
        put("events", JSONArray(events))
        put("active", active)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/hooks", "POST", body)
    return r.success
}

internal suspend fun GitHubManager.updateWebhook(context: Context, owner: String, repo: String, hookId: Long, config: Map<String, String>, events: List<String>, active: Boolean = true): Boolean {
    val configJson = JSONObject().apply { config.forEach { (k, v) -> put(k, v) } }
    val body = JSONObject().apply {
        put("config", configJson)
        put("events", JSONArray(events))
        put("active", active)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId", "PATCH", body)
    return r.success
}

internal suspend fun GitHubManager.pingWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/pings", "POST", "{}")
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.testWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/tests", "POST", "{}")
    return r.code == 204 || r.success
}

internal suspend fun GitHubManager.getWebhookConfig(context: Context, owner: String, repo: String, hookId: Long): GHWebhookConfig? {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/config")
    if (!r.success) return null
    return try { parseGHWebhookConfig(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateWebhookConfig(context: Context, owner: String, repo: String, hookId: Long, config: Map<String, String>): Boolean {
    val body = JSONObject().apply { config.forEach { (k, v) -> put(k, v) } }.toString()
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/config", "PATCH", body)
    return r.success
}

internal suspend fun GitHubManager.getWebhookDeliveries(context: Context, owner: String, repo: String, hookId: Long): List<GHWebhookDelivery> {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/deliveries?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).mapNotNull { i ->
            parseGHWebhookDelivery(arr.optJSONObject(i) ?: return@mapNotNull null)
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getWebhookDelivery(context: Context, owner: String, repo: String, hookId: Long, deliveryId: Long): GHWebhookDelivery? {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/deliveries/$deliveryId")
    if (!r.success) return null
    return try { parseGHWebhookDelivery(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.redeliverWebhookDelivery(context: Context, owner: String, repo: String, hookId: Long, deliveryId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId/deliveries/$deliveryId/attempts", "POST", "{}")
    return r.code == 202 || r.success
}

internal suspend fun GitHubManager.deleteWebhook(context: Context, owner: String, repo: String, hookId: Long): Boolean {
    val r = request(context, "/repos/$owner/$repo/hooks/$hookId", "DELETE")
    return r.code == 204 || r.success
}

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHWebhookDelivery(j: JSONObject): GHWebhookDelivery {
    val request = j.optJSONObject("request")
    val response = j.optJSONObject("response")
    return GHWebhookDelivery(
        id = j.optLong("id"),
        guid = j.optString("guid", ""),
        event = j.optString("event", ""),
        action = j.optString("action", ""),
        deliveredAt = j.optString("delivered_at", ""),
        duration = j.optDouble("duration", 0.0),
        status = j.optString("status", ""),
        statusCode = j.optInt("status_code", 0),
        redelivery = j.optBoolean("redelivery", false),
        requestHeaders = parseHeaderMap(request?.optJSONObject("headers")),
        requestPayload = request?.opt("payload")?.toString() ?: "",
        responseHeaders = parseHeaderMap(response?.optJSONObject("headers")),
        responsePayload = response?.opt("payload")?.toString() ?: ""
    )
}

internal fun parseGHWebhookConfig(j: JSONObject): GHWebhookConfig =
    GHWebhookConfig(
        url = j.optString("url", ""),
        contentType = j.optString("content_type", "json"),
        insecureSsl = j.optString("insecure_ssl", "0"),
        secret = j.optString("secret", "")
    )

internal fun parseGHWebhook(j: JSONObject): GHWebhook {
    val config = j.optJSONObject("config")
    val lastResponse = j.optJSONObject("last_response")
    return GHWebhook(
        id = j.optLong("id"),
        name = j.optString("name"),
        url = config?.optString("url") ?: "",
        contentType = config?.optString("content_type") ?: "json",
        insecureSsl = config?.optString("insecure_ssl") ?: "0",
        events = j.optJSONArray("events")?.let { ev -> (0 until ev.length()).map { ev.getString(it) } } ?: emptyList(),
        active = j.optBoolean("active", true),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        lastResponseCode = lastResponse?.optInt("code", 0) ?: 0,
        lastResponseStatus = lastResponse?.optString("status", "") ?: "",
        lastResponseMessage = lastResponse?.optString("message", "") ?: ""
    )
}

private fun parseHeaderMap(headers: JSONObject?): List<Pair<String, String>> {
    if (headers == null) return emptyList()
    return headers.keys().asSequence().map { key -> key to headers.optString(key, "") }.toList()
}
