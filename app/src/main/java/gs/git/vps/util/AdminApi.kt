package gs.git.vps.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент встроенной админки GsGit: правка appconfig (техработы, версии, чейнджлог)
 * и рассылка анонсов. Все запросы — на api.gsgit.org с X-Admin-Key; ключ хранится
 * локально и известен только владельцу сервера (/data/admin.key на VPS).
 */
object AdminApi {

    private const val TAG = "AdminApi"
    private const val API_BASE = "https://api.gsgit.org"
    private const val PREFS = "github_prefs"
    private const val KEY_ADMIN = "server_admin_key"

    fun savedKey(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ADMIN, "").orEmpty()

    fun saveKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ADMIN, key.trim()).apply()
    }

    /** Текущий конфиг с сервера (публичный GET, ключ не нужен). */
    suspend fun fetchConfig(context: Context): AppUpdate.Config? = AppUpdate.fetch(context)

    /**
     * Проверка, что сохранённый ключ настоящий (сервер принял пустое обновление
     * конфига). Используется для владельческого байпаса гейтов техработ/обновлений —
     * иначе включённый maintenance запер бы и саму админку.
     */
    suspend fun validateKey(context: Context): Boolean {
        if (savedKey(context).isBlank()) return false
        return post(context, "/admin/appconfig", "{}") != null
    }

    /** Сохранить конфиг. true = сервер принял (значит и ключ верный). */
    suspend fun saveConfig(
        context: Context,
        maintenanceSoon: String,
        maintenance: String,
        latestVersion: String,
        minVersion: String,
        changelog: String,
        downloadUrl: String,
    ): Boolean {
        val body = JSONObject()
            .put("maintenanceSoon", maintenanceSoon)
            .put("maintenance", maintenance)
            .put("latestVersion", latestVersion.trim().removePrefix("v"))
            .put("minVersion", minVersion.trim().removePrefix("v"))
            .put("changelog", changelog)
            .put("downloadUrl", downloadUrl.trim())
        return post(context, "/admin/appconfig", body.toString()) != null
    }

    /** Разослать анонс всем устройствам. Возвращает delivered или null при ошибке. */
    suspend fun announce(context: Context, title: String, text: String, url: String): Int? {
        val body = JSONObject().put("title", title).put("body", text).put("url", url)
        val resp = post(context, "/announce", body.toString()) ?: return null
        return try { JSONObject(resp).optInt("delivered", 0) } catch (_: Exception) { 0 }
    }

    private suspend fun post(context: Context, path: String, body: String): String? =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(API_BASE + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "GsGit-Android")
                    setRequestProperty("X-Admin-Key", savedKey(context))
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.close()
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "admin $path failed: ${e.message}")
                null
            } finally {
                conn?.disconnect()
            }
        }
}
