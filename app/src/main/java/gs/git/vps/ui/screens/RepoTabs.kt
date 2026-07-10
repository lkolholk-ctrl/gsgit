package gs.git.vps.ui.screens

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.em
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor


import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.material.icons.outlined.Link
import gs.git.vps.data.Strings
import gs.git.vps.util.DownloadStorage
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHContributor
import gs.git.vps.data.github.model.GHReaction
import gs.git.vps.data.github.model.GHBlameRange
import gs.git.vps.data.github.model.GHGitCommit
import gs.git.vps.data.github.model.GHGitTagDetail
import gs.git.vps.data.github.model.GHGitBlob
import gs.git.vps.data.github.model.GHGitTree
import gs.git.vps.data.github.model.GHGitRef
import gs.git.vps.data.github.model.GHCommitStatus
import gs.git.vps.data.github.model.GHCommitDetail
import gs.git.vps.data.github.model.GHCommit
import gs.git.vps.data.github.model.GHRelease
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoEvent
import gs.git.vps.data.github.model.GHRepoPerson
import gs.git.vps.data.github.model.GHTrafficPath
import gs.git.vps.data.github.model.GHTrafficReferrer
import gs.git.vps.data.github.model.GHTrafficSeries
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHCheckSuite
import gs.git.vps.data.github.model.GHPullRequest
import gs.git.vps.data.github.model.GHPullMergeStatus
import gs.git.vps.data.github.model.GHPullReview
import gs.git.vps.data.github.model.GHPullFile
import gs.git.vps.data.github.model.GHReviewComment
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHComment
import gs.git.vps.data.github.model.GHIssue
import gs.git.vps.data.github.model.GHIssueDetail
import gs.git.vps.data.github.model.GHIssueEvent
import gs.git.vps.data.github.model.GHLabel
import gs.git.vps.data.github.model.GHMilestone
import gs.git.vps.data.github.model.GHTimelineEvent
import gs.git.vps.notifications.GitHubNotificationTarget
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import okhttp3.OkHttpClient
import org.json.JSONObject

