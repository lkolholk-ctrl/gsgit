package gs.git.vps.data.github.model

/**
 * Результаты GitHub Search API (code/issues/commits/topics/labels). Модели вынесены из
 * god-файла GitHubManager.kt в рамках декомпозиции data-слоя (домен Search,
 * см. docs/decomposition-log.md).
 */
data class GHCodeResult(val name: String, val path: String, val sha: String, val htmlUrl: String, val score: Double)

data class GHSearchIssueResult(
    val id: Long,
    val number: Int,
    val title: String,
    val body: String,
    val state: String,
    val author: String,
    val avatarUrl: String,
    val comments: Int,
    val labels: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val htmlUrl: String,
    val repository: String,
    val isPullRequest: Boolean,
    val score: Double
)

data class GHSearchCommitResult(
    val sha: String,
    val message: String,
    val author: String,
    val avatarUrl: String,
    val date: String,
    val repository: String,
    val htmlUrl: String,
    val score: Double
)

data class GHTopicSearchResult(
    val name: String,
    val displayName: String,
    val shortDescription: String,
    val description: String,
    val createdBy: String,
    val released: String,
    val updatedAt: String,
    val featured: Boolean,
    val curated: Boolean,
    val score: Double,
    val aliases: List<String>,
    val related: List<String>
)

data class GHLabelSearchResult(
    val name: String,
    val color: String,
    val description: String,
    val repository: String,
    val score: Double
)
