package gs.git.vps.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import gs.git.vps.data.github.CodeChange
import gs.git.vps.data.github.CodeChangeKind
import gs.git.vps.data.github.CodeRename

/**
 * Панель «изменения» Code-таба — **bottom-sheet поверх дерева** (VS Code SCM), терминальный стиль:
 * mono-заголовок, бордерная поверхность (как ghGlassCard), GitHubTerminalButton для commit, mono-маркер
 * маркер типа A/M/D/R. Дерево видно сверху (тап закрывает); sheet выезжает снизу (slide-up + fade) и
 * поднят над плавающим bottom-bar. Источник правды — типизированный A/M/D/R draft в
 * RepoDetailScreen. См. docs/code-tab-spec.md.
 */
@Composable
internal fun CodeChangesPanel(
    changes: Collection<CodeChange>,
    onOpenPath: (String) -> Unit,
    onDiscardPath: (String) -> Unit,
    onCommit: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val sorted = changes.sortedBy { it.path }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Column(Modifier.fillMaxSize()) {
        // Зона над sheet'ом = видимое дерево; тап по ней закрывает панель (dismiss). Без скрима.
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
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(animationSpec = tween(240)) { it } + fadeIn(tween(240)),
            exit = fadeOut(tween(120)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = RepoBottomBarReservedHeight + navBottom) // над плавающим bottom-bar
                    .clip(sheetShape)
                    .background(palette.surface)
                    .border(1.dp, palette.border.copy(alpha = 0.65f), sheetShape),
            ) {
                // header: «changes (N)» mono | commit (бордерный GitHubTerminalButton)
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AiModuleText("changes (${sorted.size})", color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    if (sorted.isNotEmpty()) {
                        GitHubTerminalButton(label = "commit", onClick = onCommit, color = palette.accent)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
                if (sorted.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AiModuleText("·", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 22.sp)
                        AiModuleText("no changes", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(sorted, key = { "${it.kind}:${it.path}" }) { change ->
                            CodeChangeRow(
                                change = change,
                                onOpen = { onOpenPath(change.path) },
                                onDiscard = { onDiscardPath(change.path) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeChangeRow(change: CodeChange, onOpen: () -> Unit, onDiscard: () -> Unit) {
    val palette = AiModuleTheme.colors
    val path = change.path
    val name = path.substringAfterLast('/')
    val dir = path.substringBeforeLast('/', "")
    val markerColor = when (change.kind) {
        CodeChangeKind.ADD -> palette.syntaxString
        CodeChangeKind.MODIFY -> palette.warning
        CodeChangeKind.DELETE -> palette.error
        CodeChangeKind.RENAME -> palette.accent
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiModuleText(change.kind.marker, color = markerColor, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AiModuleText(name, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dir.isNotEmpty()) {
                AiModuleText(dir, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (change is CodeRename) {
                AiModuleText(
                    "from ${change.oldPath}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
