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
 * Pull-requests репо-модуля: PullsTab, PullRequestDetailScreen, карточки/бейджи,
 * все Pull*-диалоги (edit/reviewers/review-history/merge/files/...).
 * Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun PullsTab(
    pulls: List<GHPullRequest>,
    repo: GHRepo,
    onRefresh: () -> Unit,
    listState: LazyListState,
    onOpenDetail: (GHPullRequest) -> Unit = {},
    onFilesClick: (Int) -> Unit = {},
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reviewTarget by remember { mutableStateOf<GHPullRequest?>(null) }
    var checkRunTarget by remember { mutableStateOf<GHPullRequest?>(null) }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        items(pulls) { pr ->
            val palette = AiModuleTheme.colors
            val prColor = pullStateColor(pr)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).height(IntrinsicSize.Min).ghGlassCard(14.dp).clickable { onOpenDetail(pr) }) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(prColor))
                Column(Modifier.weight(1f).padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(prColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MergeType, contentDescription = null, modifier = Modifier.size(16.dp), tint = prColor)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            AiModuleText(pr.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary, lineHeight = 18.sp, maxLines = 2)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                                AiModuleText("#${pr.number}", fontSize = 11.sp, color = palette.textSecondary, fontFamily = FontFamily.Monospace)
                                AiModuleText("${pr.head} -> ${pr.base}", fontSize = 11.sp, color = palette.textSecondary, fontFamily = FontFamily.Monospace)
                                AiModuleText(pr.author, fontSize = 11.sp, color = palette.textSecondary)
                                AiModuleText(if (pr.merged) "MERGED" else pr.state.uppercase(), fontSize = 10.sp, color = prColor, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                                if (pr.draft) AiModuleText("DRAFT", fontSize = 10.sp, color = palette.textSecondary, letterSpacing = 0.6.sp)
                                if (pr.reviewComments > 0) AiModuleText("${formatGitHubNumber(pr.reviewComments)} review comments", fontSize = 11.sp, color = palette.textSecondary)
                            }
                        }
                    }
                    if (pr.body.isNotBlank()) {
                        AiModuleText(
                            pr.body.replace("\n", " ").take(140),
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                            maxLines = 2,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp, start = 38.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 38.dp).horizontalScroll(rememberScrollState())) {
                        GitHubTerminalButton("details", onClick = { onOpenDetail(pr) }, color = AiModuleTheme.colors.accent)
                        GitHubTerminalButton("files", onClick = { onFilesClick(pr.number) }, color = AiModuleTheme.colors.textSecondary)
                        GitHubTerminalButton("review", onClick = { reviewTarget = pr }, color = AiModuleTheme.colors.textSecondary)
                        GitHubTerminalButton("checks", onClick = { checkRunTarget = pr }, color = AiModuleTheme.colors.textSecondary)
                        if (pr.state == "open" && !pr.merged && !pr.draft) {
                            GitHubTerminalButton("update branch", color = AiModuleTheme.colors.accent, onClick = {
                                scope.launch {
                                    val ok = GitHubManager.updatePullRequestBranch(context, repo.owner, repo.name, pr.number)
                                    Toast.makeText(context, if (ok) "Branch updated" else "Update failed", Toast.LENGTH_SHORT).show()
                                    if (ok) onRefresh()
                                }
                            })
                            GitHubTerminalButton("merge", color = GitHubSuccessGreen, onClick = {
                                scope.launch {
                                    val ok = GitHubManager.mergePullRequest(context, repo.owner, repo.name, pr.number)
                                    Toast.makeText(context, if (ok) Strings.ghMerged else Strings.error, Toast.LENGTH_SHORT).show()
                                    if (ok) onRefresh()
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    if (reviewTarget != null) {
        PullReviewDialog(
            repo = repo,
            pr = reviewTarget!!,
            onDismiss = { reviewTarget = null },
            onDone = {
                reviewTarget = null
                onRefresh()
            }
        )
    }

    if (checkRunTarget != null) {
        CheckRunsScreen(
            repoOwner = repo.owner,
            repoName = repo.name,
            ref = checkRunTarget!!.headSha.ifBlank { checkRunTarget!!.head },
            onBack = { checkRunTarget = null },
            onNavigateToCode = onNavigateToCode?.let { nav ->
                { path, line ->
                    checkRunTarget = null
                    nav(path, line)
                }
            }
        )
    }
}

@Composable
internal fun PullRequestDetailScreen(
    repo: GHRepo,
    pullNumber: Int,
    onBack: () -> Unit,
    onOpenFiles: (Int) -> Unit,
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pr by remember { mutableStateOf<GHPullRequest?>(null) }
    var files by remember { mutableStateOf<List<GHPullFile>>(emptyList()) }
    var comments by remember { mutableStateOf<List<GHReviewComment>>(emptyList()) }
    var checks by remember { mutableStateOf<List<GHCheckRun>>(emptyList()) }
    var checkSuites by remember { mutableStateOf<List<GHCheckSuite>>(emptyList()) }
    var mergedStatus by remember { mutableStateOf<GHPullMergeStatus?>(null) }
    var reviews by remember { mutableStateOf<List<GHPullReview>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showReview by remember { mutableStateOf(false) }
    var showChecks by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showReviewers by remember { mutableStateOf(false) }
    var showReviews by remember { mutableStateOf(false) }
    var showMerge by remember { mutableStateOf(false) }
    var merging by remember { mutableStateOf(false) }

    suspend fun refreshPull() {
        loading = true
        val detail = GitHubManager.getPullRequestDetail(context, repo.owner, repo.name, pullNumber)
        pr = detail
        files = GitHubManager.getPullRequestFiles(context, repo.owner, repo.name, pullNumber)
        comments = GitHubManager.getPullRequestReviewComments(context, repo.owner, repo.name, pullNumber)
        reviews = GitHubManager.getPullRequestReviews(context, repo.owner, repo.name, pullNumber)
        mergedStatus = GitHubManager.getPullRequestMergedStatus(context, repo.owner, repo.name, pullNumber)
        val ref = detail?.let { pull -> pull.headSha.ifBlank { pull.head } }.orEmpty()
        checks = if (ref.isBlank()) emptyList() else GitHubManager.getPullRequestCheckRuns(context, repo.owner, repo.name, ref)
        checkSuites = if (ref.isBlank()) emptyList() else GitHubManager.getPullRequestCheckSuites(context, repo.owner, repo.name, ref)
        loading = false
    }

    LaunchedEffect(pullNumber) { refreshPull() }

    fun handlePullDetailBack() {
        when {
            showReview -> showReview = false
            showChecks -> showChecks = false
            showEdit -> showEdit = false
            showReviewers -> showReviewers = false
            showReviews -> showReviews = false
            showMerge -> showMerge = false
            else -> onBack()
        }
    }

    val current = pr
    val currentHtmlUrl = current?.htmlUrl.orEmpty()
    if (showChecks && current != null) {
        CheckRunsScreen(
            repoOwner = repo.owner,
            repoName = repo.name,
            ref = current.headSha.ifBlank { current.head },
            onBack = ::handlePullDetailBack,
            onNavigateToCode = onNavigateToCode?.let { nav ->
                { path, line ->
                    showChecks = false
                    nav(path, line)
                }
            }
        )
        return
    }

    AiModuleSurface {
    val prPalette = AiModuleTheme.colors
    Column(Modifier.fillMaxSize().background(prPalette.background)) {
        GitHubPageBar(
            title = "> pr #$pullNumber",
            subtitle = repo.name,
            onBack = ::handlePullDetailBack,
            trailing = {
                AiModuleGlyphAction(
                    glyph = GhGlyphs.REFRESH,
                    onClick = { scope.launch { refreshPull() } },
                    tint = prPalette.accent,
                    contentDescription = "refresh",
                )
                if (currentHtmlUrl.isNotBlank()) {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.OPEN_NEW,
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentHtmlUrl)))
                            } catch (_: Exception) {
                                Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                            }
                        },
                        tint = prPalette.accent,
                        contentDescription = "open in browser",
                    )
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading…")
            }
            return@Column
        }

        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load pull request", color = TextTertiary, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth().ghGlassCard(16.dp).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AiModuleGlyph(GhGlyphs.MERGE, Modifier.size(22.dp), tint = pullStateColor(current), fontSize = 13.sp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(current.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 20.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PullBadge(pullStateLabel(current), pullStateColor(current))
                                if (current.draft) PullBadge("Draft", TextTertiary)
                                PullBadge("${current.head} -> ${current.base}", Blue)
                            }
                            Text("Opened by ${current.author}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    if (current.body.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(current.body, fontSize = 12.sp, color = TextSecondary, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    }
                    if (current.requestedReviewers.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            current.requestedReviewers.forEach { reviewer -> PullBadge("review: $reviewer", TextSecondary) }
                        }
                    }
                }
            }

            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PullMetric("${current.commits.takeIf { it > 0 } ?: "?"}", "commits", Blue)
                    PullMetric("${current.changedFiles.takeIf { it > 0 } ?: files.size}", "files", TextSecondary)
                    PullMetric("+${current.additions.takeIf { it > 0 } ?: files.sumOf { f -> f.additions }}", "added", Color(0xFF34C759))
                    PullMetric("-${current.deletions.takeIf { it > 0 } ?: files.sumOf { f -> f.deletions }}", "deleted", Color(0xFFFF3B30))
                    PullMetric("${comments.size}", "review comments", TextSecondary)
                    PullMetric("${reviews.size}", "reviews", TextSecondary)
                }
            }

            item {
                PullMergeabilityCard(current, checks, checkSuites, mergedStatus)
            }

            item {
                val canWrite = repo.canWrite()
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canWrite) GitHubTerminalButton("edit", onClick = { showEdit = true }, color = TextSecondary)
                    GitHubTerminalButton("files", onClick = { onOpenFiles(pullNumber) }, color = Blue)
                    if (canWrite) GitHubTerminalButton("review", onClick = { showReview = true }, color = Blue)
                    if (canWrite) GitHubTerminalButton("reviewers", onClick = { showReviewers = true }, color = Blue)
                    GitHubTerminalButton("reviews", onClick = { showReviews = true }, color = TextSecondary)
                    GitHubTerminalButton("checks", onClick = { showChecks = true }, color = Blue)
                    if (canWrite && current.state == "open" && !current.merged && !current.draft) {
                        GitHubTerminalButton(if (merging) "merging..." else "merge", color = GitHubSuccessGreen, enabled = !merging, onClick = {
                            if (!merging) showMerge = true
                        })
                    }
                }
            }

            item {
                Text("Changed files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            if (files.isEmpty()) {
                item { Text("No files loaded", fontSize = 13.sp, color = TextTertiary) }
            } else {
                items(files.take(12)) { file ->
                    PullFileSummaryRow(file)
                }
                if (files.size > 12) {
                    item {
                        Text("+${files.size - 12} more files", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }

            if (comments.isNotEmpty()) {
                item { Text("Review comments", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) }
                items(comments) { c ->
                    Column(Modifier.fillMaxWidth().ghGlassCard(12.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.author, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, fontFamily = JetBrainsMono)
                            Spacer(Modifier.width(6.dp))
                            Text(c.path, fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(":${c.line}", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
                        }
                        Text(c.body, fontSize = 12.sp, color = TextPrimary, lineHeight = 16.sp)
                        val rcContext = LocalContext.current
                        val rcScope = rememberCoroutineScope()
                        val rcCurrentLogin = remember { GitHubManager.getCachedUser(rcContext)?.login.orEmpty() }
                        var rcReactions by remember(c.id) { mutableStateOf<List<GHReaction>>(emptyList()) }
                        var rcMutatingEmoji by remember(c.id) { mutableStateOf<String?>(null) }
                        var showReactionPicker by remember(c.id) { mutableStateOf(false) }
                        var editingComment by remember(c.id) { mutableStateOf(false) }
                        var editedBody by remember(c.id) { mutableStateOf(c.body) }
                        LaunchedEffect(c.id) {
                            rcReactions = GitHubManager.getPullRequestReviewCommentReactions(rcContext, repo.owner, repo.name, c.id)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rcReactions.groupBy { it.content }.forEach { (emoji, reacts) ->
                                val ownReaction = reacts.firstOrNull {
                                    rcCurrentLogin.isNotBlank() && it.user.equals(rcCurrentLogin, ignoreCase = true)
                                }
                                AiModulePillButton(label = "$emoji ${reacts.size}", onClick = {
                                    if (rcMutatingEmoji != null) return@AiModulePillButton
                                    rcMutatingEmoji = emoji
                                    rcScope.launch {
                                        if (ownReaction != null) {
                                            GitHubManager.deletePullRequestReviewCommentReaction(rcContext, repo.owner, repo.name, ownReaction.id)
                                        } else {
                                            GitHubManager.addPullRequestReviewCommentReaction(rcContext, repo.owner, repo.name, c.id, emoji)
                                        }
                                        rcReactions = GitHubManager.getPullRequestReviewCommentReactions(rcContext, repo.owner, repo.name, c.id)
                                        rcMutatingEmoji = null
                                    }
                                }, enabled = rcMutatingEmoji == null, accent = ownReaction != null)
                            }
                            AiModulePillButton(label = "+react", enabled = rcMutatingEmoji == null, onClick = {
                                showReactionPicker = !showReactionPicker
                            }, accent = true)
                            if (c.author == GitHubManager.getCachedUser(rcContext)?.login) {
                                AiModulePillButton(label = "edit", onClick = {
                                    editedBody = c.body
                                    editingComment = true
                                })
                            }
                        }
                        if (showReactionPicker) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("+1", "-1", "laugh", "hooray", "rocket", "heart", "eyes").forEach { emoji ->
                                    val ownReaction = rcReactions.firstOrNull {
                                        it.content == emoji && rcCurrentLogin.isNotBlank() && it.user.equals(rcCurrentLogin, ignoreCase = true)
                                    }
                                    AiModulePillButton(label = emoji, onClick = {
                                        if (rcMutatingEmoji != null) return@AiModulePillButton
                                        rcMutatingEmoji = emoji
                                        rcScope.launch {
                                            val ok = if (ownReaction != null) {
                                                GitHubManager.deletePullRequestReviewCommentReaction(rcContext, repo.owner, repo.name, ownReaction.id)
                                            } else {
                                                GitHubManager.addPullRequestReviewCommentReaction(rcContext, repo.owner, repo.name, c.id, emoji)
                                            }
                                            rcReactions = GitHubManager.getPullRequestReviewCommentReactions(rcContext, repo.owner, repo.name, c.id)
                                            if (ok) showReactionPicker = false
                                            rcMutatingEmoji = null
                                        }
                                    }, enabled = rcMutatingEmoji == null, accent = ownReaction != null)
                                }
                            }
                        }
                        if (editingComment) {
                            AiModuleAlertDialog(
                                onDismissRequest = { editingComment = false },
                                title = "Edit review comment",
                                content = {
                                    AiModuleTextField(
                                        value = editedBody,
                                        onValueChange = { editedBody = it },
                                        label = "Comment",
                                        minLines = 3,
                                        maxLines = 8,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                },
                                confirmButton = {
                                    AiModuleTextAction(label = "save", onClick = {
                                        val nextBody = editedBody.trim()
                                        if (nextBody.isBlank()) return@AiModuleTextAction
                                        rcScope.launch {
                                            val updated = GitHubManager.updatePullRequestReviewComment(
                                                rcContext, repo.owner, repo.name, c.id, nextBody
                                            )
                                            if (updated) {
                                                comments = comments.map { comment ->
                                                    if (comment.id == c.id) comment.copy(body = nextBody) else comment
                                                }
                                                editingComment = false
                                            } else {
                                                Toast.makeText(rcContext, "Could not update review comment", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                                },
                                dismissButton = {
                                    AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { editingComment = false })
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showReview && current != null) {
        PullReviewDialog(
            repo = repo,
            pr = current,
            onDismiss = { showReview = false },
            onDone = {
                showReview = false
                scope.launch { refreshPull() }
            }
        )
    }
    if (showEdit && current != null) {
        PullEditDialog(
            repo = repo,
            pr = current,
            onDismiss = { showEdit = false },
            onDone = {
                showEdit = false
                scope.launch { refreshPull() }
            }
        )
    }
    if (showReviewers && current != null) {
        PullReviewersDialog(
            repo = repo,
            pr = current,
            onDismiss = { showReviewers = false },
            onDone = {
                showReviewers = false
                scope.launch { refreshPull() }
            }
        )
    }
    if (showReviews && current != null) {
        PullReviewHistoryDialog(
            repo = repo,
            pr = current,
            reviews = reviews,
            onDismiss = { showReviews = false },
            onChanged = { scope.launch { refreshPull() } }
        )
    }
    if (showMerge && current != null) {
        PullMergeDialog(
            pr = current,
            merging = merging,
            onDismiss = { showMerge = false },
            onMerge = { method, title, message ->
                if (!merging) {
                    merging = true
                    scope.launch {
                        val ok = GitHubManager.mergePullRequest(context, repo.owner, repo.name, pullNumber, message = message, method = method, title = title)
                        merging = false
                        showMerge = false
                        Toast.makeText(context, if (ok) Strings.ghMerged else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) refreshPull()
                    }
                }
            }
        )
    }
}

@Composable
internal fun PullBadge(text: String, color: Color) {
    Text(
        text,
        fontSize = 10.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(color.copy(0.1f)).padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun PullMetric(value: String, label: String, color: Color) {
    Row(
        Modifier.ghGlassCard(10.dp).padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(value, fontSize = 15.sp, color = color, fontWeight = FontWeight.Light, fontFamily = FontFamily.Monospace)
        Text(label.uppercase(), fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun PullMergeabilityCard(
    pr: GHPullRequest,
    checks: List<GHCheckRun>,
    checkSuites: List<GHCheckSuite>,
    mergedStatus: GHPullMergeStatus?
) {
    val palette = AiModuleTheme.colors
    val failedChecks = checks.count { it.conclusion in listOf("failure", "cancelled", "timed_out", "action_required") }
    val activeChecks = checks.count { it.status != "completed" }
    val successChecks = checks.count { it.conclusion == "success" }
    val failedSuites = checkSuites.count { it.conclusion in listOf("failure", "cancelled", "timed_out", "action_required") }
    val activeSuites = checkSuites.count { it.status != "completed" }
    val successSuites = checkSuites.count { it.conclusion == "success" }
    val mergeColor = pullMergeColor(pr)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("merge", color = mergeColor, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(pullMergeText(pr), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary, modifier = Modifier.weight(1f))
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PullBadge("$successChecks successful", Color(0xFF34C759))
            if (activeChecks > 0) PullBadge("$activeChecks active", Blue)
            if (failedChecks > 0) PullBadge("$failedChecks failed", Color(0xFFFF3B30))
            if (checks.isEmpty()) PullBadge("No checks", TextTertiary)
            if (checkSuites.isNotEmpty()) {
                PullBadge("$successSuites suites ok", Color(0xFF34C759))
                if (activeSuites > 0) PullBadge("$activeSuites suites active", Blue)
                if (failedSuites > 0) PullBadge("$failedSuites suites failed", Color(0xFFFF3B30))
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val explicitLabel = when {
                mergedStatus == null -> "merged endpoint: pending"
                !mergedStatus.checked -> "merged endpoint: unavailable"
                mergedStatus.merged -> "merged endpoint: merged"
                else -> "merged endpoint: not merged"
            }
            PullBadge(explicitLabel, if (mergedStatus?.merged == true) GitHubMergedPurple else palette.textSecondary)
            if (checkSuites.isEmpty()) PullBadge("No check suites", TextTertiary)
        }
        if (pr.mergeableState.isNotBlank()) {
            Text("Merge state: ${pr.mergeableState}", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
        }
        if (mergedStatus != null && !mergedStatus.checked && mergedStatus.message.isNotBlank()) {
            Text("Merged endpoint: ${mergedStatus.message}", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PullFileSummaryRow(file: GHPullFile) {
    val color = when (file.status) {
        "added" -> Color(0xFF34C759)
        "removed" -> Color(0xFFFF3B30)
        "renamed" -> Color(0xFF5856D6)
        else -> Color(0xFFFF9500)
    }
    Row(
        Modifier.fillMaxWidth().ghGlassCard(12.dp).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(file.filename, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("+${file.additions}", fontSize = 11.sp, color = Color(0xFF34C759))
        Text("-${file.deletions}", fontSize = 11.sp, color = Color(0xFFFF3B30))
    }
}

private fun pullStateLabel(pr: GHPullRequest): String = when {
    pr.merged -> "Merged"
    pr.state == "closed" -> "Closed"
    pr.state == "open" -> "Open"
    else -> pr.state.replaceFirstChar { it.uppercase() }
}

@Composable
private fun pullStateColor(pr: GHPullRequest): Color = when {
    pr.draft -> AiModuleTheme.colors.border
    pr.merged -> GitHubMergedPurple
    pr.state == "open" -> GitHubSuccessGreen
    else -> GitHubErrorRed
}

@Composable
private fun pullMergeColor(pr: GHPullRequest): Color = when {
    pr.draft -> AiModuleTheme.colors.textMuted
    pr.mergeable == true && pr.mergeableState in listOf("clean", "has_hooks", "unstable") -> GitHubSuccessGreen
    pr.mergeable == false || pr.mergeableState in listOf("dirty", "blocked") -> GitHubErrorRed
    else -> AiModuleTheme.colors.accent
}

private fun pullMergeText(pr: GHPullRequest): String = when {
    pr.draft -> "Draft pull request cannot be merged yet"
    pr.merged -> "Pull request has been merged"
    pr.state == "closed" -> "Pull request is closed"
    pr.mergeable == true && pr.mergeableState == "clean" -> "Ready to merge"
    pr.mergeable == true && pr.mergeableState == "unstable" -> "Mergeable, but checks need attention"
    pr.mergeableState == "blocked" -> "Blocked by required checks or reviews"
    pr.mergeable == false || pr.mergeableState == "dirty" -> "Cannot merge cleanly"
    else -> "Mergeability is being calculated"
}

@Composable
private fun PullEditDialog(repo: GHRepo, pr: GHPullRequest, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(pr.number) { mutableStateOf(pr.title) }
    var body by remember(pr.number) { mutableStateOf(pr.body) }
    var base by remember(pr.number) { mutableStateOf(pr.base) }
    var state by remember(pr.number) { mutableStateOf(pr.state) }
    var saving by remember { mutableStateOf(false) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Edit PR #${pr.number}",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AiModuleTextField(title, { title = it }, label = "Title", singleLine = true, modifier = Modifier.fillMaxWidth())
                AiModuleTextField(body, { body = it }, label = "Body", minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
                AiModuleTextField(base, { base = it.trim() }, label = "Base branch", singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("open" to "Open", "closed" to "Closed").forEach { (value, label) ->
                        val selected = state == value
                        Box(
                            Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                .clickable { state = value }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(
                label = if (saving) "saving" else "save",
                enabled = !saving && title.isNotBlank() && base.isNotBlank(),
                onClick = {
                    if (saving) return@AiModuleTextAction
                    saving = true
                    scope.launch {
                        val ok = GitHubManager.updatePullRequest(
                            context = context,
                            owner = repo.owner,
                            repo = repo.name,
                            number = pr.number,
                            title = title.trim(),
                            body = body,
                            base = base.trim(),
                            state = state
                        )
                        saving = false
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) onDone()
                    }
                },
            )
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = TextSecondary) }
    )
}

@Composable
private fun PullReviewersDialog(repo: GHRepo, pr: GHPullRequest, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reviewersRaw by remember { mutableStateOf(pr.requestedReviewers.joinToString(",")) }
    var saving by remember { mutableStateOf(false) }
    val reviewers = reviewersRaw.split(",").map { it.trim().removePrefix("@") }.filter { it.isNotBlank() }.distinct()

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Reviewers #${pr.number}",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pr.requestedReviewers.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pr.requestedReviewers.forEach { reviewer -> PullBadge(reviewer, TextSecondary) }
                    }
                } else {
                    Text("No requested reviewers", fontSize = 12.sp, color = TextTertiary)
                }
                AiModuleTextField(
                    value = reviewersRaw,
                    onValueChange = { reviewersRaw = it },
                    label = "Usernames, comma-separated",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Request adds reviewers; remove removes the typed usernames.", fontSize = 11.sp, color = TextTertiary)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiModuleTextAction(
                    label = "remove",
                    enabled = !saving && reviewers.isNotEmpty(),
                    onClick = {
                        saving = true
                        scope.launch {
                            val ok = GitHubManager.removePullRequestReviewers(context, repo.owner, repo.name, pr.number, reviewers)
                            saving = false
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            if (ok) onDone()
                        }
                    },
                    tint = Color(0xFFFF3B30),
                )
                AiModuleTextAction(
                    label = if (saving) "saving" else "request",
                    enabled = !saving && reviewers.isNotEmpty(),
                    onClick = {
                        saving = true
                        scope.launch {
                            val ok = GitHubManager.requestPullRequestReviewers(context, repo.owner, repo.name, pr.number, reviewers)
                            saving = false
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            if (ok) onDone()
                        }
                    },
                )
            }
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = TextSecondary) }
    )
}

@Composable
private fun PullReviewHistoryDialog(
    repo: GHRepo,
    pr: GHPullRequest,
    reviews: List<GHPullReview>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedReview by remember { mutableStateOf<GHPullReview?>(null) }
    var editReview by remember { mutableStateOf<GHPullReview?>(null) }
    var deleteReview by remember { mutableStateOf<GHPullReview?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Review history",
        content = {
            if (reviews.isEmpty()) {
                Text("No reviews yet", fontSize = 13.sp, color = TextTertiary)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reviews) { review ->
                        val canMutate = review.state.equals("PENDING", ignoreCase = true)
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(SurfaceLight)
                                .clickable {
                                    scope.launch {
                                        selectedReview = GitHubManager.getPullRequestReview(context, repo.owner, repo.name, pr.number, review.id) ?: review
                                    }
                                }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PullBadge(review.state.ifBlank { "review" }, reviewStateColor(review.state))
                                Text(review.user.ifBlank { "GitHub" }, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (review.submittedAt.isNotBlank()) Text(review.submittedAt.take(10), fontSize = 10.sp, color = TextTertiary)
                                if (review.htmlUrl.isNotBlank()) {
                                    AiModuleIconButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(review.htmlUrl)))
                                            } catch (_: Exception) {
                                                Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        AiModuleIcon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp), tint = TextSecondary)
                                    }
                                }
                                if (canMutate) {
                                    AiModuleIconButton(onClick = { editReview = review }, modifier = Modifier.size(28.dp)) {
                                        AiModuleIcon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = Blue)
                                    }
                                    AiModuleIconButton(onClick = { deleteReview = review }, modifier = Modifier.size(28.dp)) {
                                        AiModuleIcon(Icons.Rounded.Delete, null, Modifier.size(16.dp), tint = Color(0xFFFF3B30))
                                    }
                                }
                            }
                            if (review.body.isNotBlank()) Text(review.body, fontSize = 11.sp, color = TextSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            if (review.commitId.length >= 7) Text(review.commitId.take(7), fontSize = 10.sp, color = TextTertiary)
                        }
                    }
                }
            }
        },
        confirmButton = { AiModuleTextAction(label = "close", onClick = onDismiss) }
    )

    selectedReview?.let { review ->
        PullReviewDetailDialog(review = review, onDismiss = { selectedReview = null })
    }

    editReview?.let { review ->
        PullReviewEditDialog(
            review = review,
            saving = actionInFlight,
            onDismiss = { if (!actionInFlight) editReview = null },
            onSave = { body ->
                actionInFlight = true
                scope.launch {
                    val updated = GitHubManager.updatePullRequestReview(context, repo.owner, repo.name, pr.number, review.id, body)
                    actionInFlight = false
                    Toast.makeText(context, if (updated != null) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (updated != null) {
                        editReview = null
                        selectedReview = updated
                        onChanged()
                    }
                }
            }
        )
    }

    deleteReview?.let { review ->
        AiModuleAlertDialog(
            onDismissRequest = { if (!actionInFlight) deleteReview = null },
            title = "Delete pending review?",
            content = { Text("Delete review #${review.id}?", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                AiModuleTextAction(
                    label = "delete",
                    enabled = !actionInFlight,
                    onClick = {
                        actionInFlight = true
                        scope.launch {
                            val ok = GitHubManager.deletePullRequestReview(context, repo.owner, repo.name, pr.number, review.id)
                            actionInFlight = false
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            if (ok) {
                                deleteReview = null
                                selectedReview = null
                                onChanged()
                            }
                        }
                    },
                    tint = Color(0xFFFF3B30),
                )
            },
            dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), enabled = !actionInFlight, onClick = { deleteReview = null }, tint = TextSecondary) }
        )
    }
}

@Composable
private fun PullReviewDetailDialog(review: GHPullReview, onDismiss: () -> Unit) {
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Review #${review.id}",
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PullBadge(review.state.ifBlank { "review" }, reviewStateColor(review.state))
                    if (review.commitId.length >= 7) PullBadge(review.commitId.take(7), TextSecondary)
                }
                PullReviewDetailLine("Reviewer", review.user.ifBlank { "GitHub" })
                PullReviewDetailLine("Submitted", review.submittedAt.take(19).replace('T', ' '))
                PullReviewDetailLine("Commit", review.commitId)
                if (review.body.isNotBlank()) {
                    Text(review.body, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                }
            }
        },
        confirmButton = { AiModuleTextAction(label = "close", onClick = onDismiss) }
    )
}

@Composable
private fun PullReviewEditDialog(
    review: GHPullReview,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var body by remember(review.id) { mutableStateOf(review.body) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Edit pending review",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PullBadge(review.state.ifBlank { "review" }, reviewStateColor(review.state))
                AiModuleTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = "Review body",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            AiModuleTextAction(label = if (saving) "saving" else "save", enabled = !saving, onClick = { onSave(body) })
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), enabled = !saving, onClick = onDismiss, tint = TextSecondary) }
    )
}

@Composable
private fun PullReviewDetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = TextPrimary)
    }
}

