package gs.git.vps.data.github.model

/** Пользователь в контексте репозитория (stargazer/watcher). Домен Repos. */
data class GHRepoPerson(
    val login: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val starredAt: String = ""
)

/** Событие репозитория/пользователя из events-эндпоинтов. Домен Repos. */
data class GHRepoEvent(
    val id: String,
    val type: String,
    val actor: String,
    val createdAt: String,
    val action: String,
    val ref: String,
    val refType: String,
    val size: Int,
    val repoName: String = ""
)
