package gs.git.vps.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo

/**
 * Оболочка нижнего таба **Code** (мини-IDE). Держит навигацию браузера (codePath) и переключатель
 * панели «изменения» (showChanges), изолированные от навигации репо. Открытие файла делегируется
 * наверх ([onOpenFile]/[onOpenPath]) — редактор показывается full-screen на уровне RepoDetailScreen.
 *
 * Бэк: панель «изменения» открыта → закрыть её; глубже в дереве → вверх по пути; из КОРНЯ браузера
 * → [onExit] (→ лендинг Repo). Полная спека: docs/code-tab-spec.md.
 */
@Composable
internal fun CodeTabShell(
    repo: GHRepo,
    branch: String,
    draftPaths: Set<String>,
    recents: List<GHContent>,
    onOpenFile: (GHContent) -> Unit,
    onOpenPath: (String) -> Unit,
    onCommit: () -> Unit,
    onDiscardFile: (String) -> Unit,
    onDiscardAll: () -> Unit,
    onExit: () -> Unit,
) {
    var codePath by rememberSaveable(repo.fullName) { mutableStateOf("") }
    var showChanges by rememberSaveable(repo.fullName) { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            showChanges -> showChanges = false
            codePath.isNotBlank() -> codePath = codePath.substringBeforeLast('/', "")
            else -> onExit()
        }
    }

    if (showChanges) {
        CodeChangesPanel(
            draftPaths = draftPaths,
            onOpenPath = onOpenPath,
            onDiscardPath = onDiscardFile,
            onCommit = onCommit,
            onBack = { showChanges = false },
        )
    } else {
        CodeBrowser(
            repo = repo,
            branch = branch,
            path = codePath,
            onOpenDir = { codePath = it.path },
            onOpenFile = onOpenFile,
            onNavigatePath = { codePath = it },
            draftPaths = draftPaths,
            draftCount = draftPaths.size,
            recents = recents,
            onCommit = onCommit,
            onShowChanges = { showChanges = true },
            onDiscardAll = onDiscardAll,
        )
    }
}
