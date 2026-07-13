package gs.git.vps.data.github.model

/**
 * Модели домена PullRequests+Reviews+Checks слоя GitHub API. Вынесены из god-файла
 * GitHubManager.kt в рамках декомпозиции data-слоя (см. docs/decomposition-log.md).
 *
 * GHCheckRun остаётся в model/GHWorkflow.kt (переехал с доменом Actions) — здесь не дублируется.
 */

/** Pull request: краткая запись в списке либо детальная (доп. поля заполняются только в detail). */
data class GHPullRequest(val number: Int, val title: String, val state: String, val author: String,
    val createdAt: String, val head: String, val base: String, val comments: Int,
    val merged: Boolean, val body: String,
    val draft: Boolean = false,
    val htmlUrl: String = "",
    val headSha: String = "",
    val mergeable: Boolean? = null,
    val mergeableState: String = "",
    val reviewComments: Int = 0,
    val commits: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changedFiles: Int = 0,
    val requestedReviewers: List<String> = emptyList())

/** Результат проверки «смержен ли PR» (HTTP 204/404 → merged/not, иначе ошибка). */
data class GHPullMergeStatus(
    val merged: Boolean,
    val checked: Boolean,
    val code: Int,
    val message: String
)

/** Ревью PR (approved/changes_requested/commented). */
data class GHPullReview(
    val id: Long,
    val user: String,
    val state: String,
    val body: String,
    val submittedAt: String,
    val commitId: String,
    val htmlUrl: String
)

/** Изменённый в PR файл (для diff-просмотра). */
data class GHPullFile(val filename: String, val status: String, val additions: Int, val deletions: Int, val patch: String)

/** Комментарий ревью PR (привязан к строке/диффу). */
data class GHReviewComment(
    val id: Long,
    val body: String,
    val path: String,
    val line: Int,
    val originalLine: Int,
    val diffHunk: String,
    val author: String,
    val avatarUrl: String,
    val createdAt: String,
    val inReplyToId: Long?,
    val side: String = "",
    val originalSide: String = ""
)

/** Check-suite по коммиту (агрегат проверок одного приложения). */
data class GHCheckSuite(
    val id: Long,
    val status: String,
    val conclusion: String,
    val app: String,
    val headBranch: String,
    val headSha: String,
    val before: String,
    val after: String,
    val createdAt: String,
    val updatedAt: String,
    val latestCheckRunsCount: Int
)
