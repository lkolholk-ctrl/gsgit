package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHBlockedEntry
import gs.git.vps.data.github.model.GHContributionDay
import gs.git.vps.data.github.model.GHEmailEntry
import gs.git.vps.data.github.model.GHFollowerEntry
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHSocialAccountEntry
import gs.git.vps.data.github.model.GHUser
import gs.git.vps.data.github.model.GHUserKeyEntry
import gs.git.vps.data.github.model.GHUserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Домен Users слоя GitHub API: текущий пользователь и его кэш, публичные профили и контрибуции,
 * подписки (follow), почты, SSH/GPG-ключи, соц-аккаунты, подписчики, блок-лист, interaction-limits.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро
 * `GitHubManager.request()`, парсинг — чистые `parseGHX`-функции. Сигнатуры вызовов не менялись.
 *
 * НЕ здесь: searchUsers переехал в домен Orgs-соседний? нет — он в этом файле (поиск пользователей).
 * Активность (getUserReceived/PublicEvents) оставлена в core — домен Events; OAuth/device-flow и
 * validateToken/getCopilotToken — домен Auth.
 */

private const val USERS_TAG = "GH"

// ─── Текущий пользователь + кэш ──────────────────────────────────────────────

internal suspend fun GitHubManager.getUser(context: Context): GHUser? {
    val r = request(context, "/user")
    if (!r.success) return null
    return try {
        val user = parseGHUser(JSONObject(r.body))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_USER, r.body).apply()
        user
    } catch (e: Exception) { Log.e(USERS_TAG, "Parse user failed"); null }
}

internal fun GitHubManager.getCachedUser(context: Context): GHUser? {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null) ?: return null
    return try { parseGHUser(JSONObject(raw)) } catch (_: Exception) { null }
}

internal fun GitHubManager.clearGitHubUserCache(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_USER).apply()
}

