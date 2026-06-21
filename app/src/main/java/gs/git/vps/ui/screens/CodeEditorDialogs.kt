package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WrapText
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.model.GHCommit
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.getRepoContents
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.Blue
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.ui.theme.SeparatorColor
import gs.git.vps.ui.theme.TextSecondary
import gs.git.vps.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Диалоги/доп-UI CodeEditor: FilePickerDialog, ConflictResolverDialog (+парсинг/сборка merge),
 * EditorCompactField, gutter-diff. Вынесено из GitHubCodeEditorModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun FilePickerDialog(
    repoOwner: String,
    repoName: String,
    branch: String,
    onFileSelected: (GHContent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors

    var currentPath by remember { mutableStateOf("") }
    var contents by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun loadPath(path: String) {
        loading = true
        currentPath = path
        scope.launch {
            try {
                contents = GitHubManager.getRepoContents(context, repoOwner, repoName, path, branch)
            } catch (_: Exception) {
                contents = emptyList()
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPath("")
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "select file to open",
        confirmButton = {},
        dismissButton = {
            AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = palette.textSecondary)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Path: /${currentPath.ifBlank { "" }}",
                fontSize = 11.sp,
                fontFamily = JetBrainsMono,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            if (loading) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner("loading files...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (currentPath.isNotBlank()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parent = currentPath.substringBeforeLast("/", "")
                                        loadPath(parent)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("../", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.accent)
                                Text("go up", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.textSecondary)
                            }
                        }
                    }
                    itemsIndexed(contents) { _, item ->
                        val isDir = item.type == "dir"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDir) {
                                        loadPath(item.path)
                                    } else {
                                        onFileSelected(item)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDir) "/" else " ",
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono,
                                color = if (isDir) palette.accent else palette.textSecondary
                            )
                            Text(
                                text = item.name,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono,
                                color = if (isDir) palette.accent else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (contents.isEmpty()) {
                        item {
                            Text("empty directory", fontSize = 11.sp, color = palette.textMuted, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

private sealed class MergeBlock {
    data class Normal(val content: String) : MergeBlock()
    data class Conflict(
        val base: String,
        val incoming: String,
        val baseName: String = "Current (Base)",
        val incomingName: String = "Incoming (Head)",
        var resolvedContent: String? = null
    ) : MergeBlock()
}

private fun parseConflictContent(content: String): List<MergeBlock> {
    val lines = content.lineSequence().toList()
    val blocks = mutableListOf<MergeBlock>()
    val currentNormal = StringBuilder()
    
    var inConflict = false
    val baseBuilder = StringBuilder()
    val incomingBuilder = StringBuilder()
    var inIncoming = false
    
    var baseLabel = "Current (Base)"
    var incomingLabel = "Incoming (Head)"
    
    for (line in lines) {
        if (line.startsWith("<<<<<<<")) {
            if (currentNormal.isNotEmpty()) {
                blocks.add(MergeBlock.Normal(currentNormal.toString()))
                currentNormal.clear()
            }
            inConflict = true
            inIncoming = false
            baseLabel = line.substring(7).trim().ifBlank { "Current (Base)" }
            baseBuilder.clear()
        } else if (line.startsWith("=======")) {
            if (inConflict) {
                inIncoming = true
            } else {
                currentNormal.append(line).append("\n")
            }
        } else if (line.startsWith(">>>>>>>")) {
            if (inConflict) {
                incomingLabel = line.substring(7).trim().ifBlank { "Incoming (Head)" }
                blocks.add(
                    MergeBlock.Conflict(
                        base = baseBuilder.toString().trimEnd('\n'),
                        incoming = incomingBuilder.toString().trimEnd('\n'),
                        baseName = baseLabel,
                        incomingName = incomingLabel
                    )
                )
                baseBuilder.clear()
                incomingBuilder.clear()
                inConflict = false
                inIncoming = false
            } else {
                currentNormal.append(line).append("\n")
            }
        } else {
            if (inConflict) {
                if (inIncoming) {
                    incomingBuilder.append(line).append("\n")
                } else {
                    baseBuilder.append(line).append("\n")
                }
            } else {
                currentNormal.append(line).append("\n")
            }
        }
    }
    
    if (inConflict) {
        currentNormal.append("<<<<<<< ").append(baseLabel).append("\n")
        currentNormal.append(baseBuilder)
        if (inIncoming) {
            currentNormal.append("=======\n")
            currentNormal.append(incomingBuilder)
        }
    }
    
    if (currentNormal.isNotEmpty()) {
        blocks.add(MergeBlock.Normal(currentNormal.toString().trimEnd('\n')))
    }
    
    return blocks
}

private fun assembleResolvedContent(blocks: List<MergeBlock>): String {
    val sb = StringBuilder()
    for (block in blocks) {
        when (block) {
            is MergeBlock.Normal -> {
                sb.append(block.content).append("\n")
            }
            is MergeBlock.Conflict -> {
                val resolved = block.resolvedContent ?: "<<<<<<< ${block.baseName}\n${block.base}\n=======\n${block.incoming}\n>>>>>>> ${block.incomingName}"
                if (resolved.isNotEmpty()) {
                    sb.append(resolved).append("\n")
                }
            }
        }
    }
    return sb.toString().trimEnd('\n')
}

@Composable
internal fun ConflictResolverDialog(
    content: String,
    onDismiss: () -> Unit,
    onResolved: (String) -> Unit
) {
    val palette = AiModuleTheme.colors
    val blocks = remember(content) { parseConflictContent(content) }
    val resolvedBlocks = remember { mutableStateListOf<MergeBlock>().apply { addAll(blocks) } }
    
    val totalConflicts = blocks.count { it is MergeBlock.Conflict }
    val resolvedCount = resolvedBlocks.count { it is MergeBlock.Conflict && it.resolvedContent != null }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "resolve conflicts",
        confirmButton = {
            AiModuleTextAction(
                label = "apply",
                tint = palette.accent,
                onClick = {
                    val resolvedText = assembleResolvedContent(resolvedBlocks)
                    onResolved(resolvedText)
                }
            )
        },
        dismissButton = {
            AiModuleTextAction(
                label = Strings.cancel.lowercase(),
                tint = palette.textSecondary,
                onClick = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resolved: $resolvedCount of $totalConflicts conflicts",
                fontSize = 12.sp,
                fontFamily = JetBrainsMono,
                color = if (resolvedCount == totalConflicts) Color(0xFF34C759) else palette.textSecondary
            )
            
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(resolvedBlocks) { index, block ->
                    when (block) {
                        is MergeBlock.Normal -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.background.copy(alpha = 0.5f))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = block.content,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMono,
                                    color = palette.textMuted,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        is MergeBlock.Conflict -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(GitHubControlRadius))
                                    .background(palette.surface)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Conflict #${resolvedBlocks.take(index + 1).count { it is MergeBlock.Conflict }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9500),
                                    fontFamily = JetBrainsMono
                                )
                                
                                val isBaseActive = block.resolvedContent == block.base
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isBaseActive) Color(0xFF34C759) else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = block.base)
                                        }
                                        .background(if (isBaseActive) Color(0xFF34C759).copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Current: " + block.baseName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBaseActive) Color(0xFF34C759) else palette.textPrimary,
                                        fontFamily = JetBrainsMono
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = block.base.ifBlank { "(empty)" },
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono,
                                        color = if (isBaseActive) palette.textPrimary else palette.textSecondary,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                val isIncomingActive = block.resolvedContent == block.incoming
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isIncomingActive) palette.accent else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = block.incoming)
                                        }
                                        .background(if (isIncomingActive) palette.accent.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Incoming: " + block.incomingName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncomingActive) palette.accent else palette.textPrimary,
                                        fontFamily = JetBrainsMono
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = block.incoming.ifBlank { "(empty)" },
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono,
                                        color = if (isIncomingActive) palette.textPrimary else palette.textSecondary,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                val bothText = block.base + "\n" + block.incoming
                                val isBothActive = block.resolvedContent == bothText
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isBothActive) Color(0xFF8E8E93) else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = bothText)
                                        }
                                        .background(if (isBothActive) Color(0xFF8E8E93).copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Accept both changes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isBothActive) palette.textPrimary else palette.textSecondary,
                                        fontFamily = JetBrainsMono
                                    )
                                    if (isBothActive) {
                                        Text(
                                            text = "Active",
                                            fontSize = 10.sp,
                                            color = Color(0xFF8E8E93),
                                            fontFamily = JetBrainsMono
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditorCompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "> ${label.lowercase()}",
            color = AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
        )
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AiModuleTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AiModuleTheme.colors.accent),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
    }
}

