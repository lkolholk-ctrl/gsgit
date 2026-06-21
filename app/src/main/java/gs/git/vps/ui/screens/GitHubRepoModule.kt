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

// Compact mode — propagates through all sub-screens automatically

internal enum class RepoTab { FILES, COMMITS, ISSUES, PULLS, RELEASES, ACTIONS, HISTORY, PROJECTS, README, CODE_SEARCH, TIME_TRAVEL, TELEMETRY }


@Composable
internal fun RepoDetailScreen(
    repo: GHRepo,
    onBack: () -> Unit,
    onMinimize: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    initialTarget: GitHubNotificationTarget? = null,
    onInitialTargetConsumed: () -> Unit = {},
    onOpenAiAgent: ((repoFullName: String, branch: String?, prompt: String?) -> Unit)? = null
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val colors = AiModuleTheme.colors
    var selectedTab by rememberSaveable(repo.fullName) { mutableStateOf(RepoTab.FILES) }; var contents by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var currentPath by rememberSaveable(repo.fullName) { mutableStateOf("") }; var commits by remember { mutableStateOf<List<GHCommit>>(emptyList()) }
    var issues by remember { mutableStateOf<List<GHIssue>>(emptyList()) }; var pulls by remember { mutableStateOf<List<GHPullRequest>>(emptyList()) }
    var releases by remember { mutableStateOf<List<GHRelease>>(emptyList()) }; var readme by remember { mutableStateOf<String?>(null) }
    var readmeHtml by remember { mutableStateOf<String?>(null) }
    var readmeBlocks by remember { mutableStateOf<List<ReadmeRenderBlock>?>(null) }
    var readmeError by remember { mutableStateOf<String?>(null) }
    var readmeReloadNonce by remember { mutableIntStateOf(0) }
    var workflowRuns by remember { mutableStateOf<List<GHWorkflowRun>>(emptyList()) }; var selectedRunId by remember { mutableStateOf<Long?>(null) }
    var workflows by remember { mutableStateOf<List<GHWorkflow>>(emptyList()) }; var showDispatch by remember { mutableStateOf(false) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }; var loading by remember { mutableStateOf(true) }
    var fileContent by remember { mutableStateOf<String?>(null) }; var openedFile by remember { mutableStateOf<GHContent?>(null) }; var editingFile by remember { mutableStateOf<GHContent?>(null) }
    var showBlameFor by remember { mutableStateOf<GHContent?>(null) }; var showFileHistoryFor by remember { mutableStateOf<GHContent?>(null) }
    var repoQuery by rememberSaveable(repo.fullName) { mutableStateOf("") }
    var cloneProgress by remember { mutableStateOf<String?>(null) }; var isStarred by remember { mutableStateOf(false) }
    var isWatching by remember { mutableStateOf(false) }
    var showRepoOverflow by remember { mutableStateOf(false) }
    var showRepoInsights by remember { mutableStateOf(false) }
    var showGitDataTools by remember { mutableStateOf(false) }
    var repoReloadNonce by remember { mutableIntStateOf(0) }
    var showCopilotChat by rememberSaveable(repo.fullName) { mutableStateOf(false) }
    var copilotPrompt by remember { mutableStateOf<String?>(null) }
    var selectedBranch by rememberSaveable(repo.fullName) { mutableStateOf(repo.defaultBranch) }
    var prevBranchForReflog by remember { mutableStateOf(selectedBranch) }
    LaunchedEffect(selectedBranch) {
        if (prevBranchForReflog != selectedBranch) {
            gs.git.vps.data.github.LocalTimeTravelManager.addReflogEntry(
                context,
                repo.fullName,
                "refs/heads/$selectedBranch",
                beforeSha = "branch:$prevBranchForReflog",
                afterSha = "branch:$selectedBranch",
                action = "checkout: moving from $prevBranchForReflog to $selectedBranch"
            )
            prevBranchForReflog = selectedBranch
        }
    }
    val childCache = remember(repo.fullName, selectedBranch) { mutableStateMapOf<String, List<GHContent>>() }
    var expandedPaths by rememberSaveable(
        repo.fullName, selectedBranch,
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() }),
    ) { mutableStateOf(setOf<String>()) }
    var loadingPaths by remember(repo.fullName, selectedBranch) { mutableStateOf(setOf<String>()) }
    var showUpload by remember { mutableStateOf(false) }; var showCreateFile by remember { mutableStateOf(false) }
    var showCreateBranch by remember { mutableStateOf(false) }; var showCreateIssue by remember { mutableStateOf(false) }
    var showIssueEvents by remember { mutableStateOf(false) }
    var showCreatePR by remember { mutableStateOf(false) }; var selectedIssue by remember { mutableStateOf<GHIssue?>(null) }
    var selectedCommitSha by remember { mutableStateOf<String?>(null) }; var deleteTarget by remember { mutableStateOf<GHContent?>(null) }
    var showBranchPicker by remember { mutableStateOf(false) }
    var selectedPRNumber by remember { mutableStateOf<Int?>(null) }
    var selectedPullNumber by remember { mutableStateOf<Int?>(null) }
    var showRepoSettings by remember { mutableStateOf(false) }
    var showBranchProtection by remember { mutableStateOf(false) }
    var showCollaborators by remember { mutableStateOf(false) }
    var showTeams by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }
    var showWebhooks by remember { mutableStateOf(false) }
    var showDiscussions by remember { mutableStateOf(false) }
    var showRulesets by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showAutolinks by remember { mutableStateOf(false) }
    var showLfs by remember { mutableStateOf(false) }
    var showInteractionLimits by remember { mutableStateOf(false) }
    var showActionsTroubleshoot by remember { mutableStateOf(false) }
    var showBuilds by remember { mutableStateOf(false) }
    var returnToRepoSettings by remember { mutableStateOf(false) }
    var editorInitialLine by remember { mutableStateOf<Int?>(null) }
    var languages by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }; var contributors by remember { mutableStateOf<List<GHContributor>>(emptyList()) }
    // Pagination
    var commitsPage by rememberSaveable(repo.fullName) { mutableIntStateOf(1) }; var commitsHasMore by rememberSaveable(repo.fullName) { mutableStateOf(true) }
    var issuesPage by rememberSaveable(repo.fullName) { mutableIntStateOf(1) }; var issuesHasMore by rememberSaveable(repo.fullName) { mutableStateOf(true) }
    var pullsPage by rememberSaveable(repo.fullName) { mutableIntStateOf(1) }; var pullsHasMore by rememberSaveable(repo.fullName) { mutableStateOf(true) }
    val filesListState = rememberSaveable(repo.fullName, "files", saver = LazyListState.Saver) { LazyListState(0, 0) }
    val commitsListState = rememberSaveable(repo.fullName, "commits", saver = LazyListState.Saver) { LazyListState(0, 0) }
    val issuesListState = rememberSaveable(repo.fullName, "issues", saver = LazyListState.Saver) { LazyListState(0, 0) }
    val pullsListState = rememberSaveable(repo.fullName, "pulls", saver = LazyListState.Saver) { LazyListState(0, 0) }
    val canWrite = repo.canWrite()
    val canAdmin = repo.canAdmin()

    val navigateToCode: (String, Int) -> Unit = { path, line ->
        scope.launch {
            try {
                val content = GitHubManager.getFileContent(context, repo.owner, repo.name, path, selectedBranch)
                val name = path.substringAfterLast("/")
                val dir = path.substringBeforeLast("/", "").ifEmpty { "/" }
                val contentsList = GitHubManager.getRepoContents(context, repo.owner, repo.name, dir, selectedBranch)
                val targetFile = contentsList.find { it.path == path || it.name == name } ?: GHContent(
                    name = name,
                    path = path,
                    type = "file",
                    size = 0L,
                    downloadUrl = "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/${selectedBranch}/$path",
                    sha = ""
                )
                openedFile = null
                fileContent = content
                editingFile = targetFile
                editorInitialLine = line
                selectedPullNumber = null
                selectedPRNumber = null
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(initialTarget) {
        val target = initialTarget ?: return@LaunchedEffect
        when (target.subjectType) {
            "PullRequest" -> {
                selectedTab = RepoTab.PULLS
                target.number?.let { selectedPullNumber = it }
            }
            "Issue" -> {
                selectedTab = RepoTab.ISSUES
                target.number?.let {
                    selectedIssue = GHIssue(
                        number = it,
                        title = "",
                        state = "",
                        author = "",
                        createdAt = "",
                        comments = 0,
                        isPR = false
                    )
                }
            }
            "Release" -> selectedTab = RepoTab.RELEASES
            "Discussion" -> showDiscussions = true
            "File" -> {
                target.branch?.let { selectedBranch = it }
                val path = target.filePath.orEmpty()
                if (path.isNotBlank()) {
                    scope.launch {
                        val branch = target.branch ?: selectedBranch
                        val dir = path.substringBeforeLast("/", "").ifEmpty { "/" }
                        val list = GitHubManager.getRepoContents(context, repo.owner, repo.name, dir, branch)
                        val file = list.find { it.path == path || it.name == path.substringAfterLast("/") }
                        if (file != null) {
                            openedFile = file
                            fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, file.path, branch)
                        } else {
                            val name = path.substringAfterLast("/")
                            openedFile = GHContent(name, path, "file", 0L,
                                "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/$branch/$path", "")
                            fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, path, branch)
                        }
                    }
                }
            }
            "Dir" -> {
                selectedTab = RepoTab.FILES
                target.branch?.let { selectedBranch = it }
                val path = target.filePath.orEmpty()
                if (path.isNotBlank()) {
                    currentPath = path
                    scope.launch { contents = GitHubManager.getRepoContents(context, repo.owner, repo.name, path, target.branch) }
                }
            }
            else -> {
                selectedTab = if (target.number != null) RepoTab.ISSUES else RepoTab.FILES
            }
        }
        onInitialTargetConsumed()
    }

    fun restoreRepoSettingsIfNeeded() {
        if (returnToRepoSettings) {
            returnToRepoSettings = false
            showRepoSettings = true
        }
    }

    fun handleRepoBack() {
        when {
            showUpload -> showUpload = false
            showCreateFile -> showCreateFile = false
            showCreateBranch -> showCreateBranch = false
            showCreateIssue -> showCreateIssue = false
            showIssueEvents -> showIssueEvents = false
            showCreatePR -> showCreatePR = false
            showBranchPicker -> showBranchPicker = false
            showDispatch -> showDispatch = false
            showRepoOverflow -> showRepoOverflow = false
            showRepoInsights -> showRepoInsights = false
            showGitDataTools -> showGitDataTools = false
            showActionsTroubleshoot -> showActionsTroubleshoot = false
            showBuilds -> showBuilds = false
            deleteTarget != null -> deleteTarget = null
            editingFile != null -> {
                editingFile = null
                fileContent = null
            }
            openedFile != null -> {
                openedFile = null
                fileContent = null
            }
            selectedIssue != null -> selectedIssue = null
            selectedCommitSha != null -> selectedCommitSha = null
            selectedPRNumber != null -> selectedPRNumber = null
            selectedPullNumber != null -> selectedPullNumber = null
            selectedRunId != null -> selectedRunId = null
            showRepoSettings -> showRepoSettings = false
            showBranchProtection -> { showBranchProtection = false; restoreRepoSettingsIfNeeded() }
            showCollaborators -> { showCollaborators = false; restoreRepoSettingsIfNeeded() }
            showTeams -> { showTeams = false; restoreRepoSettingsIfNeeded() }
            showCompare -> showCompare = false
            showWebhooks -> { showWebhooks = false; restoreRepoSettingsIfNeeded() }
            showDiscussions -> { showDiscussions = false; restoreRepoSettingsIfNeeded() }
            showRulesets -> { showRulesets = false; restoreRepoSettingsIfNeeded() }
            showSecurity -> { showSecurity = false; restoreRepoSettingsIfNeeded() }
            showAutolinks -> { showAutolinks = false; restoreRepoSettingsIfNeeded() }
            showLfs -> { showLfs = false; restoreRepoSettingsIfNeeded() }
            showInteractionLimits -> { showInteractionLimits = false; restoreRepoSettingsIfNeeded() }
            currentPath.isNotBlank() && selectedTab == RepoTab.FILES -> currentPath = currentPath.substringBeforeLast("/", "")
            else -> onBack()
        }
    }
    BackHandler(onBack = ::handleRepoBack)

    fun openRepoSettingsChild(openChild: () -> Unit) {
        returnToRepoSettings = true
        showRepoSettings = false
        openChild()
    }

    fun closeRepoSettingsChild(closeChild: () -> Unit) {
        closeChild()
        restoreRepoSettingsIfNeeded()
    }

    LaunchedEffect(Unit) {
        isStarred = GitHubManager.isStarred(context, repo.owner, repo.name)
        isWatching = GitHubManager.isWatching(context, repo.owner, repo.name)
        branches = GitHubManager.getBranches(context, repo.owner, repo.name)
    }
    LaunchedEffect(selectedTab, currentPath, selectedBranch, readmeReloadNonce, repoReloadNonce) { loading = true; when (selectedTab) {
        RepoTab.FILES -> {
            val rootItems = GitHubManager.getRepoContents(context, repo.owner, repo.name, currentPath, selectedBranch)
            contents = rootItems
            childCache[currentPath] = rootItems
        }
        RepoTab.COMMITS -> { commitsPage = 1; val r = GitHubManager.getCommits(context, repo.owner, repo.name, 1); commits = r; commitsHasMore = r.size >= 30 }
        RepoTab.ISSUES -> { issuesPage = 1; val r = GitHubManager.getIssues(context, repo.owner, repo.name, page = 1); issues = r; issuesHasMore = r.size >= 30 }
        RepoTab.PULLS -> { pullsPage = 1; val r = GitHubManager.getPullRequests(context, repo.owner, repo.name, page = 1); pulls = r; pullsHasMore = r.size >= 30 }
        RepoTab.RELEASES -> releases = GitHubManager.getReleases(context, repo.owner, repo.name)
        RepoTab.ACTIONS, RepoTab.HISTORY -> { workflowRuns = GitHubManager.getWorkflowRuns(context, repo.owner, repo.name); workflows = GitHubManager.getWorkflows(context, repo.owner, repo.name) }
        RepoTab.PROJECTS -> { /* loaded inside ProjectsTab */ }
        RepoTab.README -> {
            readmeError = null
            languages = emptyMap()
            contributors = emptyList()
            releases = emptyList()
            
            // 1. SWR Cache check
            val cached = MarkdownCacheManager.get(context, repo.owner, repo.name, selectedBranch)
            if (cached != null) {
                readme = cached.markdown
                readmeHtml = cached.renderedHtml
                if (cached.renderedHtml.isBlank() && cached.markdown.isNotBlank()) {
                    scope.launch(Dispatchers.Default) {
                        val parsed = parseReadmeBlocks(cached.markdown, repo, cached.path)
                        if (readmeBlocks == null) {
                            readmeBlocks = parsed
                        }
                    }
                }
            } else {
                readme = null
                readmeHtml = null
                readmeBlocks = null
            }
            
            val loadStart = System.currentTimeMillis()
            val loadResult = runCatching {
                withTimeout(README_TOTAL_TIMEOUT_MS) readmeLoad@{
                    Log.d(README_RENDER_TAG, "fetch start ${repo.owner}/${repo.name}")
                    val fetched = withTimeout(README_FETCH_TIMEOUT_MS) {
                        fetchReadmeForRender(context, repo.owner, repo.name, selectedBranch)
                    }
                    val markdownBytes = fetched.markdown.toByteArray().size
                    val htmlBytes = fetched.renderedHtml.toByteArray().size
                    Log.d(README_RENDER_TAG, "fetch complete, markdown=$markdownBytes bytes html=$htmlBytes bytes ${repo.owner}/${repo.name}")
                    
                    // Update cache
                    MarkdownCacheManager.put(context, repo.owner, repo.name, selectedBranch, fetched)
                    
                    if (fetched.markdown.isBlank() && fetched.renderedHtml.isBlank()) {
                        readme = ""
                        readmeHtml = ""
                        readmeBlocks = emptyList()
                        return@readmeLoad
                    }
                    readme = fetched.markdown
                    readmeHtml = fetched.renderedHtml
                    if (fetched.renderedHtml.isNotBlank()) {
                        readmeBlocks = emptyList()
                        return@readmeLoad
                    }
                    if (markdownBytes > README_MAX_RENDER_BYTES) {
                        readme = fetched.markdown
                        readmeBlocks = emptyList()
                        readmeError = "README is too large to render safely (${ghFmtSize(markdownBytes.toLong())})."
                        Log.w(README_RENDER_TAG, "parse skipped large README ${repo.owner}/${repo.name} bytes=$markdownBytes")
                        return@readmeLoad
                    }
                    Log.d(README_RENDER_TAG, "parse start ${repo.owner}/${repo.name}")
                    val parsed = withContext(Dispatchers.Default) { parseReadmeBlocks(fetched.markdown, repo, fetched.path) }
                    Log.d(README_RENDER_TAG, "parse complete, blocks=${parsed.size} ${repo.owner}/${repo.name}")
                    readmeBlocks = parsed
                }
            }
            val loadMs = System.currentTimeMillis() - loadStart
            loadResult.onFailure { throwable ->
                if (readme == null && readmeHtml == null) {
                    readme = null
                    readmeHtml = null
                    readmeBlocks = emptyList()
                    readmeError = "README load timed out or failed"
                } else {
                    Log.i(README_RENDER_TAG, "README load failed, keeping cached version")
                }
                languages = emptyMap()
                contributors = emptyList()
                Log.e(README_RENDER_TAG, "README guarded load failed ${repo.owner}/${repo.name} ${loadMs}ms", throwable)
            }.onSuccess {
                Log.d(README_RENDER_TAG, "README guarded load complete ${repo.owner}/${repo.name} ${loadMs}ms")
                if ((!readme.isNullOrBlank() || !readmeHtml.isNullOrBlank()) && readmeError == null) {
                    scope.launch {
                        languages = withTimeoutOrNull(3_000L) {
                            withContext(Dispatchers.IO) { GitHubManager.getLanguages(context, repo.owner, repo.name) }
                        }.orEmpty()
                    }
                    scope.launch {
                        contributors = withTimeoutOrNull(3_000L) {
                            withContext(Dispatchers.IO) { GitHubManager.getContributors(context, repo.owner, repo.name) }
                        }.orEmpty()
                    }
                    scope.launch {
                        releases = withTimeoutOrNull(3_000L) {
                            withContext(Dispatchers.IO) { GitHubManager.getReleases(context, repo.owner, repo.name) }
                        }.orEmpty()
                    }
                }
            }
        }
        RepoTab.CODE_SEARCH -> { /* searches on demand */ }
        RepoTab.TIME_TRAVEL -> { /* loaded dynamically */ }
        RepoTab.TELEMETRY -> { /* loaded dynamically */ }
    }; loading = false }

    if (showIssueEvents) {
        IssueEventsScreen(
            repo = repo,
            onBack = { showIssueEvents = false },
            onOpenIssue = { number, title ->
                showIssueEvents = false
                selectedIssue = GHIssue(
                    number = number,
                    title = title,
                    state = "",
                    author = "",
                    createdAt = "",
                    comments = 0,
                    isPR = false
                )
            },
        )
        return
    }
    if (showRepoInsights) { RepoInsightsScreen(repo) { showRepoInsights = false }; return }
    if (showGitDataTools) { GitDataToolsScreen(repo, canWrite = canWrite) { showGitDataTools = false }; return }
    if (selectedIssue != null) { IssueDetailScreen(repo, selectedIssue!!.number) { selectedIssue = null }; return }
    if (selectedCommitSha != null) { 
        CommitDiffScreen(repo.owner, repo.name, selectedCommitSha!!) { selectedCommitSha = null }; 
        return 
    }
    if (selectedPRNumber != null) {
        PullRequestDiffScreen(
            repoOwner = repo.owner,
            repoName = repo.name,
            pullNumber = selectedPRNumber!!,
            onBack = { selectedPRNumber = null }
        )
        return
    }
    if (selectedPullNumber != null) {
        PullRequestDetailScreen(
            repo = repo,
            pullNumber = selectedPullNumber!!,
            onBack = { selectedPullNumber = null },
            onOpenFiles = { selectedPRNumber = it },
            onNavigateToCode = navigateToCode
        )
        return
    }
    if (selectedRunId != null) {
        // B — wire onOpenAiAgent through so the run-detail screen can
        // ask the AI Agent to look at a failed run. Composable receiver
        // is responsible for opening the agent on a prefilled prompt.
        WorkflowRunDetailScreen(
            repo = repo,
            runId = selectedRunId!!,
            onSuggestFix = { prompt ->
                copilotPrompt = prompt
                showCopilotChat = true
            },
            onBack = { selectedRunId = null },
            onNavigateToCode = navigateToCode
        )
        return
    }
    if (showRepoSettings) {
        if (canAdmin) {
            RepoSettingsScreen(
                repoOwner = repo.owner,
                repoName = repo.name,
                onBack = { showRepoSettings = false },
                onBranchProtection = { openRepoSettingsChild { showBranchProtection = true } },
                onCollaborators = { openRepoSettingsChild { showCollaborators = true } },
                onTeams = { openRepoSettingsChild { showTeams = true } },
                onWebhooks = { openRepoSettingsChild { showWebhooks = true } },
                onDiscussions = { openRepoSettingsChild { showDiscussions = true } },
                onRulesets = { openRepoSettingsChild { showRulesets = true } },
                onSecurity = { openRepoSettingsChild { showSecurity = true } },
                onAutolinks = { openRepoSettingsChild { showAutolinks = true } },
                onLfs = { openRepoSettingsChild { showLfs = true } },
                onInteractionLimits = { openRepoSettingsChild { showInteractionLimits = true } },
                onDeleteRepo = { onBack() },
            )
        } else {
            GitHubAdminRequiredScreen(title = "> settings", repoFullName = repo.fullName) { showRepoSettings = false }
        }
        return
    }
    if (showBranchProtection) { if (canAdmin) BranchProtectionScreen(repoOwner = repo.owner, repoName = repo.name, branches = branches, onBack = { closeRepoSettingsChild { showBranchProtection = false } }) else GitHubAdminRequiredScreen(title = "> branch protection", repoFullName = repo.fullName) { closeRepoSettingsChild { showBranchProtection = false } } ; return }
    if (showCollaborators) { if (canAdmin) CollaboratorsScreen(repoOwner = repo.owner, repoName = repo.name) { closeRepoSettingsChild { showCollaborators = false } } else GitHubAdminRequiredScreen(title = "> collaborators", repoFullName = repo.fullName) { closeRepoSettingsChild { showCollaborators = false } }; return }
    if (showTeams) { if (canAdmin) RepoTeamsScreen(repoOwner = repo.owner, repoName = repo.name) { closeRepoSettingsChild { showTeams = false } } else GitHubAdminRequiredScreen(title = "> teams", repoFullName = repo.fullName) { closeRepoSettingsChild { showTeams = false } }; return }
    if (showCompare) { CompareCommitsScreen(repoOwner = repo.owner, repoName = repo.name, initialBase = selectedBranch) { showCompare = false }; return }
    if (showWebhooks) { WebhooksScreen(repoOwner = repo.owner, repoName = repo.name, canAdmin = canAdmin) { closeRepoSettingsChild { showWebhooks = false } }; return }
    if (showDiscussions) { DiscussionsScreen(repoOwner = repo.owner, repoName = repo.name, canWrite = canWrite) { closeRepoSettingsChild { showDiscussions = false } }; return }
    if (showRulesets) { if (canAdmin) RulesetsScreen(repoOwner = repo.owner, repoName = repo.name) { closeRepoSettingsChild { showRulesets = false } } else GitHubAdminRequiredScreen(title = "> rulesets", repoFullName = repo.fullName) { closeRepoSettingsChild { showRulesets = false } }; return }
    if (showSecurity) { if (canAdmin) SecurityScreen(repoOwner = repo.owner, repoName = repo.name) { closeRepoSettingsChild { showSecurity = false } } else GitHubAdminRequiredScreen(title = "> security", repoFullName = repo.fullName) { closeRepoSettingsChild { showSecurity = false } }; return }
    if (showAutolinks) { GitHubScreenFrame(title = "> autolinks", onBack = { closeRepoSettingsChild { showAutolinks = false } }) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) { AutolinksPanel(owner = repo.owner, repo = repo.name) } }; return }
    if (showLfs) { GitHubScreenFrame(title = "> git lfs", onBack = { closeRepoSettingsChild { showLfs = false } }) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) { LfsPanel(owner = repo.owner, repo = repo.name) } }; return }
    if (showInteractionLimits) { GitHubScreenFrame(title = "> interaction limits", onBack = { closeRepoSettingsChild { showInteractionLimits = false } }) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) { InteractionLimitsPanel(owner = repo.owner, repo = repo.name) } }; return }
    if (showBuilds) { BuildsScreen(repo = repo, branches = branches, workflows = workflows, selectedBranch = repo.defaultBranch, onRunSelected = { selectedRunId = it }, onBack = { showBuilds = false }); return }
    if (showActionsTroubleshoot) {
        GitHubActionsTroubleshootScreen(
            repo = repo,
            onBack = { showActionsTroubleshoot = false },
            onOpenRun = {
                selectedRunId = it
                showActionsTroubleshoot = false
            },
            onSuggestFix = { prompt ->
                copilotPrompt = prompt
                showCopilotChat = true
            }
        )
        return
    }
    
    // File editor screen
    val safeEditingFile = editingFile
    val safeFileContent = fileContent
    if (safeEditingFile != null && safeFileContent != null) {
        gs.git.vps.ui.screens.CodeEditorScreen(
            repoOwner = repo.owner,
            repoName = repo.name,
            file = safeEditingFile,
            branch = selectedBranch,
            initialContent = safeFileContent,
            initialLine = editorInitialLine,
            onBack = { 
                editingFile = null
                fileContent = null
                editorInitialLine = null
                scope.launch { contents = GitHubManager.getRepoContents(context, repo.owner, repo.name, currentPath, selectedBranch) }
            },
            onAskAi = onOpenAiAgent?.let { open ->
                { customPrompt ->
                    val prompt = customPrompt ?: "Look at the file `${safeEditingFile.path}` on branch `$selectedBranch` and explain what it does."
                    open(repo.fullName, selectedBranch, prompt)
                }
            }
        )
        return
    }
    if (safeEditingFile != null) {
        LaunchedEffect(safeEditingFile.path, selectedBranch) {
            fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, safeEditingFile.path, selectedBranch)
        }
        AiModuleSurface {
            val loadingPalette = AiModuleTheme.colors
            Column(Modifier.fillMaxSize().background(loadingPalette.background)) {
                GitHubPageBar(
                    title = "> ${safeEditingFile.name}",
                    subtitle = "loading editor…",
                    onBack = {
                        editingFile = null
                        fileContent = null
                    },
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
            }
        }
        return
    }
    
    // Blame screen
    val safeBlameFile = showBlameFor
    if (safeBlameFile != null) {
        BlameViewScreen(
            repo = repo,
            file = safeBlameFile,
            branch = selectedBranch,
            onBack = { showBlameFor = null },
            onCommitClick = { sha -> selectedCommitSha = sha; showBlameFor = null }
        )
        return
    }

    // File History screen
    val safeHistoryFile = showFileHistoryFor
    if (safeHistoryFile != null) {
        FileHistoryScreen(
            repo = repo,
            file = safeHistoryFile,
            branch = selectedBranch,
            onBack = { showFileHistoryFor = null },
            onCommitClick = { sha -> selectedCommitSha = sha; showFileHistoryFor = null }
        )
        return
    }

    // Releases screen
    if (selectedTab == RepoTab.RELEASES) {
        gs.git.vps.ui.screens.ReleasesScreen(
            repoOwner = repo.owner,
            repoName = repo.name,
            defaultBranch = repo.defaultBranch,
            canWrite = canWrite,
            onBack = { selectedTab = RepoTab.FILES },
            onReleaseClick = { /* optional */ }
        )
        return
    }
    
    val safeOpenedFile = openedFile
    if (safeOpenedFile != null && safeFileContent == null) {
        LaunchedEffect(safeOpenedFile.path, selectedBranch) {
            fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, safeOpenedFile.path, selectedBranch)
        }
        AiModuleSurface {
            Column(Modifier.fillMaxSize().background(AiModuleTheme.colors.background)) {
                GitHubPageBar(
                    title = "> ${safeOpenedFile.name}",
                    subtitle = "loading…",
                    onBack = { openedFile = null; fileContent = null },
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading file…")
                }
            }
        }
        return
    }
    if (safeFileContent != null && safeOpenedFile != null) {
        val ext = safeOpenedFile.name.substringAfterLast(".", "").lowercase()
        val isImage = ext in listOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico")
        val isMd = ext in listOf("md", "markdown")
        val cachedLines = remember(safeFileContent) { safeFileContent.lines() }
        AiModuleSurface {
        val viewerPalette = AiModuleTheme.colors
        Column(Modifier.fillMaxSize().background(viewerPalette.background)) {
            GitHubPageBar(
                title = "> ${safeOpenedFile.name}",
                subtitle = if (ext.isNotBlank()) "${cachedLines.size} lines · ${safeFileContent.length} chars · ${ext.uppercase()}" else "${cachedLines.size} lines · ${safeFileContent.length} chars",
                onBack = {
                    fileContent = null
                    openedFile = null
                },
                trailing = {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.COPY,
                        onClick = {
                            val clip = android.content.ClipData.newPlainText("code", safeFileContent)
                            (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                        },
                        tint = viewerPalette.textSecondary,
                        fontSize = 12.sp,
                        contentDescription = "copy",
                    )
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.DOWNLOAD,
                        onClick = {
                            scope.launch {
                                val dest = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                    "GlassFiles_Git/${safeOpenedFile.name}"
                                )
                                val ok = GitHubManager.downloadFile(
                                    context, repo.owner, repo.name, safeOpenedFile.path, dest, selectedBranch
                                )
                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            }
                        },
                        tint = viewerPalette.textSecondary,
                        fontSize = 12.sp,
                        contentDescription = "download",
                    )
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.EDIT,
                        onClick = { editingFile = safeOpenedFile },
                        tint = viewerPalette.accent,
                        contentDescription = "edit",
                    )
                    AiModuleGlyphAction(
                        glyph = "B",
                        onClick = { showBlameFor = safeOpenedFile },
                        tint = viewerPalette.textSecondary,
                        fontSize = 12.sp,
                        contentDescription = "blame",
                    )
                    AiModuleGlyphAction(
                        glyph = "H",
                        onClick = { showFileHistoryFor = safeOpenedFile },
                        tint = viewerPalette.textSecondary,
                        fontSize = 12.sp,
                        contentDescription = "history",
                    )
                },
            )
            if (isImage) {
                var imgScale by remember { mutableFloatStateOf(1f) }
                var imgOffset by remember { mutableStateOf(Offset.Zero) }
                val imgState = rememberTransformableState { zoomChange, panChange, _ ->
                    imgScale = (imgScale * zoomChange).coerceIn(0.75f, 6f)
                    imgOffset += panChange
                }
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    if (safeOpenedFile.downloadUrl.isBlank()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Image, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(34.dp))
                            Text("Image preview unavailable", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    } else {
                        AsyncImage(
                            model = safeOpenedFile.downloadUrl,
                            contentDescription = safeOpenedFile.name,
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = imgScale, scaleY = imgScale,
                                translationX = imgOffset.x, translationY = imgOffset.y
                            ).transformable(imgState)
                        )
                    }
                    Row(
                        Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(GitHubControlRadius)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("IMAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
                        Text(safeOpenedFile.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.ZoomOutMap, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }
            } else if (isMd) {
                var mdHtml by remember(safeFileContent) { mutableStateOf("") }
                var mdLoading by remember(safeFileContent) { mutableStateOf(true) }
                LaunchedEffect(safeFileContent) {
                    mdHtml = GitHubManager.renderMarkdown(context, safeFileContent, "gfm", "${repo.owner}/${repo.name}")
                    mdLoading = false
                }
                val onMdLinkClick: (String) -> Unit = { url ->
                    if (url.isNotBlank() && !url.startsWith("#")) {
                        val resolved = resolveReadmeLink(url, repo)
                        if (resolved != null) {
                            scope.launch {
                                val dir = resolved.substringBeforeLast("/", "")
                                val list = GitHubManager.getRepoContents(context, repo.owner, repo.name, dir.ifEmpty { "/" }, selectedBranch)
                                val f = list.find { it.path == resolved || it.name == resolved.substringAfterLast("/") }
                                if (f != null) { openedFile = f; fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, f.path, selectedBranch) }
                                else { openedFile = GHContent(resolved.substringAfterLast("/"), resolved, "file", 0L, "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/${selectedBranch}/$resolved", ""); fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, resolved, selectedBranch) }
                            }
                        } else context.openReadmeUrl(url)
                    }
                }
                Box(Modifier.fillMaxSize().background(viewerPalette.background)) {
                    if (mdLoading) {
                        AiModuleSpinner(label = "rendering…")
                    } else if (mdHtml.isNotBlank()) {
                        ReadmeHtmlDocument(html = mdHtml, repo = repo, modifier = Modifier.fillMaxSize(), onNavigateLink = onMdLinkClick)
                    } else {
                        Text("No renderable content.", fontSize = 15.sp, color = viewerPalette.textMuted, lineHeight = 22.sp, modifier = Modifier.padding(22.dp))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(start = 4.dp, end = 4.dp, top = 4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(cachedLines.size) { idx ->
                        Row(Modifier.fillMaxWidth()) {
                            Text("${idx + 1}".padStart(4), fontSize = 11.sp, fontFamily = JetBrainsMono, color = viewerPalette.textMuted, modifier = Modifier.padding(end = 10.dp))
                            Text(highlightLine(cachedLines[idx], ext), fontSize = 12.sp, fontFamily = JetBrainsMono, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
        }; return
    }
    val filteredContents = remember(contents, repoQuery) {
        if (repoQuery.isBlank()) contents else contents.filter { it.name.contains(repoQuery, ignoreCase = true) || it.path.contains(repoQuery, ignoreCase = true) }
    }
    val filteredCommits = remember(commits, repoQuery) {
        if (repoQuery.isBlank()) commits else commits.filter {
            it.message.contains(repoQuery, ignoreCase = true) || it.author.contains(repoQuery, ignoreCase = true) || it.sha.contains(repoQuery, ignoreCase = true)
        }
    }
    val filteredIssues = remember(issues, repoQuery) {
        if (repoQuery.isBlank()) issues else issues.filter {
            it.title.contains(repoQuery, ignoreCase = true) || it.author.contains(repoQuery, ignoreCase = true) || it.number.toString().contains(repoQuery)
        }
    }
    val filteredPulls = remember(pulls, repoQuery) {
        if (repoQuery.isBlank()) pulls else pulls.filter {
            it.title.contains(repoQuery, ignoreCase = true) || it.author.contains(repoQuery, ignoreCase = true) || it.number.toString().contains(repoQuery)
        }
    }


    AiModuleSurface {
    val palette = AiModuleTheme.colors
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(palette.background)) {
        GitHubPageBar(
            title = "> ${repo.name}",
            subtitle = if (currentPath.isNotBlank()) "${repo.fullName} \u00B7 $currentPath" else repo.fullName,
            onBack = ::handleRepoBack,
            trailing = {
                GitHubTopBarAction(
                    glyph = GhGlyphs.AI,
                    onClick = {
                        copilotPrompt = null
                        showCopilotChat = true
                    },
                    tint = palette.accent,
                    contentDescription = "copilot chat",
                )
                GitHubTopBarAction(
                    glyph = GhGlyphs.REFRESH,
                    onClick = { repoReloadNonce++ },
                    tint = palette.accent,
                    contentDescription = "refresh",
                )
                GitHubTopBarAction(
                    glyph = GhGlyphs.MORE,
                    onClick = { showRepoOverflow = !showRepoOverflow },
                    tint = palette.accent,
                    contentDescription = "more",
                )
            },
        )
        if (showRepoOverflow) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .border(1.dp, palette.border)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GitHubTerminalButton(if (isStarred) "\u2605 unstar" else "\u2606 star", {
                        scope.launch {
                            if (isStarred) GitHubManager.unstarRepo(context, repo.owner, repo.name)
                            else GitHubManager.starRepo(context, repo.owner, repo.name)
                            isStarred = !isStarred
                            showRepoOverflow = false
                        }
                    }, color = if (isStarred) palette.warning else palette.textSecondary)
                    GitHubTerminalButton(if (isWatching) "\u25C9 unwatch" else "\u25CE watch", {
                        scope.launch {
                            val ok = if (isWatching) GitHubManager.unwatchRepo(context, repo.owner, repo.name)
                            else GitHubManager.watchRepo(context, repo.owner, repo.name)
                            if (ok) isWatching = !isWatching
                            showRepoOverflow = false
                        }
                    }, color = palette.textSecondary)
                    GitHubTerminalButton("\u2442 fork", {
                        scope.launch {
                            val ok = GitHubManager.forkRepo(context, repo.owner, repo.name)
                            Toast.makeText(context, if (ok) Strings.ghForked else Strings.error, Toast.LENGTH_SHORT).show()
                            showRepoOverflow = false
                        }
                    }, color = palette.accent)
                    GitHubTerminalButton("\u2193 download zip", {
                        cloneProgress = "starting"
                        scope.launch {
                            val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_Git")
                            val ok = GitHubManager.cloneRepo(context, repo.owner, repo.name, dest) { cloneProgress = it.lowercase() }
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            cloneProgress = null
                            showRepoOverflow = false
                        }
                    }, color = palette.accent)
                    GitHubTerminalButton("\u2197 share", {
                        val repoUrl = "https://github.com/${repo.owner}/${repo.name}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, repoUrl)
                            putExtra(Intent.EXTRA_SUBJECT, repo.fullName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share repo link").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        showRepoOverflow = false
                    }, color = palette.textSecondary)
                    GitHubTerminalButton("\u2398 copy link", {
                        val repoUrl = "https://github.com/${repo.owner}/${repo.name}"
                        val clip = android.content.ClipData.newPlainText("repo-url", repoUrl)
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                        Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
                        showRepoOverflow = false
                    }, color = palette.textSecondary)
                    if (onMinimize != null) {
                        GitHubTerminalButton("\u229F floating window", {
                            showRepoOverflow = false
                            onMinimize()
                        }, color = palette.textSecondary)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canAdmin) {
                        GitHubTerminalButton("settings", {
                            showRepoOverflow = false
                            showRepoSettings = true
                        }, color = palette.textSecondary)
                    } else if (repo.permissions != null) {
                        GitHubTerminalButton("settings", {}, color = palette.textMuted, enabled = false)
                        GitHubPermissionHint("admin required")
                    }
                    GitHubTerminalButton("compare", {
                        showRepoOverflow = false
                        showCompare = true
                    }, color = palette.accent)
                    GitHubTerminalButton("insights", {
                        showRepoOverflow = false
                        showRepoInsights = true
                    }, color = palette.accent)
                    GitHubTerminalButton("git data", {
                        showRepoOverflow = false
                        showGitDataTools = true
                    }, color = palette.textSecondary)
                    if (onClose != null) {
                        GitHubTerminalButton("\u00D7 close repo", {
                            showRepoOverflow = false
                            onClose()
                        }, color = palette.error)
                    }
                }
            }
        }
        if (!canWrite && repo.permissions != null) {
            Row(
                Modifier.fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "! read-only · permission=${githubRepoPermissionLabel(repo)} · write/admin actions are hidden",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
            }
        }
        if (cloneProgress != null) {
            Row(
                Modifier.fillMaxWidth()
                    .background(palette.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "\u25B8 cloning: ${cloneProgress!!}",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
            }
        }
        // Branch + actions
        Row(
            Modifier.fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.accent.copy(alpha = 0.10f))
                    .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
                    .clickable { showBranchPicker = true }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "\u2442 ${selectedBranch} \u25BE",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            when (selectedTab) {
                RepoTab.FILES -> if (canWrite) {
                    AiModulePillButton(label = "+ file", onClick = { showCreateFile = true })
                    AiModulePillButton(label = "upload \u2191", onClick = { showUpload = true })
                } else if (repo.permissions != null) {
                    GitHubPermissionHint("write required")
                }
                RepoTab.ISSUES -> {
                    GitHubTerminalButton("events", onClick = { showIssueEvents = true }, color = palette.textSecondary)
                    if (canWrite || repo.permissions == null) {
                        GitHubTerminalButton("+ issue", onClick = { showCreateIssue = true }, color = palette.accent)
                    } else {
                        GitHubPermissionHint("write required")
                    }
                }
                RepoTab.PULLS -> if (canWrite || repo.permissions == null) {
                    AiModulePillButton(label = "+ pr", onClick = { showCreatePR = true })
                } else {
                    GitHubPermissionHint("write required")
                }
                RepoTab.ACTIONS -> if (canWrite) {
                    GitHubTerminalButton("troubleshoot", onClick = { showActionsTroubleshoot = true }, color = palette.textSecondary)
                    AiModulePillButton(label = "run \u25B6", onClick = { showDispatch = true })
                } else {
                    GitHubTerminalButton("troubleshoot", onClick = { showActionsTroubleshoot = true }, color = palette.textSecondary)
                    if (repo.permissions != null) GitHubPermissionHint("write required")
                }
                else -> {}
            }
        }
        // Tabs
        Row(
            Modifier.fillMaxWidth()
                .background(palette.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RepoTab.entries.forEach { tab ->
                val sel = selectedTab == tab
                val label = when (tab) {
                    RepoTab.FILES -> "files"
                    RepoTab.COMMITS -> "commits"
                    RepoTab.ISSUES -> "issues"
                    RepoTab.PULLS -> "pulls"
                    RepoTab.RELEASES -> "releases"
                    RepoTab.ACTIONS -> "actions"
                    RepoTab.HISTORY -> "history"
                    RepoTab.PROJECTS -> "projects"
                    RepoTab.README -> "readme"
                    RepoTab.CODE_SEARCH -> "search"
                    RepoTab.TIME_TRAVEL -> "time travel"
                    RepoTab.TELEMETRY -> "telemetry"
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (sel) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
                        .border(1.dp, if (sel) palette.accent.copy(alpha = 0.55f) else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { selectedTab = tab; repoQuery = "" }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        label,
                        color = if (sel) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        if (selectedTab in listOf(RepoTab.FILES, RepoTab.COMMITS, RepoTab.ISSUES, RepoTab.PULLS)) {
            val palette = AiModuleTheme.colors
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "> ",
                        color = palette.textMuted,
                        fontSize = 13.sp,
                        fontFamily = JetBrainsMono,
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (repoQuery.isEmpty()) {
                            Text(
                                when (selectedTab) {
                                    RepoTab.FILES -> "filter files"
                                    RepoTab.COMMITS -> "filter commits"
                                    RepoTab.ISSUES -> "filter issues"
                                    RepoTab.PULLS -> "filter pull requests"
                                    else -> ""
                                },
                                color = palette.textMuted,
                                fontSize = 13.sp,
                                fontFamily = JetBrainsMono,
                            )
                        }
                        BasicTextField(
                            value = repoQuery,
                            onValueChange = { repoQuery = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = JetBrainsMono,
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (repoQuery.isNotBlank()) {
                        Text(
                            "[x]",
                            color = palette.textMuted,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMono,
                            modifier = Modifier.clickable { repoQuery = "" }.padding(start = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                AiModuleHairline()
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.10f)))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") }
        else when (selectedTab) {
            RepoTab.FILES -> FilesTab(
                rootContents = filteredContents,
                childCache = childCache,
                expandedPaths = expandedPaths,
                loadingPaths = loadingPaths,
                listState = filesListState,
                canWrite = canWrite,
                onToggleDir = { item ->
                    if (item.path in expandedPaths) {
                        expandedPaths = expandedPaths - item.path
                    } else {
                        expandedPaths = expandedPaths + item.path
                        if (item.path !in childCache && item.path !in loadingPaths) {
                            loadingPaths = loadingPaths + item.path
                            scope.launch {
                                val children = GitHubManager.getRepoContents(context, repo.owner, repo.name, item.path, selectedBranch)
                                childCache[item.path] = children
                                loadingPaths = loadingPaths - item.path
                            }
                        }
                    }
                },
                onOpenDir = { currentPath = it.path },
                onFileClick = { scope.launch { openedFile = it; fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, it.path, selectedBranch) } },
                onEdit = { openedFile = null; fileContent = null; editingFile = it },
                onDelete = { deleteTarget = it },
                onDownload = { scope.launch { val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_Git/${it.name}"); val ok = GitHubManager.downloadFile(context, repo.owner, repo.name, it.path, dest, selectedBranch); Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show() } },
                onCopyPath = { item ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("path", item.path))
                    Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                },
            )
            RepoTab.COMMITS -> CommitsTab(filteredCommits, commitsHasMore, { scope.launch { commitsPage++; val r = GitHubManager.getCommits(context, repo.owner, repo.name, commitsPage); if (r.size < 30) commitsHasMore = false; commits = commits + r } }, listState = commitsListState, onClick = { selectedCommitSha = it.sha }, onExploreFiles = { selectedBranch = it; selectedTab = RepoTab.FILES })
            RepoTab.ISSUES -> IssuesTab(filteredIssues, issuesHasMore, { scope.launch { issuesPage++; val r = GitHubManager.getIssues(context, repo.owner, repo.name, page = issuesPage); if (r.size < 30) issuesHasMore = false; issues = issues + r } }, listState = issuesListState) { selectedIssue = it }
            RepoTab.PULLS -> PullsTab(
                pulls = filteredPulls,
                repo = repo,
                onRefresh = { scope.launch { pulls = GitHubManager.getPullRequests(context, repo.owner, repo.name) } },
                listState = pullsListState,
                onOpenDetail = { selectedPullNumber = it.number },
                onFilesClick = { prNumber -> selectedPRNumber = prNumber },
                onNavigateToCode = navigateToCode
            )
            RepoTab.RELEASES -> ReleasesTab(releases, repo)
            RepoTab.ACTIONS -> ActionsTab(workflowRuns, repo, { selectedRunId = it.id }, onShowBuilds = { showBuilds = true })
            RepoTab.HISTORY -> ActionsHistoryTab(workflowRuns, repo) { selectedRunId = it.id }
            RepoTab.PROJECTS -> ProjectsTab(repo)
            RepoTab.README -> ReadmeTab(readme, readmeHtml, readmeBlocks, readmeError, languages, contributors, releases, repo, { readmeReloadNonce++ }, onOpenFile = { path ->
                scope.launch {
                    val dir = path.substringBeforeLast("/", "")
                    val list = GitHubManager.getRepoContents(context, repo.owner, repo.name, dir.ifEmpty { "/" }, selectedBranch)
                    val file = list.find { it.path == path || it.name == path.substringAfterLast("/") }
                    if (file != null) {
                        openedFile = file
                        fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, file.path, selectedBranch)
                    } else {
                        val name = path.substringAfterLast("/")
                        openedFile = GHContent(name, path, "file", 0L,
                            "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/${selectedBranch}/$path", "")
                        fileContent = GitHubManager.getFileContent(context, repo.owner, repo.name, path, selectedBranch)
                    }
                }
            })
            RepoTab.CODE_SEARCH -> CodeSearchTab(repo)
            RepoTab.TIME_TRAVEL -> TimeTravelTab(
                repo = repo,
                selectedBranch = selectedBranch,
                onOpenFileWithContent = { path, content ->
                    val name = path.substringAfterLast("/")
                    val tempContent = GHContent(
                        name = name,
                        path = path,
                        type = "file",
                        size = content.length.toLong(),
                        downloadUrl = "",
                        sha = ""
                    )
                    editingFile = tempContent
                    fileContent = content
                }
            )
            RepoTab.TELEMETRY -> TelemetryTab(repo, commits)
        }
    }

        AnimatedVisibility(
            visible = showCopilotChat,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            CopilotChatPanel(
                palette = palette,
                filePath = if (currentPath.isNotBlank()) currentPath else "repository root",
                branch = selectedBranch,
                selectedText = "",
                initialPrompt = copilotPrompt,
                onClose = { showCopilotChat = false },
                onApplyCode = { codeSnippet ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Copilot Code", codeSnippet))
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    showCopilotChat = false
                }
            )
        }
    }
    if (showUpload) UploadDialog(repo, currentPath, selectedBranch, { showUpload = false }) { showUpload = false; scope.launch { contents = GitHubManager.getRepoContents(context, repo.owner, repo.name, currentPath, selectedBranch) } }
    if (showCreateFile) CreateFileDialog(repo, currentPath, selectedBranch, { showCreateFile = false }) { showCreateFile = false; scope.launch { contents = GitHubManager.getRepoContents(context, repo.owner, repo.name, currentPath, selectedBranch) } }
    if (showCreateBranch) CreateBranchDialog(repo, branches, { showCreateBranch = false }) { showCreateBranch = false; scope.launch { branches = GitHubManager.getBranches(context, repo.owner, repo.name) } }
    if (showCreateIssue) CreateIssueDialog(repo, { showCreateIssue = false }) { showCreateIssue = false; scope.launch { issues = GitHubManager.getIssues(context, repo.owner, repo.name) } }
    if (showCreatePR) CreatePRDialog(repo, branches, { showCreatePR = false }) { showCreatePR = false; scope.launch { pulls = GitHubManager.getPullRequests(context, repo.owner, repo.name) } }
    if (deleteTarget != null) DeleteFileDialog(repo, deleteTarget!!, selectedBranch, { deleteTarget = null }) { deleteTarget = null; scope.launch { contents = GitHubManager.getRepoContents(context, repo.owner, repo.name, currentPath, selectedBranch) } }
    if (showBranchPicker) BranchPickerDialog(branches, selectedBranch, canWrite, { selectedBranch = it; showBranchPicker = false }, { showBranchPicker = false }) { showBranchPicker = false; showCreateBranch = true }
    if (showDispatch && workflows.isNotEmpty()) DispatchWorkflowDialog(repo, workflows, branches, { showDispatch = false }) { showDispatch = false; scope.launch { workflowRuns = GitHubManager.getWorkflowRuns(context, repo.owner, repo.name) } }
    }
}

@Composable
private fun GitHubAdminRequiredScreen(title: String, repoFullName: String, onBack: () -> Unit) {
    GitHubScreenFrame(
        title = title,
        subtitle = repoFullName,
        onBack = onBack,
    ) {
        val palette = AiModuleTheme.colors
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "! admin access required",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "This repository is read-only for the current token. Admin-only GitHub endpoints are hidden to avoid 403 responses.",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                GitHubTerminalButton("back", onClick = onBack, color = palette.textSecondary)
            }
        }
    }
}

private data class FilesTreeRow(
    val key: String,
    val item: GHContent?,
    val depth: Int,
    val parentLines: List<Boolean>,
    val isLastInParent: Boolean,
    val isLoading: Boolean,
)

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
private fun IssueEventsScreen(
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

internal enum class RepoInsightsTab { TRAFFIC, PEOPLE, EVENTS }

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
private fun PullRequestDetailScreen(
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
    // One-shot AI summary state. Populated by the chip in the action
    // row below; nulled out when the dialog is dismissed.
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiSummaryLoading by remember { mutableStateOf(false) }
    var aiSummaryError by remember { mutableStateOf<String?>(null) }
    var aiSummaryShown by remember { mutableStateOf(false) }

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
            aiSummaryShown -> aiSummaryShown = false
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
                    GitHubTerminalButton(
                        label = if (aiSummaryLoading) "loading summary" else "ai summary",
                        color = Blue,
                        enabled = !aiSummaryLoading,
                        onClick = {
                            if (aiSummaryLoading) return@GitHubTerminalButton
                            aiSummaryShown = true
                            aiSummaryError = null
                            if (aiSummary == null) {
                                aiSummaryLoading = true
                                scope.launch {
                                    try {
                                        aiSummary = generatePullRequestSummary(
                                            context = context,
                                            pr = current,
                                            files = files,
                                        )
                                    } catch (e: Exception) {
                                        aiSummaryError = e.message ?: e.javaClass.simpleName
                                    } finally {
                                        aiSummaryLoading = false
                                    }
                                }
                            }
                        },
                    )
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val rcContext = LocalContext.current
                            val rcScope = rememberCoroutineScope()
                            var rcReactions by remember { mutableStateOf<List<GHReaction>>(emptyList()) }
                            LaunchedEffect(c.id) { rcReactions = GitHubManager.getPullRequestReviewCommentReactions(rcContext, repo.owner, repo.name, c.id) }
                            rcReactions.groupBy { it.content }.forEach { (emoji, reacts) ->
                                AiModulePillButton(label = "$emoji ${reacts.size}", onClick = {}, accent = false)
                            }
                            AiModulePillButton(label = "+react", onClick = {
                                rcScope.launch {
                                    val emojis = listOf("+1", "-1", "laugh", "hooray", "rocket", "heart", "eyes")
                                    val picked = emojis.first()
                                    GitHubManager.addPullRequestReviewCommentReaction(rcContext, repo.owner, repo.name, c.id, picked)
                                    rcReactions = GitHubManager.getPullRequestReviewCommentReactions(rcContext, repo.owner, repo.name, c.id)
                                }
                            }, accent = true)
                            AiModulePillButton(label = "edit", onClick = {
                                rcScope.launch {
                                    val newBody = c.body
                                    if (newBody.isNotBlank()) {
                                        GitHubManager.updatePullRequestReviewComment(rcContext, repo.owner, repo.name, c.id, newBody)
                                    }
                                }
                            })
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
    if (aiSummaryShown) {
        AiPullSummaryDialog(
            loading = aiSummaryLoading,
            summary = aiSummary,
            error = aiSummaryError,
            onRegenerate = {
                if (current == null) return@AiPullSummaryDialog
                aiSummaryLoading = true
                aiSummary = null
                aiSummaryError = null
                scope.launch {
                    try {
                        aiSummary = generatePullRequestSummary(
                            context = context,
                            pr = current,
                            files = files,
                        )
                    } catch (e: Exception) {
                        aiSummaryError = e.message ?: e.javaClass.simpleName
                    } finally {
                        aiSummaryLoading = false
                    }
                }
            },
            onDismiss = { aiSummaryShown = false },
        )
    }
}

/**
 * One-shot prompt builder for the "AI summary" chip on the PR detail
 * screen. Sends the PR's title / description / head & base / commit
 * count / changed-file list (with patches truncated for long diffs)
 * to the picked model and asks for a concise three-bullet TL;DR.
 */
private suspend fun generatePullRequestSummary(
    context: Context,
    pr: GHPullRequest,
    files: List<GHPullFile>,
): String {
    val payload = buildString {
        appendLine("Title: ${pr.title}")
        appendLine("State: ${pr.state}${if (pr.draft) " (draft)" else ""}${if (pr.merged) " (merged)" else ""}")
        appendLine("Author: ${pr.author}")
        appendLine("Branches: ${pr.head} -> ${pr.base}")
        appendLine("Stats: ${pr.commits} commits, +${pr.additions}/-${pr.deletions}, ${pr.changedFiles} files")
        if (pr.body.isNotBlank()) {
            appendLine()
            appendLine("Description:")
            appendLine(pr.body.take(2000))
        }
        if (files.isNotEmpty()) {
            appendLine()
            appendLine("Changed files (${files.size}):")
            // 12 files * 600 char patch = ~7k tokens of context max.
            // Beyond that the model can't keep it all in mind anyway.
            files.take(12).forEach { f ->
                append("- ${f.filename}  (+${f.additions} -${f.deletions})")
                if (f.patch.isNotBlank()) {
                    appendLine()
                    appendLine(f.patch.take(600))
                } else {
                    appendLine()
                }
            }
            if (files.size > 12) appendLine("[…${files.size - 12} more files omitted]")
        }
    }
    val systemPrompt =
        "You are a code-review assistant. Given the metadata and diff of a GitHub pull request, " +
        "produce a concise summary that helps a reviewer decide whether to approve. " +
        "Use 3-5 short bullet points. First bullet is the one-line intent. " +
        "Mention notable risks or test gaps if visible from the diff. Plain text, no markdown headers."
    return ""
}

@Composable
private fun AiPullSummaryDialog(
    loading: Boolean,
    summary: String?,
    error: String?,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = Strings.aiSummary,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AiModuleGlyph(GhGlyphs.AI, Modifier.size(18.dp), tint = palette.accent, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .heightIn(min = 80.dp, max = 360.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            AiModuleSpinner()
                            Spacer(Modifier.width(10.dp))
                            AiModuleText(Strings.aiSummaryLoading, fontSize = 13.sp, color = palette.textSecondary)
                        }
                        error != null -> AiModuleText(
                            error,
                            fontSize = 13.sp,
                            color = palette.error,
                        )
                        summary != null -> AiModuleText(
                            summary,
                            fontSize = 13.sp,
                            color = palette.textPrimary,
                            lineHeight = 18.sp,
                        )
                        else -> AiModuleText(Strings.aiSummaryEmpty, fontSize = 13.sp, color = palette.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(label = Strings.close.lowercase(), onClick = onDismiss)
        },
        dismissButton = {
            AiModuleTextAction(label = Strings.aiSummaryRegenerate.lowercase(), onClick = onRegenerate, enabled = !loading)
        },
    )
}

@Composable
private fun PullBadge(text: String, color: Color) {
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
                r.assets.forEach { a -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(colors.background).clickable { scope.launch { val dest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_Git/${a.name}"); GitHubManager.downloadFile(context, repo.owner, repo.name, a.downloadUrl, dest) } }.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(releaseAssetIcon(a.name), null, Modifier.size(24.dp), tint = colors.accent.copy(alpha = 0.72f)); Column(Modifier.weight(1f)) { Text(a.name, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${ghFmtSize(a.size)} · ${formatGitHubNumber(a.downloadCount)} downloads", fontSize = 10.sp, color = colors.textMuted) }; Icon(Icons.Rounded.Download, null, Modifier.size(16.dp), tint = colors.textMuted) } }
            }
        }
    } } }
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

