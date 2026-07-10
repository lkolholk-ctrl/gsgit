package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import coil.compose.AsyncImage
import gs.git.vps.R
import gs.git.vps.data.Strings
import gs.git.vps.util.DownloadStorage
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHActionSecret
import gs.git.vps.data.github.model.GHActionVariable
import gs.git.vps.data.github.model.GHCodespace
import gs.git.vps.data.github.model.GHEnvironment
import gs.git.vps.data.github.model.GHEnvironmentProtectionRule
import gs.git.vps.data.github.model.GHDeploymentBranchPolicy
import gs.git.vps.data.github.model.GHEnvironmentSecret
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.data.github.canWrite
import gs.git.vps.data.github.KernelErrorCatalog
import gs.git.vps.data.github.KernelErrorPatterns
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHActionRunner
import gs.git.vps.data.github.model.GHActionRunnerGroup
import gs.git.vps.data.github.model.GHActionsCacheEntry
import gs.git.vps.data.github.model.GHActionsCacheUsage
import gs.git.vps.data.github.model.GHActionsPermissions
import gs.git.vps.data.github.model.GHActionsRetention
import gs.git.vps.data.github.model.GHActionsUsage
import gs.git.vps.data.github.model.GHArtifact
import gs.git.vps.data.github.model.GHCheckAnnotation
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHDeployment
import gs.git.vps.data.github.model.GHJob
import gs.git.vps.data.github.model.GHPendingDeployment
import gs.git.vps.data.github.model.GHStep
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowDispatchInput
import gs.git.vps.data.github.model.GHWorkflowDispatchSchema
import gs.git.vps.data.github.model.GHWorkflowPermissions
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHWorkflowRunReview
import gs.git.vps.ui.theme.Blue
import gs.git.vps.ui.theme.Green
import gs.git.vps.ui.theme.Orange
import gs.git.vps.ui.theme.Purple
import gs.git.vps.ui.theme.Red
import gs.git.vps.ui.theme.Teal
import gs.git.vps.ui.components.AiModuleDestructiveButton
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.components.AiModulePrimaryButton
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.aiModuleStatusBadge
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.ui.theme.SeparatorColor
import gs.git.vps.ui.theme.SurfaceLight
import gs.git.vps.ui.theme.SurfaceWhite
import gs.git.vps.ui.theme.TextPrimary
import gs.git.vps.ui.theme.TextSecondary
import gs.git.vps.ui.theme.TextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Экран деталей workflow-рана: WorkflowRunDetailScreen + секции (summary/jobs/pipeline/
 * checks/artifacts), карточки job/step, лог-вьюер, диагностика падений, диалоги.
 * Вынесено из GitHubActionsModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun WorkflowRunDetailScreen(
    repo: GHRepo,
    runId: Long,
    onSuggestFix: ((prompt: String) -> Unit)? = null,
    onBack: () -> Unit,
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobListState = rememberSaveable(runId, "jobs", saver = LazyListState.Saver) { LazyListState(0, 0) }
    var run by remember { mutableStateOf<GHWorkflowRun?>(null) }
    var jobs by remember { mutableStateOf<List<GHJob>>(emptyList()) }
    var artifacts by remember { mutableStateOf<List<GHArtifact>>(emptyList()) }
    var checkRuns by remember { mutableStateOf<List<GHCheckRun>>(emptyList()) }
    var pendingDeployments by remember { mutableStateOf<List<GHPendingDeployment>>(emptyList()) }
    var reviewHistory by remember { mutableStateOf<List<GHWorkflowRunReview>>(emptyList()) }
    var usage by remember { mutableStateOf<GHActionsUsage?>(null) }
    var selectedAttempt by remember { mutableStateOf<Int?>(null) }
    var maxAttempt by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf<Long?>(null) }
    var deletingArtifactId by remember { mutableStateOf<Long?>(null) }
    val jobLogs = remember { mutableStateMapOf<Long, String>() }
    val jobStepLogs = remember { mutableStateMapOf<Long, Map<Int, String>>() }
    val jobLogMeta = remember { mutableStateMapOf<Long, JobLogMeta>() }
    val checkAnnotations = remember { mutableStateMapOf<Long, List<GHCheckAnnotation>>() }
    var loadingJobId by remember { mutableStateOf<Long?>(null) }
    var expandedJobId by remember { mutableStateOf<Long?>(null) }
    var expandedStepKey by remember { mutableStateOf<String?>(null) }
    val expandedMatrixGroups = remember(runId) { mutableStateMapOf<String, Boolean>() }
    var onlyFailedJobs by rememberSaveable(runId) { mutableStateOf(false) }
    var onlyActiveJobs by rememberSaveable(runId) { mutableStateOf(false) }
    var loadedLogsFilter by rememberSaveable(runId, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var selectedSection by rememberSaveable(runId) { mutableStateOf(RunDetailSection.JOBS) }
    var refreshing by remember { mutableStateOf(false) }
    var metadataLoaded by remember { mutableStateOf(false) }
    var artifactsLoaded by remember { mutableStateOf(false) }
    var checksLoaded by remember { mutableStateOf(false) }
    var loadingMetadata by remember { mutableStateOf(false) }
    var loadingArtifacts by remember { mutableStateOf(false) }
    var loadingChecks by remember { mutableStateOf(false) }
    var downloadingAllArtifacts by remember { mutableStateOf(false) }
    var showPublishRelease by remember { mutableStateOf(false) }
    var detailNotice by remember { mutableStateOf<String?>(null) }
    var kernelErrorCatalog by remember { mutableStateOf<KernelErrorCatalog?>(null) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    suspend fun loadMetadata(force: Boolean = false) {
        if (metadataLoaded && !force) return
        loadingMetadata = true
        try {
            pendingDeployments = GitHubManager.getPendingDeployments(context, repo.owner, repo.name, runId)
            reviewHistory = GitHubManager.getWorkflowRunReviewHistory(context, repo.owner, repo.name, runId)
            usage = GitHubManager.getWorkflowRunUsage(context, repo.owner, repo.name, runId)
            metadataLoaded = true
        } finally {
            loadingMetadata = false
        }
    }

    suspend fun loadArtifacts(force: Boolean = false) {
        if (artifactsLoaded && !force) return
        loadingArtifacts = true
        try {
            artifacts = GitHubManager.getRunArtifacts(context, repo.owner, repo.name, runId)
            artifactsLoaded = true
        } finally {
            loadingArtifacts = false
        }
    }

    suspend fun loadChecks(force: Boolean = false) {
        if (checksLoaded && !force) return
        loadingChecks = true
        try {
            val currentRun = run ?: GitHubManager.getWorkflowRun(context, repo.owner, repo.name, runId)
            checkRuns = currentRun?.headSha?.takeIf { it.isNotBlank() }?.let {
                GitHubManager.getCheckRunsForRef(context, repo.owner, repo.name, it)
            } ?: emptyList()
            checksLoaded = true
        } finally {
            loadingChecks = false
        }
    }

    suspend fun refreshAll(refreshSection: Boolean = true) {
        refreshing = true
        try {
            val latestRun = GitHubManager.getWorkflowRun(context, repo.owner, repo.name, runId)
            maxAttempt = latestRun?.runAttempt?.coerceAtLeast(1) ?: 1
            val attempt = selectedAttempt
            run = if (attempt != null) GitHubManager.getWorkflowRunAttempt(context, repo.owner, repo.name, runId, attempt) ?: latestRun else latestRun
            jobs = if (attempt != null) GitHubManager.getWorkflowRunAttemptJobs(context, repo.owner, repo.name, runId, attempt)
                else GitHubManager.getWorkflowRunJobs(context, repo.owner, repo.name, runId)
            if (refreshSection) {
                when (selectedSection) {
                    RunDetailSection.SUMMARY -> loadMetadata(force = true)
                    RunDetailSection.ARTIFACTS -> loadArtifacts(force = true)
                    RunDetailSection.CHECKS -> loadChecks(force = true)
                    RunDetailSection.JOBS -> {}
                    RunDetailSection.PIPELINE -> {}
                }
            }
            detailNotice = null
        } catch (e: Exception) {
            detailNotice = actionsFriendlyError(e.message)
        }
        refreshing = false
        loading = false
    }

    LaunchedEffect(runId) { refreshAll(refreshSection = false) }

    LaunchedEffect(Unit) {
        kernelErrorCatalog = KernelErrorPatterns.load(context)
    }

    LaunchedEffect(selectedSection, loading, run?.headSha) {
        if (!loading) {
            when (selectedSection) {
                RunDetailSection.SUMMARY -> loadMetadata()
                RunDetailSection.ARTIFACTS -> loadArtifacts()
                RunDetailSection.CHECKS -> loadChecks()
                RunDetailSection.JOBS -> {}
                RunDetailSection.PIPELINE -> {}
            }
        }
    }

    LaunchedEffect(jobs, expandedJobId, expandedStepKey) {
        while (true) {
            val hasLive = jobs.any { isJobActive(it) }
            nowMs = System.currentTimeMillis()
            if (hasLive) {
                delay(if (detailNotice != null) ACTIONS_BACKOFF_DELAY_MS else 1000L)
                if (nowMs % 3000L < 1100L) {
                    refreshAll(refreshSection = false)
                    val expandedLiveJob = jobs.firstOrNull {
                        (it.id == expandedJobId || expandedStepKey?.startsWith("${it.id}:") == true) &&
                            isJobActive(it)
                    }
                    if (expandedLiveJob != null) {
                        ensureJobLogsLoaded(scope, context, repo, expandedLiveJob, jobLogs, jobStepLogs, jobLogMeta, force = false) { loadingJobId = it }
                    }
                }
            } else {
                delay(1500)
            }
        }
    }

    val filteredJobs = remember(jobs, onlyFailedJobs, onlyActiveJobs, loadedLogsFilter.text, jobLogs) {
        jobs.filter { job ->
            val failedOk = !onlyFailedJobs || job.conclusion == "failure"
            val activeOk = !onlyActiveJobs || isJobActive(job)
            val q = loadedLogsFilter.text.trim()
            val searchOk = q.isBlank() || job.name.contains(q, true) || (jobLogs[job.id]?.contains(q, true) == true)
            failedOk && activeOk && searchOk
        }
    }
    val firstFailedJob = remember(jobs) { jobs.firstOrNull { it.conclusion == "failure" } }
    val firstFailedStep = remember(firstFailedJob) { firstFailedJob?.steps?.firstOrNull { isFailedStep(it) } }
    val firstFailedLog = firstFailedJob?.let { jobLogs[it.id] }.orEmpty()
    val firstFailedStepLog = firstFailedJob?.let { job ->
        firstFailedStep?.let { step -> jobStepLogs[job.id]?.get(step.number) }
    }.orEmpty()
    val failureDiagnostics = remember(firstFailedJob?.id, firstFailedStep?.number, firstFailedLog, kernelErrorCatalog) {
        buildFailureDiagnostics(context, firstFailedJob, firstFailedStep, firstFailedLog, kernelErrorCatalog)
    }
    val failureEvidence = remember(firstFailedJob?.id, firstFailedStep?.number, firstFailedLog, firstFailedStepLog) {
        buildFailureEvidence(firstFailedJob, firstFailedStep, firstFailedLog, firstFailedStepLog)
    }
    val patternInfo = kernelErrorCatalog?.let { "${Strings.actions_kernel_patterns_info} ${it.version}" }
    val groupedJobItems = remember(filteredJobs, jobs.size, expandedMatrixGroups.toMap(), nowMs) {
        buildJobListItems(
            jobs = filteredJobs,
            totalJobCount = jobs.size,
            expandedGroups = expandedMatrixGroups
        )
    }
    val failedJobItemIndexes = remember(groupedJobItems) {
        groupedJobItems.mapIndexedNotNull { index, item ->
            when (item) {
                is JobListItem.JobRow -> index.takeIf { item.job.conclusion == "failure" }
                is JobListItem.GroupHeader -> index.takeIf { item.group.jobs.any { job -> job.conclusion == "failure" } && !item.expanded }
            }
        }
    }
    val jobItemsStartIndex = remember(maxAttempt, firstFailedJob?.id) {
        1 + (if (maxAttempt > 1) 1 else 0) + (if (firstFailedJob != null) 1 else 0)
    }

    fun handleRunDetailBack() {
        if (showPublishRelease) showPublishRelease = false else onBack()
    }

    AiModuleSurface {
    val palette = AiModuleTheme.colors
    Column(Modifier.fillMaxSize().background(palette.background)) {
        GitHubPageBar(
            title = "> ${run?.name?.ifBlank { "run" } ?: "run"} #${run?.runNumber ?: runId}",
            subtitle = run?.let { displayRunStatus(it) },
            onBack = ::handleRunDetailBack,
            trailing = {
                if (refreshing) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner()
                    }
                } else {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.REFRESH,
                        onClick = { scope.launch { refreshAll() } },
                        tint = palette.textSecondary,
                        contentDescription = "refresh",
                    )
                }
                run?.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.OUTLINE,
                        onClick = { openExternalUrl(context, url) },
                        tint = palette.textSecondary,
                        contentDescription = "open in browser",
                    )
                }
                AiModuleGlyphAction(
                    glyph = GhGlyphs.PLAY,
                    onClick = {
                        scope.launch {
                            val ok = GitHubManager.rerunWorkflow(context, repo.owner, repo.name, runId)
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                    },
                    tint = palette.accent,
                    contentDescription = "rerun",
                )
                AiModuleGlyphAction(
                    glyph = GhGlyphs.WARN,
                    onClick = {
                        scope.launch {
                            val ok = GitHubManager.rerunFailedJobs(context, repo.owner, repo.name, runId)
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                    },
                    tint = palette.warning,
                    contentDescription = "rerun failed jobs",
                )
                if (onSuggestFix != null && run?.conclusion == "failure") {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.AI,
                        onClick = {
                            val r = run
                            val firstFailed = jobs.firstOrNull { it.conclusion == "failure" }
                            val prompt = buildString {
                                append(Strings.aiAgentSuggestFixPrompt)
                                append("\n\n")
                                append("Workflow: ").append(r?.name ?: "?").append('\n')
                                append("Run: #").append(r?.runNumber ?: runId).append('\n')
                                if (firstFailed != null) {
                                    append("Failed job: ").append(firstFailed.name).append('\n')
                                }
                                r?.branch?.takeIf { it.isNotBlank() }?.let {
                                    append("Branch: ").append(it).append('\n')
                                }
                                r?.htmlUrl?.takeIf { it.isNotBlank() }?.let {
                                    append("URL: ").append(it).append('\n')
                                }
                            }
                            onSuggestFix(prompt)
                        },
                        tint = palette.accent,
                        contentDescription = "ai suggest fix",
                    )
                }
                if (run != null && isRunActive(run!!)) {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.CLOSE,
                        onClick = {
                            scope.launch {
                                val ok = GitHubManager.cancelWorkflowRun(context, repo.owner, repo.name, runId)
                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                        },
                        tint = palette.error,
                        contentDescription = "cancel run",
                    )
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.STOP,
                        onClick = {
                            scope.launch {
                                val ok = GitHubManager.forceCancelWorkflowRun(context, repo.owner, repo.name, runId)
                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                        },
                        tint = palette.error,
                        contentDescription = "force cancel",
                    )
                }
            },
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading run\u2026")
            }
        } else {
            val successCount = jobs.count { it.conclusion == "success" }
            val failedCount = jobs.count { it.conclusion == "failure" }
            val runningCount = jobs.count { isJobActive(it) }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionsFilterChip("Jobs", selectedSection == RunDetailSection.JOBS) { selectedSection = RunDetailSection.JOBS }
                ActionsFilterChip("Pipeline", selectedSection == RunDetailSection.PIPELINE) { selectedSection = RunDetailSection.PIPELINE }
                ActionsFilterChip("Summary", selectedSection == RunDetailSection.SUMMARY) { selectedSection = RunDetailSection.SUMMARY }
                ActionsFilterChip(
                    if (artifactsLoaded) "Artifacts ${artifacts.size}" else "Artifacts",
                    selectedSection == RunDetailSection.ARTIFACTS
                ) { selectedSection = RunDetailSection.ARTIFACTS }
                ActionsFilterChip(
                    if (checksLoaded) "Checks ${checkRuns.size}" else "Checks",
                    selectedSection == RunDetailSection.CHECKS
                ) { selectedSection = RunDetailSection.CHECKS }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GitHubTerminalButton(
                    "export report",
                    onClick = {
                        val file = saveWorkflowRunReport(
                            repo = repo,
                            run = run,
                            jobs = jobs,
                            artifacts = artifacts,
                            checkRuns = checkRuns,
                            pendingDeployments = pendingDeployments,
                            reviewHistory = reviewHistory,
                            usage = usage,
                            jobLogs = jobLogs,
                        )
                        Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                    },
                    enabled = run != null,
                    color = Blue,
                )
                GitHubTerminalButton(
                    "export loaded logs",
                    onClick = {
                        val file = saveWorkflowRunLoadedLogsExport(repo, run, jobs, jobLogs, jobLogMeta)
                        Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                    },
                    enabled = run != null && jobLogs.isNotEmpty(),
                    color = Blue,
                )
            }

            detailNotice?.let { notice ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(AiModuleTheme.colors.surface)
                        .border(1.dp, Orange.copy(alpha = 0.35f), RoundedCornerShape(GitHubControlRadius))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, null, tint = Orange, modifier = Modifier.size(16.dp))
                    Text(notice, fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 16.sp)
                }
            }

            if (selectedSection == RunDetailSection.JOBS) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionsFilterChip("Success $successCount", false) {}
                ActionsFilterChip("Failed $failedCount", onlyFailedJobs) { onlyFailedJobs = !onlyFailedJobs }
                ActionsFilterChip("Running $runningCount", onlyActiveJobs) { onlyActiveJobs = !onlyActiveJobs }
                ActionsFilterChip("Failed logs", false) {
                    val failed = jobs.firstOrNull { it.conclusion == "failure" }
                    if (failed != null) {
                        expandedJobId = failed.id
                        failed.steps.firstOrNull { isFailedStep(it) }?.let { step ->
                            expandedStepKey = "${failed.id}:${step.number}"
                        }
                        ensureJobLogsLoaded(scope, context, repo, failed, jobLogs, jobStepLogs, jobLogMeta, force = false) { loadingJobId = it }
                    }
                }
                ActionsFilterChip("Running logs", false) {
                    val running = jobs.firstOrNull { isJobActive(it) }
                    if (running != null) {
                        expandedJobId = running.id
                        ensureJobLogsLoaded(scope, context, repo, running, jobLogs, jobStepLogs, jobLogMeta, force = false) { loadingJobId = it }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                AiModuleSearchField(
                    value = loadedLogsFilter.text,
                    onValueChange = { loadedLogsFilter = TextFieldValue(it) },
                    placeholder = "filter loaded logs\u2026",
                )
            }
            }

            Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), state = jobListState, contentPadding = PaddingValues(12.dp)) {
                run?.let { currentRun ->
                    item {
                        WorkflowRunDetailHeader(currentRun, nowMs)
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (maxAttempt > 1) {
                    item {
                        AttemptSelector(
                            maxAttempt = maxAttempt,
                            selectedAttempt = selectedAttempt ?: maxAttempt,
                            onSelect = {
                                selectedAttempt = it
                                scope.launch { refreshAll() }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                firstFailedJob?.let { failedJob ->
                    item {
                        FailureDiagnosisCard(
                            job = failedJob,
                            step = firstFailedStep,
                            diagnostics = failureDiagnostics,
                            evidence = failureEvidence,
                            patternInfo = patternInfo,
                            logLoaded = jobLogs[failedJob.id] != null,
                            loading = loadingJobId == failedJob.id,
                            onCopySummary = {
                                val summary = failureSummaryText(failedJob, firstFailedStep, failureDiagnostics)
                                val clip = android.content.ClipData.newPlainText("failure-summary", summary)
                                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                            },
                            onCopyEvidence = {
                                val text = failureEvidenceText(repo, run, failedJob, firstFailedStep, failureDiagnostics, failureEvidence)
                                val clip = android.content.ClipData.newPlainText("failure-evidence", text)
                                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                            },
                            onExportEvidence = {
                                val file = saveFailureEvidenceExport(repo, run, failedJob, firstFailedStep, failureDiagnostics, failureEvidence)
                                Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                            },
                            onShareSummary = run?.let { currentRun ->
                                {
                                    shareFailureSummary(
                                        context = context,
                                        repo = repo,
                                        run = currentRun,
                                        job = failedJob,
                                        step = firstFailedStep,
                                        diagnostics = failureDiagnostics
                                    )
                                }
                            },
                            onOpenFailedLog = {
                                expandedJobId = failedJob.id
                                firstFailedStep?.let { step -> expandedStepKey = "${failedJob.id}:${step.number}" }
                                ensureJobLogsLoaded(scope, context, repo, failedJob, jobLogs, jobStepLogs, jobLogMeta, force = false) { loadingJobId = it }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (selectedSection == RunDetailSection.SUMMARY) {
                    if (loadingMetadata) {
                        item { LoadingActionsText("Loading run metadata...") }
                    }
                usage?.let { runUsage ->
                    item {
                        WorkflowUsageCard(runUsage)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item {
                    RunDangerActionsCard(
                        onDeleteLogs = {
                            scope.launch {
                                val ok = GitHubManager.deleteWorkflowRunLogs(context, repo.owner, repo.name, runId)
                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                        },
                        onDeleteRun = {
                            scope.launch {
                                val ok = GitHubManager.deleteWorkflowRun(context, repo.owner, repo.name, runId)
                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                if (ok) onBack() else refreshAll()
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (pendingDeployments.isNotEmpty()) {
                    item {
                        PendingDeploymentsCard(
                            deployments = pendingDeployments,
                            onReview = { deployment, approve ->
                                scope.launch {
                                    val ok = GitHubManager.reviewPendingDeployments(
                                        context = context,
                                        owner = repo.owner,
                                        repo = repo.name,
                                        runId = runId,
                                        environmentIds = listOf(deployment.environmentId),
                                        approve = approve,
                                        comment = if (approve) "Approved from GlassFiles" else "Rejected from GlassFiles"
                                    )
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (reviewHistory.isNotEmpty()) {
                    item {
                        ReviewHistoryCard(reviewHistory)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                }

                if (selectedSection == RunDetailSection.CHECKS) {
                    if (loadingChecks) {
                        item { LoadingActionsText("Loading checks...") }
                    } else if (checkRuns.isEmpty()) {
                        item { EmptyActionsText("No checks found") }
                    } else {
                    item {
                        CheckRunsCard(
                            checkRuns = checkRuns.filter { checkRun ->
                                checkRun.id != 0L && (
                                    checkRun.name.isNotBlank() ||
                                        checkRun.title.isNotBlank() ||
                                        checkRun.summary.isNotBlank() ||
                                        checkRun.annotationsCount > 0
                                    )
                            },
                            annotations = checkAnnotations,
                            onLoadAnnotations = { checkRun ->
                                scope.launch {
                                    checkAnnotations[checkRun.id] = GitHubManager.getCheckRunAnnotations(context, repo.owner, repo.name, checkRun.id)
                                        .filter { annotation ->
                                            annotation.message.isNotBlank() ||
                                                annotation.title.isNotBlank() ||
                                                annotation.path.isNotBlank()
                                        }
                                }
                            },
                            onNavigateToCode = onNavigateToCode
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    }
                }

                if (selectedSection == RunDetailSection.PIPELINE) {
                    item {
                        PipelineView(
                            jobs = jobs,
                            expandedJobId = expandedJobId,
                            onJobClick = { clickedJob ->
                                selectedSection = RunDetailSection.JOBS
                                expandedJobId = clickedJob.id
                                val indexInGroup = groupedJobItems.indexOfFirst {
                                    it is JobListItem.JobRow && it.job.id == clickedJob.id
                                }
                                if (indexInGroup != -1) {
                                    scope.launch {
                                        jobListState.animateScrollToItem(jobItemsStartIndex + indexInGroup)
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (selectedSection == RunDetailSection.JOBS) {
                    items(groupedJobItems, key = { item ->
                        when (item) {
                            is JobListItem.GroupHeader -> "group:${item.group.name}"
                            is JobListItem.JobRow -> "job:${item.job.id}"
                        }
                    }) { item ->
                        when (item) {
                            is JobListItem.GroupHeader -> {
                                MatrixJobGroupHeader(
                                    group = item.group,
                                    expanded = item.expanded,
                                    nowMs = nowMs,
                                    onToggle = { expandedMatrixGroups[item.group.name] = !item.expanded }
                                )
                            }
                            is JobListItem.JobRow -> {
                                WorkflowJobCard(
                                    job = item.job,
                                    nowMs = nowMs,
                                    repo = repo,
                                    runHtmlUrl = run?.htmlUrl.orEmpty(),
                                    context = context,
                                    scope = scope,
                                    jobLogs = jobLogs,
                                    jobStepLogs = jobStepLogs,
                                    jobLogMeta = jobLogMeta,
                                    loadingJobId = loadingJobId,
                                    expandedJobId = expandedJobId,
                                    expandedStepKey = expandedStepKey,
                                    onExpandedJobChange = { expandedJobId = it },
                                    onExpandedStepChange = { expandedStepKey = it },
                                    setLoadingJobId = { loadingJobId = it },
                                    onRefreshRun = { scope.launch { refreshAll() } }
                                )
                            }
                        }
                    }
                }

                if (selectedSection == RunDetailSection.ARTIFACTS) {
                    if (loadingArtifacts) {
                        item { LoadingActionsText("Loading artifacts...") }
                    } else if (artifacts.isEmpty()) {
                        item { EmptyActionsText("No artifacts found") }
                    } else {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("▸ artifacts", fontSize = 16.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = TextPrimary)
                                GitHubTerminalButton(
                                    label = if (downloadingAllArtifacts) "⠋ downloading" else "↓ download all",
                                    enabled = !downloadingAllArtifacts && artifacts.any { !it.expired },
                                    onClick = {
                                        downloadingAllArtifacts = true
                                        scope.launch {
                                            var count = 0
                                            artifacts.filter { !it.expired }.forEach { artifact ->
                                                val dest = DownloadStorage.file(context, safeArtifactZipName(artifact))
                                                if (GitHubManager.downloadArtifact(context, repo.owner, repo.name, artifact.id, dest)) count++
                                            }
                                            downloadingAllArtifacts = false
                                            Toast.makeText(context, "${Strings.done}: $count/${artifacts.count { !it.expired }}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    color = Blue,
                                )
                                GitHubTerminalButton(
                                    label = "↗ publish release",
                                    enabled = artifacts.any { !it.expired },
                                    onClick = { showPublishRelease = true },
                                    color = Green,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        groupedArtifacts(artifacts).forEach { group ->
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MiniActionsBadge(group.label, group.color)
                                    Text("${group.items.size} artifacts", fontSize = 11.sp, color = TextTertiary)
                                }
                            }
                            items(group.items) { artifact ->
                                ArtifactRunRow(
                                    artifact = artifact,
                                    downloading = downloading == artifact.id,
                                    deleting = deletingArtifactId == artifact.id,
                                    onCopyName = {
                                        val clip = android.content.ClipData.newPlainText("artifact-name", artifact.name)
                                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                        Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                                    },
                                    onDownload = {
                                        downloading = artifact.id
                                        scope.launch {
                                            val dest = DownloadStorage.file(context, safeArtifactZipName(artifact))
                                            val ok = GitHubManager.downloadArtifact(context, repo.owner, repo.name, artifact.id, dest)
                                            Toast.makeText(context, if (ok) "${Strings.done}: ${dest.name}" else Strings.error, Toast.LENGTH_SHORT).show()
                                            downloading = null
                                        }
                                    },
                                    onDelete = {
                                        deletingArtifactId = artifact.id
                                        scope.launch {
                                            val ok = GitHubManager.deleteArtifact(context, repo.owner, repo.name, artifact.id)
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            deletingArtifactId = null
                                            refreshAll()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
            if (selectedSection == RunDetailSection.JOBS && failedJobItemIndexes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Red)
                        .clickable {
                        scope.launch {
                            try {
                                val currentJobIndex = (jobListState.firstVisibleItemIndex - jobItemsStartIndex).coerceAtLeast(-1)
                                val targetJobIndex = failedJobItemIndexes.firstOrNull { it > currentJobIndex } ?: failedJobItemIndexes.firstOrNull()
                                if (targetJobIndex == null) return@launch

                                (groupedJobItems.getOrNull(targetJobIndex) as? JobListItem.GroupHeader)?.let { header ->
                                    if (!header.expanded) expandedMatrixGroups[header.group.name] = true
                                }

                                val targetListIndex = (jobItemsStartIndex + targetJobIndex).coerceAtLeast(0)
                                val totalItems = jobListState.layoutInfo.totalItemsCount
                                if (totalItems <= 0) return@launch

                                val safeIndex = targetListIndex.coerceIn(0, totalItems - 1)
                                jobListState.animateScrollToItem(safeIndex)
                            } catch (t: Throwable) {
                                Log.e(ACTIONS_JOB_LOG_TAG, "failed job FAB scroll failed", t)
                                Toast.makeText(context, "Cannot jump to failed job", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Error, "Jump to next failed job", Modifier.size(22.dp), tint = Color.White)
                }
            }
            }
        }
    }

    if (showPublishRelease && run != null) {
        PublishArtifactsReleaseDialog(
            repo = repo,
            run = run!!,
            artifacts = artifacts,
            onDismiss = { showPublishRelease = false },
            onPublished = {
                showPublishRelease = false
                selectedSection = RunDetailSection.SUMMARY
            }
        )
    }
    }
}

@Composable
private fun WorkflowRunDetailHeader(run: GHWorkflowRun, nowMs: Long) {
    val palette = AiModuleTheme.colors
    val badge = aiModuleStatusBadge(run.status, run.conclusion, palette)
    val elapsed = calcRunDuration(run, nowMs)
    val started = run.createdAt.take(19).replace('T', ' ')

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                badge.glyph,
                color = badge.color.copy(alpha = badge.alpha),
                fontFamily = JetBrainsMono,
                fontSize = 16.sp,
            )
            Text(
                run.displayTitle.ifBlank { run.name }.ifBlank { "workflow" } + " #${run.runNumber}",
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        ActionsHeaderField("status", "${badge.glyph} ${displayRunStatus(run)}", badge.color.copy(alpha = badge.alpha), palette)
        if (run.event.isNotBlank()) {
            ActionsHeaderField("trigger", run.event, palette.textPrimary, palette)
        }
        if (run.branch.isNotBlank()) {
            ActionsHeaderField("branch", run.branch, palette.textPrimary, palette)
        }
        if (run.headSha.length >= 7) {
            ActionsHeaderField("commit", run.headSha.take(7), palette.textPrimary, palette)
        }
        if (run.actor.isNotBlank()) {
            ActionsHeaderField("actor", run.actor, palette.textPrimary, palette)
        }
        if (run.runAttempt > 1) {
            ActionsHeaderField("attempt", run.runAttempt.toString(), palette.warning, palette)
        }
        if (started.isNotBlank()) {
            ActionsHeaderField("started", started, palette.textSecondary, palette)
        }
        if (elapsed.isNotBlank()) {
            ActionsHeaderField("duration", elapsed, palette.textPrimary, palette)
        }
        if (run.headRepository.isNotBlank()) {
            ActionsHeaderField("repo", run.headRepository, palette.textMuted, palette)
        }
    }
}

@Composable
private fun ActionsHeaderField(
    label: String,
    value: String,
    valueColor: Color,
    palette: gs.git.vps.ui.theme.AiModuleColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${label.padEnd(8)}: ",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp,
        )
        Text(
            value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FailureDiagnosisCard(
    job: GHJob,
    step: GHStep?,
    diagnostics: List<String>,
    evidence: FailureEvidence,
    patternInfo: String?,
    logLoaded: Boolean,
    loading: Boolean,
    onCopySummary: () -> Unit,
    onCopyEvidence: () -> Unit,
    onExportEvidence: () -> Unit,
    onShareSummary: (() -> Unit)?,
    onOpenFailedLog: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(AiModuleTheme.colors.surface)
            .border(1.dp, Red.copy(alpha = 0.35f), RoundedCornerShape(GitHubControlRadius))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Error, null, tint = Red, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Failed build", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(job.name, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GitHubTerminalButton(
                if (loading) "loading log" else "open failed log",
                onClick = onOpenFailedLog,
                enabled = !loading,
                color = Red,
            )
        }
        step?.let {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniActionsBadge("step ${it.number}", Red)
                MiniActionsBadge(it.name, TextSecondary)
            }
        }
        if (!logLoaded) {
            Text("Load the failed log to see likely causes.", fontSize = 11.sp, color = TextTertiary)
        } else if (diagnostics.isEmpty()) {
            Text("No known pattern detected. Check the failed step output.", fontSize = 11.sp, color = TextTertiary)
        } else {
            diagnostics.take(3).forEach { message ->
                Text(message, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
            }
        }
        if (logLoaded && evidence.hasContent) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(AiModuleTheme.colors.background)
                    .border(1.dp, Red.copy(alpha = 0.22f), RoundedCornerShape(GitHubControlRadius))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("evidence: ${evidence.source}", fontSize = 11.sp, color = Red, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium)
                if (evidence.signalLines.isNotEmpty()) {
                    Text("signals", fontSize = 10.sp, color = TextTertiary, fontFamily = JetBrainsMono)
                    evidence.signalLines.take(5).forEach { line ->
                        Text(line, fontSize = 10.sp, color = TextSecondary, fontFamily = JetBrainsMono, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (evidence.tailLines.isNotEmpty()) {
                    Text("tail", fontSize = 10.sp, color = TextTertiary, fontFamily = JetBrainsMono)
                    evidence.tailLines.takeLast(6).forEach { line ->
                        Text(line, fontSize = 10.sp, color = TextTertiary, fontFamily = JetBrainsMono, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(Icons.Rounded.ContentCopy, Strings.actions_copy_failure_summary, Red, onCopySummary)
            if (evidence.hasContent) {
                Chip(Icons.Rounded.ContentCopy, "Copy evidence", Red, onCopyEvidence)
                Chip(Icons.Rounded.Article, "Export evidence", Red, onExportEvidence)
            }
            if (onShareSummary != null) {
                Chip(Icons.Rounded.Share, Strings.actions_share, Red, onShareSummary)
            }
            Chip(Icons.Rounded.Article, "Open failed log", Red, onOpenFailedLog)
        }
        if (!patternInfo.isNullOrBlank()) {
            Text(patternInfo, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun PublishArtifactsReleaseDialog(
    repo: GHRepo,
    run: GHWorkflowRun,
    artifacts: List<GHArtifact>,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val publishableArtifacts = remember(artifacts) { artifacts.filter { !it.expired } }
    var tag by remember(run.id) { mutableStateOf(defaultReleaseTag(run)) }
    var name by remember(run.id) { mutableStateOf(defaultReleaseName(run)) }
    var body by remember(run.id) { mutableStateOf(defaultReleaseBody(run, publishableArtifacts)) }
    var draft by remember { mutableStateOf(true) }
    var prerelease by remember { mutableStateOf(false) }
    var publishing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }

    AiModuleAlertDialog(
        onDismissRequest = { if (!publishing) onDismiss() },
        title = "publish artifacts",
        content = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${publishableArtifacts.size} artifacts from run #${run.runNumber}", fontSize = 12.sp, color = TextSecondary)
                AiModuleTextField(tag, { tag = it.trim() }, label = "Tag", singleLine = true, modifier = Modifier.fillMaxWidth())
                AiModuleTextField(name, { name = it }, label = "Release title", singleLine = true, modifier = Modifier.fillMaxWidth())
                AiModuleTextField(body, { body = it }, label = "Release notes", minLines = 5, maxLines = 8, modifier = Modifier.fillMaxWidth())
                GitHubTerminalCheckbox("draft", draft, onToggle = { draft = !draft })
                GitHubTerminalCheckbox("pre-release", prerelease, onToggle = { prerelease = !prerelease })
                if (progress.isNotBlank()) Text(progress, fontSize = 11.sp, color = TextTertiary)
            }
        },
        confirmButton = {
            AiModuleTextAction(
                label = if (publishing) "publishing" else "publish",
                enabled = !publishing && tag.isNotBlank() && publishableArtifacts.isNotEmpty(),
                onClick = {
                    publishing = true
                    progress = "Creating release..."
                    scope.launch {
                        val release = GitHubManager.createReleaseDetailed(
                            context = context,
                            owner = repo.owner,
                            repo = repo.name,
                            tag = tag,
                            name = name.ifBlank { tag },
                            body = body,
                            prerelease = prerelease,
                            draft = draft,
                            targetCommitish = run.branch
                        ) ?: GitHubManager.getReleaseByTag(context, repo.owner, repo.name, tag)

                        if (release == null || release.id == 0L) {
                            publishing = false
                            progress = ""
                            Toast.makeText(context, "Failed to create release", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val dir = File(context.cacheDir, "release-artifacts/${run.id}").apply { mkdirs() }
                        var uploaded = 0
                        publishableArtifacts.forEachIndexed { index, artifact ->
                            progress = "Uploading ${index + 1}/${publishableArtifacts.size}: ${artifact.name}"
                            val file = File(dir, safeArtifactZipName(artifact))
                            val downloaded = GitHubManager.downloadArtifact(context, repo.owner, repo.name, artifact.id, file)
                            if (downloaded && GitHubManager.uploadReleaseAssetDetailed(context, repo.owner, repo.name, release.id, file) != null) {
                                uploaded++
                            }
                        }
                        publishing = false
                        Toast.makeText(context, "Uploaded $uploaded/${publishableArtifacts.size}", Toast.LENGTH_SHORT).show()
                        onPublished()
                    }
                }
            )
        },
        dismissButton = {
            AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, enabled = !publishing, tint = AiModuleTheme.colors.textSecondary)
        }
    )
}

private fun defaultReleaseTag(run: GHWorkflowRun): String {
    val base = run.name.ifBlank { "build" }
        .replace(Regex("""[^A-Za-z0-9._-]+"""), "-")
        .trim('-')
        .lowercase()
        .ifBlank { "build" }
    return "$base-${run.runNumber}"
}

private fun defaultReleaseName(run: GHWorkflowRun): String =
    "${run.name.ifBlank { "Build" }} #${run.runNumber}"

private fun defaultReleaseBody(run: GHWorkflowRun, artifacts: List<GHArtifact>): String {
    val lines = mutableListOf<String>()
    lines += "Published from GitHub Actions run #${run.runNumber}."
    if (run.branch.isNotBlank()) lines += "Branch: ${run.branch}"
    if (run.headSha.length >= 7) lines += "Commit: ${run.headSha.take(7)}"
    if (artifacts.isNotEmpty()) {
        lines += ""
        lines += "Artifacts:"
        artifacts.forEach { artifact ->
            lines += "- ${artifact.name} (${formatArtifactSize(artifact.sizeInBytes)})"
        }
    }
    return lines.joinToString("\n")
}

@Composable
private fun WorkflowUsageCard(usage: GHActionsUsage) {
    val palette = AiModuleTheme.colors
    Column(Modifier.fillMaxWidth().border(1.dp, palette.border).background(palette.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("▸ usage", fontSize = 15.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
        Text("duration: ${formatDuration(usage.runDurationMs)}", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.textSecondary)
        usage.billableMs.forEach { (os, ms) ->
            Text("${os.lowercase().padEnd(8)}: ${formatDuration(ms)}", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.textSecondary)
        }
    }
}

@Composable
private fun AttemptSelector(maxAttempt: Int, selectedAttempt: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Attempts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..maxAttempt).forEach { attempt ->
                ActionsFilterChip("Attempt $attempt", selectedAttempt == attempt) { onSelect(attempt) }
            }
        }
    }
}

@Composable
private fun RunDangerActionsCard(onDeleteLogs: () -> Unit, onDeleteRun: () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(Modifier.fillMaxWidth().border(1.dp, palette.border).background(palette.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("run management", fontSize = 15.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubTerminalButton("× delete logs", onClick = onDeleteLogs, color = palette.error)
            GitHubTerminalButton("× delete run", onClick = onDeleteRun, color = palette.error)
        }
    }
}

@Composable
private fun PendingDeploymentsCard(
    deployments: List<GHPendingDeployment>,
    onReview: (GHPendingDeployment, Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Warning, null, tint = Orange, modifier = Modifier.size(18.dp))
            Text("Pending deployments", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        deployments.forEach { deployment ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(deployment.environmentName.ifBlank { "Environment ${deployment.environmentId}" }, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    val reviewers = deployment.reviewers.joinToString(", ").ifBlank { "No reviewers listed" }
                    Text(reviewers, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (deployment.currentUserCanApprove) {
                    GitHubTerminalButton("approve", onClick = { onReview(deployment, true) }, color = Green)
                    GitHubTerminalButton("reject", onClick = { onReview(deployment, false) }, color = Red)
                } else {
                    MiniActionsBadge("waiting", Orange)
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryCard(reviews: List<GHWorkflowRunReview>) {
    Column(Modifier.fillMaxWidth().ghGlassCard(14.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Blue, modifier = Modifier.size(18.dp))
            Text("Deployment review history", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        reviews.forEach { review ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceLight).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniActionsBadge(review.state.ifBlank { "reviewed" }, if (review.state == "approved") Green else Orange)
                    Text(review.user.ifBlank { "GitHub" }, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                if (review.environments.isNotEmpty()) Text(review.environments.joinToString(", "), fontSize = 11.sp, color = TextSecondary)
                if (review.comment.isNotBlank()) Text(review.comment, fontSize = 11.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun getJobStage(jobName: String): String {
    val lower = jobName.lowercase()
    return when {
        lower.contains("build") || lower.contains("compile") || lower.contains("package") || lower.contains("assemble") -> "Build"
        lower.contains("test") || lower.contains("check") || lower.contains("lint") || lower.contains("detekt") || lower.contains("verify") || lower.contains("spec") || lower.contains("unit") || lower.contains("ui-test") -> "Test"
        lower.contains("deploy") || lower.contains("publish") || lower.contains("release") || lower.contains("upload") || lower.contains("distribute") -> "Deploy"
        else -> "Other"
    }
}

@Composable
private fun PipelineJobNode(
    job: GHJob,
    nowMs: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = AiModuleTheme.colors
    val status = displayJobStatus(job)
    val statusColor = jobStatusColor(status)
    val statusIcon = jobStatusIcon(status)
    val jobElapsed = calcJobDuration(job, nowMs)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) palette.surfaceElevated else palette.background)
            .border(
                1.dp,
                if (isSelected) palette.accent else palette.border.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = status,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.name,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (jobElapsed.isNotBlank()) {
                Text(
                    text = jobElapsed,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    color = palette.textMuted
                )
            }
        }
        
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "go to details",
            tint = palette.textMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun PipelineView(
    jobs: List<GHJob>,
    expandedJobId: Long?,
    onJobClick: (GHJob) -> Unit
) {
    val palette = AiModuleTheme.colors
    val nowMs = remember { System.currentTimeMillis() }
    val stageNames = listOf("Build", "Test", "Deploy", "Other")
    
    val grouped = remember(jobs) {
        val map = jobs.groupBy { getJobStage(it.name) }
        map.mapValues { (_, jobList) ->
            jobList.sortedWith(compareBy<GHJob> { it.startedAt }.thenBy { it.name })
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stageNames.forEachIndexed { index, stage ->
            val stageJobs = grouped[stage].orEmpty()
            if (stageJobs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "▸ ${stage.uppercase()}",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = palette.accent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    stageJobs.forEach { job ->
                        PipelineJobNode(
                            job = job,
                            nowMs = nowMs,
                            isSelected = expandedJobId == job.id,
                            onClick = { onJobClick(job) }
                        )
                        if (job != stageJobs.last()) {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
                
                val hasMoreStages = stageNames.subList(index + 1, stageNames.size).any { grouped[it]?.isNotEmpty() == true }
                if (hasMoreStages) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "next stage connector",
                            tint = palette.textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRunsCard(
    checkRuns: List<GHCheckRun>,
    annotations: Map<Long, List<GHCheckAnnotation>>,
    onLoadAnnotations: (GHCheckRun) -> Unit,
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null
) {
    val palette = AiModuleTheme.colors
    Column(Modifier.fillMaxWidth().border(1.dp, palette.border).background(palette.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✓ checks and annotations", fontSize = 15.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
        checkRuns.forEach { checkRun ->
            Column(Modifier.fillMaxWidth().border(1.dp, palette.border).background(palette.background).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val checkName = cleanGithubText(checkRun.name)
                    val checkTitle = cleanGithubText(checkRun.title)
                    MiniActionsBadge(displayCheckStatus(checkRun), checkStatusColor(checkRun))
                    Text("▸ ${checkName.ifBlank { checkTitle.ifBlank { "build" } }}", fontSize = 13.sp, fontFamily = JetBrainsMono, color = palette.textPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("annotations: ${checkRun.annotationsCount}", fontSize = 11.sp, fontFamily = JetBrainsMono, color = palette.textMuted)
                    if (checkRun.annotationsCount > 0 && annotations[checkRun.id] == null) {
                        GitHubTerminalButton("load", onClick = { onLoadAnnotations(checkRun) }, color = palette.accent)
                    }
                }
                cleanGithubText(checkRun.title).takeIf { it.isNotBlank() }?.let { title ->
                    Text(title, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                annotations[checkRun.id].orEmpty().filter { annotation ->
                    cleanGithubText(annotation.message).isNotBlank() ||
                        cleanGithubText(annotation.title).isNotBlank() ||
                        cleanGithubText(annotation.path).isNotBlank()
                }.take(10).forEach { annotation ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .then(
                                if (onNavigateToCode != null && annotation.path.isNotBlank()) {
                                    Modifier.clickable {
                                        onNavigateToCode(annotation.path, annotation.startLine.coerceAtLeast(1))
                                    }
                                } else Modifier
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val location = buildAnnotationLocation(annotation)
                        if (location.isNotBlank()) {
                            Text(
                                text = location,
                                fontSize = 11.sp,
                                color = if (onNavigateToCode != null && annotation.path.isNotBlank()) palette.accent else TextTertiary,
                                fontFamily = JetBrainsMono,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val body = cleanGithubText(annotation.message).ifBlank { cleanGithubText(annotation.title) }
                        if (body.isNotBlank()) Text(body, fontSize = 11.sp, color = if (annotation.annotationLevel == "failure") Red else TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun buildAnnotationLocation(annotation: GHCheckAnnotation): String {
    val path = cleanGithubText(annotation.path).ifBlank { return "" }
    return if (annotation.startLine > 0) "$path:${annotation.startLine}" else path
}

@Composable
private fun MatrixJobGroupHeader(
    group: MatrixJobGroup,
    expanded: Boolean,
    nowMs: Long,
    onToggle: () -> Unit
) {
    val status = aggregateJobStatus(group.jobs)
    val color = jobStatusColor(status)
    Column(
        Modifier.fillMaxWidth()
            .padding(bottom = 8.dp)
            .ghGlassCard(14.dp)
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(jobStatusIcon(status), null, Modifier.size(18.dp), tint = color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(group.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${group.jobs.size} jobs · ${matrixGroupDuration(group.jobs, nowMs)}", fontSize = 11.sp, color = TextTertiary)
            }
            MiniActionsBadge(status, color)
            Icon(
                if (expanded) Icons.Rounded.FilterList else Icons.Rounded.Article,
                null,
                Modifier.size(16.dp),
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun WorkflowJobCard(
    job: GHJob,
    nowMs: Long,
    repo: GHRepo,
    runHtmlUrl: String,
    context: Context,
    scope: CoroutineScope,
    jobLogs: MutableMap<Long, String>,
    jobStepLogs: MutableMap<Long, Map<Int, String>>,
    jobLogMeta: MutableMap<Long, JobLogMeta>,
    loadingJobId: Long?,
    expandedJobId: Long?,
    expandedStepKey: String?,
    onExpandedJobChange: (Long?) -> Unit,
    onExpandedStepChange: (String?) -> Unit,
    setLoadingJobId: (Long?) -> Unit,
    onRefreshRun: () -> Unit
) {
    val palette = AiModuleTheme.colors
    val status = displayJobStatus(job)
    val jobBadge = aiModuleStatusBadge(job.status, job.conclusion, palette)
    val jobElapsed = calcJobDuration(job, nowMs)
    val logMeta = jobLogMeta[job.id]

    // Lazy load: only when user expands the job or a step.
    // Auto-loading all jobs at once causes OOM on large Android builds
    // (multiple 10-50MB logs in parallel).
    LaunchedEffect(expandedJobId, expandedStepKey, job.id) {
        val isJobExpanded = expandedJobId == job.id
        val isStepExpanded = expandedStepKey?.startsWith("${job.id}:") == true
        if (isJobExpanded || isStepExpanded) {
            ensureJobLogsLoaded(scope, context, repo, job, jobLogs, jobStepLogs, jobLogMeta, force = false, setLoading = setLoadingJobId)
        }
    }

    val statusBarColor = when (status) {
        "failed", "failure", "timed_out", "action_required" -> palette.error
        "running", "in_progress" -> palette.warning
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius)),
    ) {
        Box(
            Modifier.width(2.dp).fillMaxHeight().background(statusBarColor)
        )
        Column(Modifier.weight(1f).padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    jobBadge.glyph,
                    fontFamily = JetBrainsMono,
                    fontSize = 14.sp,
                    color = jobBadge.color.copy(alpha = jobBadge.alpha),
                )
                Text(
                    job.name,
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    jobElapsed,
                    fontSize = 11.sp,
                    color = if (isJobActive(job)) palette.warning else palette.textMuted,
                    fontFamily = JetBrainsMono,
                )
            }

            Spacer(Modifier.height(6.dp))
            if (loadingJobId == job.id && jobLogs[job.id] == null) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiModuleSpinner(label = "loading job log\u2026")
                }
            }
            job.steps.forEach { step ->
                val stepStatus = displayStepStatus(step)
                val sColor = stepStatusColor(step)
                val stepKey = "${job.id}:${step.number}"
                val stepLog = jobStepLogs[job.id]?.get(step.number)
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).clickable {
                            onExpandedStepChange(if (expandedStepKey == stepKey) null else stepKey)
                        }.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepStatusMark(stepStatus, sColor)
                        Text(
                            step.name,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMono,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        StepStatusPill(stepStatus, sColor)
                    }
                    if (expandedStepKey == stepKey) {
                        val liveMessage = when (stepStatus) {
                            "queued" -> "Log not available yet."
                            "running" -> "Waiting for live log..."
                            else -> "No log output for this step."
                        }
                        val shownStepLog = remember(stepLog, logMeta?.warning, liveMessage) {
                            compactLogForDisplay(stepLog ?: logMeta?.warning ?: liveMessage)
                        }
                        Box(
                            Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp, bottom = 8.dp)
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                                .padding(8.dp)
                        ) {
                            if (jobLogs[job.id] == null || loadingJobId == job.id) {
                                AiModuleSpinner()
                            } else {
                                LogLinesView(shownStepLog, Modifier.fillMaxWidth().heightIn(max = 220.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Chip(
                    if (expandedJobId == job.id) Icons.Rounded.FilterList else Icons.Rounded.Article,
                    if (expandedJobId == job.id) "Hide full log" else "Show full log"
                ) {
                    if (expandedJobId == job.id) {
                        onExpandedJobChange(null)
                    } else {
                        onExpandedJobChange(job.id)
                        ensureJobLogsLoaded(scope, context, repo, job, jobLogs, jobStepLogs, jobLogMeta, force = false, setLoading = setLoadingJobId)
                    }
                }
                if (jobLogs[job.id] != null) {
                    if (logMeta?.warning?.startsWith("Failed to load logs") == true) {
                        Chip(Icons.Rounded.Refresh, "Retry log") {
                            ensureJobLogsLoaded(scope, context, repo, job, jobLogs, jobStepLogs, jobLogMeta, force = true, setLoading = setLoadingJobId)
                        }
                    }
                    Chip(Icons.Rounded.ContentCopy, "Copy full log") {
                        val clip = android.content.ClipData.newPlainText("logs", jobLogs[job.id])
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                        Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                    }
                    Chip(Icons.Rounded.Article, "Save log") {
                        val dest = DownloadStorage.file(context, "${safeLogFileName(job)}.log")
                        val ok = runCatching {
                            logMeta?.cacheFile?.takeIf { it.exists() }?.copyTo(dest, overwrite = true)?.exists()
                                ?: saveTextFile(dest, jobLogs[job.id].orEmpty())
                        }.getOrElse {
                            Log.e(ACTIONS_JOB_LOG_TAG, "save log failed job=${job.id}", it)
                            false
                        }
                        Toast.makeText(context, if (ok) "${Strings.done}: ${dest.name}" else Strings.error, Toast.LENGTH_SHORT).show()
                    }
                    if (logMeta?.tooLarge == true && runHtmlUrl.isNotBlank()) {
                        Chip(Icons.Rounded.Article, "Open in browser") { openExternalUrl(context, runHtmlUrl) }
                    }
                }
                Chip(Icons.Rounded.Refresh, "Rerun job") {
                    scope.launch {
                        val ok = GitHubManager.rerunJob(context, repo.owner, repo.name, job.id)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        onRefreshRun()
                    }
                }
            }

            if (expandedJobId == job.id && jobLogs[job.id] != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        logMeta?.warning?.let {
                            Text(
                                it,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                color = palette.error,
                                lineHeight = 15.sp,
                            )
                        }
                        LogLinesView(compactLogForDisplay(jobLogs[job.id]!!), Modifier.fillMaxWidth().heightIn(max = 390.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepStatusMark(status: String, color: Color) {
    when (status) {
        "success" -> Icon(Icons.Rounded.CheckCircle, null, Modifier.size(14.dp), tint = color)
        "failed", "timed out", "action required", "startup failure" -> Icon(Icons.Rounded.Error, null, Modifier.size(14.dp), tint = color)
        "cancelled", "skipped" -> Icon(Icons.Rounded.Cancel, null, Modifier.size(14.dp), tint = color)
        else -> Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun StepStatusPill(status: String, color: Color) {
    Text(
        status.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 0.6.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AiModuleTheme.colors.background)
            .border(1.dp, AiModuleTheme.colors.border.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun LogLinesView(log: String, modifier: Modifier = Modifier) {
    val palette = AiModuleTheme.colors
    val lines = remember(log) { log.lineSequence().toList() }
    
    var query by rememberSaveable { mutableStateOf("") }
    var isRegex by rememberSaveable { mutableStateOf(false) }
    var filterOnlyMatches by rememberSaveable { mutableStateOf(false) }
    
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val displayLines = remember(lines, query, isRegex, filterOnlyMatches) {
        if (query.isBlank()) {
            lines.mapIndexed { idx, s -> idx to s }
        } else {
            val regex = runCatching {
                if (isRegex) Regex(query, RegexOption.IGNORE_CASE) else Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
            }.getOrNull() ?: Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
            
            val filtered = mutableListOf<Pair<Int, String>>()
            lines.forEachIndexed { idx, s ->
                val matches = regex.containsMatchIn(s)
                if (matches) {
                    filtered.add(idx to s)
                } else if (!filterOnlyMatches) {
                    filtered.add(idx to s)
                }
            }
            filtered
        }
    }
    
    LaunchedEffect(displayLines.size) {
        if (displayLines.isNotEmpty()) {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems > 0) {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                val isAtBottom = lastVisibleItem == null || lastVisibleItem.index >= totalItems - 5
                if (isAtBottom) {
                    lazyListState.scrollToItem(displayLines.size - 1)
                }
            }
        }
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = palette.textPrimary, fontSize = 11.sp, fontFamily = JetBrainsMono),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surfaceElevated)
                    .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text("search logs...", color = palette.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
                        }
                        inner()
                    }
                }
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(if (isRegex) palette.accent.copy(alpha = 0.20f) else palette.border)
                    .clickable { isRegex = !isRegex }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(".*", color = if (isRegex) palette.accent else palette.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(if (filterOnlyMatches) palette.accent.copy(alpha = 0.20f) else palette.border)
                    .clickable { filterOnlyMatches = !filterOnlyMatches }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text("filter", color = if (filterOnlyMatches) palette.accent else palette.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.error.copy(alpha = 0.16f))
                    .clickable {
                        scope.launch {
                            val errorRegex = Regex("(?i)(error|failed|failure|warning|exception)")
                            val targetIdx = displayLines.indexOfFirst { (_, text) -> errorRegex.containsMatchIn(text) }
                            if (targetIdx != -1) {
                                lazyListState.animateScrollToItem(targetIdx)
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text("JUMP ERROR", color = palette.error, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMono)
            }
        }
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(GitHubControlRadius))
                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .padding(8.dp)
        ) {
            items(displayLines) { (originalIdx, line) ->
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val annotatedText = buildAnnotatedString {
                        if (query.isNotBlank()) {
                            val regex = runCatching {
                                if (isRegex) Regex(query, RegexOption.IGNORE_CASE) else Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
                            }.getOrNull() ?: Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
                            
                            var lastIdx = 0
                            regex.findAll(line).forEach { matchResult ->
                                append(line.substring(lastIdx, matchResult.range.first))
                                pushStyle(SpanStyle(background = palette.accent.copy(alpha = 0.40f), color = palette.background))
                                append(matchResult.value)
                                pop()
                                lastIdx = matchResult.range.last + 1
                            }
                            if (lastIdx < line.length) {
                                append(line.substring(lastIdx))
                            }
                        } else {
                            append(line)
                        }
                    }
                    
                    Text(
                        text = annotatedText.ifEmpty { AnnotatedString(" ") },
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMono,
                        color = when {
                            line.contains("error", true) || line.contains("failed", true) -> palette.error
                            line.contains("warning", true) -> palette.accent
                            else -> palette.textPrimary
                        },
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}
