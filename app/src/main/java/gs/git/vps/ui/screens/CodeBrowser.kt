package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** Цвет иконки по типу — из syntax-токенов темы (не хардкод). */
private fun codeEntryTint(item: GHContent, palette: gs.git.vps.ui.theme.AiModuleColors): Color {
    if (item.type == "dir") return palette.accent
    return when (item.name.substringAfterLast('.', "").lowercase()) {
        "kt", "java", "js", "ts", "tsx", "jsx", "py", "c", "cc", "cpp", "h", "hpp", "go", "rs",
        "rb", "swift", "cs", "php", "kts" -> palette.syntaxKeyword
        "sh", "bash", "gradle" -> palette.syntaxFlag
        "json", "yml", "yaml", "toml" -> palette.syntaxNumber
        "xml", "html", "css", "sql" -> palette.syntaxArg
        "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico" -> palette.syntaxString
        "md", "markdown", "txt", "rst", "adoc" -> palette.syntaxComment
        else -> palette.textSecondary
    }
}

/**
 * Браузер «рабочего дерева» Code-таба — терминальный стиль (как остальной GitHub-модуль): mono,
 * бордерные контролы (GitHubTerminalButton, GitHubControlRadius), GitHubMonoEmpty для состояний.
 * Цветные Material-иконки типов из syntax-токенов. P1: грязные маркеры (amber). См. docs/code-tab-spec.md.
 */
@Composable
internal fun CodeBrowser(
    repo: GHRepo,
    branch: String,
    path: String,
    onOpenDir: (GHContent) -> Unit,
    onOpenFile: (GHContent) -> Unit,
    onNavigatePath: (String) -> Unit,
    draftPaths: Set<String>,
    draftCount: Int,
    recents: List<GHContent>,
    onCommit: () -> Unit,
    onShowChanges: () -> Unit,
    onDiscardAll: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(repo.fullName, branch, path, reloadNonce) {
        loading = true; failed = false
        val r = runCatching { GitHubManager.getRepoContents(context, repo.owner, repo.name, path, branch) }.getOrNull()
        if (r == null) failed = true
        else items = r.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        CodeBreadcrumbs(path = path, branch = branch, onNavigatePath = onNavigatePath)
        if (recents.isNotEmpty()) CodeRecentsRow(recents = recents, onOpen = onOpenFile)
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
        if (draftCount > 0) {
            // Source Control bar (терминальный): счётчик (тап → панель) | discard / commit — бордерные
            // GitHubTerminalButton (6dp, mono), без filled-pill'ов.
            Row(
                Modifier.fillMaxWidth().background(palette.surfaceElevated).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.weight(1f).clickable(onClick = onShowChanges),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = palette.accent)
                    Spacer(Modifier.width(8.dp))
                    AiModuleText(
                        "$draftCount ${if (draftCount == 1) "change" else "changes"}",
                        color = palette.textPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
                GitHubTerminalButton(label = "discard", onClick = onDiscardAll, color = palette.error)
                GitHubTerminalButton(label = "commit", onClick = onCommit, color = palette.accent)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.12f)))
        }
        when {
            loading -> GitHubMonoEmpty(title = "loading…")
            failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiModuleText("!", color = palette.error, fontFamily = JetBrainsMono, fontSize = 22.sp)
                    AiModuleText("failed to load", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 13.sp)
                    GitHubTerminalButton(label = "retry", onClick = { reloadNonce++ }, color = palette.accent)
                }
            }
            items.isEmpty() -> GitHubMonoEmpty(title = "empty directory")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.path }) { item ->
                    CodeEntryRow(
                        item = item,
                        dirty = if (item.type == "dir") draftPaths.any { it.startsWith(item.path + "/") } else item.path in draftPaths,
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
        Icon(Icons.Rounded.AccountTree, contentDescription = null, modifier = Modifier.size(13.dp), tint = palette.textMuted)
        Spacer(Modifier.width(5.dp))
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
private fun CodeRecentsRow(recents: List<GHContent>, onOpen: (GHContent) -> Unit) {
    val palette = AiModuleTheme.colors
    val shape = RoundedCornerShape(GitHubControlRadius)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiModuleText("recent", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        recents.forEach { f ->
            Row(
                Modifier
                    .clip(shape)
                    .border(1.dp, palette.border.copy(alpha = 0.65f), shape)
                    .clickable { onOpen(f) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(codeEntryIcon(f), contentDescription = null, modifier = Modifier.size(13.dp), tint = codeEntryTint(f, palette))
                Spacer(Modifier.width(5.dp))
                AiModuleText(f.name, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CodeEntryRow(item: GHContent, dirty: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val isDir = item.type == "dir"
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            codeEntryIcon(item),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = codeEntryTint(item, palette),
        )
        Spacer(Modifier.width(12.dp))
        AiModuleText(
            item.name,
            color = palette.textPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = if (isDir) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (dirty) {
            // Грязный маркер = «modified» (amber/warning), консистентно с «M» в панели изменений.
            Box(Modifier.size(7.dp).clip(CircleShape).background(palette.warning))
            Spacer(Modifier.width(10.dp))
        }
        if (isDir) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.textMuted)
        } else if (item.size > 0) {
            AiModuleText(formatCodeSize(item.size), color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
    }
}

private fun formatCodeSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