data class HeadingItem(val id: String, val title: String, val level: Int)

class ToCInterface(
    private val context: android.content.Context,
    private val onHeadingsParsed: (List<HeadingItem>) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun sendHeadings(jsonStr: String) {
        try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<HeadingItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(HeadingItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    level = obj.getInt("level")
                ))
            }
            onHeadingsParsed(list)
        } catch (e: Exception) {
            android.util.Log.e("ToCInterface", "Error parsing headings", e)
        }
    }

    @android.webkit.JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Code Block", text)
            clipboard.setPrimaryClip(clip)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                android.widget.Toast.makeText(context, "Code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ToCInterface", "Error copying to clipboard", e)
        }
    }
}

internal object MarkdownCacheManager {
    fun get(context: android.content.Context, owner: String, repo: String, branch: String?): ReadmeFetchResult? {
        return try {
            val key = "${owner}_${repo}_${branch ?: "default"}"
            val file = java.io.File(context.cacheDir, "readme_cache/$key.json")
            if (file.exists()) {
                val json = org.json.JSONObject(file.readText())
                ReadmeFetchResult(
                    markdown = json.optString("markdown", ""),
                    renderedHtml = json.optString("renderedHtml", ""),
                    path = json.optString("path", "")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun put(context: android.content.Context, owner: String, repo: String, branch: String?, result: ReadmeFetchResult) {
        try {
            val key = "${owner}_${repo}_${branch ?: "default"}"
            val file = java.io.File(context.cacheDir, "readme_cache/$key.json")
            file.parentFile?.mkdirs()
            val json = org.json.JSONObject().apply {
                put("markdown", result.markdown)
                put("renderedHtml", result.renderedHtml)
                put("path", result.path)
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            // ignore
        }
    }
}

fun Color.toHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

