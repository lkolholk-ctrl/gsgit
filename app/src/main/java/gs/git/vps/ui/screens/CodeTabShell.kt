package gs.git.vps.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Оболочка нижнего таба **Code** (мини-IDE) — Стадия 0 редизайна: внутренний бэк-стек
 * (browser → editor → back в browser) + проводка секции. Контент пока ЗАГЛУШКИ; реальный
 * changes-oriented браузер (Стадия 1) и редактор (Стадия 2) подменят их.
 *
 * Навигация Code изолирована своим бэк-стеком (не выход из репо). Назад из КОРНЯ браузера →
 * [onExit] (экран зовёт его, чтобы вернуться на лендинг Repo, selectedSection = null).
 * Полная спека: docs/code-tab-spec.md.
 */
internal sealed interface CodeScreen {
    data object Browser : CodeScreen
    data class Editor(val path: String) : CodeScreen
}

@Composable
internal fun CodeTabShell(
    repo: GHRepo,
    branch: String,
    onExit: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val backStack = remember(repo.fullName) { mutableStateListOf<CodeScreen>(CodeScreen.Browser) }

    // Внутренний бэк: editor → browser; из корня браузера → выход из Code на лендинг Repo.
    BackHandler(enabled = true) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else onExit()
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val top = backStack.last()) {
            is CodeScreen.Browser -> {
                AiModuleText("CODE", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                AiModuleText("мини-IDE · рабочее дерево", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                AiModuleText("${repo.fullName} · $branch", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                AiModuleText("Стадия 0: оболочка + бэк-стек (заглушка)", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
                AiModuleText(
                    "[ открыть файл-заглушку → ]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .clickable { backStack.add(CodeScreen.Editor("README.md")) },
                )
            }
            is CodeScreen.Editor -> {
                AiModuleText("CODE · editor", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                AiModuleText(top.path, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                AiModuleText("Стадия 0: заглушка редактора", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
                AiModuleText(
                    "[ ← назад в браузер ]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .clickable { backStack.removeAt(backStack.lastIndex) },
                )
            }
        }
    }
}
