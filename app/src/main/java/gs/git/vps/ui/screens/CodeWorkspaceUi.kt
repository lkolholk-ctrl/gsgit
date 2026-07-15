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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.CodeAdd
import gs.git.vps.data.github.CodeChange
import gs.git.vps.data.github.CodeDelete
import gs.git.vps.data.github.CodeModify
import gs.git.vps.data.github.CodeRename
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.getGitCommit
import gs.git.vps.data.github.getGitRef
import gs.git.vps.data.github.getGitTree
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.searchCode
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

/** Compact controls shared by the Code browser and the lite editor. */
@Composable
internal fun CodeWorkspaceToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onQuickOpen: () -> Unit,
    onGlobalSearch: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GitHubTopBarAction(
            glyph = GhGlyphs.ARROW_LEFT,
            onClick = onGoBack,
            tint = palette.accent,
            enabled = canGoBack,
            contentDescription = "previous file",
        )
        GitHubTopBarAction(
            glyph = GhGlyphs.ARROW_RIGHT,
            onClick = onGoForward,
            tint = palette.accent,
            enabled = canGoForward,
            contentDescription = "next file",
        )
        Box(Modifier.height(22.dp).width(1.dp).background(palette.border))
        GitHubTerminalButton("quick open", onQuickOpen, color = palette.accent)
        GitHubTerminalButton("search repo", onGlobalSearch, color = palette.textSecondary)
    }
}

/** Persistent workspace tabs. Closing a tab never discards its draft. */
@Composable
internal fun CodeWorkspaceTabsRow(
    tabs: List<GHContent>,
    activePath: String?,
    dirtyPaths: Set<String>,
    onSelect: (GHContent) -> Unit,
    onClose: (GHContent) -> Unit,
) {
    if (tabs.isEmpty()) return
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(palette.surfaceElevated)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { file ->
            val active = file.path == activePath
            Row(
                Modifier
                    .height(38.dp)
                    .background(if (active) palette.background else Color.Transparent)
                    .clickable { onSelect(file) }
                    .padding(start = 10.dp, end = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AiModuleText(
                    text = file.name,
                    color = if (active) palette.accent else palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (file.path in dirtyPaths) {
                    AiModuleText("●", color = palette.warning, fontFamily = JetBrainsMono, fontSize = 9.sp)
                }
                AiModuleText(
                    text = GhGlyphs.CLOSE,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .clickable { onClose(file) }
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                )
            }
            Box(Modifier.height(38.dp).width(1.dp).background(palette.border))
        }
    }
}

@Composable
internal fun CodeQuickOpenDialog(
    repo: GHRepo,
    branch: String,
    changes: Collection<CodeChange>,
    preferredPaths: List<String>,
    onOpen: (GHContent) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    var query by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }

    LaunchedEffect(repo.fullName, branch, changes.toList()) {
        loading = true
        failed = false
        val tree = runCatching {
            val ref = GitHubManager.getGitRef(context, repo.owner, repo.name, "heads/$branch")
                ?: return@runCatching null
            val commit = GitHubManager.getGitCommit(context, repo.owner, repo.name, ref.nodeSha)
                ?: return@runCatching null
            GitHubManager.getGitTree(context, repo.owner, repo.name, commit.treeSha, recursive = true)
        }.getOrNull()
        if (tree == null) {
            failed = true
            files = draftOnlyFiles(changes)
        } else {
            truncated = tree.truncated
            val remote = tree.items.asSequence()
                .filter { it.type == "blob" }
                .map { GHContent(it.path.substringAfterLast('/'), it.path, "file", it.size, "", it.sha) }
                .toList()
            files = applyWorkspaceChanges(remote, changes)
        }
        loading = false
    }

    val shown = remember(files, query, preferredPaths) {
        val preferred = preferredPaths.withIndex().associate { it.value to it.index }
        files.asSequence()
            .filterNot { isLikelyBinaryFile(it.name) }
            .filter { query.isBlank() || it.path.contains(query.trim(), ignoreCase = true) }
            .sortedWith(
                compareBy<GHContent> {
                    val q = query.trim()
                    when {
                        q.isBlank() && it.path in preferred -> preferred.getValue(it.path)
                        q.isBlank() -> preferred.size + 1
                        it.name.equals(q, ignoreCase = true) -> 0
                        it.name.startsWith(q, ignoreCase = true) -> 1
                        it.path.startsWith(q, ignoreCase = true) -> 2
                        else -> 3
                    }
                }.thenBy { it.path.length }.thenBy { it.path.lowercase() },
            )
            .take(100)
            .toList()
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "quick open · $branch",
        confirmButton = {},
        dismissButton = { AiModuleTextAction("close", onDismiss, tint = palette.textSecondary) },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 520.dp)) {
            GitHubTerminalTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "type a file name or path",
                singleLine = true,
                minHeight = 40.dp,
            )
            Spacer(Modifier.height(8.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner("indexing tree…")
                }
                failed && files.isEmpty() -> GitHubMonoEmpty("failed to load repository tree")
                shown.isEmpty() -> GitHubMonoEmpty("no matching text files")
                else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(shown, key = { it.path }) { file ->
                        WorkspaceFileResult(
                            file = file,
                            badge = if (file.path in preferredPaths) "open" else null,
                            onClick = { onOpen(file) },
                        )
                    }
                }
            }
            val note = when {
                truncated -> "GitHub truncated this very large tree · showing available paths"
                query.isBlank() -> "${files.count { !isLikelyBinaryFile(it.name) }} text files · open tabs first"
                else -> "${shown.size} matches"
            }
            AiModuleText(note, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
    }
}

