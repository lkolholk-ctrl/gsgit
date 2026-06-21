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
 * Под-экраны репо-модуля: RepoInsightsScreen (трафик/люди/события) и GitDataToolsScreen
 * (низкоуровневый Git Data API: refs/trees/blobs/tags/commits) + их карточки.
 * Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun RepoInsightsScreen(repo: GHRepo, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var selectedTab by remember { mutableStateOf(RepoInsightsTab.TRAFFIC) }
    var loading by remember { mutableStateOf(true) }
    var trafficViews by remember { mutableStateOf<GHTrafficSeries?>(null) }
    var trafficClones by remember { mutableStateOf<GHTrafficSeries?>(null) }
    var referrers by remember { mutableStateOf<List<GHTrafficReferrer>>(emptyList()) }
    var paths by remember { mutableStateOf<List<GHTrafficPath>>(emptyList()) }
    var stargazers by remember { mutableStateOf<List<GHRepoPerson>>(emptyList()) }
    var watchers by remember { mutableStateOf<List<GHRepoPerson>>(emptyList()) }
    var events by remember { mutableStateOf<List<GHRepoEvent>>(emptyList()) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    suspend fun loadInsights() {
        loading = true
        trafficViews = GitHubManager.getRepoTrafficViews(context, repo.owner, repo.name)
        trafficClones = GitHubManager.getRepoTrafficClones(context, repo.owner, repo.name)
        referrers = GitHubManager.getRepoTrafficReferrers(context, repo.owner, repo.name)
        paths = GitHubManager.getRepoTrafficPaths(context, repo.owner, repo.name)
        stargazers = GitHubManager.getRepoStargazers(context, repo.owner, repo.name)
        watchers = GitHubManager.getRepoWatchers(context, repo.owner, repo.name)
        events = GitHubManager.getRepoEvents(context, repo.owner, repo.name)
        loading = false
    }

    LaunchedEffect(repo.fullName, reloadNonce) {
        loadInsights()
    }

    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(
                title = "> repo insights",
                subtitle = repo.fullName,
                onBack = onBack,
                trailing = {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.REFRESH,
                        onClick = { reloadNonce++ },
                        tint = palette.accent,
                        enabled = !loading,
                        contentDescription = "refresh insights",
                    )
                },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GitHubTerminalTab("traffic", selectedTab == RepoInsightsTab.TRAFFIC) { selectedTab = RepoInsightsTab.TRAFFIC }
                GitHubTerminalTab("people", selectedTab == RepoInsightsTab.PEOPLE) { selectedTab = RepoInsightsTab.PEOPLE }
                GitHubTerminalTab("events", selectedTab == RepoInsightsTab.EVENTS) { selectedTab = RepoInsightsTab.EVENTS }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading insights…")
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (selectedTab) {
                        RepoInsightsTab.TRAFFIC -> {
                            item { TrafficSummaryRow(trafficViews, trafficClones) }
                            item { TrafficSeriesCard("views", trafficViews) }
                            item { TrafficSeriesCard("clones", trafficClones) }
                            item { TrafficRankedCard("referrers", referrers.map { it.referrer to "${it.count}/${it.uniques}" }) }
                            item { TrafficRankedCard("paths", paths.map { it.path to "${it.count}/${it.uniques}" }) }
                        }
                        RepoInsightsTab.PEOPLE -> {
                            item { RepoPeopleSection("stargazers", stargazers) }
                            item { RepoPeopleSection("watchers", watchers) }
                        }
                        RepoInsightsTab.EVENTS -> {
                            if (events.isEmpty()) {
                                item { TerminalEmptyLine("// no repository events") }
                            } else {
                                items(events) { event -> RepoEventRow(event) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficSummaryRow(views: GHTrafficSeries?, clones: GHTrafficSeries?) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RepoInsightMetric("views", views?.count ?: 0, views?.uniques ?: 0)
        RepoInsightMetric("clones", clones?.count ?: 0, clones?.uniques ?: 0)
    }
}

@Composable
private fun RepoInsightMetric(label: String, count: Int, uniques: Int) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .widthIn(min = 118.dp)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(count.toString(), color = palette.accent, fontFamily = JetBrainsMono, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("$label / $uniques unique", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
    }
}

@Composable
private fun TrafficSeriesCard(title: String, series: GHTrafficSeries?) {
    TerminalInsightCard(title) {
        if (series == null || series.items.isEmpty()) {
            TerminalEmptyLine("// no $title data or no permission")
        } else {
            val maxCount = series.items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            series.items.takeLast(14).forEach { point ->
                val barWidth = ((point.count * 96) / maxCount).coerceAtLeast(2).dp
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(point.timestamp.take(10), color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.width(74.dp))
                    Box(Modifier.width(barWidth).height(7.dp).background(AiModuleTheme.colors.accent.copy(alpha = 0.58f)))
                    Spacer(Modifier.weight(1f))
                    Text("${point.count}/${point.uniques}", color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.width(54.dp))
                }
            }
        }
    }
}

@Composable
private fun TrafficRankedCard(title: String, rows: List<Pair<String, String>>) {
    TerminalInsightCard(title) {
        if (rows.isEmpty()) {
            TerminalEmptyLine("// no $title data or no permission")
        } else {
            rows.take(10).forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}".padStart(2, '0'), color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                    Text(row.first.ifBlank { "(unknown)" }, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(row.second, color = AiModuleTheme.colors.accent, fontFamily = JetBrainsMono, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RepoPeopleSection(title: String, people: List<GHRepoPerson>) {
    TerminalInsightCard(title) {
        if (people.isEmpty()) {
            TerminalEmptyLine("// no $title loaded")
        } else {
            people.take(50).forEach { person ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("@", color = AiModuleTheme.colors.accent, fontFamily = JetBrainsMono, fontSize = 12.sp)
                    Text(person.login, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (person.starredAt.isNotBlank()) {
                        Text(person.starredAt.take(10), color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoEventRow(event: GHRepoEvent) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(repoEventLabel(event.type), color = palette.accent, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 92.dp, max = 148.dp))
            Text(event.actor.ifBlank { "github" }, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(event.createdAt.take(10), color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
        val detail = listOf(event.action, event.refType, event.ref, if (event.size > 0) "${event.size} items" else "")
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (detail.isNotBlank()) {
            Text(detail, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TerminalInsightCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun TerminalEmptyLine(text: String) {
    Text(text, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp)
}

private fun repoEventLabel(type: String): String =
    type.removeSuffix("Event").replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()

private enum class GitDataTab { REFS, TREE, BLOB, TAG, COMMIT }

@Composable
internal fun GitDataToolsScreen(repo: GHRepo, canWrite: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var selectedTab by remember { mutableStateOf(GitDataTab.REFS) }
    var loading by remember { mutableStateOf(false) }
    var refQuery by rememberSaveable(repo.fullName, "git-ref") { mutableStateOf("heads/${repo.defaultBranch}") }
    var matchingPrefix by rememberSaveable(repo.fullName, "git-matching") { mutableStateOf("heads/") }
    var treeSha by rememberSaveable(repo.fullName, "git-tree") { mutableStateOf("") }
    var blobSha by rememberSaveable(repo.fullName, "git-blob") { mutableStateOf("") }
    var tagSha by rememberSaveable(repo.fullName, "git-tag") { mutableStateOf("") }
    var commitSha by rememberSaveable(repo.fullName, "git-commit") { mutableStateOf("") }
    var recursiveTree by rememberSaveable(repo.fullName, "git-tree-recursive") { mutableStateOf(true) }
    var refResult by remember { mutableStateOf<GHGitRef?>(null) }
    var matchingRefs by remember { mutableStateOf<List<GHGitRef>>(emptyList()) }
    var treeResult by remember { mutableStateOf<GHGitTree?>(null) }
    var blobResult by remember { mutableStateOf<GHGitBlob?>(null) }
    var tagResult by remember { mutableStateOf<GHGitTagDetail?>(null) }
    var commitResult by remember { mutableStateOf<GHGitCommit?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var operationMessage by remember { mutableStateOf<String?>(null) }
    var newBlobContent by rememberSaveable(repo.fullName, "git-new-blob-content") { mutableStateOf("") }
    var newBlobBase64 by rememberSaveable(repo.fullName, "git-new-blob-base64") { mutableStateOf(false) }
    var newTreeBase by rememberSaveable(repo.fullName, "git-new-tree-base") { mutableStateOf("") }
    var newTreePath by rememberSaveable(repo.fullName, "git-new-tree-path") { mutableStateOf("") }
    var newTreeMode by rememberSaveable(repo.fullName, "git-new-tree-mode") { mutableStateOf("100644") }
    var newTreeType by rememberSaveable(repo.fullName, "git-new-tree-type") { mutableStateOf("blob") }
    var newTreeSha by rememberSaveable(repo.fullName, "git-new-tree-sha") { mutableStateOf("") }
    var newTreeContent by rememberSaveable(repo.fullName, "git-new-tree-content") { mutableStateOf("") }
    var newTagName by rememberSaveable(repo.fullName, "git-new-tag-name") { mutableStateOf("") }
    var newTagMessage by rememberSaveable(repo.fullName, "git-new-tag-message") { mutableStateOf("") }
    var newTagObjectSha by rememberSaveable(repo.fullName, "git-new-tag-object") { mutableStateOf("") }
    var newTagObjectType by rememberSaveable(repo.fullName, "git-new-tag-type") { mutableStateOf("commit") }
    var newTaggerName by rememberSaveable(repo.fullName, "git-new-tagger-name") { mutableStateOf("") }
    var newTaggerEmail by rememberSaveable(repo.fullName, "git-new-tagger-email") { mutableStateOf("") }
    var newCommitMessage by rememberSaveable(repo.fullName, "git-new-commit-message") { mutableStateOf("") }
    var newCommitTreeSha by rememberSaveable(repo.fullName, "git-new-commit-tree") { mutableStateOf("") }
    var newCommitParents by rememberSaveable(repo.fullName, "git-new-commit-parents") { mutableStateOf("") }
    var updateRefName by rememberSaveable(repo.fullName, "git-update-ref-name") { mutableStateOf("heads/${repo.defaultBranch}") }
    var updateRefSha by rememberSaveable(repo.fullName, "git-update-ref-sha") { mutableStateOf("") }
    var updateRefForce by rememberSaveable(repo.fullName, "git-update-ref-force") { mutableStateOf(false) }
    var createRefName by rememberSaveable(repo.fullName, "git-create-ref-name") { mutableStateOf("") }
    var createRefSha by rememberSaveable(repo.fullName, "git-create-ref-sha") { mutableStateOf("") }
    var deleteRefName by rememberSaveable(repo.fullName, "git-delete-ref-name") { mutableStateOf("") }
    var deleteRefConfirm by rememberSaveable(repo.fullName, "git-delete-ref-confirm") { mutableStateOf("") }

    suspend fun loadRefData() {
        loading = true
        error = null
        refResult = GitHubManager.getGitRef(context, repo.owner, repo.name, refQuery)
        matchingRefs = GitHubManager.getMatchingGitRefs(context, repo.owner, repo.name, matchingPrefix)
        refResult?.takeIf { it.nodeType == "commit" && commitSha.isBlank() }?.let { commitSha = it.nodeSha }
        if (refResult == null && matchingRefs.isEmpty()) error = "no refs found"
        loading = false
    }

    suspend fun loadTreeData() {
        loading = true
        error = null
        treeResult = GitHubManager.getGitTree(context, repo.owner, repo.name, treeSha, recursive = recursiveTree)
        if (treeResult == null) error = "tree not found"
        loading = false
    }

    suspend fun loadBlobData() {
        loading = true
        error = null
        blobResult = GitHubManager.getGitBlob(context, repo.owner, repo.name, blobSha)
        if (blobResult == null) error = "blob not found"
        loading = false
    }

    suspend fun loadTagData() {
        loading = true
        error = null
        tagResult = GitHubManager.getGitTag(context, repo.owner, repo.name, tagSha)
        if (tagResult == null) error = "tag object not found"
        loading = false
    }

    suspend fun loadCommitData() {
        loading = true
        error = null
        commitResult = GitHubManager.getGitCommit(context, repo.owner, repo.name, commitSha)
        commitResult?.takeIf { it.treeSha.isNotBlank() && treeSha.isBlank() }?.let { treeSha = it.treeSha }
        if (commitResult == null) error = "git commit not found"
        loading = false
    }

    LaunchedEffect(repo.fullName) {
        loadRefData()
    }

    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(
                title = "> git data",
                subtitle = repo.fullName,
                onBack = onBack,
                trailing = {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.REFRESH,
                        onClick = {
                            scope.launch {
                                when (selectedTab) {
                                    GitDataTab.REFS -> loadRefData()
                                    GitDataTab.TREE -> loadTreeData()
                                    GitDataTab.BLOB -> loadBlobData()
                                    GitDataTab.TAG -> loadTagData()
                                    GitDataTab.COMMIT -> loadCommitData()
                                }
                            }
                        },
                        tint = palette.accent,
                        enabled = !loading,
                        contentDescription = "refresh git data",
                    )
                },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GitDataTab.entries.forEach { tab ->
                    GitHubTerminalTab(tab.name.lowercase(), selectedTab == tab) { selectedTab = tab }
                }
            }
            if (error != null) {
                Text(
                    "! ${error.orEmpty()}",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (operationMessage != null) {
                Text(
                    "▸ ${operationMessage.orEmpty()}",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading git data…")
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (selectedTab) {
                        GitDataTab.REFS -> {
                            item {
                                GitDataInputCard(
                                    title = "refs",
                                    fields = listOf(
                                        "exact ref" to refQuery,
                                        "matching prefix" to matchingPrefix
                                    ),
                                    onFieldChange = { index, value ->
                                        if (index == 0) refQuery = value else matchingPrefix = value
                                    },
                                    action = "load refs",
                                    onAction = { scope.launch { loadRefData() } },
                                )
                            }
                            refResult?.let { ref ->
                                item {
                                    GitRefCard(
                                        ref = ref,
                                        onCommit = {
                                            commitSha = ref.nodeSha
                                            selectedTab = GitDataTab.COMMIT
                                            scope.launch { loadCommitData() }
                                        },
                                    )
                                }
                            }
                            item { GitRefListCard(matchingRefs, onSelect = { refQuery = it.ref.removePrefix("refs/"); refResult = it }) }
                            item {
                                GitDataWriteCard("write: refs", canWrite) {
                                    GitDataWriteField("ref to create", createRefName, { createRefName = it }, "tags/v1.0.0 or heads/tmp")
                                    GitDataWriteField("source sha", createRefSha, { createRefSha = it }, "commit or tag object sha")
                                    GitHubTerminalButton("create ref", onClick = {
                                        scope.launch {
                                            loading = true
                                            error = null
                                            val created = GitHubManager.createGitRef(context, repo.owner, repo.name, createRefName, createRefSha)
                                            loading = false
                                            if (created != null) {
                                                refQuery = created.ref.removePrefix("refs/")
                                                refResult = created
                                                updateRefName = created.ref.removePrefix("refs/")
                                                operationMessage = "created ${created.ref}"
                                            } else {
                                                error = "failed to create ref"
                                            }
                                        }
                                    }, color = palette.accent)
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                                    GitDataWriteField("ref to update", updateRefName, { updateRefName = it }, "heads/${repo.defaultBranch}")
                                    GitDataWriteField("target sha", updateRefSha, { updateRefSha = it }, "commit sha")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        GitHubTerminalButton("update ref", onClick = {
                                            scope.launch {
                                                loading = true
                                                error = null
                                                val updated = GitHubManager.updateGitRef(context, repo.owner, repo.name, updateRefName, updateRefSha, updateRefForce)
                                                loading = false
                                                if (updated != null) {
                                                    refQuery = updated.ref.removePrefix("refs/")
                                                    refResult = updated
                                                    operationMessage = "updated ${updated.ref}"
                                                } else {
                                                    error = "failed to update ref"
                                                }
                                            }
                                        }, color = palette.accent)
                                        GitHubTerminalCheckbox("force", updateRefForce, { updateRefForce = !updateRefForce })
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                                    GitDataWriteField("ref to delete", deleteRefName, { deleteRefName = it }, "heads/tmp-branch")
                                    GitDataWriteField("confirm", deleteRefConfirm, { deleteRefConfirm = it }, "type delete")
                                    GitHubTerminalButton("delete ref", onClick = {
                                        scope.launch {
                                            if (deleteRefConfirm.trim() != "delete") {
                                                error = "type delete to confirm ref deletion"
                                                return@launch
                                            }
                                            loading = true
                                            error = null
                                            val ok = GitHubManager.deleteGitRef(context, repo.owner, repo.name, deleteRefName)
                                            loading = false
                                            if (ok) {
                                                operationMessage = "deleted ${deleteRefName.removePrefix("refs/")}"
                                                deleteRefName = ""
                                                deleteRefConfirm = ""
                                                loadRefData()
                                            } else {
                                                error = "failed to delete ref"
                                            }
                                        }
                                    }, color = palette.error)
                                }
                            }
                        }
                        GitDataTab.TREE -> {
                            item {
                                GitDataInputCard(
                                    title = "tree",
                                    fields = listOf("tree sha" to treeSha),
                                    onFieldChange = { _, value -> treeSha = value },
                                    action = "load tree",
                                    onAction = { scope.launch { loadTreeData() } },
                                    trailing = {
                                        GitHubTerminalCheckbox("recursive", recursiveTree, { recursiveTree = !recursiveTree })
                                    },
                                )
                            }
                            treeResult?.let { tree -> item { GitTreeCard(tree, onBlob = { blobSha = it; selectedTab = GitDataTab.BLOB; scope.launch { loadBlobData() } }) } }
                            item {
                                GitDataWriteCard("write: create tree", canWrite) {
                                    GitDataWriteField("base tree", newTreeBase, { newTreeBase = it }, "optional base tree sha")
                                    GitDataWriteField("path", newTreePath, { newTreePath = it }, "path/in/repo.txt")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GitDataWriteField("mode", newTreeMode, { newTreeMode = it }, "100644", modifier = Modifier.weight(1f))
                                        GitDataWriteField("type", newTreeType, { newTreeType = it }, "blob", modifier = Modifier.weight(1f))
                                    }
                                    GitDataWriteField("sha", newTreeSha, { newTreeSha = it }, "blob/tree sha")
                                    GitDataWriteField("content", newTreeContent, { newTreeContent = it }, "inline blob content", singleLine = false, minHeight = 96.dp)
                                    GitHubTerminalButton("create tree", onClick = {
                                        scope.launch {
                                            loading = true
                                            error = null
                                            val created = GitHubManager.createGitTree(
                                                context = context,
                                                owner = repo.owner,
                                                repo = repo.name,
                                                baseTree = newTreeBase,
                                                path = newTreePath,
                                                mode = newTreeMode,
                                                type = newTreeType,
                                                sha = newTreeSha,
                                                content = newTreeContent
                                            )
                                            loading = false
                                            if (created != null) {
                                                treeResult = created
                                                treeSha = created.sha
                                                newCommitTreeSha = created.sha
                                                operationMessage = "created tree ${created.sha.take(12)}"
                                            } else {
                                                error = "failed to create tree"
                                            }
                                        }
                                    }, color = palette.accent)
                                }
                            }
                        }
                        GitDataTab.BLOB -> {
                            item {
                                GitDataInputCard(
                                    title = "blob",
                                    fields = listOf("blob sha" to blobSha),
                                    onFieldChange = { _, value -> blobSha = value },
                                    action = "load blob",
                                    onAction = { scope.launch { loadBlobData() } },
                                )
                            }
                            blobResult?.let { blob -> item { GitBlobCard(blob) } }
                            item {
                                GitDataWriteCard("write: create blob", canWrite) {
                                    GitDataWriteField("content", newBlobContent, { newBlobContent = it }, "blob content", singleLine = false, minHeight = 140.dp)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        GitHubTerminalButton("create blob", onClick = {
                                            scope.launch {
                                                loading = true
                                                error = null
                                                val created = GitHubManager.createGitBlob(
                                                    context,
                                                    repo.owner,
                                                    repo.name,
                                                    newBlobContent,
                                                    if (newBlobBase64) "base64" else "utf-8"
                                                )
                                                loading = false
                                                if (created != null) {
                                                    blobResult = created
                                                    blobSha = created.sha
                                                    newTreeSha = created.sha
                                                    operationMessage = "created blob ${created.sha.take(12)}"
                                                } else {
                                                    error = "failed to create blob"
                                                }
                                            }
                                        }, color = palette.accent)
                                        GitHubTerminalCheckbox("base64", newBlobBase64, { newBlobBase64 = !newBlobBase64 })
                                    }
                                }
                            }
                        }
                        GitDataTab.TAG -> {
                            item {
                                GitDataInputCard(
                                    title = "tag",
                                    fields = listOf("tag sha" to tagSha),
                                    onFieldChange = { _, value -> tagSha = value },
                                    action = "load tag",
                                    onAction = { scope.launch { loadTagData() } },
                                )
                            }
                            tagResult?.let { tag -> item { GitTagCard(tag) } }
                            item {
                                GitDataWriteCard("write: create annotated tag", canWrite) {
                                    GitDataWriteField("tag", newTagName, { newTagName = it }, "v1.0.0")
                                    GitDataWriteField("message", newTagMessage, { newTagMessage = it }, "release message", singleLine = false, minHeight = 86.dp)
                                    GitDataWriteField("object sha", newTagObjectSha, { newTagObjectSha = it }, "commit sha")
                                    GitDataWriteField("object type", newTagObjectType, { newTagObjectType = it }, "commit")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GitDataWriteField("tagger name", newTaggerName, { newTaggerName = it }, "optional", modifier = Modifier.weight(1f))
                                        GitDataWriteField("tagger email", newTaggerEmail, { newTaggerEmail = it }, "optional", modifier = Modifier.weight(1f))
                                    }
                                    GitHubTerminalButton("create tag object", onClick = {
                                        scope.launch {
                                            loading = true
                                            error = null
                                            val created = GitHubManager.createGitTag(
                                                context = context,
                                                owner = repo.owner,
                                                repo = repo.name,
                                                tag = newTagName,
                                                message = newTagMessage,
                                                objectSha = newTagObjectSha,
                                                objectType = newTagObjectType,
                                                taggerName = newTaggerName,
                                                taggerEmail = newTaggerEmail
                                            )
                                            loading = false
                                            if (created != null) {
                                                tagResult = created
                                                tagSha = created.sha
                                                createRefName = "tags/${created.tag}"
                                                createRefSha = created.sha
                                                operationMessage = "created tag object ${created.sha.take(12)}"
                                            } else {
                                                error = "failed to create tag object"
                                            }
                                        }
                                    }, color = palette.accent)
                                }
                            }
                        }
                        GitDataTab.COMMIT -> {
                            item {
                                GitDataInputCard(
                                    title = "commit",
                                    fields = listOf("commit sha" to commitSha),
                                    onFieldChange = { _, value -> commitSha = value },
                                    action = "load commit",
                                    onAction = { scope.launch { loadCommitData() } },
                                )
                            }
                            commitResult?.let { commit ->
                                item {
                                    GitCommitCard(
                                        commit = commit,
                                        onTree = {
                                            treeSha = commit.treeSha
                                            selectedTab = GitDataTab.TREE
                                            scope.launch { loadTreeData() }
                                        },
                                    )
                                }
                            }
                            item {
                                GitDataWriteCard("write: create commit", canWrite) {
                                    GitDataWriteField("message", newCommitMessage, { newCommitMessage = it }, "commit message", singleLine = false, minHeight = 86.dp)
                                    GitDataWriteField("tree sha", newCommitTreeSha, { newCommitTreeSha = it }, "tree sha")
                                    GitDataWriteField("parents", newCommitParents, { newCommitParents = it }, "comma or newline separated parent shas", singleLine = false, minHeight = 72.dp)
                                    GitHubTerminalButton("create commit", onClick = {
                                        scope.launch {
                                            loading = true
                                            error = null
                                            val parents = newCommitParents.split(',', '\n', ' ')
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            val created = GitHubManager.createGitCommit(
                                                context = context,
                                                owner = repo.owner,
                                                repo = repo.name,
                                                message = newCommitMessage,
                                                treeSha = newCommitTreeSha,
                                                parentShas = parents
                                            )
                                            loading = false
                                            if (created != null) {
                                                commitResult = created
                                                commitSha = created.sha
                                                updateRefSha = created.sha
                                                operationMessage = "created commit ${created.sha.take(12)}"
                                            } else {
                                                error = "failed to create commit"
                                            }
                                        }
                                    }, color = palette.accent)
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
private fun GitDataInputCard(
    title: String,
    fields: List<Pair<String, String>>,
    onFieldChange: (Int, String) -> Unit,
    action: String,
    onAction: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    TerminalInsightCard(title) {
        fields.forEachIndexed { index, field ->
            Text(field.first, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
            GitHubTerminalTextField(
                value = field.second,
                onValueChange = { onFieldChange(index, it) },
                placeholder = field.first,
                singleLine = true,
                minHeight = 38.dp,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GitHubTerminalButton(action, onClick = onAction, color = AiModuleTheme.colors.accent)
            trailing?.invoke()
        }
    }
}

@Composable
private fun GitDataWriteCard(title: String, canWrite: Boolean, content: @Composable ColumnScope.() -> Unit) {
    TerminalInsightCard(title) {
        if (!canWrite) {
            GitHubPermissionHint("write permission required")
            Text(
                "This low-level Git Data mutation is disabled for the current token.",
                color = AiModuleTheme.colors.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        } else {
            content()
        }
    }
}

@Composable
private fun GitDataWriteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 38.dp,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
        GitHubTerminalTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            singleLine = singleLine,
            minHeight = minHeight,
        )
    }
}

@Composable
private fun GitRefCard(ref: GHGitRef, onCommit: () -> Unit) {
    TerminalInsightCard("ref result") {
        GitDataKv("ref", ref.ref)
        GitDataKv("type", ref.nodeType)
        GitDataKv("sha", ref.nodeSha)
        if (ref.nodeType == "commit") {
            GitHubTerminalButton("open commit", onClick = onCommit, color = AiModuleTheme.colors.accent)
        }
    }
}

@Composable
private fun GitRefListCard(refs: List<GHGitRef>, onSelect: (GHGitRef) -> Unit) {
    TerminalInsightCard("matching refs") {
        if (refs.isEmpty()) {
            TerminalEmptyLine("// no matching refs")
        } else {
            refs.take(60).forEach { ref ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ref.ref.removePrefix("refs/"), color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(ref.nodeType, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                    GitHubTerminalButton("use", onClick = { onSelect(ref) }, color = AiModuleTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun GitTreeCard(tree: GHGitTree, onBlob: (String) -> Unit) {
    TerminalInsightCard("tree ${tree.sha.take(12)}") {
        GitDataKv("items", tree.items.size.toString())
        GitDataKv("truncated", tree.truncated.toString())
        tree.items.take(120).forEach { item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.type, color = gitObjectColor(item.type), fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.width(38.dp))
                Text(item.path, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(item.mode, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                if (item.type == "blob") {
                    GitHubTerminalButton("blob", onClick = { onBlob(item.sha) }, color = AiModuleTheme.colors.textSecondary)
                }
            }
        }
        if (tree.items.size > 120) TerminalEmptyLine("// ${tree.items.size - 120} more items hidden")
    }
}

@Composable
private fun GitBlobCard(blob: GHGitBlob) {
    TerminalInsightCard("blob ${blob.sha.take(12)}") {
        GitDataKv("size", ghFmtSize(blob.size))
        GitDataKv("encoding", blob.encoding)
        val decoded = remember(blob.content, blob.encoding) { decodeGitBlobPreview(blob) }
        Text(
            decoded.ifBlank { "// empty or binary blob" },
            color = AiModuleTheme.colors.textPrimary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius))
                .padding(8.dp),
        )
    }
}

@Composable
private fun GitTagCard(tag: GHGitTagDetail) {
    TerminalInsightCard("tag ${tag.tag.ifBlank { tag.sha.take(12) }}") {
        GitDataKv("sha", tag.sha)
        GitDataKv("object", "${tag.objectType}:${tag.objectSha}")
        GitDataKv("tagger", listOf(tag.taggerName, tag.taggerEmail).filter { it.isNotBlank() }.joinToString(" "))
        GitDataKv("date", tag.date)
        if (tag.message.isNotBlank()) {
            Text(tag.message, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun GitCommitCard(commit: GHGitCommit, onTree: () -> Unit) {
    TerminalInsightCard("git commit ${commit.sha.take(12)}") {
        GitDataKv("tree", commit.treeSha)
        GitDataKv("parents", commit.parentShas.joinToString(", ") { it.take(12) })
        GitDataKv("author", listOf(commit.authorName, commit.authorEmail).filter { it.isNotBlank() }.joinToString(" "))
        GitDataKv("author date", commit.authorDate)
        GitDataKv("committer", listOf(commit.committerName, commit.committerEmail).filter { it.isNotBlank() }.joinToString(" "))
        GitDataKv("commit date", commit.committerDate)
        Text(commit.message.ifBlank { "(no message)" }, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp)
        if (commit.treeSha.isNotBlank()) GitHubTerminalButton("open tree", onClick = onTree, color = AiModuleTheme.colors.accent)
    }
}

@Composable
internal fun GitDataKv(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.width(72.dp))
        Text(value, color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun gitObjectColor(type: String): Color =
    when (type) {
        "tree" -> AiModuleTheme.colors.accent
        "blob" -> AiModuleTheme.colors.textSecondary
        "commit" -> GitHubSuccessGreen
        "tag" -> GitHubMergedPurple
        else -> AiModuleTheme.colors.textMuted
    }

private fun decodeGitBlobPreview(blob: GHGitBlob): String {
    if (blob.content.isBlank()) return ""
    return try {
        val raw = if (blob.encoding == "base64") {
            android.util.Base64.decode(blob.content.replace("\n", ""), android.util.Base64.DEFAULT)
        } else {
            blob.content.toByteArray()
        }
        String(raw).take(6000)
    } catch (_: Exception) {
        ""
    }
}

