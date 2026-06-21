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
 * Issue-экраны репо-модуля: IssueEventsScreen (лента событий), IssueDetailScreen + карточки,
 * markdown, все Issue*-диалоги (lock/edit/meta/reactions/timeline).
 * Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun IssueEventsScreen(
    repo: GHRepo,
    onBack: () -> Unit,
    onOpenIssue: (Int, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var events by remember(repo.fullName) { mutableStateOf<List<GHIssueEvent>>(emptyList()) }
    var page by remember(repo.fullName) { mutableIntStateOf(1) }
    var hasMore by remember(repo.fullName) { mutableStateOf(true) }
    var loading by remember(repo.fullName) { mutableStateOf(true) }
    var query by rememberSaveable(repo.fullName, "issue-events-query") { mutableStateOf("") }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var detailEvent by remember { mutableStateOf<GHIssueEvent?>(null) }

    suspend fun loadEvents(reset: Boolean) {
        loading = true
        val nextPage = if (reset) 1 else page + 1
        val result = GitHubManager.getIssueEvents(context, repo.owner, repo.name, page = nextPage)
        events = if (reset) result else events + result
        page = nextPage
        hasMore = result.size >= 100
        loading = false
    }

    LaunchedEffect(repo.fullName, reloadNonce) {
        loadEvents(reset = true)
    }

    val filteredEvents = remember(events, query) {
        val q = query.trim()
        if (q.isBlank()) events else events.filter { event ->
            event.event.contains(q, ignoreCase = true) ||
                event.actor.contains(q, ignoreCase = true) ||
                event.issueTitle.contains(q, ignoreCase = true) ||
                event.issueNumber.toString().contains(q) ||
                event.label.contains(q, ignoreCase = true) ||
                event.assignee.contains(q, ignoreCase = true) ||
                event.milestone.contains(q, ignoreCase = true)
        }
    }

    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(
                title = "> issue events",
                subtitle = repo.fullName,
                onBack = onBack,
                trailing = {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.REFRESH,
                        onClick = { reloadNonce++ },
                        tint = palette.accent,
                        enabled = !loading,
                        contentDescription = "refresh issue events",
                    )
                },
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GitHubTerminalTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "filter events",
                    singleLine = true,
                    minHeight = 38.dp,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "events=${events.size}",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                    if (query.isNotBlank()) {
                        Text(
                            "filtered=${filteredEvents.size}",
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                        GitHubTerminalButton("clear", onClick = { query = "" }, color = palette.textSecondary)
                    }
                }
            }

            when {
                loading && events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
                filteredEvents.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        if (query.isBlank()) "// no issue events" else "// no matching issue events",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                    )
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredEvents) { event ->
                        IssueEventRow(
                            event = event,
                            onOpenIssue = onOpenIssue,
                            onDetails = {
                                detailEvent = event
                                scope.launch {
                                    detailEvent = GitHubManager.getIssueEvent(context, repo.owner, repo.name, event.id) ?: event
                                }
                            },
                        )
                    }
                    if (hasMore && query.isBlank()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                GitHubTerminalButton(
                                    label = if (loading) "loading…" else "load more",
                                    onClick = { scope.launch { loadEvents(reset = false) } },
                                    color = if (loading) palette.textMuted else palette.accent,
                                    enabled = !loading,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailEvent?.let { event ->
        IssueEventDetailDialog(
            event = event,
            onDismiss = { detailEvent = null },
            onOpenIssue = {
                detailEvent = null
                onOpenIssue(event.issueNumber, event.issueTitle)
            },
        )
    }
}

@Composable
private fun IssueEventRow(
    event: GHIssueEvent,
    onOpenIssue: (Int, String) -> Unit,
    onDetails: (() -> Unit)? = null,
    showOpenIssue: Boolean = true,
) {
    val palette = AiModuleTheme.colors
    val color = issueEventColor(event.event, palette)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .let {
                if (event.issueNumber > 0) it.clickable { onOpenIssue(event.issueNumber, event.issueTitle) } else it
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                event.event.ifBlank { "event" },
                color = color,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.widthIn(min = 82.dp, max = 132.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "#${event.issueNumber} ${event.issueTitle.ifBlank { "untitled issue" }}",
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${event.actor.ifBlank { "github" }} · ${event.createdAt.take(19).replace("T", " ")}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (event.issueNumber > 0) {
                AiModuleGlyph(GhGlyphs.ARROW_RIGHT, Modifier.size(16.dp), tint = palette.textMuted, fontSize = 13.sp)
            }
        }
        val chips = buildList {
            if (event.label.isNotBlank()) add("label:${event.label}" to color)
            if (event.assignee.isNotBlank()) add("assignee:${event.assignee}" to palette.accent)
            if (event.milestone.isNotBlank()) add("milestone:${event.milestone}" to GitHubSuccessGreen)
            if (event.renameFrom.isNotBlank() || event.renameTo.isNotBlank()) {
                add("rename:${event.renameFrom}->${event.renameTo}" to palette.warning)
            }
            if (event.commitId.isNotBlank()) add("commit:${event.commitId.take(7)}" to palette.textSecondary)
        }
        if (chips.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { (label, chipColor) ->
                    IssueEventChip(label = label, color = chipColor)
                }
            }
        }
        if (onDetails != null || (showOpenIssue && event.issueNumber > 0)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDetails?.let { GitHubTerminalButton("details", onClick = it, color = palette.textSecondary) }
                if (showOpenIssue && event.issueNumber > 0) {
                    GitHubTerminalButton("open issue", onClick = { onOpenIssue(event.issueNumber, event.issueTitle) }, color = palette.accent)
                }
            }
        }
    }
}

