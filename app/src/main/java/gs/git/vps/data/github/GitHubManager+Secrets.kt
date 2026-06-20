package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHActionPublicKey
import gs.git.vps.data.github.model.GHActionSecret
import gs.git.vps.data.github.model.GHActionVariable
import gs.git.vps.data.github.model.GHEnvironmentSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Secrets слоя GitHub API — секреты и переменные Actions (repo + environment).
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Вся сеть — через ядро
 * `GitHubManager.request()`; значения секретов шифруются `GitHubSecretCrypto.encryptSecret`
 * публичным ключом репозитория (libsodium sealed box). Парсинг — чистые `parseGH*`.
 * Сигнатуры публичных вызовов не изменились при выносе.
 */

private const val SECRETS_TAG = "GH"

// --- Repo Actions secrets ---

internal suspend fun GitHubManager.getRepoActionsSecrets(context: Context, owner: String, repo: String): List<GHActionSecret> {
    val r = request(context, "/repos/$owner/$repo/actions/secrets?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("secrets") ?: JSONArray()
        (0 until arr.length()).map { i -> parseGHActionSecret(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getRepoActionsPublicKey(context: Context, owner: String, repo: String): GHActionPublicKey? {
    val r = request(context, "/repos/$owner/$repo/actions/secrets/public-key")
    if (!r.success) return null
    return try { parseGHActionPublicKey(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createOrUpdateRepoActionsSecret(context: Context, owner: String, repo: String, name: String, value: String): Boolean {
    return try {
        val publicKey = getRepoActionsPublicKey(context, owner, repo) ?: return false
        val encrypted = withContext(Dispatchers.Default) {
            GitHubSecretCrypto.encryptSecret(publicKey.key, value)
        }
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val body = JSONObject().apply {
            put("encrypted_value", encrypted)
            put("key_id", publicKey.keyId)
        }.toString()
        request(context, "/repos/$owner/$repo/actions/secrets/$encodedName", "PUT", body).let {
            it.code == 201 || it.code == 204 || it.success
        }
    } catch (e: Exception) {
        Log.e(SECRETS_TAG, "Save actions secret: ${e.message}")
        false
    }
}

internal suspend fun GitHubManager.deleteRepoActionsSecret(context: Context, owner: String, repo: String, name: String): Boolean {
    val encodedName = URLEncoder.encode(name, "UTF-8")
    return request(context, "/repos/$owner/$repo/actions/secrets/$encodedName", "DELETE").let { it.code == 204 || it.success }
}

// --- Environment secrets ---

internal suspend fun GitHubManager.getEnvironmentSecrets(context: Context, owner: String, repo: String, envName: String): List<GHEnvironmentSecret> {
    val encoded = URLEncoder.encode(envName, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/environments/$encoded/secrets?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("secrets") ?: JSONArray()
        (0 until arr.length()).map { i -> parseGHEnvironmentSecret(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createOrUpdateEnvironmentSecret(context: Context, owner: String, repo: String, envName: String, secretName: String, value: String): Boolean {
    return try {
        val encodedEnv = URLEncoder.encode(envName, "UTF-8")
        val encodedSecret = URLEncoder.encode(secretName, "UTF-8")
        val pubKey = getRepoActionsPublicKey(context, owner, repo) ?: return false
        val encrypted = withContext(Dispatchers.Default) {
            GitHubSecretCrypto.encryptSecret(pubKey.key, value)
        }
        val body = JSONObject().apply {
            put("encrypted_value", encrypted)
            put("key_id", pubKey.keyId)
        }.toString()
        request(context, "/repos/$owner/$repo/environments/$encodedEnv/secrets/$encodedSecret", "PUT", body).let {
            it.code == 201 || it.code == 204 || it.success
        }
    } catch (e: Exception) {
        Log.e(SECRETS_TAG, "Save env secret: ${e.message}")
        false
    }
}

internal suspend fun GitHubManager.deleteEnvironmentSecret(context: Context, owner: String, repo: String, envName: String, secretName: String): Boolean {
    val encodedEnv = URLEncoder.encode(envName, "UTF-8")
    val encodedSecret = URLEncoder.encode(secretName, "UTF-8")
    return request(context, "/repos/$owner/$repo/environments/$encodedEnv/secrets/$encodedSecret", "DELETE").let { it.code == 204 || it.success }
}

// --- Repo Actions variables ---

internal suspend fun GitHubManager.getRepoActionsVariables(context: Context, owner: String, repo: String): List<GHActionVariable> {
    val r = request(context, "/repos/$owner/$repo/actions/variables?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("variables") ?: JSONArray()
        (0 until arr.length()).map { i -> parseGHActionVariable(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.createRepoActionsVariable(context: Context, owner: String, repo: String, name: String, value: String): Boolean {
    val body = JSONObject().apply { put("name", name); put("value", value) }.toString()
    return request(context, "/repos/$owner/$repo/actions/variables", "POST", body).success
}

internal suspend fun GitHubManager.updateRepoActionsVariable(context: Context, owner: String, repo: String, name: String, value: String): Boolean {
    val encodedName = URLEncoder.encode(name, "UTF-8")
    val body = JSONObject().apply { put("name", name); put("value", value) }.toString()
    return request(context, "/repos/$owner/$repo/actions/variables/$encodedName", "PATCH", body).success
}

internal suspend fun GitHubManager.deleteRepoActionsVariable(context: Context, owner: String, repo: String, name: String): Boolean {
    val encodedName = URLEncoder.encode(name, "UTF-8")
    return request(context, "/repos/$owner/$repo/actions/variables/$encodedName", "DELETE").let { it.code == 204 || it.success }
}

// --- Парсинг: чистые функции JSON→модель, без IO ---

internal fun parseGHActionSecret(j: JSONObject): GHActionSecret =
    GHActionSecret(j.optString("name"), j.optString("created_at", ""), j.optString("updated_at", ""))

internal fun parseGHActionPublicKey(j: JSONObject): GHActionPublicKey =
    GHActionPublicKey(j.optString("key_id"), j.optString("key"))

internal fun parseGHActionVariable(j: JSONObject): GHActionVariable =
    GHActionVariable(j.optString("name"), j.optString("value"), j.optString("created_at", ""), j.optString("updated_at", ""))

internal fun parseGHEnvironmentSecret(j: JSONObject): GHEnvironmentSecret =
    GHEnvironmentSecret(j.optString("name"), j.optString("created_at", ""), j.optString("updated_at", ""))