@Composable
private fun PullMergeDialog(
    pr: GHPullRequest,
    merging: Boolean,
    onDismiss: () -> Unit,
    onMerge: (method: String, title: String, message: String) -> Unit
) {
    var method by remember { mutableStateOf("merge") }
    var title by remember { mutableStateOf("${pr.title} (#${pr.number})") }
    var message by remember { mutableStateOf("") }

    AiModuleAlertDialog(
        onDismissRequest = { if (!merging) onDismiss() },
        title = "Merge PR #${pr.number}",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("merge" to "Merge", "squash" to "Squash", "rebase" to "Rebase").forEach { (value, label) ->
                        val selected = method == value
                        Box(
                            Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                .clickable { method = value }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                        }
                    }
                }
                AiModuleTextField(title, { title = it }, label = "Commit title", singleLine = true, modifier = Modifier.fillMaxWidth())
                AiModuleTextField(message, { message = it }, label = "Commit message", minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                Text("${pr.head} -> ${pr.base}", fontSize = 11.sp, color = TextTertiary)
            }
        },
        confirmButton = {
            AiModuleTextAction(label = if (merging) "merging" else Strings.ghMerge.lowercase(), enabled = !merging && title.isNotBlank(), onClick = { onMerge(method, title, message) }, tint = Color(0xFF34C759))
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, enabled = !merging, tint = TextSecondary) }
    )
}