@Composable
private fun IssueEventDetailDialog(event: GHIssueEvent, onDismiss: () -> Unit, onOpenIssue: () -> Unit) {
    val palette = AiModuleTheme.colors
    GitHubTerminalModal(title = "▸ issue event ${event.id}", onDismiss = onDismiss) {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GitDataKv("event", event.event)
            GitDataKv("issue", if (event.issueNumber > 0) "#${event.issueNumber} ${event.issueTitle}" else event.issueTitle)
            GitDataKv("actor", event.actor.ifBlank { "github" })
            GitDataKv("created", event.createdAt)
            GitDataKv("label", event.label)
            GitDataKv("assignee", event.assignee)
            GitDataKv("milestone", event.milestone)
            if (event.renameFrom.isNotBlank() || event.renameTo.isNotBlank()) {
                GitDataKv("rename", "${event.renameFrom} -> ${event.renameTo}")
            }
            GitDataKv("commit", event.commitId)
            GitDataKv("association", event.authorAssociation)
            GitDataKv("state reason", event.stateReason)
            GitDataKv("github app", event.performedViaGithubApp)
            GitDataKv("api", event.url)
            GitDataKv("commit api", event.commitUrl)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (event.issueNumber > 0) GitHubTerminalButton("open issue", onClick = onOpenIssue, color = palette.accent)
            GitHubTerminalButton("close", onClick = onDismiss, color = palette.textSecondary)
        }
    }
}