internal suspend fun GitHubManager.searchUsers(context: Context, query: String): List<GHUser> {
    val q = URLEncoder.encode(query, "UTF-8")
    val r = request(context, "/search/users?q=$q&per_page=30")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).getJSONArray("items")
        (0 until arr.length()).map { i -> parseGHUser(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

// ─── Профиль ─────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getUserProfile(context: Context, username: String): GHUserProfile? {
    val r = request(context, "/users/$username")
    if (!r.success) return null
    return try { parseGHUserProfile(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getUserContributions(context: Context, username: String): List<GHContributionDay> {
    val r = request(context, "https://github.com/users/$username/contributions", extraHeaders = mapOf("Accept" to "text/html"))
    if (!r.success) return emptyList()
    return try {
        val tdRegex = """<td[^>]*class="[^"]*ContributionCalendar-day[^"]*"[^>]*>""".toRegex()
        val dateRegex = """data-date="(\d{4}-\d{2}-\d{2})"""".toRegex()
        val levelRegex = """data-level="(\d)"""".toRegex()

        tdRegex.findAll(r.body).mapNotNull { td ->
            val content = td.value
            val date = dateRegex.find(content)?.groupValues?.get(1) ?: return@mapNotNull null
            val level = levelRegex.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            GHContributionDay(date, level)
        }.toList()
    } catch (e: Exception) {
        emptyList()
    }
}

internal suspend fun GitHubManager.getCurrentUserProfile(context: Context): GHUserProfile? {
    val r = request(context, "/user")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_USER, r.body).apply()
        parseGHUserProfile(j)
    } catch (e: Exception) {
        Log.e(USERS_TAG, "Parse current profile failed")
        null
    }
}

internal suspend fun GitHubManager.updateCurrentUserProfile(
    context: Context,
    name: String,
    bio: String,
    company: String,
    location: String,
    blog: String,
    email: String? = null,
    twitterUsername: String? = null,
    hireable: Boolean? = null
): Boolean {
    val body = JSONObject().apply {
        put("name", name)
        put("bio", bio)
        put("company", company)
        put("location", location)
        put("blog", blog)
        if (email != null && email.isNotBlank()) put("email", email.trim())
        if (twitterUsername != null) {
            if (twitterUsername.isBlank()) put("twitter_username", JSONObject.NULL)
            else put("twitter_username", twitterUsername.trim().removePrefix("@"))
        }
        if (hireable != null) put("hireable", hireable)
    }.toString()
    val ok = request(context, "/user", "PATCH", body).success
    if (ok) getUser(context)
    return ok
}

// ─── Подписки (follow) ───────────────────────────────────────────────────────

internal suspend fun GitHubManager.isFollowing(context: Context, username: String): Boolean =
    request(context, "/user/following/$username").code == 204

internal suspend fun GitHubManager.followUser(context: Context, username: String): Boolean =
    request(context, "/user/following/$username", "PUT").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.unfollowUser(context: Context, username: String): Boolean =
    request(context, "/user/following/$username", "DELETE").let { it.code == 204 || it.success }

// ─── Почты ───────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getEmailEntries(context: Context): List<GHEmailEntry> {
    val r = request(context, "/user/emails")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHEmailEntry(arr.getJSONObject(i)) }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addEmailAddress(context: Context, email: String): Boolean {
    val body = JSONArray().put(email).toString()
    return request(context, "/user/emails", "POST", body).success
}

internal suspend fun GitHubManager.deleteEmailAddress(context: Context, email: String): Boolean {
    val body = JSONArray().put(email).toString()
    return request(context, "/user/emails", "DELETE", body).success
}

internal suspend fun GitHubManager.setEmailVisibility(context: Context, visibility: String): Boolean {
    val body = JSONObject().apply { put("visibility", visibility) }.toString()
    return request(context, "/user/email/visibility", "PATCH", body).success
}

// ─── SSH / GPG ключи ─────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getSshKeysNative(context: Context): List<GHUserKeyEntry> {
    val r = request(context, "/user/keys")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHUserKeyEntry(j.optLong("id"), j.optString("title"), j.optString("key"), j.optString("created_at", ""), "ssh")
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getSshSigningKeysNative(context: Context): List<GHUserKeyEntry> {
    val r = request(context, "/user/ssh_signing_keys")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHUserKeyEntry(j.optLong("id"), j.optString("title"), j.optString("key"), j.optString("created_at", ""), "ssh_signing")
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getGpgKeysNative(context: Context): List<GHUserKeyEntry> {
    val r = request(context, "/user/gpg_keys")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHUserKeyEntry(j.optLong("id"), j.optString("name"), j.optString("public_key"), j.optString("created_at", ""), "gpg")
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addSshKeyNative(context: Context, title: String, key: String): Boolean {
    val body = JSONObject().apply { put("title", title); put("key", key) }.toString()
    return request(context, "/user/keys", "POST", body).success
}

internal suspend fun GitHubManager.addSshSigningKeyNative(context: Context, title: String, key: String): Boolean {
    val body = JSONObject().apply { put("title", title); put("key", key) }.toString()
    return request(context, "/user/ssh_signing_keys", "POST", body).success
}

internal suspend fun GitHubManager.addGpgKeyNative(context: Context, armoredKey: String): Boolean {
    val body = JSONObject().apply { put("armored_public_key", armoredKey) }.toString()
    return request(context, "/user/gpg_keys", "POST", body).success
}

internal suspend fun GitHubManager.deleteSshKeyNative(context: Context, id: Long): Boolean =
    request(context, "/user/keys/$id", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.deleteSshSigningKeyNative(context: Context, id: Long): Boolean =
    request(context, "/user/ssh_signing_keys/$id", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.deleteGpgKeyNative(context: Context, id: Long): Boolean =
    request(context, "/user/gpg_keys/$id", "DELETE").let { it.code == 204 || it.success }

// ─── Соц-аккаунты ────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getSocialAccountsNative(context: Context): List<GHSocialAccountEntry> {
    val r = request(context, "/user/social_accounts")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHSocialAccountEntry(j.optString("provider", "social"), j.optString("url"))
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addSocialAccountNative(context: Context, url: String): Boolean {
    val body = JSONArray().put(url).toString()
    return request(context, "/user/social_accounts", "POST", body).success
}

internal suspend fun GitHubManager.deleteSocialAccountNative(context: Context, url: String): Boolean {
    val body = JSONArray().put(url).toString()
    return request(context, "/user/social_accounts", "DELETE", body).success
}

// ─── Подписчики / подписки / блок-лист ───────────────────────────────────────

internal suspend fun GitHubManager.getFollowersNative(context: Context): List<GHFollowerEntry> {
    val r = request(context, "/user/followers?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHFollowerEntry(j.optString("login"), j.optString("avatar_url", ""))
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getFollowingNative(context: Context): List<GHFollowerEntry> {
    val r = request(context, "/user/following?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHFollowerEntry(j.optString("login"), j.optString("avatar_url", ""))
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getBlockedUsersNative(context: Context): List<GHBlockedEntry> {
    val r = request(context, "/user/blocks?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHBlockedEntry(j.optString("login"), j.optString("avatar_url", ""))
        }
    } catch (_: Exception) { emptyList() }
}

internal suspend fun GitHubManager.blockUserNative(context: Context, username: String): Boolean =
    request(context, "/user/blocks/${URLEncoder.encode(username, "UTF-8")}", "PUT", "").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.unblockUserNative(context: Context, username: String): Boolean =
    request(context, "/user/blocks/${URLEncoder.encode(username, "UTF-8")}", "DELETE").let { it.code == 204 || it.success }

// ─── Interaction limits (аккаунт) ────────────────────────────────────────────

internal suspend fun GitHubManager.getInteractionLimitNative(context: Context): GHInteractionLimitEntry? {
    val r = request(context, "/user/interaction-limits")
    if (!r.success || r.body.isBlank()) return null
    return try {
        val j = JSONObject(r.body)
        GHInteractionLimitEntry(j.optString("limit"), j.optString("expires_at", "").ifBlank { null })
    } catch (_: Exception) { null }
}

internal suspend fun GitHubManager.setInteractionLimitNative(context: Context, limit: String, expiry: String): Boolean {
    val body = JSONObject().apply {
        put("limit", limit)
        put("expiry", expiry)
    }.toString()
    return request(context, "/user/interaction-limits", "PUT", body).success
}

internal suspend fun GitHubManager.removeInteractionLimitNative(context: Context): Boolean =
    request(context, "/user/interaction-limits", "DELETE").let { it.code == 204 || it.success }

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun JSONObject.cleanString(key: String): String =
    optString(key, "").trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

/** Единый парсер GHUser: search-item не содержит счётчиков → opt-дефолты дают тот же результат. */
private fun parseGHUser(j: JSONObject): GHUser =
    GHUser(
        login = j.optString("login"),
        name = j.optString("name", ""),
        avatarUrl = j.optString("avatar_url", ""),
        bio = j.optString("bio", ""),
        publicRepos = j.optInt("public_repos", 0),
        privateRepos = j.optInt("total_private_repos", 0),
        followers = j.optInt("followers", 0),
        following = j.optInt("following", 0)
    )

private fun parseGHUserProfile(j: JSONObject): GHUserProfile =
    GHUserProfile(
        login = j.cleanString("login"),
        name = j.cleanString("name"),
        avatarUrl = j.cleanString("avatar_url"),
        bio = j.cleanString("bio"),
        company = j.cleanString("company"),
        location = j.cleanString("location"),
        blog = j.cleanString("blog"),
        email = j.cleanString("email"),
        twitterUsername = j.cleanString("twitter_username"),
        hireable = if (j.isNull("hireable")) false else j.optBoolean("hireable", false),
        publicRepos = j.optInt("public_repos", 0),
        publicGists = j.optInt("public_gists", 0),
        privateRepos = j.optInt("total_private_repos", 0),
        ownedPrivateRepos = j.optInt("owned_private_repos", 0),
        privateGists = j.optInt("private_gists", 0),
        diskUsageKb = j.optLong("disk_usage", 0L),
        collaborators = j.optInt("collaborators", 0),
        followers = j.optInt("followers", 0),
        following = j.optInt("following", 0),
        twoFactorAuthentication = if (j.has("two_factor_authentication") && !j.isNull("two_factor_authentication")) {
            j.optBoolean("two_factor_authentication", false)
        } else null,
        planName = j.optJSONObject("plan")?.cleanString("name").orEmpty(),
        planSpace = j.optJSONObject("plan")?.optLong("space", 0L) ?: 0L,
        createdAt = j.cleanString("created_at"),
        updatedAt = j.cleanString("updated_at")
    )

private fun parseGHEmailEntry(j: JSONObject): GHEmailEntry =
    GHEmailEntry(
        email = j.optString("email"),
        primary = j.optBoolean("primary", false),
        verified = j.optBoolean("verified", false),
        visibility = j.optString("visibility", "")
    )
