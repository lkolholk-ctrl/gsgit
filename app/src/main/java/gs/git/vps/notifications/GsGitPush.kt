package gs.git.vps.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import gs.git.vps.R
import gs.git.vps.data.github.GitHubAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент моментальных пушей: регистрация устройства на api.gsgit.org и
 * инициализация Firebase БЕЗ google-services.json (значения — в ресурсах
 * gsgit_push.xml; пустые значения = функция выключена, поведение как раньше).
 *
 * Это НЕ GitHub API, поэтому ядро GitHubManager.request() тут не используется —
 * у бэкенда пушей свой маленький HTTP-клиент.
 */
object GsGitPush {

    private const val TAG = "GsGitPush"
    private const val PREFS = "github_prefs"
    private const val KEY_ENABLED = "push_instant_enabled"
    private const val KEY_LAST_TOKEN = "push_last_fcm_token"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isConfigured(context: Context): Boolean =
        context.getString(R.string.gsgit_firebase_project_id).isNotBlank() &&
        context.getString(R.string.gsgit_firebase_app_id).isNotBlank() &&
        context.getString(R.string.gsgit_firebase_api_key).isNotBlank()

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) registerAsync(context) else unregisterAsync(context)
    }

    /** Ручная инициализация FirebaseApp из ресурсов. Безопасно звать повторно. */
    fun ensureInit(context: Context): Boolean {
        if (!isConfigured(context)) return false
        if (FirebaseApp.getApps(context).isNotEmpty()) return true
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(context.getString(R.string.gsgit_firebase_project_id))
                .setApplicationId(context.getString(R.string.gsgit_firebase_app_id))
                .setApiKey(context.getString(R.string.gsgit_firebase_api_key))
                .setGcmSenderId(context.getString(R.string.gsgit_firebase_sender_id))
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed")
            false
        }
    }

    /** Регистрация текущего устройства на бэкенде. Тихий no-op, если не настроено/не залогинен. */
    suspend fun register(context: Context): Boolean {
        if (!ensureInit(context) || !isEnabled(context)) return false
        val userToken = GitHubAuth.getValidGitHubAppUserToken(context)
        if (userToken.isBlank()) return false
        val fcmToken = currentFcmToken() ?: return false
        return registerToken(context, userToken, fcmToken)
    }

    /** Используется и из onNewToken (FCM сменил токен устройства). */
    suspend fun registerToken(context: Context, userToken: String, fcmToken: String): Boolean {
        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
        val body = JSONObject()
            .put("fcmToken", fcmToken)
            .put("device", deviceName)
            .toString()
        val ok = post(
            context, "/register", body,
            headers = mapOf("Authorization" to "Bearer $userToken"),
        )
        if (ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_TOKEN, fcmToken).apply()
        }
        return ok
    }

    fun registerAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { runCatching { register(app) } }
    }

    /** Снятие устройства с пушей (логаут/выключение тумблера). Авторизации не требует. */
    fun unregisterAsync(context: Context) {
        val app = context.applicationContext
        val last = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_TOKEN, "").orEmpty()
        if (last.isBlank()) return
        scope.launch {
            runCatching {
                post(app, "/unregister", JSONObject().put("fcmToken", last).toString())
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().remove(KEY_LAST_TOKEN).apply()
            }
        }
    }

    fun onNewToken(context: Context, fcmToken: String) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                if (!isEnabled(app)) return@runCatching
                val userToken = GitHubAuth.getValidGitHubAppUserToken(app)
                if (userToken.isNotBlank()) registerToken(app, userToken, fcmToken)
            }
        }
    }

    private suspend fun currentFcmToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> if (cont.isActive) cont.resume(token) {} }
            .addOnFailureListener { if (cont.isActive) cont.resume(null) {} }
    }

    private suspend fun post(
        context: Context,
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val base = context.getString(R.string.gsgit_push_api_base).trimEnd('/')
            conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "GsGit-Android")
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            val code = conn.responseCode
            (if (code in 200..299) conn.inputStream else conn.errorStream)?.close()
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "push api $path failed: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }
}
