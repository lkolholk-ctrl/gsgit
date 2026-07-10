package gs.git.vps.data.github

import gs.git.vps.data.github.model.GHDeviceCode
import gs.git.vps.data.github.model.GHDeviceTokenResult
import gs.git.vps.data.github.model.GHOAuthTokenInfo
import org.json.JSONObject

/**
 * Домен Auth слоя GitHub API: управление OAuth-app токенами (check/reset/delete token, delete grant)
 * и OAuth device-flow (initiate/poll). Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Запросы — через ядровый `requestBasic()` (basic-auth вариант). Сигнатуры вызовов не менялись.
 *
 * Валидация PAT (validateToken/getCopilotToken) оставлена в core — она завязана на вложенный
 * GitHubManager.TokenValidation и базовое token-хранилище.
 */

internal suspend fun GitHubManager.checkOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): GHOAuthTokenInfo? {
    val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
    val r = requestBasic("/applications/${clientId.trim()}/token", "POST", body, clientId.trim(), clientSecret)
    if (!r.success) return null
    return try { parseOAuthTokenInfo(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.resetOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): GHOAuthTokenInfo? {
    val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
    val r = requestBasic("/applications/${clientId.trim()}/token", "PATCH", body, clientId.trim(), clientSecret)
    if (!r.success) return null
    return try { parseOAuthTokenInfo(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deleteOAuthAppToken(clientId: String, clientSecret: String, accessToken: String): Boolean {
    val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
    return requestBasic("/applications/${clientId.trim()}/token", "DELETE", body, clientId.trim(), clientSecret).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.deleteOAuthAppGrant(clientId: String, clientSecret: String, accessToken: String): Boolean {
    val body = JSONObject().apply { put("access_token", accessToken.trim()) }.toString()
    return requestBasic("/applications/${clientId.trim()}/grant", "DELETE", body, clientId.trim(), clientSecret).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.initiateDeviceFlow(clientId: String): GHDeviceCode? {
    val body = JSONObject().apply {
        put("client_id", clientId)
        put("scope", "read:user repo write:repo_hook admin:repo_hook copilot")
    }.toString()
    val webUrl = getWebUrl()
    val r = requestBasic("$webUrl/login/device/code", "POST", body, clientId, "")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHDeviceCode(
            deviceCode = j.optString("device_code"),
            userCode = j.optString("user_code"),
            verificationUri = j.optString("verification_uri"),
            expiresIn = j.optInt("expires_in"),
            interval = j.optInt("interval", 5)
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.pollDeviceToken(clientId: String, deviceCode: String): GHDeviceTokenResult {
    val body = JSONObject().apply {
        put("client_id", clientId)
        put("device_code", deviceCode)
        put("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
    }.toString()
    val webUrl = getWebUrl()
    val r = requestBasic("$webUrl/login/oauth/access_token", "POST", body, clientId, "")
    return try {
        val j = JSONObject(r.body)
        val error = j.optString("error", "")
        if (error.isBlank()) {
            GHDeviceTokenResult(token = j.optString("access_token"), error = null)
        } else {
            GHDeviceTokenResult(token = null, error = error)
        }
    } catch (e: Exception) {
        GHDeviceTokenResult(token = null, error = "parse_error")
    }
}

// ─── Парсер (чистый, без IO) ─────────────────────────────────────────────────

private fun parseOAuthTokenInfo(j: JSONObject): GHOAuthTokenInfo {
    val app = j.optJSONObject("app")
    val scopes = j.optJSONArray("scopes")?.let { arr ->
        (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
    } ?: emptyList()
    return GHOAuthTokenInfo(
        id = j.optLong("id"),
        url = j.optString("url", ""),
        appName = app?.optString("name", "") ?: "",
        appUrl = app?.optString("url", "") ?: "",
        clientId = app?.optString("client_id", "") ?: "",
        tokenLastEight = j.optString("token_last_eight", ""),
        note = j.optString("note", ""),
        noteUrl = j.optString("note_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", ""),
        scopes = scopes,
        fingerprint = j.optString("fingerprint", ""),
        token = j.optString("token", "")
    )
}
