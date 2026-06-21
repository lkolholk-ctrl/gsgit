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

internal enum class ActionsRunFilter { ALL, ACTIVE, QUEUED, FAILED, SUCCESS, CANCELLED, SKIPPED }

internal enum class RunDetailSection { SUMMARY, JOBS, PIPELINE, ARTIFACTS, CHECKS }

internal const val ACTIONS_INPUT_PREFS = "github_actions_dispatch_inputs"
internal const val ACTIONS_JOB_LOG_TAG = "ActionsJobLog"
internal const val ACTIONS_JOB_LOG_CACHE_BYTES = 5 * 1024 * 1024
internal const val ACTIONS_JOB_LOG_HARD_CAP_BYTES = 10 * 1024 * 1024
internal const val ACTIONS_JOB_LOG_DISPLAY_BYTES = 512 * 1024
internal const val ACTIONS_STEP_LOG_DISPLAY_BYTES = 384 * 1024

internal data class ArtifactGroup(
    val label: String,
    val color: Color,
    val order: Int,
    val items: List<GHArtifact>
)

internal data class MatrixJobGroup(
    val name: String,
    val jobs: List<GHJob>
)

internal sealed class JobListItem {
    data class GroupHeader(val group: MatrixJobGroup, val expanded: Boolean) : JobListItem()
    data class JobRow(val job: GHJob) : JobListItem()
}

internal data class JobLogMeta(
    val cacheFile: File? = null,
    val warning: String? = null,
    val tooLarge: Boolean = false
)

internal data class FailureEvidence(
    val source: String,
    val signalLines: List<String>,
    val tailLines: List<String>,
) {
    val hasContent: Boolean get() = signalLines.isNotEmpty() || tailLines.isNotEmpty()
}

private const val ACTIONS_POLL_DELAY_MS = 5000L
internal const val ACTIONS_BACKOFF_DELAY_MS = 15000L

