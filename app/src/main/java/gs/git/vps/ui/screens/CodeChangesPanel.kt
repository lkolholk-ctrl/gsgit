package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Панель «изменения» Code-таба (Стадия 5) — отдельная поверхность Source Control в духе VS Code:
 * плоский список ВСЕХ файлов с незакоммиченными правками (через все папки), маркер «M», per-file
 * discard, тап → открыть файл, кнопка commit. Источник правды — buffer черновика (path → content)
 * на уровне RepoDetailScreen. См. docs/code-tab-spec.md.
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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AiModuleText(
                "← changes (${draftPaths.size})",
                color = palette.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.weight(1f))
            if (draftPaths.isNotEmpty()) {
                AiModuleText(
                    "commit",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onCommit),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleText("no changes", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
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

@Composable
private fun CodeChangeRow(path: String, onOpen: () -> Unit, onDiscard: () -> Unit) {
    val palette = AiModuleTheme.colors
    val name = path.substringAfterLast('/')
    val dir = path.substringBeforeLast('/', "")
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // «M» — modified-маркер (Source Control)
        AiModuleText("M", color = palette.warning, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
