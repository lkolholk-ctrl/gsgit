package gs.git.vps.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Рантайм-конфиг приложения с gsgit.org/app.json — управляется правкой файла
 * на сервере, без пересборки приложения:
 *   maintenance   — текст плашки техработ ("" = плашки нет)
 *   latestVersion — актуальная версия: старее неё → мягкий диалог «доступно обновление»
 *   minVersion    — минимально допустимая: старее неё → блокирующий диалог без входа
 *   changelog     — что добавлено/исправлено (показывается в диалоге обновления)
 *   downloadUrl   — куда вести за APK
 *
 * Fail-open: если конфиг недоступен (оффлайн/сервер лёг), приложение работает как обычно.
 */
object AppUpdate {

    private const val TAG = "AppUpdate"
    private const val CONFIG_URL = "https://gsgit.org/app.json"

    data class Config(
        val maintenance: String,
        val latestVersion: String,
        val minVersion: String,
        val changelog: String,
        val downloadUrl: String,
    )

    suspend fun fetch(@Suppress("UNUSED_PARAMETER") context: Context): Config? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(CONFIG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "GsGit-Android")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            Config(
                maintenance = json.optString("maintenance", ""),
                latestVersion = json.optString("latestVersion", ""),
                minVersion = json.optString("minVersion", ""),
                changelog = json.optString("changelog", ""),
                downloadUrl = json.optString("downloadUrl", "https://github.com/lkolholk-ctrl/gsgit/releases/latest"),
            )
        } catch (e: Exception) {
            Log.w(TAG, "config fetch failed: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** true, если current старее required (сравнение по числовым сегментам "1.0.76"). */
    fun isOlder(current: String, required: String): Boolean {
        if (current.isBlank() || required.isBlank()) return false
        val a = current.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val b = required.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x < y
        }
        return false
    }
}
