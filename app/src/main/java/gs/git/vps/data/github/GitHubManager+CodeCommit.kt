package gs.git.vps.data.github

import android.content.Context

/**
 * Результат батч-коммита Code-таба.
 * - [Success] — коммит создан, ref обновлён (sha нового коммита).
 * - [Conflict] — ref ветки уехал во время коммита (updateRef не fast-forward). БЕЗ авто-мёрджа:
 *   вызывающий перефетчит и даёт ретрай.
 * - [Error] — иная ошибка на одном из шагов.
 */
internal sealed interface CodeCommitResult {
    data class Success(val commitSha: String) : CodeCommitResult
    data object Conflict : CodeCommitResult
    data class Error(val message: String) : CodeCommitResult
}

/**
 * Батч-коммит черновика Code-таба через Git Data API — один коммит на много файлов в ТЕКУЩУЮ ветку.
 * Шаги (см. docs/code-tab-spec.md): ref→head sha → base tree → blob на каждый файл → tree(base_tree)
 * → commit(parent=head) → update ref (force=false). force=false: если ветка уехала, updateRef падает
 * → [CodeCommitResult.Conflict] (без авто-мёрджа). Вся GitHub-логика коммита — здесь, не в редакторе.
 *
 * @param changes типизированные операции рабочего дерева; rename кладётся как delete+add в один tree.
 */
internal suspend fun GitHubManager.commitCodeDraft(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    message: String,
    changes: Collection<CodeChange>,
): CodeCommitResult {
    if (changes.isEmpty()) return CodeCommitResult.Error("no changes")

    val ref = getGitRef(context, owner, repo, "heads/$branch")
        ?: return CodeCommitResult.Error("failed to get branch ref")
    val headSha = ref.nodeSha
    if (headSha.isBlank()) return CodeCommitResult.Error("empty head sha")

    val headCommit = getGitCommit(context, owner, repo, headSha)
        ?: return CodeCommitResult.Error("failed to get base commit")
    val baseTree = headCommit.treeSha

    // One final entry per path. This also prevents an intermediate operation from leaking into tree.
    val entries = linkedMapOf<String, GHTreeEntry>()
    suspend fun putContent(path: String, content: String?, sourceSha: String?): CodeCommitResult.Error? {
        val cleanPath = path.trim('/')
        if (cleanPath.isBlank()) return CodeCommitResult.Error("invalid path")
        val entry = when {
            content != null && content.isNotBlank() -> {
                val blob = createGitBlob(context, owner, repo, content)
                    ?: return CodeCommitResult.Error("blob failed: $cleanPath")
                GHTreeEntry(path = cleanPath, sha = blob.sha)
            }
            content != null -> GHTreeEntry(path = cleanPath, content = content)
            !sourceSha.isNullOrBlank() -> GHTreeEntry(path = cleanPath, sha = sourceSha)
            else -> return CodeCommitResult.Error("missing source: $cleanPath")
        }
        entries[cleanPath] = entry
        return null
    }

    for (change in changes) {
        when (change) {
            is CodeAdd -> putContent(change.path, change.content, change.sourceSha)?.let { return it }
            is CodeModify -> putContent(change.path, change.content, null)?.let { return it }
            is CodeDelete -> entries[change.path] = GHTreeEntry(path = change.path, delete = true)
            is CodeRename -> {
                entries[change.oldPath] = GHTreeEntry(path = change.oldPath, delete = true)
                putContent(change.path, change.content, change.sourceSha)?.let { return it }
            }
        }
    }

    val tree = createGitTreeBatch(context, owner, repo, baseTree, entries.values.toList())
        ?: return CodeCommitResult.Error("tree failed")
    val commit = createGitCommit(context, owner, repo, message, tree.sha, listOf(headSha))
        ?: return CodeCommitResult.Error("commit failed")

    val updated = updateGitRef(context, owner, repo, "heads/$branch", commit.sha, force = false)
    return if (updated != null) CodeCommitResult.Success(commit.sha) else CodeCommitResult.Conflict
}
