package gs.git.vps.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Оболочка нижнего таба **Code** (мини-IDE). Внутренняя навигация изолирована своим состоянием
 * (не выход из репо):
 * - editor открыт → назад закрывает editor (→ browser);
 * - глубже в дереве (codePath не пуст) → назад поднимается на уровень вверх;
 * - из КОРНЯ браузера → [onExit] (экран зовёт его → лендинг Repo, selectedSection = null).
 *
 * Стадия 1: реальный read-only [CodeBrowser]. Редактор — пока заглушка (Стадия 2 подменит на
 * существующий CodeEditorScreen: фетч контента + детект языка). Полная спека: docs/code-tab-spec.md.
 */
@Composable
internal fun CodeTabShell(
    repo: GHRepo,
    branch: String,
    onExit: () -> Unit,
) {
    var codePath by rememberSaveable(repo.fullName) { mutableStateOf("") }
    var openedFile by remember(repo.fullName) { mutableStateOf<GHContent?>(null) }

    BackHandler(enabled = true) {
        when {
            openedFile != null -> openedFile = null
            codePath.isNotBlank() -> codePath = codePath.substringBeforeLast('/', "")
            else -> onExit()
        }
    }

    val file = openedFile
    if (file != null) {
        CodeEditorStub(file = file, onBack = { openedFile = null })
    } else {
        CodeBrowser(
            repo = repo,
            branch = branch,
            path = codePath,
            onOpenDir = { codePath = it.path },
            onOpenFile = { openedFile = it },
            onNavigatePath = { codePath = it },
        )
    }
}

/** Заглушка редактора (Стадия 1). Стадия 2 заменит на реальный CodeEditorScreen. */
@Composable
private fun CodeEditorStub(file: GHContent, onBack: () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AiModuleText("CODE · editor", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        AiModuleText(file.path, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        AiModuleText("Стадия 2: реальный редактор (фетч + детект языка)", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
        AiModuleText(
            "[ ← назад в браузер ]",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 20.dp).clickable(onClick = onBack),
        )
    }
}
