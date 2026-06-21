package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.getRepoContents
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/** Иконка по типу/расширению файла (read-only браузер Code-таба). */
private fun codeEntryIcon(item: GHContent): ImageVector {
    if (item.type == "dir") return Icons.Rounded.Folder
    return when (item.name.substringAfterLast('.', "").lowercase()) {
        "kt", "java", "js", "ts", "tsx", "jsx", "py", "c", "cc", "cpp", "h", "hpp", "go", "rs",
        "rb", "swift", "cs", "php", "sh", "bash", "gradle", "kts", "xml", "json", "yml", "yaml",
        "toml", "html", "css", "sql" -> Icons.Rounded.Code
        "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico" -> Icons.Rounded.Image
        "md", "markdown", "txt", "rst", "adoc" -> Icons.Rounded.Article
        else -> Icons.Rounded.Description
    }
}

/**
 * Read-only браузер «рабочего дерева» для Code-таба (Стадия 1). Фетчит содержимое одной директории
 * (GitHubManager.getRepoContents), показывает крошки пути, навигацию по папкам и иконки по типу.
 * Маркеры грязных/коммит/инлайн create-rename-delete — поздние стадии. GitHub-логика живёт ЗДЕСЬ
 * (в табе Code), не в редакторе. См. docs/code-tab-spec.md.
 */
@Composable
internal fun CodeBrowser(
    repo: GHRepo,
    branch: String,
    path: String,
    onOpenDir: (GHContent) -> Unit,
    onOpenFile: (GHContent) -> Unit,
    onNavigatePath: (String) -> Unit,
) {
    val palette = AiModuleTheme.colors
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(repo.fullName, branch, path) {
        loading = true; failed = false
        val r = runCatching { GitHubManager.getRepoContents(context, repo.owner, repo.name, path, branch) }.getOrNull()
        if (r == null) failed = true
        else items = r.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        CodeBreadcrumbs(path = path, branch = branch, onNavigatePath = onNavigatePath)
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading…")
            }
            failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleText("ошибка загрузки дерева", color = palette.error, fontFamily = JetBrainsMono, fontSize = 13.sp)
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleText("пусто", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.path }) { item ->
                    CodeEntryRow(
                        item = item,
                        onClick = { if (item.type == "dir") onOpenDir(item) else onOpenFile(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBreadcrumbs(path: String, branch: String, onNavigatePath: (String) -> Unit) {
    val palette = AiModuleTheme.colors
    val segments = if (path.isBlank()) emptyList() else path.split('/')
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiModuleText(branch, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
        AiModuleText("  ", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
        AiModuleText(
            "/",
            color = if (segments.isEmpty()) palette.textSecondary else palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.clickable { onNavigatePath("") },
        )
        segments.forEachIndexed { i, seg ->
            val isLast = i == segments.lastIndex
            val subPath = segments.take(i + 1).joinToString("/")
            AiModuleText(" / ", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
            AiModuleText(
                seg,
                color = if (isLast) palette.textSecondary else palette.accent,
                fontFamily = JetBrainsMono,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                modifier = Modifier.clickable(enabled = !isLast) { onNavigatePath(subPath) },
            )
        }
    }
}

@Composable
private fun CodeEntryRow(item: GHContent, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            codeEntryIcon(item),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (item.type == "dir") palette.accent else palette.textSecondary,
        )
        Spacer(Modifier.width(12.dp))
        AiModuleText(
            item.name,
            color = palette.textPrimary,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.type != "dir" && item.size > 0) {
            AiModuleText(formatCodeSize(item.size), color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
    }
}

private fun formatCodeSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
