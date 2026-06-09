package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.Strings
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.data.github.GHCommitDetail
import gs.git.vps.data.github.GHDiffFile
import gs.git.vps.data.github.GHPullFile
import gs.git.vps.data.github.GHReviewComment
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DiffViewerScreen(
    title: String,
    subtitle: String? = null,
    files: List<GHDiffFile>,
    totalAdditions: Int,
    totalDeletions: Int,
    repoOwner: String? = null,
    repoName: String? = null,
    pullNumber: Int? = null,
    comments: List<GHReviewComment> = emptyList(),
    onCommentAdded: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFile by remember { mutableStateOf<GHDiffFile?>(null) }
    var viewMode by remember { mutableStateOf(DiffViewMode.UNIFIED) }
    var showComments by remember { mutableStateOf(false) }

    if (selectedFile != null && !showComments) {
        FileDiffScreen(
            file = selectedFile!!,
            viewMode = viewMode,
            repoOwner = repoOwner,
            repoName = repoName,
            pullNumber = pullNumber,
            comments = comments.filter { it.path == selectedFile!!.filename },
            onCommentAdded = onCommentAdded,
            onBack = { selectedFile = null },
            onViewModeChange = { viewMode = it }
        )
        return
    }

    if (showComments && pullNumber != null) {
        PRReviewCommentsScreen(
            repoOwner = repoOwner!!,
            repoName = repoName!!,
            pullNumber = pullNumber,
            comments = comments,
            onCommentChanged = onCommentAdded,
            onBack = { showComments = false }
        )
        return
    }

    GitHubScreenFrame(
        title = "> ${title.lowercase()}",
        subtitle = subtitle,
        onBack = onBack,
        trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("+$totalAdditions", color = Color(0xFF34C759), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = JetBrainsMono)
                        Text("-$totalDeletions", color = Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = JetBrainsMono)
                    }
                    if (pullNumber != null) {
                        GitHubTopBarAction(
                            glyph = GhGlyphs.REACT,
                            onClick = { showComments = true },
                            tint = AiModuleTheme.colors.accent,
                            contentDescription = "review comments",
                        )
                    }
                }
        },
    ) {

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(files) { file ->
                val fileComments = comments.filter { it.path == file.filename }
                DiffFileCard(file, files.indexOf(file) + 1, files.size, fileComments.size) { selectedFile = file }
                if (file != files.lastOrNull()) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DiffFileCard(file: GHDiffFile, index: Int, total: Int, commentCount: Int = 0, onClick: () -> Unit) {
    val statusColor = when (file.status) {
        "added" -> Color(0xFF34C759)
        "removed" -> Color(0xFFFF3B30)
        "modified" -> Color(0xFFFF9500)
        "renamed" -> Color(0xFF5856D6)
        else -> AiModuleTheme.colors.textSecondary
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
            .background(AiModuleTheme.colors.surface).clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(statusColor))
            Text(file.filename, modifier = Modifier.weight(1f), fontSize = 14.sp, color = AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("${index}/$total", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(file.status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)
            Text("+${file.additions}", fontSize = 12.sp, color = Color(0xFF34C759))
            Text("-${file.deletions}", fontSize = 12.sp, color = Color(0xFFFF3B30))
            if (commentCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Comment, null, Modifier.size(12.dp), tint = AiModuleTheme.colors.accent)
                    Text("$commentCount", fontSize = 12.sp, color = AiModuleTheme.colors.accent, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

enum class DiffViewMode { UNIFIED, SPLIT }

@Composable
private fun FileDiffScreen(
    file: GHDiffFile,
    viewMode: DiffViewMode,
    repoOwner: String? = null,
    repoName: String? = null,
    pullNumber: Int? = null,
    comments: List<GHReviewComment> = emptyList(),
    onCommentAdded: () -> Unit = {},
    onBack: () -> Unit,
    onViewModeChange: (DiffViewMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lines = remember(file.patch) { parseDiffLines(file.patch) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentLine by remember { mutableStateOf<Int?>(null) }
    var commentPath by remember { mutableStateOf("") }
    var editComment by remember { mutableStateOf<GHReviewComment?>(null) }
    var deleteComment by remember { mutableStateOf<GHReviewComment?>(null) }
    var commentActionInFlight by remember { mutableStateOf(false) }
    val canMutateComments = repoOwner != null && repoName != null && pullNumber != null

    fun handleFileDiffBack() {
        when {
            showCommentDialog -> showCommentDialog = false
            editComment != null -> editComment = null
            deleteComment != null -> deleteComment = null
            else -> onBack()
        }
    }

    val splitLines = remember(lines) { alignSplitLines(lines) }

    GitHubScreenFrame(
        title = "> ${file.filename.substringAfterLast("/")}",
        subtitle = "${file.status} +${file.additions} -${file.deletions}",
        onBack = ::handleFileDiffBack,
        trailing = {
                GitHubTopBarAction(
                    glyph = if (viewMode == DiffViewMode.UNIFIED) GhGlyphs.LIST else GhGlyphs.LINES,
                    onClick = {
                    onViewModeChange(if (viewMode == DiffViewMode.UNIFIED) DiffViewMode.SPLIT else DiffViewMode.UNIFIED)
                    },
                    tint = AiModuleTheme.colors.accent,
                    contentDescription = "toggle diff view",
                )
        },
    ) {

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (viewMode == DiffViewMode.UNIFIED) {
                items(lines) { line ->
                    val lineNum = when (line) {
                        is PatchDiffLine.Added -> line.lineNum
                        is PatchDiffLine.Context -> line.newLineNum
                        else -> 0
                    }
                    val lineComments = if (lineNum > 0) comments.filter { it.line == lineNum } else emptyList()

                    Column {
                        DiffLineItem(
                            line = line,
                            viewMode = viewMode,
                            onAddComment = if (pullNumber != null && lineNum > 0) {
                                {
                                    commentLine = lineNum
                                    commentPath = file.filename
                                    showCommentDialog = true
                                }
                            } else null
                        )

                        if (lineComments.isNotEmpty()) {
                            lineComments.forEach { comment ->
                                CommentBubble(
                                    comment = comment,
                                    onEdit = if (canMutateComments) ({ editComment = comment }) else null,
                                    onDelete = if (canMutateComments) ({ deleteComment = comment }) else null
                                )
                            }
                        }
                    }
                }
            } else {
                items(splitLines) { pair ->
                    val lineNum = when (val right = pair.right) {
                        is PatchDiffLine.Added -> right.lineNum
                        is PatchDiffLine.Context -> right.newLineNum
                        else -> 0
                    }
                    val lineComments = if (lineNum > 0) comments.filter { it.line == lineNum } else emptyList()

                    Column {
                        SplitDiffLineRow(
                            pair = pair,
                            onAddCommentRight = if (pullNumber != null && lineNum > 0) {
                                {
                                    commentLine = lineNum
                                    commentPath = file.filename
                                    showCommentDialog = true
                                }
                            } else null
                        )

                        if (lineComments.isNotEmpty()) {
                            lineComments.forEach { comment ->
                                CommentBubble(
                                    comment = comment,
                                    onEdit = if (canMutateComments) ({ editComment = comment }) else null,
                                    onDelete = if (canMutateComments) ({ deleteComment = comment }) else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add comment dialog
    if (showCommentDialog && pullNumber != null) {
        var commentBody by remember { mutableStateOf("") }
        AiModuleAlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = "Add comment on line $commentLine",
            content = {
                AiModuleTextField(
                    value = commentBody,
                    onValueChange = { commentBody = it },
                    label = "Comment",
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6
                )
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "comment",
                    enabled = commentBody.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val ok = GitHubManager.createPullRequestReviewComment(
                                context, repoOwner!!, repoName!!, pullNumber,
                                commentBody, commentPath, commentLine!!
                            )
                            Toast.makeText(context, if (ok) "Comment added" else "Failed", Toast.LENGTH_SHORT).show()
                            if (ok) {
                                showCommentDialog = false
                                commentBody = ""
                                onCommentAdded()
                            }
                        }
                    },
                )
            },
            dismissButton = {
                AiModuleTextAction(label = "cancel", onClick = { showCommentDialog = false }, tint = AiModuleTheme.colors.textSecondary)
            }
        )
    }

    editComment?.let { comment ->
        ReviewCommentEditDialog(
            comment = comment,
            saving = commentActionInFlight,
            onDismiss = { if (!commentActionInFlight) editComment = null },
            onSave = { body ->
                commentActionInFlight = true
                scope.launch {
                    val ok = GitHubManager.updatePullRequestReviewComment(context, repoOwner!!, repoName!!, comment.id, body)
                    commentActionInFlight = false
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) {
                        editComment = null
                        onCommentAdded()
                    }
                }
            }
        )
    }

    deleteComment?.let { comment ->
        ReviewCommentDeleteDialog(
            comment = comment,
            deleting = commentActionInFlight,
            onDismiss = { if (!commentActionInFlight) deleteComment = null },
            onDelete = {
                commentActionInFlight = true
                scope.launch {
                    val ok = GitHubManager.deletePullRequestReviewComment(context, repoOwner!!, repoName!!, comment.id)
                    commentActionInFlight = false
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) {
                        deleteComment = null
                        onCommentAdded()
                    }
                }
            }
        )
    }
}

sealed class PatchDiffLine {
    data class Header(val text: String) : PatchDiffLine()
    data class Added(val text: String, val lineNum: Int) : PatchDiffLine()
    data class Removed(val text: String, val lineNum: Int) : PatchDiffLine()
    data class Context(val text: String, val oldLineNum: Int, val newLineNum: Int) : PatchDiffLine()
    data class NoNewline(val text: String) : PatchDiffLine()
}

private fun parseDiffLines(patch: String): List<PatchDiffLine> {
    val result = mutableListOf<PatchDiffLine>()
    val lines = patch.lines()
    var oldLine = 0
    var newLine = 0
    var inHunk = false

    for (line in lines) {
        when {
            line.startsWith("@@") -> {
                inHunk = true
                val match = Regex("@@ -(\\d+).*?\\+(\\d+)").find(line)
                oldLine = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                newLine = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
                result.add(PatchDiffLine.Header(line))
            }
            line.startsWith("+") -> {
                if (inHunk) {
                    result.add(PatchDiffLine.Added(line.drop(1), newLine))
                    newLine++
                }
            }
            line.startsWith("-") -> {
                if (inHunk) {
                    result.add(PatchDiffLine.Removed(line.drop(1), oldLine))
                    oldLine++
                }
            }
            line.startsWith(" ") -> {
                if (inHunk) {
                    result.add(PatchDiffLine.Context(line.drop(1), oldLine, newLine))
                    oldLine++
                    newLine++
                }
            }
            line.startsWith("\\") -> result.add(PatchDiffLine.NoNewline(line))
            else -> result.add(PatchDiffLine.Context(line, oldLine, newLine))
        }
    }
    return result
}

@Composable
private fun DiffLineItem(line: PatchDiffLine, viewMode: DiffViewMode, onAddComment: (() -> Unit)? = null) {
    when (line) {
        is PatchDiffLine.Header -> {
            Box(Modifier.fillMaxWidth().background(Color(0xFF2C2C2E)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(line.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF8E8E93))
            }
        }
        is PatchDiffLine.Added -> {
            Row(
                Modifier.fillMaxWidth().background(Color(0x0D34C759))
                    .clickable(enabled = onAddComment != null) { onAddComment?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+${line.lineNum}", modifier = Modifier.width(40.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF34C759))
                Text(line.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary)
                if (onAddComment != null) {
                    Icon(Icons.Rounded.AddComment, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.accent.copy(0.5f))
                }
            }
        }
        is PatchDiffLine.Removed -> {
            Row(Modifier.fillMaxWidth().background(Color(0x0DFF3B30)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("-${line.lineNum}", modifier = Modifier.width(40.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFFF3B30))
                Text(line.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary)
            }
        }
        is PatchDiffLine.Context -> {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(enabled = onAddComment != null) { onAddComment?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${line.oldLineNum}", modifier = Modifier.width(40.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF6E7681))
                Text(line.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = AiModuleTheme.colors.textSecondary)
                if (onAddComment != null) {
                    Icon(Icons.Rounded.AddComment, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.accent.copy(0.3f))
                }
            }
        }
        is PatchDiffLine.NoNewline -> {
            Text(line.text, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFFF9500))
        }
    }
}

@Composable
private fun CommentBubble(
    comment: GHReviewComment,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 48.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(comment.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.accent)
            Text(comment.createdAt.take(10), fontSize = 10.sp, color = AiModuleTheme.colors.textMuted)
            Spacer(Modifier.weight(1f))
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(15.dp), tint = AiModuleTheme.colors.accent)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(15.dp), tint = Color(0xFFFF3B30))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.body, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 18.sp)
    }
}

@Composable
fun PRReviewCommentsScreen(
    repoOwner: String,
    repoName: String,
    pullNumber: Int,
    comments: List<GHReviewComment>,
    onCommentChanged: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editComment by remember { mutableStateOf<GHReviewComment?>(null) }
    var deleteComment by remember { mutableStateOf<GHReviewComment?>(null) }
    var commentActionInFlight by remember { mutableStateOf(false) }

    fun handleReviewCommentsBack() {
        when {
            editComment != null -> editComment = null
            deleteComment != null -> deleteComment = null
            else -> onBack()
        }
    }

    GitHubScreenFrame(
        title = "> review comments",
        subtitle = "#$pullNumber · ${comments.size} comments",
        onBack = ::handleReviewCommentsBack,
    ) {

        if (comments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No review comments yet", fontSize = 14.sp, color = AiModuleTheme.colors.textMuted)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(comments) { comment ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(comment.path.substringAfterLast("/"), fontSize = 12.sp, color = AiModuleTheme.colors.accent, fontWeight = FontWeight.Medium)
                            Text("Line ${comment.line}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { editComment = comment }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.accent)
                            }
                            IconButton(onClick = { deleteComment = comment }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp), tint = Color(0xFFFF3B30))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(comment.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                            Text(comment.createdAt.take(10), fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(comment.body, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 18.sp)
                    }
                }
            }
        }
    }

    editComment?.let { comment ->
        ReviewCommentEditDialog(
            comment = comment,
            saving = commentActionInFlight,
            onDismiss = { if (!commentActionInFlight) editComment = null },
            onSave = { body ->
                commentActionInFlight = true
                scope.launch {
                    val ok = GitHubManager.updatePullRequestReviewComment(context, repoOwner, repoName, comment.id, body)
                    commentActionInFlight = false
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) {
                        editComment = null
                        onCommentChanged()
                    }
                }
            }
        )
    }

    deleteComment?.let { comment ->
        ReviewCommentDeleteDialog(
            comment = comment,
            deleting = commentActionInFlight,
            onDismiss = { if (!commentActionInFlight) deleteComment = null },
            onDelete = {
                commentActionInFlight = true
                scope.launch {
                    val ok = GitHubManager.deletePullRequestReviewComment(context, repoOwner, repoName, comment.id)
                    commentActionInFlight = false
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) {
                        deleteComment = null
                        onCommentChanged()
                    }
                }
            }
        )
    }
}

@Composable
private fun ReviewCommentEditDialog(
    comment: GHReviewComment,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var body by remember(comment.id) { mutableStateOf(comment.body) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Edit review comment",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${comment.path}:${comment.line}", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
                AiModuleTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = "Comment",
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            AiModuleTextAction(label = if (saving) "saving" else "save", enabled = !saving && body.isNotBlank(), onClick = { onSave(body) })
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), enabled = !saving, onClick = onDismiss, tint = AiModuleTheme.colors.textSecondary) }
    )
}

