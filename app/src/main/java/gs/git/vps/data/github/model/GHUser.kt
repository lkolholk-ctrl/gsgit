package gs.git.vps.data.github.model

/**
 * Модели домена Users слоя GitHub API: текущий пользователь, публичный профиль, активность
 * аккаунта (почты, ключи, соц-аккаунты, подписки, блок-лист). Вынесены из god-файла
 * GitHubManager.kt в рамках декомпозиции data-слоя (см. docs/decomposition-log.md).
 *
 * GHUserLite (широко шарится, в т.ч. доменом Issues) ОСТАВЛЕН в core — здесь не дублируется.
 */

/** Краткая карточка пользователя из `/user` (текущий) — с кэш-полями счётчиков. */
data class GHUser(val login: String, val name: String, val avatarUrl: String, val bio: String,
    val publicRepos: Int, val privateRepos: Int, val followers: Int, val following: Int)

/** Полный публичный профиль пользователя (`/users/{login}` или `/user`). */
data class GHUserProfile(
    val login: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val company: String,
    val location: String,
    val blog: String,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
    val createdAt: String,
    val email: String = "",
    val twitterUsername: String = "",
    val hireable: Boolean = false,
    val publicGists: Int = 0,
    val privateRepos: Int = 0,
    val ownedPrivateRepos: Int = 0,
    val privateGists: Int = 0,
    val diskUsageKb: Long = 0L,
    val collaborators: Int = 0,
    val twoFactorAuthentication: Boolean? = null,
    val planName: String = "",
    val planSpace: Long = 0L,
    val updatedAt: String = ""
)

/** День в календаре контрибуций (парсится из HTML профиля). */
data class GHContributionDay(
    val date: String,
    val level: Int,
    /** Exact contribution count returned by GraphQL; level is only GitHub's color bucket. */
    val count: Int = 0,
)

data class GHLanguageUsage(val name: String, val bytes: Long)

data class GHContributionActivity(
    val commits: Int,
    val pullRequests: Int,
    val issues: Int,
    val reviews: Int,
)

/** Real, time-bounded profile metrics returned by GitHub GraphQL. */
data class GHProfileInsights(
    val contributionDays: List<GHContributionDay>,
    val totalContributions: Int,
    val activity: GHContributionActivity,
    val languages: List<GHLanguageUsage>,
    val windowStartedAt: String,
    val windowEndedAt: String,
    val repositoriesSampled: Int,
)

data class GHEmailEntry(val email: String, val primary: Boolean, val verified: Boolean, val visibility: String)
data class GHUserKeyEntry(val id: Long, val title: String, val key: String, val createdAt: String, val kind: String)
data class GHSocialAccountEntry(val provider: String, val url: String)
data class GHFollowerEntry(val login: String, val avatarUrl: String)
data class GHBlockedEntry(val login: String, val avatarUrl: String)