private data class WorkspaceSearchHit(val file: GHContent, val source: String)

@Composable
internal fun CodeGlobalSearchDialog(
    repo: GHRepo,
    branch: String,
    changes: Collection<CodeChange>,
    onOpen: (GHContent) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<WorkspaceSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun startSearch() {
        val q = query.trim()
        if (q.length < 2 || searching) return
        searching = true
        searched = true
        scope.launch {
            val local = changes.mapNotNull { change ->
                val content = when (change) {
                    is CodeAdd -> change.content
                    is CodeModify -> change.content
                    is CodeRename -> change.content
                    is CodeDelete -> null
                } ?: return@mapNotNull null
                if (!content.contains(q, ignoreCase = true) && !change.path.contains(q, ignoreCase = true)) {
                    return@mapNotNull null
                }
                WorkspaceSearchHit(
                    GHContent(change.path.substringAfterLast('/'), change.path, "file", content.length.toLong(), "", ""),
                    "draft",
                )
            }
            val remote = runCatching {
                GitHubManager.searchCode(context, q, repo.owner, repo.name)
            }.getOrDefault(emptyList()).map {
                WorkspaceSearchHit(GHContent(it.name, it.path, "file", 0L, "", it.sha), "github index")
            }
            hits = (local + remote).distinctBy { it.file.path }.take(50)
            searching = false
        }
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "search repository",
        confirmButton = {
            AiModuleTextAction(if (searching) "searching…" else "search", ::startSearch, tint = palette.accent)
        },
        dismissButton = { AiModuleTextAction("close", onDismiss, tint = palette.textSecondary) },
    ) {
        Column(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 520.dp)) {
            GitHubTerminalTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "search code (2+ characters)",
                singleLine = true,
                minHeight = 40.dp,
            )
            Spacer(Modifier.height(8.dp))
            when {
                searching -> Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner("searching GitHub…")
                }
                searched && hits.isEmpty() -> GitHubMonoEmpty("no code matches")
                hits.isNotEmpty() -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(hits, key = { it.file.path }) { hit ->
                        WorkspaceFileResult(hit.file, hit.source) { onOpen(hit.file) }
                    }
                }
                else -> GitHubMonoEmpty("searches file contents across the repository")
            }
            AiModuleText(
                text = if (branch == repo.defaultBranch) {
                    "GitHub index · $branch · local draft included"
                } else {
                    "GitHub index uses ${repo.defaultBranch} · local $branch draft included"
                },
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun WorkspaceFileResult(file: GHContent, badge: String?, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiModuleText(">", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            AiModuleText(
                file.name,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AiModuleText(
                file.path,
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge != null) {
            AiModuleText(
                badge,
                color = palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                modifier = Modifier
                    .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

private fun draftOnlyFiles(changes: Collection<CodeChange>): List<GHContent> =
    applyWorkspaceChanges(emptyList(), changes)

private fun applyWorkspaceChanges(
    remote: List<GHContent>,
    changes: Collection<CodeChange>,
): List<GHContent> {
    val files = remote.associateByTo(linkedMapOf()) { it.path }
    changes.forEach { change ->
        when (change) {
            is CodeDelete -> files.remove(change.path)
            is CodeRename -> {
                files.remove(change.oldPath)
                files[change.path] = GHContent(
                    change.path.substringAfterLast('/'), change.path, "file",
                    change.content?.length?.toLong() ?: 0L, "", change.sourceSha.orEmpty(),
                )
            }
            is CodeAdd -> files[change.path] = GHContent(
                change.path.substringAfterLast('/'), change.path, "file",
                change.content?.length?.toLong() ?: 0L, "", change.sourceSha.orEmpty(),
            )
            is CodeModify -> files[change.path] = files[change.path]?.copy(size = change.content.length.toLong())
                ?: GHContent(change.path.substringAfterLast('/'), change.path, "file", change.content.length.toLong(), "", "")
        }
    }
    return files.values.toList()
}