internal enum class GutterDiffState { NONE, ADDED, MODIFIED, DELETED_ABOVE }

internal fun computeGutterDiff(oldText: String, newText: String): List<GutterDiffState> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val n = oldLines.size
    val m = newLines.size
    
    val states = MutableList(m) { GutterDiffState.NONE }
    if (oldText == newText) return states

    // Fallback/Fast path for very large files to avoid OOM/ANR
    if (n * m > 100_000) {
        val oldSet = oldLines.toSet()
        for (j in 0 until m) {
            if (newLines[j] !in oldSet) {
                states[j] = GutterDiffState.ADDED
            }
        }
        return states
    }

    // Standard LCS DP
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) {
        for (j in 1..m) {
            if (oldLines[i - 1] == newLines[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    // Backtrack to align
    var i = n
    var j = m
    
    val path = mutableListOf<Pair<Int, Int>>()
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            path.add(Pair(i - 1, j - 1))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            path.add(Pair(-1, j - 1))
            j--
        } else {
            path.add(Pair(i - 1, -1))
            i--
        }
    }
    path.reverse()

    var idx = 0
    while (idx < path.size) {
        val step = path[idx]
        if (step.first == -1 && step.second != -1) {
            // Addition in newText. Let's check if there was a deletion nearby in this edit group.
            var hasDeletionNearby = false
            var k = idx - 1
            while (k >= 0) {
                if (path[k].first != -1 && path[k].second != -1) break
                if (path[k].first != -1 && path[k].second == -1) {
                    hasDeletionNearby = true
                    break
                }
                k--
            }
            if (!hasDeletionNearby) {
                k = idx + 1
                while (k < path.size) {
                    if (path[k].first != -1 && path[k].second != -1) break
                    if (path[k].first != -1 && path[k].second == -1) {
                        hasDeletionNearby = true
                        break
                    }
                    k++
                }
            }
            
            if (hasDeletionNearby) {
                states[step.second] = GutterDiffState.MODIFIED
            } else {
                states[step.second] = GutterDiffState.ADDED
            }
            idx++
        } else if (step.first != -1 && step.second == -1) {
            // Deletion in oldText. Mark next line of newText as DELETED_ABOVE.
            var nextNewIdx = -1
            var k = idx + 1
            while (k < path.size) {
                if (path[k].second != -1) {
                    nextNewIdx = path[k].second
                    break
                }
                k++
            }
            if (nextNewIdx != -1) {
                if (states[nextNewIdx] == GutterDiffState.NONE) {
                    states[nextNewIdx] = GutterDiffState.DELETED_ABOVE
                }
            } else {
                if (m > 0 && states[m - 1] == GutterDiffState.NONE) {
                    states[m - 1] = GutterDiffState.DELETED_ABOVE
                }
            }
            idx++
        } else {
            idx++
        }
    }
    
    return states
}