private fun reviewStateColor(state: String): Color = when (state.lowercase()) {
    "approved" -> Color(0xFF34C759)
    "changes_requested" -> Color(0xFFFF3B30)
    "commented" -> Blue
    "dismissed" -> TextTertiary
    else -> TextSecondary
}

@Composable
private fun PullReviewDialog(repo: GHRepo, pr: GHPullRequest, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var body by remember { mutableStateOf("") }
    var event by remember { mutableStateOf("COMMENT") }
    var sending by remember { mutableStateOf(false) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Review #${pr.number}",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("COMMENT" to "Comment", "APPROVE" to "Approve", "REQUEST_CHANGES" to "Request changes").forEach { (value, label) ->
                        val selected = event == value
                        Box(
                            Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                .clickable { event = value }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                        }
                    }
                }
                AiModuleTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = "Review message",
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            AiModuleTextAction(label = if (sending) "sending" else "submit", onClick = {
                if (sending) return@AiModuleTextAction
                sending = true
                scope.launch {
                    val ok = GitHubManager.submitPullRequestReview(context, repo.owner, repo.name, pr.number, event, body)
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    sending = false
                    if (ok) onDone()
                }
            })
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = TextSecondary) }
    )
}

@Composable
private fun PullFilesDialog(repo: GHRepo, pr: GHPullRequest, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<GHPullFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(pr.number) {
        loading = true
        files = GitHubManager.getPullRequestFiles(context, repo.owner, repo.name, pr.number)
        loading = false
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Files #${pr.number}",
        content = {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files) { file ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceLight).padding(10.dp)
                        ) {
                            Text(file.filename, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("${file.status}  +${file.additions}  -${file.deletions}", fontSize = 10.sp, color = TextSecondary)
                            if (file.patch.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().background(Color(0xFF1E1E22), RoundedCornerShape(GitHubControlRadius)).padding(8.dp)) {
                                    Text(file.patch.take(1200), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFD4D4D4))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { AiModuleTextAction(label = "close", onClick = onDismiss) }
    )
}
