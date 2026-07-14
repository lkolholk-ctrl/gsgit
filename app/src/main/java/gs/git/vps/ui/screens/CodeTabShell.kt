package gs.git.vps.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.CodeChange

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
    refreshKey: Int,
    showChanges: Boolean,
    changes: Collection<CodeChange>,
    recents: List<GHContent>,
    canWrite: Boolean,
    onCodePathChange: (String) -> Unit,
    onShowChanges: () -> Unit,
    onOpenFile: (GHContent) -> Unit,
    onOpenPath: (String) -> Unit,
    onCommit: () -> Unit,
    onDiscardFile: (String) -> Unit,
    onDiscardAll: () -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameOrMove: (GHContent, String) -> Unit,
    onDuplicate: (GHContent, String) -> Unit,
    onDelete: (GHContent) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = true) { onBack() }

    // Браузер виден всегда; панель «изменения» — bottom-sheet ПОВЕРХ дерева (VS Code SCM-вид),
    // а не подмена экрана: видно и дерево, и изменения. onBack/тап-снаружи закрывают панель
    // (codeInternalBack: панель → дерево → вверх по пути → выход).
    Box(Modifier.fillMaxSize()) {
        CodeBrowser(
            repo = repo,
            branch = branch,
            path = codePath,
            refreshKey = refreshKey,
            onOpenDir = { onCodePathChange(it.path) },
            onOpenFile = onOpenFile,
            onNavigatePath = onCodePathChange,
            changes = changes,
            recents = recents,
            canWrite = canWrite,
            onCommit = onCommit,
            onShowChanges = onShowChanges,
            onDiscardAll = onDiscardAll,
            onCreateFile = onCreateFile,
            onCreateFolder = onCreateFolder,
            onRenameOrMove = onRenameOrMove,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
        )
        if (showChanges) {
            CodeChangesPanel(
                changes = changes,
                onOpenPath = onOpenPath,
                onDiscardPath = onDiscardFile,
                onCommit = onCommit,
                onBack = onBack,
            )
        }
    }
}