@Composable
internal fun ActionsTab(
    runs: List<GHWorkflowRun>,
    repo: GHRepo,
    onRunClick: (GHWorkflowRun) -> Unit,
    onShowBuilds: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var liveRuns by remember(runs) { mutableStateOf(runs) }
    var refreshing by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var workflows by remember { mutableStateOf<List<GHWorkflow>>(emptyList()) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedWorkflowId by remember { mutableStateOf<Long?>(null) }
    var workflowDetailId by remember { mutableStateOf<Long?>(null) }
    var selectedBranch by remember(repo.owner, repo.name) { mutableStateOf(repo.defaultBranch) }
    var actionsNotice by remember { mutableStateOf<String?>(null) }
    var dispatchSchema by remember { mutableStateOf<GHWorkflowDispatchSchema?>(null) }
    var dispatchInputValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var dispatching by remember { mutableStateOf(false) }
    var showDeployments by remember { mutableStateOf(false) }
    var showEnvironments by remember { mutableStateOf(false) }
    var showCodespaces by remember { mutableStateOf(false) }

    suspend fun refreshOverview() {
        refreshing = true
        try {
            val fetchedWorkflows = GitHubManager.getWorkflows(context, repo.owner, repo.name)
            workflows = fetchedWorkflows
            if (selectedWorkflowId != null && fetchedWorkflows.none { it.id == selectedWorkflowId }) {
                selectedWorkflowId = null
            }
            if (selectedWorkflowId == null && fetchedWorkflows.isNotEmpty()) {
                selectedWorkflowId = fetchedWorkflows.first().id
            }
            liveRuns = GitHubManager.getWorkflowRuns(
                context = context,
                owner = repo.owner,
                repo = repo.name,
                perPage = 20,
                page = 1
            )
            actionsNotice = null
        } catch (e: Exception) {
            actionsNotice = actionsFriendlyError(e.message)
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(runs) {
        if (liveRuns.isEmpty() && runs.isNotEmpty()) liveRuns = runs
    }

    LaunchedEffect(repo.owner, repo.name) {
        refreshOverview()
        branches = GitHubManager.getBranches(context, repo.owner, repo.name)
    }

    LaunchedEffect(workflows, selectedWorkflowId, selectedBranch) {
        val workflow = workflows.firstOrNull { it.id == selectedWorkflowId }
        val schema = workflow?.let {
            GitHubManager.getWorkflowDispatchSchema(context, repo.owner, repo.name, it.path, selectedBranch)
        }
        dispatchSchema = schema
        dispatchInputValues = if (workflow != null && schema != null) {
            loadSavedDispatchInputValues(context, repo, workflow, schema)
        } else {
            emptyMap()
        }
    }

    LaunchedEffect(liveRuns) {
        while (true) {
            val hasLive = liveRuns.any { isRunActive(it) }
            nowMs = System.currentTimeMillis()
            if (hasLive) {
                delay(if (actionsNotice != null) ACTIONS_BACKOFF_DELAY_MS else ACTIONS_POLL_DELAY_MS)
                if (liveRuns.any { isRunActive(it) }) {
                    try {
                        liveRuns = GitHubManager.getWorkflowRuns(
                            context = context,
                            owner = repo.owner,
                            repo = repo.name,
                            perPage = 20,
                            page = 1
                        )
                        actionsNotice = null
                    } catch (e: Exception) {
                        actionsNotice = actionsFriendlyError(e.message)
                    }
                }
            } else {
                delay(1500)
            }
        }
    }

    val activeCount = remember(liveRuns) { liveRuns.count { isRunActive(it) } }
    val successCount = remember(liveRuns) { liveRuns.count { it.conclusion == "success" } }
    val failedCount = remember(liveRuns) { liveRuns.count { it.conclusion == "failure" } }
    val latestRun = remember(liveRuns) { liveRuns.firstOrNull() }
    val missingRequiredInputs = remember(dispatchSchema, dispatchInputValues) {
        missingDispatchInputs(dispatchSchema, dispatchInputValues)
    }

    BackHandler(enabled = workflowDetailId != null) {
        workflowDetailId = null
    }

    workflowDetailId?.let { workflowId ->
        WorkflowDetailScreen(
            repo = repo,
            workflowId = workflowId,
            onBack = { workflowDetailId = null },
            onRunClick = onRunClick,
        )
        return
    }

    if (showDeployments) {
        GitHubScreenFrame(title = "> deployments", onBack = { showDeployments = false }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                DeploymentsPanel(repo)
            }
        }
        return
    }

    if (showEnvironments) {
        GitHubScreenFrame(title = "> environments", onBack = { showEnvironments = false }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                EnvironmentsPanel(repo)
            }
        }
        return
    }
    if (showCodespaces) {
        GitHubScreenFrame(title = "> codespaces", onBack = { showCodespaces = false }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                CodespacesPanel()
            }
        }
        return
    }

    AiModuleSurface {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        actionsNotice?.let { notice ->
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .border(1.dp, palette.warning.copy(alpha = 0.45f), RoundedCornerShape(GitHubControlRadius))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "!",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text(
                    notice,
                    fontSize = 12.sp,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    lineHeight = 16.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiModulePillButton("builds", onClick = onShowBuilds)
            AiModulePillButton("deployments", onClick = { showDeployments = true })
            AiModulePillButton("envs", onClick = { showEnvironments = true }, accent = false)
            AiModulePillButton("codespaces", onClick = { showCodespaces = true }, accent = false)
        }
        ActionsOverviewHeader(
            workflows = workflows,
            branches = branches,
            canWrite = repo.canWrite(),
            selectedWorkflowId = selectedWorkflowId,
            onSelectWorkflow = { workflowId ->
                selectedWorkflowId = workflowId
            },
            activeCount = activeCount,
            successCount = successCount,
            failedCount = failedCount,
            totalRuns = liveRuns.size,
            selectedBranch = selectedBranch,
            onBranchChange = { selectedBranch = it },
            dispatchSchema = dispatchSchema,
            dispatchInputValues = dispatchInputValues,
            missingRequiredInputs = missingRequiredInputs,
            onDispatchInputChange = { key, value -> dispatchInputValues = dispatchInputValues + (key to value) },
            onToggleWorkflowState = { workflow ->
                scope.launch {
                    val ok = if (workflow.state == "active") {
                        GitHubManager.disableWorkflow(context, repo.owner, repo.name, workflow.id)
                    } else {
                        GitHubManager.enableWorkflow(context, repo.owner, repo.name, workflow.id)
                    }
                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                    if (ok) refreshOverview()
                }
            },
            onOpenWorkflowDetail = { workflow -> workflowDetailId = workflow.id },
            dispatching = dispatching,
            onDispatch = {
                val workflowId = selectedWorkflowId
                val schema = dispatchSchema
                if (workflowId == null) {
                    Toast.makeText(context, Strings.ghNoWorkflows, Toast.LENGTH_SHORT).show()
                } else if (schema == null) {
                    Toast.makeText(context, "Manual launch unavailable", Toast.LENGTH_SHORT).show()
                } else if (selectedBranch.isBlank()) {
                    Toast.makeText(context, "Branch required", Toast.LENGTH_SHORT).show()
                } else if (missingRequiredInputs.isNotEmpty()) {
                    Toast.makeText(context, "Required inputs missing: ${missingRequiredInputs.joinToString(", ")}", Toast.LENGTH_LONG).show()
                } else {
                    dispatching = true
                    scope.launch {
                        try {
                            val knownRunIds = GitHubManager
                                .getWorkflowRuns(context, repo.owner, repo.name, workflowId, perPage = 10)
                                .map { it.id }
                                .toSet()
                            val dispatchInputs = schema.inputs.associate { input ->
                                input.key to dispatchInputValue(input, dispatchInputValues)
                            }.filterValues { it.isNotBlank() }
                            val result = GitHubManager.dispatchWorkflowDetailed(
                                context = context,
                                owner = repo.owner,
                                repo = repo.name,
                                workflowId = workflowId,
                                branch = selectedBranch,
                                inputs = dispatchInputs
                            )
                            Toast.makeText(context, if (result.success) Strings.done else result.message.ifBlank { Strings.error }, Toast.LENGTH_LONG).show()
                            if (result.success) {
                                workflows.firstOrNull { it.id == workflowId }?.let { workflow ->
                                    saveDispatchInputValues(context, repo, workflow, dispatchInputs)
                                }
                                val newRun = findNewActionsDispatchRun(
                                    context = context,
                                    repo = repo,
                                    workflowId = workflowId,
                                    branch = selectedBranch,
                                    knownRunIds = knownRunIds
                                )
                                refreshOverview()
                                if (newRun != null) onRunClick(newRun)
                            }
                        } finally {
                            dispatching = false
                        }
                    }
                }
            },
            refreshing = refreshing,
            onRefresh = { scope.launch { refreshOverview() } },
            latestRun = latestRun,
            nowMs = nowMs,
            onOpenLatestRun = { latestRun?.let(onRunClick) }
        )
        Spacer(Modifier.height(8.dp))
    }
    }
}

