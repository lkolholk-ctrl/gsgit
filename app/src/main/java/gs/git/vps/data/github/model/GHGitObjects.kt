package gs.git.vps.data.github.model

/**
 * Модели домена GitData (низкоуровневый Git Data API): refs, trees, blobs, tag- и commit-объекты.
 * Вынесены из god-файла GitHubManager.kt в рамках декомпозиции data-слоя
 * (см. docs/decomposition-log.md).
 */

data class GHGitRef(
    val ref: String,
    val nodeSha: String,
    val nodeType: String,
    val url: String
)

data class GHGitTree(
    val sha: String,
    val truncated: Boolean,
    val items: List<GHGitTreeItem>
)

data class GHGitTreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val size: Long,
    val url: String
)

data class GHGitBlob(
    val sha: String,
    val size: Long,
    val encoding: String,
    val content: String,
    val url: String
)

data class GHGitTagDetail(
    val sha: String,
    val tag: String,
    val message: String,
    val taggerName: String,
    val taggerEmail: String,
    val date: String,
    val objectSha: String,
    val objectType: String
)

data class GHGitCommit(
    val sha: String,
    val message: String,
    val treeSha: String,
    val parentShas: List<String>,
    val authorName: String,
    val authorEmail: String,
    val authorDate: String,
    val committerName: String,
    val committerEmail: String,
    val committerDate: String
)