@Composable
private fun ReviewCommentDeleteDialog(
    comment: GHReviewComment,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Delete review comment?",
        content = { Text("Delete comment on ${comment.path}:${comment.line}?", fontSize = 13.sp, color = AiModuleTheme.colors.textSecondary) },
        confirmButton = {
            AiModuleTextAction(label = if (deleting) "deleting" else "delete", enabled = !deleting, onClick = onDelete, tint = Color(0xFFFF3B30))
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), enabled = !deleting, onClick = onDismiss, tint = AiModuleTheme.colors.textSecondary) }
    )
}

// PR Diff Screen wrapper
@Composable
fun PullRequestDiffScreen(
    repoOwner: String,
    repoName: String,
    pullNumber: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf<List<GHPullFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var prTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        files = GitHubManager.getPullRequestFiles(context, repoOwner, repoName, pullNumber)
        val pr = GitHubManager.getPullRequests(context, repoOwner, repoName).find { it.number == pullNumber }
        prTitle = pr?.title ?: "PR #$pullNumber"
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AiModuleSpinner(label = "loading…")
        }
        return
    }

    val diffFiles = remember(files) {
        files.map { GHDiffFile(it.filename, it.status, it.additions, it.deletions, it.patch) }
    }
    val totalAdditions = files.sumOf { it.additions }
    val totalDeletions = files.sumOf { it.deletions }

    // Load review comments
    var comments by remember { mutableStateOf<List<GHReviewComment>>(emptyList()) }
    LaunchedEffect(Unit) {
        comments = GitHubManager.getPullRequestReviewComments(context, repoOwner, repoName, pullNumber)
    }

    DiffViewerScreen(
        title = prTitle,
        subtitle = "#$pullNumber",
        files = diffFiles,
        totalAdditions = totalAdditions,
        totalDeletions = totalDeletions,
        repoOwner = repoOwner,
        repoName = repoName,
        pullNumber = pullNumber,
        comments = comments,
        onCommentAdded = {
            scope.launch {
                comments = GitHubManager.getPullRequestReviewComments(context, repoOwner, repoName, pullNumber)
            }
        },
        onBack = onBack
    )
}

