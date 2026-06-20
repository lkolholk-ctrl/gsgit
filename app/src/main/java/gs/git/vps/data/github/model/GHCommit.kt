package gs.git.vps.data.github.model

/**
 * Модели домена Commits слоя GitHub API: коммиты (список/детали), diff-файлы, сравнение веток,
 * commit-статусы. Вынесены из god-файла GitHubManager.kt в рамках декомпозиции data-слоя
 * (см. docs/decomposition-log.md).
 *
 * GHBlameRange (blame) оставлена в core — домен Contents. GHReviewComment (commit-comments)
 * живёт в model/GHPullRequest.kt.
 */

/** Запись коммита в списке/сравнении. */
data class GHCommit(val sha: String, val message: String, val author: String, val date: String, val avatarUrl: String, val parents: List<String> = emptyList(), val verified: Boolean = false)

/** Детали коммита с изменёнными файлами. */
data class GHCommitDetail(val sha: String, val message: String, val author: String, val date: String,
    val files: List<GHDiffFile>, val totalAdditions: Int, val totalDeletions: Int)

/** Изменённый файл в diff коммита/сравнения. */
data class GHDiffFile(val filename: String, val status: String, val additions: Int, val deletions: Int, val patch: String)

/** Результат сравнения base...head. */
data class GHCompareResult(
    val status: String,
    val aheadBy: Int,
    val behindBy: Int,
    val totalCommits: Int,
    val files: List<GHDiffFile>,
    val commits: List<GHCommit> = emptyList(),
    val htmlUrl: String = ""
)

/** Commit-статус (CI/legacy statuses API). */
data class GHCommitStatus(
    val id: Long,
    val state: String,
    val context: String,
    val description: String,
    val targetUrl: String,
    val createdAt: String,
    val updatedAt: String,
    val creator: String
)
