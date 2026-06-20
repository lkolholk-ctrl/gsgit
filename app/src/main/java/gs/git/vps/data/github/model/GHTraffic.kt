package gs.git.vps.data.github.model

/** Трафик репозитория (views/clones) — серия по дням. Домен Repos. */
data class GHTrafficSeries(
    val count: Int,
    val uniques: Int,
    val items: List<GHTrafficPoint>
)

data class GHTrafficPoint(
    val timestamp: String,
    val count: Int,
    val uniques: Int
)

data class GHTrafficReferrer(
    val referrer: String,
    val count: Int,
    val uniques: Int
)

data class GHTrafficPath(
    val path: String,
    val title: String,
    val count: Int,
    val uniques: Int
)
