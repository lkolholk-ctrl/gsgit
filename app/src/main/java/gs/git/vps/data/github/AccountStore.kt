package gs.git.vps.data.github

import android.content.Context

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
}
