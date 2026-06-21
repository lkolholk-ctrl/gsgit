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
 * Оболочка нижнего таба **Code** (мини-IDE). Держит навигацию браузера (codePath), изолированную
 * от навигации репо. Открытие файла делегируется наверх ([onOpenFile]) — редактор показывается
 * full-screen на уровне RepoDetailScreen (early-return), read-only (Стадия 2).
 *
 * Бэк: глубже в дереве → вверх по пути; из КОРНЯ браузера → [onExit] (→ лендинг Repo,
 * selectedSection = null). Бэк внутри редактора обрабатывает сам редактор (он full-screen).
 * Полная спека: docs/code-tab-spec.md.
 */
@Composable
internal fun CodeTabShell(
    repo: GHRepo,
    branch: String,
    draftPaths: Set<String>,
    onOpenFile: (GHContent) -> Unit,
    onCommit: () -> Unit,
    onDiscardAll: () -> Unit,
    onExit: () -> Unit,
) {
    var codePath by rememberSaveable(repo.fullName) { mutableStateOf("") }

    BackHandler(enabled = true) {
        if (codePath.isNotBlank()) codePath = codePath.substringBeforeLast('/', "") else onExit()
    }

    CodeBrowser(
        repo = repo,
        branch = branch,
        path = codePath,
        onOpenDir = { codePath = it.path },
        onOpenFile = onOpenFile,
        onNavigatePath = { codePath = it },
        draftPaths = draftPaths,
        draftCount = draftPaths.size,
        onCommit = onCommit,
        onDiscardAll = onDiscardAll,
    )
}
