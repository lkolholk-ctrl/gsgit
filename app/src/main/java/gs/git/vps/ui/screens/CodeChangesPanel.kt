package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Панель «изменения» Code-таба (P1) — **bottom-sheet поверх дерева** в духе VS Code SCM: дерево видно
 * сверху (тап по нему закрывает панель), снизу выезжает скруглённый sheet со списком ВСЕХ файлов с
 * незакоммиченными правками. Маркер изменения цветной (M — modified/amber), per-file discard, тап →
 * открыть файл, commit-pill. Sheet поднят над плавающим bottom-bar. Источник правды — buffer
 * черновика (path → content) на уровне RepoDetailScreen. См. docs/code-tab-spec.md.
 */
@Composable
internal fun CodeChangesPanel(
    draftPaths: Set<String>,
    onOpenPath: (String) -> Unit,
    onDiscardPath: (String) -> Unit,
    onCommit: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val sorted = remember(draftPaths) { draftPaths.sorted() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize()) {
        // Зона над sheet'ом = видимое дерево; тап по ней закрывает панель (dismiss). Без скрима, чтобы
        // дерево оставалось читаемым (решение: «видно и дерево, и изменения»).
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = RepoBottomBarReservedHeight + navBottom) // над плавающим bottom-bar
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(palette.surfaceElevated),
        ) {
            // drag-handle
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(palette.border.copy(alpha = 0.5f)),
                )
            }
            // header: «changes» + чип-счётчик | commit-pill
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiModuleText("changes", color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(percent = 50)).background(palette.warning.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    AiModuleText("${sorted.size}", color = palette.warning, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                if (sorted.isNotEmpty()) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(palette.accent.copy(alpha = 0.14f))
                            .clickable(onClick = onCommit)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.accent)
                        Spacer(Modifier.width(5.dp))
                        AiModuleText("commit", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
            if (sorted.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(22.dp), tint = palette.textMuted)
                    AiModuleText("no changes", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(sorted, key = { it }) { path ->
                        CodeChangeRow(
                            path = path,
                            onOpen = { onOpenPath(path) },
                            onDiscard = { onDiscardPath(path) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeChangeRow(path: String, onOpen: () -> Unit, onDiscard: () -> Unit) {
    val palette = AiModuleTheme.colors
    val name = path.substringAfterLast('/')
    val dir = path.substringBeforeLast('/', "")
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Чип типа изменения: M (modified) — amber/warning. Будущие A (green) / D (error) — по типу.
        Box(
            Modifier.clip(RoundedCornerShape(6.dp)).background(palette.warning.copy(alpha = 0.16f)).padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            AiModuleText("M", color = palette.warning, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AiModuleText(name, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dir.isNotEmpty()) {
                AiModuleText(dir, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Rounded.Close,
            contentDescription = "discard $name",
            modifier = Modifier.size(18.dp).clickable(onClick = onDiscard),
            tint = palette.error,
        )
    }
}
