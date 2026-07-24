package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.security.TokenRepository
import org.json.JSONObject

/** Витрина аккаунта для Gmail-style переключателя. */
data class AccountInfo(
    val slot: Int,
    val login: String,      // "" если профиль ещё не закэширован
    val avatarUrl: String,
    val mode: String,       // guest / device / pat
    val active: Boolean,
)

/**
 * Мульти-аккаунт: до 3 независимых аккаунтов GitHub на устройстве.
 *
 * Слот 0 использует «безсуффиксные» ключи (`github_token`, `auth_mode`, `user_json`…),
 * поэтому существующие установки автоматически становятся аккаунтом 0 — миграция не нужна.
 * Слоты 1-2 неймспейсятся префиксом `accN_`.
 *
 * Активный слот — единственный, чей credential уходит в GitHub API и кому идут пуши
 * (один FCM-токен на устройство = один активный аккаунт). Общий PIN — app-level, не per-slot.
 */
object AccountStore {
    const val MAX_ACCOUNTS = 3
    private const val KEY_ACTIVE = "active_account"

    /** Индекс активного слота (0..MAX-1). По умолчанию 0 (совместимость с одно-аккаунтными установками). */
    fun activeSlot(context: Context): Int =
        context.getSharedPreferences(GitHubAuth.PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ACTIVE, 0).coerceIn(0, MAX_ACCOUNTS - 1)

    fun setActiveSlot(context: Context, slot: Int) {
        context.getSharedPreferences(GitHubAuth.PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACTIVE, slot.coerceIn(0, MAX_ACCOUNTS - 1)).apply()
    }

    /** Ключ SharedPreferences, неймспейснутый по слоту. Слот 0 — без суффикса (обратная совместимость). */
    fun slotKey(slot: Int, base: String): String = if (slot == 0) base else "acc${slot}_$base"

    /** Витрина слота или null, если слот пустой (нет ни credential'ов, ни закэшированного профиля). */
    fun accountInfo(context: Context, slot: Int): AccountInfo? {
        val repo = TokenRepository(context, slot)
        val session = repo.getGitHubAppSession()
        val hasCreds = session.accessToken.isNotBlank() || session.refreshToken.isNotBlank() ||
            repo.getToken().isNotBlank()
        val prefs = context.getSharedPreferences(GitHubAuth.PREFS, Context.MODE_PRIVATE)
        val rawUser = prefs.getString(slotKey(slot, GitHubManager.KEY_USER), null)
        if (!hasCreds && rawUser == null) return null
        val mode = prefs.getString(slotKey(slot, GitHubAuth.KEY_AUTH_MODE), null)
            ?: if (hasCreds) GitHubAuth.MODE_DEVICE else GitHubAuth.MODE_GUEST
        var login = ""
        var avatar = ""
        if (rawUser != null) {
            try {
                val j = JSONObject(rawUser)
                login = j.optString("login")
                avatar = j.optString("avatar_url")
            } catch (_: Exception) { /* битый кэш — игнорируем */ }
        }
        return AccountInfo(slot, login, avatar, mode, active = slot == activeSlot(context))
    }

    /** Все непустые аккаунты (для переключателя). */
    fun accounts(context: Context): List<AccountInfo> =
        (0 until MAX_ACCOUNTS).mapNotNull { accountInfo(context, it) }

    /** Первый свободный слот или null, если все 3 заняты. */
    fun firstFreeSlot(context: Context): Int? =
        (0 until MAX_ACCOUNTS).firstOrNull { accountInfo(context, it) == null }

    fun isFull(context: Context): Boolean = firstFreeSlot(context) == null

    /** Переключиться на слот (данные). ETag-кэш чистим; push перерегистрирует вызывающий UI. */
    fun switchTo(context: Context, slot: Int) {
        setActiveSlot(context, slot)
        GitHubManager.clearEtagCache()
    }

    /** Начать добавление аккаунта: делаем свободный слот активным (пустой → UI покажет вход).
     *  Возвращает слот, либо null если лимит достигнут. */
    fun beginAddAccount(context: Context): Int? {
        val free = firstFreeSlot(context) ?: return null
        setActiveSlot(context, free)
        GitHubManager.clearEtagCache()
        return free
    }

    /** Удалить аккаунт слота: чистим credential'ы, режим и кэш профиля. Если он был активным —
     *  переключаемся на другой занятый слот, иначе на 0. */
    fun removeAccount(context: Context, slot: Int) {
        TokenRepository(context, slot).apply { clearToken(); clearGitHubAppSession() }
        context.getSharedPreferences(GitHubAuth.PREFS, Context.MODE_PRIVATE).edit()
            .remove(slotKey(slot, GitHubAuth.KEY_AUTH_MODE))
            .remove(slotKey(slot, GitHubManager.KEY_USER))
            .apply()
        if (activeSlot(context) == slot) {
            val fallback = (0 until MAX_ACCOUNTS).firstOrNull { it != slot && accountInfo(context, it) != null } ?: 0
            setActiveSlot(context, fallback)
        }
        GitHubManager.clearEtagCache()
    }
}
