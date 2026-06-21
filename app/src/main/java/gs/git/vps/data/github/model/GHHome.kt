package gs.git.vps.data.github.model

/**
 * Модель домена Home слоя GitHub API: агрегат для дашборда «быстрый взгляд». Вынесена из
 * god-файла GitHubManager.kt (см. docs/decomposition-log.md).
 */
data class QuickGlanceStats(
    val assignedPrsCount: Int = 0,
    val openIssuesCount: Int = 0,
    val failedBuildsCount: Int = 0,
    val loading: Boolean = true
)
