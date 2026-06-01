package gs.git.vps.data.github

import android.content.Context
import android.util.Base64
import android.util.Log
import gs.git.vps.security.NativeSecurity
import org.json.JSONArray
import org.json.JSONObject

object GitHubAuth {
    private const val TAG = "GH-Auth"
    internal const val PREFS = "github_prefs"
    private const val KEY_TOKEN_ENC = "token_enc"
    private const val KEY_API_ERRORS = "api_error_log"
    private const val MAX_API_ERROR_LOG = 30

    fun saveToken(context: Context, token: String) {
        try {
            val encrypted = NativeSecurity.encryptToken(token)
            val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN_ENC, encoded)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "encryptToken failed, falling back to plain: ${e.message}")
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN_ENC, token)
                .apply()
        }
    }

    fun getToken(context: Context): String {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN_ENC, "") ?: ""
        if (raw.isBlank()) return ""
        return try {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            NativeSecurity.decryptToken(bytes)
        } catch (e: Exception) {
            Log.w(TAG, "decryptToken failed, returning raw: ${e.message}")
            raw
        }
    }

    fun isLoggedIn(context: Context): Boolean = getToken(context).isNotBlank()

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun getApiErrorLog(context: Context): List<GHApiErrorLogEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_API_ERRORS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { index ->
                val item = arr.optJSONObject(index) ?: return@mapNotNull null
                GHApiErrorLogEntry(
                    timestamp = item.optLong("timestamp"),
                    method = item.optString("method"),
                    endpoint = item.optString("endpoint"),
                    statusCode = item.optInt("statusCode"),
                    message = item.optString("message"),
                    body = item.optString("body"),
                    requestId = item.optString("requestId"),
                    rateRemaining = item.optString("rateRemaining"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearApiErrorLog(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_API_ERRORS).apply()
    }

    internal fun recordApiError(context: Context, endpoint: String, method: String, result: GitHubManager.ApiResult) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = try {
            JSONArray(prefs.getString(KEY_API_ERRORS, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        val entry = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("method", method)
            put("endpoint", endpoint.take(220))
            put("statusCode", result.code)
            put("message", apiErrorMessage(result).take(300))
            put("body", result.body.trim().take(900))
            put("requestId", result.headers["x-github-request-id"].orEmpty())
            put("rateRemaining", result.headers["x-ratelimit-remaining"].orEmpty())
        }
        val next = JSONArray().apply {
            put(entry)
            for (i in 0 until minOf(current.length(), MAX_API_ERROR_LOG - 1)) {
                current.optJSONObject(i)?.let { put(it) }
            }
        }
        prefs.edit().putString(KEY_API_ERRORS, next.toString()).apply()
    }

    private fun apiErrorMessage(result: GitHubManager.ApiResult): String {
        val fallback = if (result.code > 0) "HTTP ${result.code}" else "Network error"
        if (result.body.isBlank()) return fallback
        return try {
            val json = JSONObject(result.body)
            val message = json.optString("message").takeIf { it.isNotBlank() }
            val errors = json.optJSONArray("errors")
            val details = if (errors != null) {
                (0 until errors.length()).mapNotNull { index ->
                    val item = errors.opt(index)
                    when (item) {
                        is JSONObject -> listOf(
                            item.optString("field"),
                            item.optString("code"),
                            item.optString("message")
                        ).filter { it.isNotBlank() && it != "null" }.joinToString(" ")
                        else -> item?.toString()
                    }?.takeIf { it.isNotBlank() && it != "null" }
                }.take(3).joinToString("; ")
            } else ""
            listOfNotNull(message, details.takeIf { it.isNotBlank() }).joinToString(": ").ifBlank { fallback }
        } catch (_: Exception) {
            result.body.trim().take(220).ifBlank { fallback }
        }
    }
}