@Composable
private fun IssueEventChip(label: String, color: Color) {
    Text(
        text = label,
        color = color,
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

private fun issueEventColor(event: String, palette: AiModuleColors): Color =
    when (event) {
        "closed", "locked", "unassigned", "unlabeled", "demilestoned" -> GitHubErrorRed
        "reopened", "unlocked", "assigned", "labeled", "milestoned" -> GitHubSuccessGreen
        "renamed", "transferred", "converted_note_to_issue" -> palette.warning
        "referenced", "cross-referenced", "connected", "disconnected" -> palette.accent
        else -> palette.textSecondary
    }

@Composable
internal fun IssueDetailScreen(repo: GHRepo, issueNumber: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<GHIssueDetail?>(null) }
    var comments by remember { mutableStateOf<List<GHComment>>(emptyList()) }
    var issueReactions by remember { mutableStateOf<List<GHReaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newComment by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var previewComment by remember { mutableStateOf(false) }
    var showMetaDialog by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var commentReactionTarget by remember { mutableStateOf<GHComment?>(null) }
    var editingComment by remember { mutableStateOf<GHComment?>(null) }
    var deleteCommentTarget by remember { mutableStateOf<GHComment?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }

    suspend fun refreshIssueDetail(showLoader: Boolean = false) {
        if (showLoader) loading = true
        detail = GitHubManager.getIssueDetail(context, repo.owner, repo.name, issueNumber)
        comments = GitHubManager.getIssueComments(context, repo.owner, repo.name, issueNumber)
        issueReactions = GitHubManager.getIssueReactions(context, repo.owner, repo.name, issueNumber)
        loading = false
    }

    LaunchedEffect(issueNumber) {
        refreshIssueDetail(showLoader = true)
    }

    fun handleIssueBack() {
        when {
            showMetaDialog -> showMetaDialog = false
            showReactions -> showReactions = false
            showTimeline -> showTimeline = false
            commentReactionTarget != null -> commentReactionTarget = null
            editingComment != null -> editingComment = null
            deleteCommentTarget != null -> deleteCommentTarget = null
            showLockDialog -> showLockDialog = false
            else -> onBack()
        }
    }

    AiModuleSurface {
    val issuePalette = AiModuleTheme.colors
    Column(Modifier.fillMaxSize().background(issuePalette.background)) {
        GitHubPageBar(
            title = "> issue #$issueNumber",
            subtitle = detail?.title,
            onBack = ::handleIssueBack,
            trailing = {
                if (detail != null) {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.REACT,
                        onClick = { showReactions = true },
                        tint = issuePalette.accent,
                        fontSize = 12.sp,
                        contentDescription = "reactions",
                    )
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.TIMELINE,
                        onClick = { showTimeline = true },
                        tint = issuePalette.accent,
                        contentDescription = "timeline",
                    )
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.TUNE,
                        onClick = { showMetaDialog = true },
                        tint = issuePalette.accent,
                        contentDescription = "metadata",
                    )
                    if (repo.canWrite()) {
                        AiModuleGlyphAction(
                            glyph = if (detail!!.locked) GhGlyphs.UNLOCK else GhGlyphs.LOCK,
                            onClick = { showLockDialog = true },
                            tint = if (detail!!.locked) GitHubSuccessGreen else issuePalette.warning,
                            contentDescription = if (detail!!.locked) "unlock" else "lock",
                        )
                        val isOpen = detail!!.state == "open"
                        AiModuleGlyphAction(
                            glyph = if (isOpen) GhGlyphs.CLOSE else GhGlyphs.REFRESH,
                            onClick = {
                                scope.launch {
                                    val ok = if (isOpen) {
                                        GitHubManager.closeIssue(context, repo.owner, repo.name, issueNumber)
                                    } else {
                                        GitHubManager.reopenIssue(context, repo.owner, repo.name, issueNumber)
                                    }
                                    if (ok) refreshIssueDetail()
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                }
                            },
                            tint = if (isOpen) GitHubErrorRed else GitHubSuccessGreen,
                            contentDescription = if (isOpen) "close issue" else "reopen issue",
                        )
                    }
                }
            },
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading…")
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                detail?.let { issue ->
                    item {
                        IssueHeaderCard(
                            detail = issue,
                            reactions = issueReactions,
                            onMeta = { showMetaDialog = true },
                            onReactions = { showReactions = true },
                            onTimeline = { showTimeline = true }
                        )
                    }
                    item {
                        IssueMetaCard(issue) { showMetaDialog = true }
                    }
                    item {
                        IssueBodyCard(issue.body)
                    }
                    item {
                        Text("${comments.size} comments", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
                if (comments.isEmpty()) item {
                    Box(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(16.dp)) {
                        Text(Strings.ghNoComments, fontSize = 13.sp, color = TextTertiary)
                    }
                }
                items(comments) { c ->
                    IssueCommentCard(
                        comment = c,
                        onReactions = { commentReactionTarget = c },
                        onEdit = { editingComment = c },
                        onDelete = { deleteCommentTarget = c }
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("> comment", color = issuePalette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    GitHubTerminalTab("write", selected = !previewComment) { previewComment = false }
                    GitHubTerminalTab("preview", selected = previewComment) { previewComment = true }
                    Spacer(Modifier.weight(1f))
                    if (sending) AiModuleSpinner() else GitHubTerminalButton("send ↵", enabled = newComment.isNotBlank(), color = if (newComment.isBlank()) issuePalette.textMuted else issuePalette.accent, onClick = {
                        if (newComment.isBlank() || sending) return@GitHubTerminalButton
                        sending = true
                        scope.launch {
                            val ok = GitHubManager.addComment(context, repo.owner, repo.name, issueNumber, newComment)
                            if (ok) {
                                comments = GitHubManager.getIssueComments(context, repo.owner, repo.name, issueNumber)
                                newComment = ""
                                previewComment = false
                            }
                            sending = false
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                if (previewComment) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 72.dp, max = 180.dp).clip(RoundedCornerShape(GitHubControlRadius)).background(issuePalette.surface).border(1.dp, issuePalette.border, RoundedCornerShape(GitHubControlRadius)).padding(12.dp).verticalScroll(rememberScrollState())) {
                        if (newComment.isBlank()) Text(Strings.ghAddComment, color = issuePalette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 18.sp)
                        else IssueMarkdownBlock(newComment)
                    }
                } else {
                    GitHubTerminalTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        placeholder = Strings.ghAddComment,
                        minHeight = 72.dp,
                        maxLines = 8,
                    )
                }
            }
        }
    }
    }

    if (showMetaDialog && detail != null) {
        IssueMetaDialog(
            repo = repo,
            detail = detail!!,
            onDismiss = { showMetaDialog = false },
            onDone = {
                showMetaDialog = false
                scope.launch { refreshIssueDetail() }
            }
        )
    }

    if (showReactions) {
        IssueReactionsDialog(
            repo = repo,
            issueNumber = issueNumber,
            onDismiss = { showReactions = false },
            onChanged = { scope.launch { issueReactions = GitHubManager.getIssueReactions(context, repo.owner, repo.name, issueNumber) } }
        )
    }

    commentReactionTarget?.let { comment ->
        IssueCommentReactionsDialog(
            repo = repo,
            comment = comment,
            onDismiss = { commentReactionTarget = null }
        )
    }

    if (showTimeline) {
        IssueTimelineDialog(
            repo = repo,
            issueNumber = issueNumber,
            onDismiss = { showTimeline = false }
        )
    }

    if (showLockDialog && detail != null) {
        IssueLockDialog(
            repo = repo,
            detail = detail!!,
            onDismiss = { showLockDialog = false },
            onDone = {
                showLockDialog = false
                scope.launch { refreshIssueDetail() }
            }
        )
    }

    editingComment?.let { comment ->
        IssueCommentEditDialog(
            repo = repo,
            comment = comment,
            onDismiss = { editingComment = null },
            onDone = {
                editingComment = null
                scope.launch { comments = GitHubManager.getIssueComments(context, repo.owner, repo.name, issueNumber) }
            }
        )
    }

    deleteCommentTarget?.let { comment ->
        GitHubTerminalModal(title = "▸ delete comment", onDismiss = { deleteCommentTarget = null }) {
            Text(comment.body.lineSequence().firstOrNull().orEmpty().take(160), color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 13.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalButton("× delete", color = AiModuleTheme.colors.error, onClick = {
                    scope.launch {
                        val ok = GitHubManager.deleteIssueComment(context, repo.owner, repo.name, comment.id)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) comments = GitHubManager.getIssueComments(context, repo.owner, repo.name, issueNumber)
                        deleteCommentTarget = null
                    }
                })
                GitHubTerminalButton("no · cancel", color = AiModuleTheme.colors.textSecondary, onClick = { deleteCommentTarget = null })
            }
        }
    }
}


@Composable
private fun IssueHeaderCard(
    detail: GHIssueDetail,
    reactions: List<GHReaction>,
    onMeta: () -> Unit,
    onReactions: () -> Unit,
    onTimeline: () -> Unit
) {
    Column(Modifier.fillMaxWidth().ghGlassCard(16.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(detail.avatarUrl, null, Modifier.size(34.dp).clip(CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(detail.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 20.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PullBadge(if (detail.state == "open") Strings.ghOpen else Strings.ghClosed, if (detail.state == "open") Color(0xFF34C759) else Color(0xFF8E8E93))
                    Text(detail.author, fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Medium)
                    Text(detail.createdAt.take(10), fontSize = 11.sp, color = TextTertiary)
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubTerminalButton("meta", onClick = onMeta, color = Blue)
            GitHubTerminalButton("reactions ${reactions.size}", onClick = onReactions, color = Blue)
            GitHubTerminalButton("timeline", onClick = onTimeline, color = Blue)
            if (detail.locked) PullBadge("Locked ${detail.activeLockReason}".trim(), Color(0xFFFF9500))
        }
        if (reactions.isNotEmpty()) {
            IssueReactionSummary(reactions)
        }
    }
}

@Composable
private fun IssueMetaCard(detail: GHIssueDetail, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Metadata", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            GitHubTerminalButton("edit", onClick = onEdit, color = Blue)
        }
        if (detail.labels.isEmpty() && detail.assignee.isBlank() && detail.milestoneTitle.isBlank()) {
            Text("No labels, assignee, or milestone", fontSize = 12.sp, color = TextTertiary)
            return@Column
        }
        if (detail.labels.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                detail.labels.forEach { label ->
                    Text(label, fontSize = 11.sp, color = Blue, modifier = Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(Blue.copy(0.1f)).padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (detail.assignee.isNotBlank()) PullBadge("Assignee: ${detail.assignee}", TextSecondary)
            if (detail.milestoneTitle.isNotBlank()) PullBadge("Milestone: ${detail.milestoneTitle}", TextSecondary)
        }
    }
}

@Composable
private fun IssueBodyCard(body: String) {
    Column(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (body.isBlank()) {
            Text("No description provided.", fontSize = 13.sp, color = TextTertiary)
        } else {
            IssueMarkdownBlock(body)
        }
    }
}

@Composable
private fun IssueCommentCard(comment: GHComment, onReactions: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        AsyncImage(comment.avatarUrl, null, Modifier.size(28.dp).clip(CircleShape))
        Column(Modifier.weight(1f).ghGlassCard(14.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Blue)
                Text(comment.createdAt.take(10), fontSize = 10.sp, color = TextTertiary)
                Spacer(Modifier.weight(1f))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GitHubTerminalButton("react", onClick = onReactions, color = TextSecondary)
                    GitHubTerminalButton("edit", onClick = onEdit, color = TextSecondary)
                    GitHubTerminalButton("× delete", onClick = onDelete, color = GitHubErrorRed)
                }
            }
            IssueMarkdownBlock(comment.body)
        }
    }
}

@Composable
private fun IssueReactionSummary(reactions: List<GHReaction>) {
    val labelMap = issueEmojiMap()
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        reactions.groupBy { it.content }.forEach { (content, items) ->
            Row(
                Modifier.border(1.dp, AiModuleTheme.colors.border).background(AiModuleTheme.colors.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(labelMap[content] ?: content, fontSize = 11.sp, fontFamily = JetBrainsMono, color = TextSecondary)
                Text("${items.size}", fontSize = 11.sp, color = TextSecondary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun IssueMarkdownBlock(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        text.lines().forEach { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> Text(trimmed.removePrefix("### "), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
                trimmed.startsWith("## ") -> Text(trimmed.removePrefix("## "), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 5.dp))
                trimmed.startsWith("# ") -> Text(trimmed.removePrefix("# "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 6.dp))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row {
                    Text("• ", fontSize = 13.sp, color = TextSecondary)
                    Text(trimmed.drop(2), fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                }
                trimmed.startsWith("> ") -> Row(Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.width(3.dp).height(18.dp).background(SeparatorColor))
                    Text(trimmed.drop(2), fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(start = 8.dp))
                }
                trimmed.startsWith("```") -> Box(Modifier.fillMaxWidth().height(1.dp).background(SeparatorColor))
                trimmed.isBlank() -> Spacer(Modifier.height(5.dp))
                else -> Text(trimmed, fontSize = 13.sp, color = TextPrimary, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun IssueLockDialog(repo: GHRepo, detail: GHIssueDetail, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reason by remember(detail.number) { mutableStateOf(detail.activeLockReason.ifBlank { "resolved" }) }
    var saving by remember { mutableStateOf(false) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (detail.locked) "Unlock issue" else "Lock issue",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AiModuleText(detail.title, fontSize = 13.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!detail.locked) {
                    AiModuleText("Reason", fontSize = 12.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("resolved", "off-topic", "too heated", "spam").forEach { value ->
                            val selected = reason == value
                            Box(
                                Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                    .clickable { reason = value }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                AiModuleText(value, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                            }
                        }
                    }
                } else if (detail.activeLockReason.isNotBlank()) {
                    PullBadge("Reason: ${detail.activeLockReason}", Color(0xFFFF9500))
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(
                label = if (saving) "saving" else if (detail.locked) "unlock" else "lock",
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        val ok = if (detail.locked) {
                            GitHubManager.unlockIssue(context, repo.owner, repo.name, detail.number)
                        } else {
                            GitHubManager.lockIssue(context, repo.owner, repo.name, detail.number, reason)
                        }
                        saving = false
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) onDone()
                    }
                },
                tint = if (detail.locked) Color(0xFF34C759) else Color(0xFFFF9500),
            )
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = TextSecondary) }
    )
}

@Composable
private fun IssueCommentEditDialog(repo: GHRepo, comment: GHComment, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var body by remember(comment.id) { mutableStateOf(comment.body) }
    var saving by remember { mutableStateOf(false) }

    GitHubTerminalModal(title = "▸ edit comment", onDismiss = onDismiss) {
        GitHubTerminalTextField(
            value = body,
            onValueChange = { body = it },
            placeholder = "comment",
            minHeight = 140.dp,
            maxLines = 10,
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubTerminalButton(if (saving) "⠋ saving" else "save", enabled = !saving && body.isNotBlank(), color = AiModuleTheme.colors.accent, onClick = {
                saving = true
                scope.launch {
                    val ok = GitHubManager.updateIssueComment(context, repo.owner, repo.name, comment.id, body)
                    saving = false
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) onDone()
                }
            })
            GitHubTerminalButton("cancel", onClick = onDismiss, color = AiModuleTheme.colors.textSecondary)
        }
    }
}

private fun issueEmojiMap(): Map<String, String> = mapOf(
    "+1" to "+1",
    "-1" to "-1",
    "laugh" to "smile",
    "confused" to "confused",
    "heart" to "heart",
    "hooray" to "hooray",
    "eyes" to "eyes",
    "rocket" to "rocket"
)

@Composable
private fun IssueMetaDialog(repo: GHRepo, detail: GHIssueDetail, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var labels by remember { mutableStateOf<List<GHLabel>>(emptyList()) }
    var milestones by remember { mutableStateOf<List<GHMilestone>>(emptyList()) }
    var assignees by remember { mutableStateOf<List<GHUserLite>>(emptyList()) }
    val selectedLabels = remember(detail.number) { mutableStateListOf<String>().apply { addAll(detail.labels) } }
    var selectedAssignee by remember(detail.number) { mutableStateOf(detail.assignee) }
    var selectedMilestone by remember(detail.number) { mutableStateOf(detail.milestoneTitle) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(detail.number) {
        loading = true
        labels = GitHubManager.getLabels(context, repo.owner, repo.name)
        milestones = GitHubManager.getMilestones(context, repo.owner, repo.name)
        assignees = GitHubManager.getAssignees(context, repo.owner, repo.name)
        loading = false
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Issue metadata",
        content = {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Labels", fontSize = 12.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        labels.forEach { label ->
                            val selected = label.name in selectedLabels
                            Box(
                                Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                    .clickable {
                                        if (selected) selectedLabels.remove(label.name) else selectedLabels.add(label.name)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label.name, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                            }
                        }
                    }

                    Text("Assignee", fontSize = 12.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(if (selectedAssignee.isBlank()) Blue.copy(0.15f) else SurfaceLight).clickable { selectedAssignee = "" }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("None", fontSize = 12.sp, color = if (selectedAssignee.isBlank()) Blue else TextSecondary)
                        }
                        assignees.forEach { user ->
                            val selected = selectedAssignee == user.login
                            Box(
                                Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                    .clickable { selectedAssignee = user.login }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(user.login, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                            }
                        }
                    }

                    Text("Milestone", fontSize = 12.sp, color = TextSecondary)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(if (selectedMilestone.isBlank()) Blue.copy(0.15f) else SurfaceLight).clickable { selectedMilestone = "" }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("None", fontSize = 12.sp, color = if (selectedMilestone.isBlank()) Blue else TextSecondary)
                        }
                        milestones.forEach { milestone ->
                            val selected = selectedMilestone == milestone.title
                            Box(
                                Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
                                    .clickable { selectedMilestone = milestone.title }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(milestone.title, fontSize = 12.sp, color = if (selected) Blue else TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(
                label = if (saving) "saving" else "save",
                onClick = {
                    if (saving) return@AiModuleTextAction
                    saving = true
                    scope.launch {
                        val milestoneNumber = milestones.firstOrNull { it.title == selectedMilestone }?.number
                        val ok = GitHubManager.updateIssueMeta(
                            context = context,
                            owner = repo.owner,
                            repo = repo.name,
                            issueNumber = detail.number,
                            labels = selectedLabels.toList(),
                            assignees = if (selectedAssignee.isBlank()) emptyList() else listOf(selectedAssignee),
                            milestoneNumber = milestoneNumber,
                            clearMilestone = selectedMilestone.isBlank()
                        )
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        saving = false
                        if (ok) onDone()
                    }
                },
                enabled = !loading && !saving
            )
        },
        dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = TextSecondary) }
    )
}

// ═══════════════════════════════════
// Issue Reactions Dialog
// ═══════════════════════════════════

@Composable
private fun IssueReactionsDialog(repo: GHRepo, issueNumber: Int, onDismiss: () -> Unit, onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reactions by remember { mutableStateOf<List<GHReaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(issueNumber) {
        reactions = GitHubManager.getIssueReactions(context, repo.owner, repo.name, issueNumber)
        loading = false
    }

    val labelMap = issueEmojiMap()

    GitHubTerminalModal(title = "$ reactions", onDismiss = onDismiss) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("+1", "-1", "laugh", "confused", "heart", "hooray").forEach { key ->
                GitHubTerminalButton(labelMap[key] ?: key, color = AiModuleTheme.colors.accent, onClick = {
                    scope.launch {
                        val ok = GitHubManager.addIssueReaction(context, repo.owner, repo.name, issueNumber, key)
                        reactions = GitHubManager.getIssueReactions(context, repo.owner, repo.name, issueNumber)
                        if (ok) onChanged()
                    }
                })
            }
        }

        when {
            loading -> Text("⠋ loading reactions...", fontSize = 13.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted)
            reactions.isEmpty() -> Text("// no reactions yet", fontSize = 13.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reactions.groupBy { it.content }.forEach { (content, items) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(labelMap[content] ?: content, fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textPrimary)
                        Text("${items.size}", fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textSecondary)
                        Text(items.joinToString(", ") { it.user }, fontSize = 11.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        GitHubTerminalButton("× close", onClick = onDismiss, color = AiModuleTheme.colors.textSecondary)
    }
}

@Composable
private fun IssueCommentReactionsDialog(repo: GHRepo, comment: GHComment, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reactions by remember { mutableStateOf<List<GHReaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val labelMap = issueEmojiMap()

    LaunchedEffect(comment.id) {
        reactions = GitHubManager.getIssueCommentReactions(context, repo.owner, repo.name, comment.id)
        loading = false
    }

    GitHubTerminalModal(title = "▸ reactions", onDismiss = onDismiss) {
        Text(comment.body.lineSequence().firstOrNull().orEmpty().take(120), fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textSecondary, maxLines = 2)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("+1", "-1", "laugh", "confused", "heart", "hooray").forEach { key ->
                GitHubTerminalButton(labelMap[key] ?: key, color = AiModuleTheme.colors.accent, onClick = {
                    scope.launch {
                        GitHubManager.addIssueCommentReaction(context, repo.owner, repo.name, comment.id, key)
                        reactions = GitHubManager.getIssueCommentReactions(context, repo.owner, repo.name, comment.id)
                    }
                })
            }
        }
        when {
            loading -> Text("⠋ loading reactions...", fontSize = 13.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted)
            reactions.isEmpty() -> Text("// no reactions yet", fontSize = 13.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reactions.groupBy { it.content }.forEach { (content, items) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(labelMap[content] ?: content, fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textPrimary)
                        Text("${items.size}", fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textSecondary)
                        Text(items.joinToString(", ") { it.user }, fontSize = 11.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        GitHubTerminalButton("× close", onClick = onDismiss, color = AiModuleTheme.colors.textSecondary)
    }
}

// ═══════════════════════════════════
// Issue Timeline Dialog
// ═══════════════════════════════════

@Composable
private fun IssueTimelineDialog(repo: GHRepo, issueNumber: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(IssueActivityTab.TIMELINE) }
    var events by remember { mutableStateOf<List<GHTimelineEvent>>(emptyList()) }
    var issueEvents by remember { mutableStateOf<List<GHIssueEvent>>(emptyList()) }
    var detailEvent by remember { mutableStateOf<GHIssueEvent?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(issueNumber) {
        events = GitHubManager.getIssueTimeline(context, repo.owner, repo.name, issueNumber)
        issueEvents = GitHubManager.getIssueEventsForIssue(context, repo.owner, repo.name, issueNumber)
        loading = false
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "Timeline",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IssueActivityTab.entries.forEach { tab ->
                        GitHubTerminalTab(tab.name.lowercase(), selectedTab == tab) { selectedTab = tab }
                    }
                }
            if (loading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
            } else when (selectedTab) {
                IssueActivityTab.TIMELINE -> {
                    if (events.isEmpty()) {
                        Text("No timeline events", fontSize = 13.sp, color = TextTertiary)
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(events) { event ->
                                val eventText = when (event.event) {
                                    "labeled" -> "added label \"${event.label}\""
                                    "unlabeled" -> "removed label \"${event.label}\""
                                    "milestoned" -> "added to milestone \"${event.milestone}\""
                                    "demilestoned" -> "removed from milestone \"${event.milestone}\""
                                    "assigned" -> "assigned to ${event.assignee}"
                                    "unassigned" -> "unassigned ${event.assignee}"
                                    "closed" -> "closed this"
                                    "reopened" -> "reopened this"
                                    "cross-referenced" -> "referenced this"
                                    "commented" -> "commented"
                                    "committed" -> "committed"
                                    "reviewed" -> "reviewed"
                                    else -> event.event
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Blue))
                                    Column {
                                        Text("${event.actor} $eventText", fontSize = 12.sp, color = TextPrimary)
                                        Text(event.createdAt.take(10), fontSize = 10.sp, color = TextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
                IssueActivityTab.EVENTS -> {
                    if (issueEvents.isEmpty()) {
                        Text("No issue events", fontSize = 13.sp, color = TextTertiary)
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(issueEvents) { event ->
                                IssueEventRow(
                                    event = event,
                                    onOpenIssue = { _, _ -> },
                                    showOpenIssue = false,
                                    onDetails = {
                                        detailEvent = event
                                        scope.launch {
                                            detailEvent = GitHubManager.getIssueEvent(context, repo.owner, repo.name, event.id) ?: event
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            }
        },
        confirmButton = { AiModuleTextAction(label = "close", onClick = onDismiss) }
    )

    detailEvent?.let { event ->
        IssueEventDetailDialog(
            event = event,
            onDismiss = { detailEvent = null },
            onOpenIssue = { detailEvent = null },
        )
    }
}
