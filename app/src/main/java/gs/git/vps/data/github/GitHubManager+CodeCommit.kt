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
 * @param changes path → новый контент (модификации/добавления). Удаления/переименования — позже.
 */
internal suspend fun GitHubManager.commitCodeDraft(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    message: String,
    changes: Map<String, String>,
): CodeCommitResult {
    if (changes.isEmpty()) return CodeCommitResult.Error("нет изменений")

    val ref = getGitRef(context, owner, repo, "heads/$branch")
        ?: return CodeCommitResult.Error("не удалось получить ветку")
    val headSha = ref.nodeSha
    if (headSha.isBlank()) return CodeCommitResult.Error("пустой head sha")

    val headCommit = getGitCommit(context, owner, repo, headSha)
        ?: return CodeCommitResult.Error("не удалось получить базовый коммит")
    val baseTree = headCommit.treeSha

    val entries = ArrayList<GHTreeEntry>(changes.size)
    for ((path, content) in changes) {
        if (content.isNotBlank()) {
            val blob = createGitBlob(context, owner, repo, content)
                ?: return CodeCommitResult.Error("blob не создан: $path")
            entries.add(GHTreeEntry(path = path, sha = blob.sha))
        } else {
            // Пустой/whitespace файл: createGitBlob отвергает blank — кладём inline content.
            entries.add(GHTreeEntry(path = path, content = content))
        }
    }

    val tree = createGitTreeBatch(context, owner, repo, baseTree, entries)
        ?: return CodeCommitResult.Error("tree не создан")
    val commit = createGitCommit(context, owner, repo, message, tree.sha, listOf(headSha))
        ?: return CodeCommitResult.Error("commit не создан")

    val updated = updateGitRef(context, owner, repo, "heads/$branch", commit.sha, force = false)
    return if (updated != null) CodeCommitResult.Success(commit.sha) else CodeCommitResult.Conflict
}