/**
 * Табы списков репо-модуля: FilesTab, CommitsTab (+ git-граф), IssuesTab, ReleasesTab,
 * иконки/цвета файлов. Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FilesTab(
    rootContents: List<GHContent>,
    childCache: Map<String, List<GHContent>>,
    expandedPaths: Set<String>,
    loadingPaths: Set<String>,
    listState: LazyListState,
    canWrite: Boolean = true,
    onToggleDir: (GHContent) -> Unit,
    onOpenDir: (GHContent) -> Unit,
    onFileClick: (GHContent) -> Unit,
    onEdit: (GHContent) -> Unit,
    onDelete: (GHContent) -> Unit,
    onDownload: (GHContent) -> Unit,
    onCopyPath: (GHContent) -> Unit,
) {
    val palette = AiModuleTheme.colors
    val rowFontSize = 14.sp
    val sizeFontSize = 12.sp
    var menuFor by remember { mutableStateOf<String?>(null) }
    var expandedFile by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val visibleNodes = remember(rootContents, childCache.toMap(), expandedPaths, loadingPaths, searchQuery) {
        val out = mutableListOf<FilesTreeRow>()
        
        fun hasMatchingChild(path: String): Boolean {
            val children = childCache[path] ?: return false
            return children.any { child ->
                child.name.contains(searchQuery, ignoreCase = true) || 
                (child.type == "dir" && hasMatchingChild(child.path))
            }
        }
        
        fun build(items: List<GHContent>, depth: Int, parentLines: List<Boolean>) {
            val filtered = if (searchQuery.isEmpty()) {
                items
            } else {
                items.filter { item ->
                    item.name.contains(searchQuery, ignoreCase = true) || 
                    (item.type == "dir" && hasMatchingChild(item.path))
                }
            }
            val sorted = filtered.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() }))
            sorted.forEachIndexed { i, item ->
                val isLast = i == sorted.lastIndex
                out.add(FilesTreeRow(
                    key = "${depth}|${item.path}",
                    item = item,
                    depth = depth,
                    parentLines = parentLines,
                    isLastInParent = isLast,
                    isLoading = false,
                ))
                val shouldExpand = if (searchQuery.isEmpty()) {
                    item.path in expandedPaths
                } else {
                    item.type == "dir" && hasMatchingChild(item.path)
                }
                if (item.type == "dir" && shouldExpand) {
                    val nextLines = parentLines + !isLast
                    val children = childCache[item.path]
                    if (children == null) {
                        out.add(FilesTreeRow(
                            key = "${item.path}::loading",
                            item = null,
                            depth = depth + 1,
                            parentLines = nextLines,
                            isLastInParent = true,
                            isLoading = true,
                        ))
                    } else {
                        build(children, depth + 1, nextLines)
                    }
                }
            }
        }
        build(rootContents, 0, emptyList())
        out.toList()
    }

    Column(Modifier.fillMaxSize()) {
        AiModuleSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = "Filter files..."
        )

        LazyColumn(
            Modifier.fillMaxSize().weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
        items(visibleNodes, key = { it.key }) { row ->
            val prefix = buildString {
                row.parentLines.forEach { hasMore -> append(if (hasMore) "\u2502  " else "   ") }
                append(if (row.isLastInParent) "\u2514\u2500 " else "\u251C\u2500 ")
            }
            if (row.isLoading) {
                Text(
                    "${prefix}\u2026 loading",
                    fontFamily = JetBrainsMono,
                    fontSize = rowFontSize,
                    lineHeight = rowFontSize,
                    color = palette.textMuted,
                )
                return@items
            }
            val item = row.item ?: return@items
            val isDir = item.type == "dir"
            val isExpanded = isDir && (item.path in expandedPaths || searchQuery.isNotEmpty())
            val toggleGlyph = when {
                isDir && isExpanded -> "\u25BE "
                isDir -> "\u25B8 "
                else -> "  "
            }
            val isHidden = item.name.startsWith(".")
            val nameColor = when {
                isDir -> palette.accent
                isHidden -> palette.textSecondary
                else -> palette.textPrimary
            }
            val displayName = if (isDir) "${item.name}/" else item.name
            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (isDir) onToggleDir(item)
                                else expandedFile = if (expandedFile == item.path) null else item.path
                            },
                            onLongClick = {
                                if (isDir) menuFor = item.path
                                else expandedFile = item.path
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        prefix,
                        fontFamily = JetBrainsMono,
                        fontSize = rowFontSize,
                        lineHeight = rowFontSize,
                        color = palette.textMuted,
                    )
                    Text(
                        toggleGlyph,
                        fontFamily = JetBrainsMono,
                        fontSize = rowFontSize,
                        lineHeight = rowFontSize,
                        color = if (isDir) palette.accent else palette.textMuted,
                    )
                    Text(
                        displayName,
                        fontFamily = JetBrainsMono,
                        fontSize = rowFontSize,
                        lineHeight = rowFontSize,
                        color = nameColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!isDir && item.size > 0) {
                        Text(
                            ghFmtSize(item.size),
                            fontFamily = JetBrainsMono,
                            fontSize = sizeFontSize,
                            lineHeight = rowFontSize,
                            color = palette.textMuted,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    } else if (isDir && isExpanded) {
                        val count = childCache[item.path]?.size
                        if (count != null) {
                            Text(
                                "($count)",
                                fontFamily = JetBrainsMono,
                                fontSize = sizeFontSize,
                                lineHeight = rowFontSize,
                                color = palette.textMuted,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
                if (isDir && menuFor == item.path) {
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .width(164.dp)
                            .background(palette.surface, RoundedCornerShape(GitHubControlRadius))
                            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                            .padding(vertical = 4.dp),
                    ) {
                        Text(
                            "> open in screen",
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { menuFor = null; onOpenDir(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                        Text(
                            "> copy path",
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { menuFor = null; onCopyPath(item) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            AnimatedVisibility(expandedFile == item.path && !isDir) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = (12 + (row.parentLines.size + 1) * 18).dp,
                            top = 4.dp,
                            bottom = 6.dp,
                        )
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GitHubTerminalButton("view", onClick = { onFileClick(item) }, color = palette.accent)
                    if (canWrite) GitHubTerminalButton("edit", onClick = { onEdit(item) }, color = palette.textSecondary)
                    GitHubTerminalButton("↓ download", onClick = { onDownload(item) }, color = palette.textSecondary)
                    if (canWrite) GitHubTerminalButton("× delete", onClick = { onDelete(item) }, color = palette.error)
                }
            }
        }
    }
}
}

@Composable internal fun Chip(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) { val chipTint = tint ?: AiModuleTheme.colors.accent; Row(Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(chipTint.copy(0.08f)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(icon, null, Modifier.size(12.dp), tint = chipTint); Text(label, fontSize = 10.sp, color = chipTint, fontWeight = FontWeight.Medium, fontFamily = JetBrainsMono) } }

private fun fileIcon(name: String): ImageVector = when (name.substringAfterLast(".", "").lowercase()) {
    "kt", "java", "js", "ts", "tsx", "jsx", "py", "rb", "go", "rs", "swift", "c", "cpp", "h", "html", "css", "json", "xml", "yml", "yaml" -> Icons.Rounded.Code
    "md", "markdown", "txt" -> Icons.Rounded.Article
    "png", "jpg", "jpeg", "gif", "webp", "svg" -> Icons.Rounded.Image
    "zip", "apk", "jar", "tar", "gz" -> Icons.Rounded.Archive
    else -> Icons.Rounded.InsertDriveFile
}

@Composable
private fun fileTint(name: String): Color = when (name.substringAfterLast(".", "").lowercase()) {
    "kt" -> Color(0xFFA97BFF)
    "java" -> Color(0xFFB07219)
    "js", "jsx" -> Color(0xFFF1E05A)
    "ts", "tsx" -> Color(0xFF3178C6)
    "md", "markdown" -> Blue
    "png", "jpg", "jpeg", "gif", "webp", "svg" -> Blue
    else -> TextSecondary
}

internal class GitGraphLayout(val commits: List<GHCommit>) {
    class CommitNode(
        val commit: GHCommit,
        val lane: Int,
        val activeLanes: List<String>,
        val nextActiveLanes: List<String>
    )

    val nodes: List<CommitNode>

    init {
        val list = mutableListOf<CommitNode>()
        val currentLanes = mutableListOf<String>()

        commits.forEach { c ->
            var laneIdx = currentLanes.indexOf(c.sha)
            if (laneIdx == -1) {
                laneIdx = currentLanes.size
                currentLanes.add(c.sha)
            }

            val snapshotBefore = currentLanes.toList()

            if (c.parents.isNotEmpty()) {
                currentLanes[laneIdx] = c.parents[0]
                for (pIdx in 1 until c.parents.size) {
                    val p = c.parents[pIdx]
                    if (p !in currentLanes) {
                        currentLanes.add(p)
                    }
                }
            } else {
                currentLanes.removeAt(laneIdx)
            }

            list.add(CommitNode(c, laneIdx, snapshotBefore, currentLanes.toList()))
        }
        nodes = list
    }
}

@Composable
private fun GitGraphCanvas(
    node: GitGraphLayout.CommitNode?,
    modifier: Modifier = Modifier
) {
    val palette = AiModuleTheme.colors
    val spacing = 12.dp
    val spacingPx = with(androidx.compose.ui.platform.LocalDensity.current) { spacing.toPx() }
    val nodeRadius = 4.dp
    val nodeRadiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { nodeRadius.toPx() }

    val colors = listOf(
        Color(0xFF58A6FF), // Blue
        Color(0xFF3FB950), // Green
        Color(0xFFBC8CFF), // Purple
        Color(0xFFF0883E), // Orange
        Color(0xFFFF7B72), // Red
        Color(0xFFDB6D28)  // Dark Orange
    )

    Canvas(modifier = modifier) {
        val h = size.height
        if (node == null) return@Canvas

        node.activeLanes.forEachIndexed { idx, sha ->
            val nextIdx = node.nextActiveLanes.indexOf(sha)
            val color = colors[idx % colors.size]
            val xFrom = (idx + 1) * spacingPx
            
            if (sha == node.commit.sha) {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(xFrom, 0f),
                    end = androidx.compose.ui.geometry.Offset(xFrom, h / 2),
                    strokeWidth = 2.dp.toPx()
                )
                node.commit.parents.forEach { parentSha ->
                    val pIdx = node.nextActiveLanes.indexOf(parentSha)
                    if (pIdx != -1) {
                        val xTo = (pIdx + 1) * spacingPx
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(xFrom, h / 2)
                            cubicTo(xFrom, h * 0.75f, xTo, h * 0.25f, xTo, h)
                        }
                        drawPath(
                            path = path,
                            color = colors[pIdx % colors.size],
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            } else if (nextIdx != -1) {
                val xTo = (nextIdx + 1) * spacingPx
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(xFrom, 0f)
                    cubicTo(xFrom, h / 2, xTo, h / 2, xTo, h)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        val nodeX = (node.lane + 1) * spacingPx
        val nodeY = h / 2
        drawCircle(
            color = colors[node.lane % colors.size],
            radius = nodeRadiusPx,
            center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
        )
        drawCircle(
            color = palette.background,
            radius = nodeRadiusPx / 2,
            center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
        )
    }
}

@Composable
internal fun CommitsTab(
    commits: List<GHCommit>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    listState: LazyListState,
    onClick: (GHCommit) -> Unit,
    onExploreFiles: (String) -> Unit
) {
    val palette = AiModuleTheme.colors

    Column(Modifier.fillMaxSize()) {
        val graphLayout = remember(commits) { GitGraphLayout(commits) }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(commits) { index, c ->
                val node = graphLayout.nodes.getOrNull(index)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .ghGlassCard(14.dp)
                        .clickable { onClick(c) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val graphWidth = remember(node) {
                        val maxLanes = maxOf(
                            node?.activeLanes?.size ?: 0,
                            node?.nextActiveLanes?.size ?: 0
                        ).coerceAtLeast(1)
                        (maxLanes * 12 + 12).dp
                    }
                    GitGraphCanvas(
                        node = node,
                        modifier = Modifier
                            .width(graphWidth)
                            .height(38.dp)
                    )

                    if (c.avatarUrl.isNotBlank()) {
                        AsyncImage(c.avatarUrl, c.author, Modifier.size(34.dp).clip(CircleShape))
                    } else {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AiModuleText(
                                c.sha.take(2).uppercase(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        AiModuleText(
                            c.message.lines().firstOrNull().orEmpty(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            AiModuleText(
                                c.author.ifBlank { "unknown" },
                                fontSize = 11.sp,
                                color = palette.accent,
                                fontWeight = FontWeight.Medium
                            )
                            if (c.verified) {
                                Box(
                                    modifier = Modifier
                                        .border(0.5.dp, Color(0xFF00FF66), RoundedCornerShape(3.dp))
                                        .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED",
                                        color = Color(0xFF00FF66),
                                        fontFamily = JetBrainsMono,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            AiModuleText(
                                c.sha.take(7),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = palette.textMuted,
                                letterSpacing = 0.5.sp
                            )
                            AiModuleText(c.date.take(10), fontSize = 11.sp, color = palette.textMuted)
                        }
                    }
                    AiModuleGlyph(GhGlyphs.ARROW_RIGHT, Modifier.size(16.dp), tint = palette.textMuted, fontSize = 13.sp)
                }
            }
            if (hasMore) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        GitHubTerminalButton("load more", onClick = onLoadMore, color = AiModuleTheme.colors.accent)
                    }
                }
            }
        }
    }
}


@Composable
internal fun IssuesTab(issues: List<GHIssue>, hasMore: Boolean, onLoadMore: () -> Unit, listState: LazyListState, onClick: (GHIssue) -> Unit) { LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) { items(issues) { issue ->
    val palette = AiModuleTheme.colors
    val stateColor = if (issue.state == "open") GitHubSuccessGreen else GitHubErrorRed
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).height(IntrinsicSize.Min).ghGlassCard(14.dp).clickable { onClick(issue) }) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(stateColor))
        Row(Modifier.weight(1f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(stateColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(stateColor))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                AiModuleText(issue.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary, maxLines = 2, lineHeight = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    AiModuleText("#${issue.number}", fontSize = 11.sp, color = palette.textSecondary, fontFamily = FontFamily.Monospace)
                    AiModuleText(issue.author, fontSize = 11.sp, color = palette.textSecondary, fontWeight = FontWeight.Medium)
                    AiModuleText(if (issue.isPR) "PR" else issue.state.uppercase(), fontSize = 10.sp, color = stateColor, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    if (issue.comments > 0) AiModuleText("${formatGitHubNumber(issue.comments)} comments", fontSize = 11.sp, color = palette.textSecondary)
                }
            }
            AiModuleGlyph(GhGlyphs.ARROW_RIGHT, Modifier.size(16.dp), tint = palette.textSecondary, fontSize = 13.sp)
        }
    }
}; if (hasMore) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { GitHubTerminalButton("load more", onClick = onLoadMore, color = AiModuleTheme.colors.accent) } } } }

@Composable
internal fun ReleasesTab(releases: List<GHRelease>, repo: GHRepo) { val context = LocalContext.current; val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) { items(releases) { r -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).ghGlassCard(14.dp).padding(14.dp)) {
        val colors = AiModuleTheme.colors
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.NewReleases, null, Modifier.size(20.dp), tint = if (r.prerelease) GitHubWarningAmber() else GitHubSuccessGreen); Text(r.name.ifBlank { r.tag }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary); if (r.prerelease) Text(Strings.ghPrerelease, fontSize = 10.sp, color = GitHubWarningAmber(), modifier = Modifier.background(GitHubWarningAmber().copy(0.1f), RoundedCornerShape(GitHubControlRadius)).padding(horizontal = 5.dp, vertical = 1.dp)) }
        Text(r.tag, fontSize = 12.sp, color = colors.textMuted, fontFamily = FontFamily.Monospace)
        if (r.body.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            GitHubMarkdownDocument(r.body, repo, onLinkClick = { context.openReadmeUrl(it) })
        }
        if (r.assets.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                r.assets.forEach { a -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(colors.background).clickable { scope.launch { val dest = DownloadStorage.file(context, a.name); GitHubManager.downloadFile(context, repo.owner, repo.name, a.downloadUrl, dest) } }.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(releaseAssetIcon(a.name), null, Modifier.size(24.dp), tint = colors.accent.copy(alpha = 0.72f)); Column(Modifier.weight(1f)) { Text(a.name, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${ghFmtSize(a.size)} · ${formatGitHubNumber(a.downloadCount)} downloads", fontSize = 10.sp, color = colors.textMuted) }; Icon(Icons.Rounded.Download, null, Modifier.size(16.dp), tint = colors.textMuted) } }
            }
        }
    } } }
}