// Commit Diff Screen wrapper
@Composable
fun CommitDiffScreen(
    repoOwner: String,
    repoName: String,
    sha: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<GHCommitDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        detail = GitHubManager.getCommitDiff(context, repoOwner, repoName, sha)
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AiModuleSpinner(label = "loading…")
        }
        return
    }

    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load diff", color = AiModuleTheme.colors.textMuted)
        }
        return
    }

    DiffViewerScreen(
        title = detail!!.sha.take(7),
        subtitle = detail!!.message.lines().firstOrNull() ?: "",
        files = detail!!.files,
        totalAdditions = detail!!.totalAdditions,
        totalDeletions = detail!!.totalDeletions,
        onBack = onBack
    )
}

private data class SplitDiffLine(
    val left: PatchDiffLine?,
    val right: PatchDiffLine?
)

private fun alignSplitLines(lines: List<PatchDiffLine>): List<SplitDiffLine> {
    val result = mutableListOf<SplitDiffLine>()
    var i = 0
    val n = lines.size
    while (i < n) {
        val line = lines[i]
        if (line is PatchDiffLine.Header || line is PatchDiffLine.NoNewline) {
            result.add(SplitDiffLine(line, null))
            i++
        } else if (line is PatchDiffLine.Context) {
            result.add(SplitDiffLine(line, line))
            i++
        } else {
            val removedRun = mutableListOf<PatchDiffLine.Removed>()
            val addedRun = mutableListOf<PatchDiffLine.Added>()
            var j = i
            while (j < n && (lines[j] is PatchDiffLine.Removed || lines[j] is PatchDiffLine.Added)) {
                val curr = lines[j]
                if (curr is PatchDiffLine.Removed) removedRun.add(curr)
                if (curr is PatchDiffLine.Added) addedRun.add(curr)
                j++
            }
            val maxLen = maxOf(removedRun.size, addedRun.size)
            for (k in 0 until maxLen) {
                val left = removedRun.getOrNull(k)
                val right = addedRun.getOrNull(k)
                result.add(SplitDiffLine(left, right))
            }
            i = j
        }
    }
    return result
}

