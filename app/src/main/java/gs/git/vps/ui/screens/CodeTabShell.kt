package gs.git.vps.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo

/**
 * Оболочка нижнего таба **Code** (мини-IDE). Навигация (codePath, showChanges) поднята в
 * RepoDetailScreen — это даёт ЕДИНЫЙ [onBack] для жеста/кнопки «назад» (этот BackHandler) И для
 * верхней стрелки топбара (handleRepoBack зовёт тот же codeInternalBack): поведение идентично
 * (панель «изменения» → дерево → вверх по пути → выход из Code). Полная спека: docs/code-tab-spec.md.
 */
@Composable
internal fun CodeTabShell(
    repo: GHRepo,
    branch: String,
    codePath: String,
    showChanges: Boolean,
    draftPaths: Set<String>,
    recents: List<GHContent>,
    onCodePathChange: (String) -> Unit,
    onShowChanges: () -> Unit,
    onOpenFile: (GHContent) -> Unit,
    onOpenPath: (String) -> Unit,
    onCommit: () -> Unit,
    onDiscardFile: (String) -> Unit,
    onDiscardAll: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = true) { onBack() }

    if (showChanges) {
        CodeChangesPanel(
            draftPaths = draftPaths,
            onOpenPath = onOpenPath,
            onDiscardPath = onDiscardFile,
            onCommit = onCommit,
            onBack = onBack,
        )
    } else {
        CodeBrowser(
            repo = repo,
            branch = branch,
            path = codePath,
            onOpenDir = { onCodePathChange(it.path) },
            onOpenFile = onOpenFile,
            onNavigatePath = onCodePathChange,
            draftPaths = draftPaths,
            draftCount = draftPaths.size,
            recents = recents,
            onCommit = onCommit,
            onShowChanges = onShowChanges,
            onDiscardAll = onDiscardAll,
        )
    }
}
