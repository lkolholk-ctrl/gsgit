package gs.git.vps.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed encrypted storage for sensitive tokens.
 * Uses Android Keystore via EncryptedSharedPreferences (AES-256).
 */
class TokenRepository(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            appContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun saveGitHubAppSession(session: GitHubAppTokenSession) {
        prefs.edit()
            .putString(KEY_APP_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_APP_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_APP_ACCESS_EXPIRES_AT, session.accessTokenExpiresAt)
            .putLong(KEY_APP_REFRESH_EXPIRES_AT, session.refreshTokenExpiresAt)
            .apply()
    }

    fun getGitHubAppSession(): GitHubAppTokenSession = GitHubAppTokenSession(
        accessToken = prefs.getString(KEY_APP_ACCESS_TOKEN, "") ?: "",
        refreshToken = prefs.getString(KEY_APP_REFRESH_TOKEN, "") ?: "",
        accessTokenExpiresAt = prefs.getLong(KEY_APP_ACCESS_EXPIRES_AT, 0L),
        refreshTokenExpiresAt = prefs.getLong(KEY_APP_REFRESH_EXPIRES_AT, 0L),
    )

    fun clearGitHubAppSession() {
        prefs.edit()
            .remove(KEY_APP_ACCESS_TOKEN)
            .remove(KEY_APP_REFRESH_TOKEN)
            .remove(KEY_APP_ACCESS_EXPIRES_AT)
            .remove(KEY_APP_REFRESH_EXPIRES_AT)
            .apply()
    }

    companion object {
        private const val PREFS_FILE = "gsgit_encrypted_tokens"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_APP_ACCESS_TOKEN = "github_app_access_token"
        private const val KEY_APP_REFRESH_TOKEN = "github_app_refresh_token"
        private const val KEY_APP_ACCESS_EXPIRES_AT = "github_app_access_expires_at"
        private const val KEY_APP_REFRESH_EXPIRES_AT = "github_app_refresh_expires_at"
    }
}

data class GitHubAppTokenSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresAt: Long = 0L,
    val refreshTokenExpiresAt: Long = 0L,
)