@Composable
private fun SplitDiffLineRow(
    pair: SplitDiffLine,
    onAddCommentLeft: (() -> Unit)? = null,
    onAddCommentRight: (() -> Unit)? = null
) {
    val palette = AiModuleTheme.colors
    
    if (pair.left is PatchDiffLine.Header) {
        Box(Modifier.fillMaxWidth().background(Color(0xFF2C2C2E)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(pair.left.text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF8E8E93))
        }
        return
    }
    if (pair.left is PatchDiffLine.NoNewline) {
        Text(pair.left.text, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFFFF9500))
        return
    }
    
    Row(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    when (pair.left) {
                        is PatchDiffLine.Removed -> Color(0x0DFF3B30)
                        else -> Color.Transparent
                    }
                )
                .border(end = 0.5.dp, color = palette.border.copy(alpha = 0.3f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            when (val left = pair.left) {
                is PatchDiffLine.Removed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("-${left.lineNum}", modifier = Modifier.width(30.dp), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFFFF3B30))
                        Text(left.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = palette.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                is PatchDiffLine.Context -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${left.oldLineNum}", modifier = Modifier.width(30.dp), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6E7681))
                        Text(left.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = palette.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                else -> {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(
                    when (pair.right) {
                        is PatchDiffLine.Added -> Color(0x0D34C759)
                        else -> Color.Transparent
                    }
                )
                .clickable(enabled = onAddCommentRight != null && pair.right != null) { onAddCommentRight?.invoke() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            when (val right = pair.right) {
                is PatchDiffLine.Added -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+${right.lineNum}", modifier = Modifier.width(30.dp), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF34C759))
                        Text(right.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = palette.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (onAddCommentRight != null) {
                            Icon(Icons.Rounded.AddComment, null, Modifier.size(11.dp), tint = palette.accent.copy(0.5f))
                        }
                    }
                }
                is PatchDiffLine.Context -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${right.newLineNum}", modifier = Modifier.width(30.dp), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF6E7681))
                        Text(right.text, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = palette.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        if (onAddCommentRight != null) {
                            Icon(Icons.Rounded.AddComment, null, Modifier.size(11.dp), tint = palette.accent.copy(0.3f))
                        }
                    }
                }
                else -> {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}