@Composable
private fun WorkflowDetailScreen(
    repo: GHRepo,
    workflowId: Long,
    onBack: () -> Unit,
    onRunClick: (GHWorkflowRun) -> Unit,
) {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    var workflow by remember(workflowId) { mutableStateOf<GHWorkflow?>(null) }
    var recentRuns by remember(workflowId) { mutableStateOf<List<GHWorkflowRun>>(emptyList()) }
    var loading by remember(workflowId) { mutableStateOf(true) }
    var reloadNonce by remember { mutableStateOf(0) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(repo.fullName, workflowId, reloadNonce) {
        loading = true
        workflow = GitHubManager.getWorkflow(context, repo.owner, repo.name, workflowId)
        recentRuns = GitHubManager.getWorkflowRuns(
            context = context,
            owner = repo.owner,
            repo = repo.name,
            workflowId = workflowId,
            perPage = 12,
            page = 1
        )
        nowMs = System.currentTimeMillis()
        loading = false
    }

    AiModuleSurface {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(
                title = "> workflow",
                subtitle = workflow?.name ?: repo.fullName,
                onBack = onBack,
                trailing = {
                    workflow?.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        GitHubTopBarAction(
                            glyph = GhGlyphs.OPEN_NEW,
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                                }
                            },
                            tint = palette.accent,
                            contentDescription = "open workflow",
                        )
                    }
                    GitHubTopBarAction(
                        glyph = GhGlyphs.REFRESH,
                        onClick = { reloadNonce++ },
                        tint = palette.accent,
                        enabled = !loading,
                        contentDescription = "refresh workflow",
                    )
                },
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading workflow…")
                }
                workflow == null -> Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.TopStart) {
                    Text("// workflow not found", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 13.sp)
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { WorkflowDetailInfoCard(workflow!!) }
                    item {
                        Text(
                            "recent runs",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        )
                    }
                    if (recentRuns.isEmpty()) {
                        item {
                            Text("// no recent runs", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp)
                        }
                    } else {
                        items(recentRuns) { run ->
                            WorkflowDetailRunRow(run = run, nowMs = nowMs, onClick = { onRunClick(run) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowDetailInfoCard(workflow: GHWorkflow) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                workflow.name.ifBlank { workflow.path.substringAfterLast('/') },
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "[${workflow.state.ifBlank { "unknown" }}]",
                color = if (workflow.state == "active") palette.accent else palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
        WorkflowDetailKv("id", workflow.id.toString())
        WorkflowDetailKv("path", workflow.path)
        WorkflowDetailKv("created", workflow.createdAt.take(19).replace("T", " "))
        WorkflowDetailKv("updated", workflow.updatedAt.take(19).replace("T", " "))
        if (workflow.badgeUrl.isNotBlank()) WorkflowDetailKv("badge", workflow.badgeUrl)
    }
}

@Composable
private fun WorkflowDetailKv(label: String, value: String) {
    if (value.isBlank()) return
    val palette = AiModuleTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.width(58.dp))
        Text(value, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WorkflowDetailRunRow(run: GHWorkflowRun, nowMs: Long, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val color = runStatusColor(run)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#${run.runNumber}", color = color, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(run.name.ifBlank { run.displayTitle }, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(displayRunStatus(run), color = color, fontFamily = JetBrainsMono, fontSize = 11.sp)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (run.branch.isNotBlank()) WorkflowRunMetaChip(run.branch)
            if (run.event.isNotBlank()) WorkflowRunMetaChip(run.event)
            WorkflowRunMetaChip(calcRunDuration(run, nowMs).ifBlank { formatTimeAgoMono(run.createdAt, nowMs) })
        }
    }
}

@Composable
private fun WorkflowRunMetaChip(label: String) {
    if (label.isBlank()) return
    Text(
        label,
        color = AiModuleTheme.colors.textMuted,
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
        modifier = Modifier
            .border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun ActionsOverviewHeader(
    workflows: List<GHWorkflow>,
    branches: List<String>,
    canWrite: Boolean = true,
    selectedWorkflowId: Long?,
    onSelectWorkflow: (Long?) -> Unit,
    activeCount: Int,
    successCount: Int,
    failedCount: Int,
    totalRuns: Int,
    selectedBranch: String,
    onBranchChange: (String) -> Unit,
    dispatchSchema: GHWorkflowDispatchSchema?,
    dispatchInputValues: Map<String, String>,
    missingRequiredInputs: List<String>,
    onDispatchInputChange: (String, String) -> Unit,
    onToggleWorkflowState: (GHWorkflow) -> Unit,
    onOpenWorkflowDetail: (GHWorkflow) -> Unit,
    dispatching: Boolean,
    onDispatch: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    latestRun: GHWorkflowRun?,
    nowMs: Long,
    onOpenLatestRun: () -> Unit
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(top = 4.dp)
    ) {
        // Stats one-liner: total: N  active: N  ok: N  fail: N
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionsStatPair("total", totalRuns.toString(), palette.textPrimary, palette)
            ActionsStatPair("active", activeCount.toString(), palette.warning, palette)
            ActionsStatPair("ok", successCount.toString(), palette.accent, palette)
            ActionsStatPair("fail", failedCount.toString(), palette.error, palette)
        }

        // > WORKFLOW CONTROL  ............................................. ⟳
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "> WORKFLOW CONTROL",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = AiModuleTheme.type.label,
                letterSpacing = 0.6.sp,
            )
            if (refreshing) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner()
                }
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Refresh,
                        null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // workflow:
                ActionsFieldLabel("workflow", palette)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (workflows.isEmpty()) {
                        Text(
                            Strings.ghNoWorkflows,
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                        )
                    } else {
                        workflows.forEach { workflow ->
                            val selected = workflow.id == selectedWorkflowId
                            ActionsTerminalFilterChip(
                                workflow.name.ifBlank { workflow.path.substringAfterLast('/') },
                                selected,
                            ) { onSelectWorkflow(workflow.id) }
                        }
                    }
                }

                // branch:
                ActionsFieldLabel("branch", palette)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (selectedBranch.isEmpty()) {
                            Text(
                                "main",
                                color = palette.textMuted,
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                            )
                        }
                        BasicTextField(
                            value = selectedBranch,
                            onValueChange = onBranchChange,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = palette.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = JetBrainsMono,
                            ),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (branches.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        branches.take(20).forEach { branch ->
                            ActionsTerminalFilterChip(branch, branch == selectedBranch) {
                                onBranchChange(branch)
                            }
                        }
                    }
                }

                val hasInputs = dispatchSchema?.inputs?.isNotEmpty() == true
                if (dispatchSchema == null) {
                    Text(
                        "no workflow_dispatch trigger",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                } else if (!hasInputs) {
                    Text(
                        "no inputs",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                } else {
                    ActionsFieldLabel("inputs", palette)
                    DynamicDispatchInputs(
                        schema = dispatchSchema,
                        values = dispatchInputValues,
                        missingRequiredInputs = missingRequiredInputs,
                        onValueChange = onDispatchInputChange,
                    )
                }

                workflows.firstOrNull { it.id == selectedWorkflowId }?.let { selectedWorkflow ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.border)
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val state = selectedWorkflow.state.ifBlank { "unknown" }
                        val stateColor = if (selectedWorkflow.state == "active") palette.accent else palette.textSecondary
                        Text(
                            "[$state]",
                            color = stateColor,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            selectedWorkflow.path,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMono,
                            color = palette.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        GitHubTerminalButton(
                            label = "details",
                            onClick = { onOpenWorkflowDetail(selectedWorkflow) },
                            color = palette.textSecondary,
                        )
                        if (canWrite) {
                            val isActive = selectedWorkflow.state == "active"
                            if (isActive) {
                                GitHubTerminalButton(
                                    label = "disable",
                                    onClick = { onToggleWorkflowState(selectedWorkflow) },
                                    color = palette.error,
                                )
                            } else {
                                GitHubTerminalButton(
                                    label = "enable",
                                    onClick = { onToggleWorkflowState(selectedWorkflow) },
                                    color = palette.accent,
                                )
                            }
                        } else {
                            GitHubPermissionHint("write required")
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (latestRun != null) {
                        GitHubTerminalButton(
                            label = "latest #${latestRun.runNumber} \u2192",
                            onClick = onOpenLatestRun,
                            color = palette.textSecondary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (!canWrite) {
                        GitHubPermissionHint("write required")
                    }
                    GitHubTerminalButton(
                        label = if (dispatching) "dispatching..." else "▶ run →",
                        onClick = onDispatch,
                        color = if (canWrite) palette.accent else palette.textMuted,
                        enabled = canWrite &&
                            !dispatching &&
                            workflows.isNotEmpty() &&
                            dispatchSchema != null &&
                            missingRequiredInputs.isEmpty(),
                    )
                }

                latestRun?.let { run ->
                    val badge = aiModuleStatusBadge(run.status, run.conclusion, palette)
                    val elapsed = calcRunDuration(run, nowMs)
                    val parts = buildList {
                        add("latest:")
                        add(badge.glyph)
                        add(displayRunStatus(run))
                        if (run.branch.isNotBlank()) add(run.branch)
                        if (run.event.isNotBlank()) add(run.event)
                        if (elapsed.isNotBlank()) add(elapsed)
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        parts.forEachIndexed { idx, part ->
                            val color = when {
                                idx == 0 -> palette.textMuted
                                idx == 1 -> badge.color.copy(alpha = badge.alpha)
                                idx == 2 -> badge.color.copy(alpha = badge.alpha)
                                else -> palette.textSecondary
                            }
                            Text(
                                part,
                                color = color,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ActionsStatPair(
    label: String,
    value: String,
    valueColor: Color,
    palette: gs.git.vps.ui.theme.AiModuleColors,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label:",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ActionsFieldLabel(
    label: String,
    palette: gs.git.vps.ui.theme.AiModuleColors,
) {
    Text(
        "$label:",
        color = palette.textSecondary,
        fontFamily = JetBrainsMono,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun InputGroup(label: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel(label)
        content()
    }
}

@Composable
internal fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = AiModuleTheme.colors
    Column(
        modifier
            .ghGlassCard(12.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            value,
            fontSize = 22.sp,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            color = colors.textMuted,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun githubStatusFilter(filter: ActionsRunFilter): String? = when (filter) {
    ActionsRunFilter.ALL -> null
    ActionsRunFilter.ACTIVE -> null
    ActionsRunFilter.QUEUED -> "queued"
    ActionsRunFilter.FAILED -> "failure"
    ActionsRunFilter.SUCCESS -> "success"
    ActionsRunFilter.CANCELLED -> "cancelled"
    ActionsRunFilter.SKIPPED -> "skipped"
}

internal fun isRunActive(run: GHWorkflowRun): Boolean =
    run.status in setOf("queued", "pending", "waiting", "requested", "in_progress")

internal fun isJobActive(job: GHJob): Boolean =
    job.status in setOf("queued", "pending", "waiting", "requested", "in_progress")

internal fun displayRunStatus(run: GHWorkflowRun): String {
    return when {
        run.status == "queued" -> "queued"
        run.status == "pending" -> "pending"
        run.status == "waiting" -> "waiting"
        run.status == "requested" -> "queued"
        run.status == "in_progress" -> "running"
        run.conclusion == "success" -> "success"
        run.conclusion == "failure" -> "failed"
        run.conclusion == "cancelled" -> "cancelled"
        run.conclusion == "skipped" -> "skipped"
        run.conclusion == "neutral" -> "neutral"
        run.conclusion == "timed_out" -> "timed out"
        run.status == "completed" -> "completed"
        else -> run.status.ifBlank { "unknown" }
    }
}

private fun buildRunSummary(run: GHWorkflowRun, elapsed: String): String {
    val parts = mutableListOf<String>()
    if (run.actor.isNotBlank()) parts += "by ${run.actor}"
    if (run.updatedAt.isNotBlank()) parts += run.updatedAt.take(19).replace('T', ' ')
    if (elapsed.isNotBlank()) parts += elapsed
    return parts.joinToString(" • ")
}

@Composable
internal fun runStatusColor(run: GHWorkflowRun): Color = when {
    run.status == "in_progress" -> AiModuleTheme.colors.accent
    run.conclusion == "success" -> AiModuleTheme.colors.accent
    run.conclusion == "failure" -> AiModuleTheme.colors.error
    run.status in setOf("queued", "pending", "waiting", "requested") -> AiModuleTheme.colors.textMuted
    run.conclusion == "cancelled" -> AiModuleTheme.colors.textMuted
    run.conclusion == "skipped" || run.conclusion == "neutral" -> AiModuleTheme.colors.textMuted
    run.conclusion == "timed_out" -> AiModuleTheme.colors.error
    else -> AiModuleTheme.colors.textMuted
}

internal fun runStatusIcon(run: GHWorkflowRun) = when {
    run.status in setOf("queued", "pending", "waiting", "requested") -> Icons.Rounded.Schedule
    run.status == "in_progress" -> Icons.Rounded.Refresh
    run.conclusion == "success" -> Icons.Rounded.CheckCircle
    run.conclusion == "failure" -> Icons.Rounded.Error
    run.conclusion == "cancelled" -> Icons.Rounded.Cancel
    run.conclusion == "skipped" || run.conclusion == "neutral" -> Icons.Rounded.Warning
    run.conclusion == "timed_out" -> Icons.Rounded.Schedule
    else -> Icons.Rounded.Warning
}

@Composable
internal fun MiniActionsBadge(text: String, color: Color) {
    val colors = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(colors.background)
            .border(1.dp, colors.border.copy(alpha = 0.10f), RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}

