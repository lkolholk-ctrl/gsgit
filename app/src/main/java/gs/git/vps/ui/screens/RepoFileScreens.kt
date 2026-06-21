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
 * Экраны просмотра файлов/диффа репо-модуля: CommitDiffScreen, BlameViewScreen,
 * FileHistoryScreen. Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun CommitDiffScreen(repo: GHRepo, sha: String, onBack: () -> Unit) { val context = LocalContext.current; var detail by remember { mutableStateOf<GHCommitDetail?>(null) }; var statuses by remember { mutableStateOf<List<GHCommitStatus>>(emptyList()) }; var loading by remember { mutableStateOf(true) }
    LaunchedEffect(sha) { detail = GitHubManager.getCommitDiff(context, repo.owner, repo.name, sha); statuses = GitHubManager.getCommitStatuses(context, repo.owner, repo.name, sha); loading = false }
    AiModuleSurface {
    val commitPalette = AiModuleTheme.colors
    Column(Modifier.fillMaxSize().background(commitPalette.background)) {
        GitHubPageBar(
            title = "> ${sha.take(7)}",
            subtitle = detail?.message?.lines()?.firstOrNull(),
            onBack = onBack,
        )
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") }
        else if (detail != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+${detail!!.totalAdditions}", fontSize = 12.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, color = GitHubSuccessGreen)
                Text("-${detail!!.totalDeletions}", fontSize = 12.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, color = GitHubErrorRed)
                Text("${detail!!.files.size} files", fontSize = 12.sp, fontFamily = JetBrainsMono, color = commitPalette.textSecondary)
            }
            if (statuses.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    statuses.forEach { s ->
                        val stateIcon = when (s.state) { "success" -> "OK"; "pending" -> ".."; "error" -> "!!"; "failure" -> "XX"; else -> "??" }
                        val stateColor = when (s.state) { "success" -> GitHubSuccessGreen; "pending" -> commitPalette.warning; "error", "failure" -> GitHubErrorRed; else -> commitPalette.textMuted }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("[$stateIcon]", fontSize = 10.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = stateColor)
                            Text(s.context, fontSize = 10.sp, fontFamily = JetBrainsMono, color = commitPalette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(commitPalette.border))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(commitPalette.border))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(detail!!.files) { f ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statusGlyph = when (f.status) { "added" -> "+"; "removed" -> "-"; "renamed" -> "~"; else -> "M" }
                            val statusColor = when (f.status) { "added" -> GitHubSuccessGreen; "removed" -> GitHubErrorRed; else -> commitPalette.warning }
                            Text(statusGlyph, color = statusColor, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(12.dp))
                            Text(f.filename, fontSize = 12.sp, fontFamily = JetBrainsMono, color = commitPalette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("+${f.additions}", fontSize = 11.sp, fontFamily = JetBrainsMono, color = GitHubSuccessGreen)
                            Text("-${f.deletions}", fontSize = 11.sp, fontFamily = JetBrainsMono, color = GitHubErrorRed)
                        }
                        if (f.patch.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .border(1.dp, commitPalette.border, RoundedCornerShape(GitHubControlRadius))
                                    .background(commitPalette.surface)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                            ) {
                                Text(
                                    f.patch.take(2000),
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMono,
                                    color = commitPalette.textPrimary,
                                    lineHeight = 14.sp,
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(commitPalette.border))
                }
            }
        }
    }
    }
}

@Composable
internal fun BlameViewScreen(
    repo: GHRepo,
    file: GHContent,
    branch: String,
    onBack: () -> Unit,
    onCommitClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var ranges by remember { mutableStateOf<List<GHBlameRange>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file.path, branch) {
        loading = true; error = null
        ranges = try { GitHubManager.getFileBlame(context, repo.owner, repo.name, file.path, branch) }
        catch (e: Exception) { error = e.message; emptyList() }
        loading = false
    }
    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(title = "BLAME", subtitle = "${file.path} · $branch", onBack = onBack)
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading blame…") }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color(0xFFFF4444), fontSize = 13.sp, fontFamily = JetBrainsMono)
                }
            } else if (ranges.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No blame data available", color = palette.textMuted, fontSize = 13.sp, fontFamily = JetBrainsMono)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    ranges.forEach { range ->
                        item(key = "${range.startLine}-${range.sha}") {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(palette.surface)
                                        .clickable { onCommitClick(range.sha) }.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        range.sha.take(7),
                                        fontSize = 11.sp, fontFamily = JetBrainsMono,
                                        color = palette.accent, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        range.author,
                                        fontSize = 11.sp, fontFamily = JetBrainsMono,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        range.date.take(10),
                                        fontSize = 10.sp, fontFamily = JetBrainsMono,
                                        color = palette.textMuted
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "L${range.startLine}-${range.endLine}",
                                        fontSize = 10.sp, fontFamily = JetBrainsMono,
                                        color = palette.textSecondary
                                    )
                                }
                                Text(
                                    range.message,
                                    fontSize = 11.sp, fontFamily = JetBrainsMono,
                                    color = palette.textSecondary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileHistoryScreen(
    repo: GHRepo,
    file: GHContent,
    branch: String,
    onBack: () -> Unit,
    onCommitClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var commits by remember { mutableStateOf<List<GHCommit>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(file.path, branch) {
        loading = true; error = null
        commits = try { GitHubManager.getFileCommits(context, repo.owner, repo.name, file.path, branch) }
        catch (e: Exception) { error = e.message; emptyList() }
        loading = false
    }
    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(title = "HISTORY", subtitle = "${file.path} · $branch", onBack = onBack)
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading history…") }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color(0xFFFF4444), fontSize = 13.sp, fontFamily = JetBrainsMono)
                }
            } else if (commits.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No commit history", color = palette.textMuted, fontSize = 13.sp, fontFamily = JetBrainsMono)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(commits.size) { idx ->
                        val c = commits[idx]
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp)).background(palette.surface)
                                .clickable { onCommitClick(c.sha) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (c.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = c.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    c.message.lineSequence().firstOrNull() ?: c.message,
                                    fontSize = 12.sp, fontFamily = JetBrainsMono,
                                    color = palette.textPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(c.author, fontSize = 10.sp, fontFamily = JetBrainsMono, color = palette.textSecondary)
                                    Text(c.date.take(10), fontSize = 10.sp, fontFamily = JetBrainsMono, color = palette.textMuted)
                                }
                            }
                            Text(
                                c.sha,
                                fontSize = 11.sp, fontFamily = JetBrainsMono,
                                color = palette.accent, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

