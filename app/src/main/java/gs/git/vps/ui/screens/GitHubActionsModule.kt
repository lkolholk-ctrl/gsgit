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

private enum class ActionsRunFilter { ALL, ACTIVE, QUEUED, FAILED, SUCCESS, CANCELLED, SKIPPED }

internal enum class RunDetailSection { SUMMARY, JOBS, PIPELINE, ARTIFACTS, CHECKS }

private const val ACTIONS_INPUT_PREFS = "github_actions_dispatch_inputs"
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
internal fun ActionsHistoryTab(
    runs: List<GHWorkflowRun>,
    repo: GHRepo,
    onRunClick: (GHWorkflowRun) -> Unit
) {
    val context = LocalContext.current
    var workflows by remember { mutableStateOf<List<GHWorkflow>>(emptyList()) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(repo.owner, repo.name) {
        workflows = GitHubManager.getWorkflows(context, repo.owner, repo.name)
        branches = GitHubManager.getBranches(context, repo.owner, repo.name)
    }

    ActionsRunsHistoryScreen(
        repo = repo,
        workflows = workflows,
        branches = branches,
        initialRuns = runs,
        showTopBar = false,
        onBack = {},
        onRunClick = onRunClick
    )
}

private suspend fun findNewActionsDispatchRun(
    context: android.content.Context,
    repo: GHRepo,
    workflowId: Long,
    branch: String,
    knownRunIds: Set<Long>
): GHWorkflowRun? {
    repeat(10) {
        delay(1500)
        val runs = GitHubManager.getWorkflowRuns(context, repo.owner, repo.name, workflowId, perPage = 10)
        val dispatchRuns = runs.filter { candidate ->
            val newRun = candidate.id !in knownRunIds
            val dispatchRun = candidate.event == "workflow_dispatch"
            newRun && dispatchRun
        }
        val run = dispatchRuns.firstOrNull { candidate ->
            candidate.branch.isBlank() || branch.isBlank() || candidate.branch == branch
        } ?: dispatchRuns.firstOrNull()
        if (run != null) return run
    }
    return null
}

@Composable
private fun ActionsRunsHistoryScreen(
    repo: GHRepo,
    workflows: List<GHWorkflow>,
    branches: List<String>,
    initialRuns: List<GHWorkflowRun>,
    showTopBar: Boolean = true,
    onBack: () -> Unit,
    onRunClick: (GHWorkflowRun) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var runs by remember(initialRuns) { mutableStateOf(initialRuns) }
    var query by rememberSaveable(repo.fullName, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var filter by rememberSaveable(repo.fullName) { mutableStateOf(ActionsRunFilter.ALL) }
    var selectedWorkflowId by rememberSaveable(repo.fullName) { mutableStateOf<Long?>(null) }
    var selectedBranch by rememberSaveable(repo.fullName) { mutableStateOf<String?>(null) }
    var selectedEvent by rememberSaveable(repo.fullName) { mutableStateOf<String?>(null) }
    var onlyMine by rememberSaveable(repo.fullName) { mutableStateOf(false) }
    var page by rememberSaveable(repo.fullName) { mutableStateOf(1) }
    var hasMore by rememberSaveable(repo.fullName) { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pullDistance by remember { mutableStateOf(0f) }
    val listState = rememberSaveable(repo.fullName, "actions-history", saver = LazyListState.Saver) { LazyListState(0, 0) }
    val currentLogin = remember { GitHubManager.getCachedUser(context)?.login.orEmpty() }

    suspend fun load(reset: Boolean = true) {
        refreshing = true
        try {
            val nextPage = if (reset) 1 else page + 1
            val fetched = GitHubManager.getWorkflowRuns(
                context = context,
                owner = repo.owner,
                repo = repo.name,
                workflowId = selectedWorkflowId,
                perPage = 30,
                page = nextPage,
                branch = selectedBranch,
                event = selectedEvent,
                status = githubStatusFilter(filter)
            )
            page = nextPage
            hasMore = fetched.size >= 30
            runs = if (reset) fetched else (runs + fetched).distinctBy { it.id }
        } finally {
            refreshing = false
        }
    }

    LaunchedEffect(repo.owner, repo.name, selectedWorkflowId, selectedBranch, selectedEvent, filter) {
        load(reset = true)
    }

    LaunchedEffect(runs) {
        while (true) {
            val hasLive = runs.any { isRunActive(it) }
            nowMs = System.currentTimeMillis()
            if (hasLive) {
                delay(5000)
                if (runs.any { isRunActive(it) }) load(reset = true)
            } else {
                delay(1500)
            }
        }
    }

    val visibleRuns = remember(runs, query.text, filter, onlyMine, currentLogin) {
        val q = query.text.trim()
        runs.filter { run ->
            val passesFilter = when (filter) {
                ActionsRunFilter.ALL -> true
                ActionsRunFilter.ACTIVE -> isRunActive(run)
                ActionsRunFilter.QUEUED -> run.status in setOf("queued", "pending", "waiting", "requested")
                ActionsRunFilter.FAILED -> run.conclusion == "failure"
                ActionsRunFilter.SUCCESS -> run.conclusion == "success"
                ActionsRunFilter.CANCELLED -> run.conclusion == "cancelled"
                ActionsRunFilter.SKIPPED -> run.conclusion == "skipped"
            }
            val passesQuery = q.isBlank() ||
                run.name.contains(q, true) ||
                run.displayTitle.contains(q, true) ||
                run.branch.contains(q, true) ||
                run.event.contains(q, true) ||
                run.actor.contains(q, true) ||
                run.headSha.contains(q, true) ||
                run.runNumber.toString().contains(q)
            val passesMine = !onlyMine || currentLogin.isNotBlank() && run.actor.equals(currentLogin, ignoreCase = true)
            passesFilter && passesQuery && passesMine
        }
    }

    AiModuleSurface {
        val palette = AiModuleTheme.colors
        Column(Modifier.fillMaxSize().background(palette.background)) {
            if (showTopBar) {
                GitHubPageBar(
                    title = "> runs",
                    subtitle = "${visibleRuns.size} workflow runs",
                    onBack = onBack,
                    trailing = {
                        if (refreshing) {
                            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                AiModuleSpinner()
                            }
                        } else {
                            AiModuleGlyphAction(
                                glyph = GhGlyphs.REFRESH,
                                onClick = { scope.launch { load(reset = true) } },
                                tint = palette.textSecondary,
                                contentDescription = "refresh",
                            )
                        }
                    },
                )
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                AiModuleSearchField(
                    value = query.text,
                    onValueChange = { query = TextFieldValue(it) },
                    placeholder = "name / branch / sha / actor",
                )
            }

            ActionsTerminalFilterRow {
                ActionsTerminalFilterChip("all", filter == ActionsRunFilter.ALL) { filter = ActionsRunFilter.ALL }
                ActionsTerminalFilterChip("active", filter == ActionsRunFilter.ACTIVE) { filter = ActionsRunFilter.ACTIVE }
                ActionsTerminalFilterChip("queued", filter == ActionsRunFilter.QUEUED) { filter = ActionsRunFilter.QUEUED }
                ActionsTerminalFilterChip("ok", filter == ActionsRunFilter.SUCCESS) { filter = ActionsRunFilter.SUCCESS }
                ActionsTerminalFilterChip("fail", filter == ActionsRunFilter.FAILED) { filter = ActionsRunFilter.FAILED }
                ActionsTerminalFilterChip("cancel", filter == ActionsRunFilter.CANCELLED) { filter = ActionsRunFilter.CANCELLED }
                ActionsTerminalFilterChip("skip", filter == ActionsRunFilter.SKIPPED) { filter = ActionsRunFilter.SKIPPED }
                if (currentLogin.isNotBlank()) {
                    ActionsTerminalFilterChip("mine", onlyMine) { onlyMine = !onlyMine }
                }
            }

            if (workflows.isNotEmpty()) {
                ActionsTerminalFilterRow {
                    ActionsTerminalFilterChip("all wf", selectedWorkflowId == null) { selectedWorkflowId = null }
                    workflows.forEach { workflow ->
                        ActionsTerminalFilterChip(
                            workflow.name.ifBlank { workflow.path.substringAfterLast('/') },
                            selectedWorkflowId == workflow.id,
                        ) { selectedWorkflowId = workflow.id }
                    }
                }
            }

            if (branches.isNotEmpty()) {
                ActionsTerminalFilterRow {
                    ActionsTerminalFilterChip("all br", selectedBranch == null) { selectedBranch = null }
                    branches.forEach { branch ->
                        ActionsTerminalFilterChip(branch, selectedBranch == branch) { selectedBranch = branch }
                    }
                }
            }

            ActionsTerminalFilterRow {
                ActionsTerminalFilterChip("all ev", selectedEvent == null) { selectedEvent = null }
                listOf("workflow_dispatch", "push", "pull_request", "schedule").forEach { event ->
                    ActionsTerminalFilterChip(event, selectedEvent == event) { selectedEvent = event }
                }
            }
            ActionsTerminalFilterRow {
                GitHubTerminalButton(
                    "export visible",
                    onClick = {
                        val workflowName = workflows.firstOrNull { it.id == selectedWorkflowId }?.name ?: "all"
                        val file = saveActionsRunsExport(
                            repo = repo,
                            runs = visibleRuns,
                            filterSummary = listOf(
                                "query=${query.text.trim().ifBlank { "all" }}",
                                "filter=${filter.name.lowercase(Locale.US)}",
                                "workflow=$workflowName",
                                "branch=${selectedBranch ?: "all"}",
                                "event=${selectedEvent ?: "all"}",
                                "mine=$onlyMine",
                                "loaded=${runs.size}",
                            ).joinToString(", ")
                        )
                        Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                    },
                    enabled = visibleRuns.isNotEmpty(),
                    color = Blue,
                )
            }

            Spacer(Modifier.height(4.dp))

            if (visibleRuns.isEmpty() && !refreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "no runs yet",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().pointerInput(
                        refreshing,
                        listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset,
                    ) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (pullDistance > 140f && !refreshing) {
                                    scope.launch { load(reset = true) }
                                }
                                pullDistance = 0f
                            },
                            onDragCancel = { pullDistance = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0 &&
                                    dragAmount > 0
                                ) {
                                    pullDistance += dragAmount
                                }
                            },
                        )
                    },
                    state = listState,
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 16.dp),
                ) {
                    if (pullDistance > 28f || refreshing) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AiModuleSpinner(
                                    label = if (refreshing) "loading runs\u2026" else "release to refresh",
                                )
                            }
                        }
                    }
                    items(visibleRuns) { run ->
                        WorkflowRunRow(run = run, nowMs = nowMs) { onRunClick(run) }
                    }
                    if (hasMore) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AiModuleSecondaryButton(
                                    label = if (refreshing) "loading\u2026" else "load more \u2192",
                                    onClick = { scope.launch { load(reset = false) } },
                                    enabled = !refreshing,
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
private fun ActionsTerminalFilterRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun ActionsTerminalFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val tint = if (selected) palette.accent else palette.textSecondary
    val bg = if (selected) palette.accent.copy(alpha = 0.10f) else Color.Transparent
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(bg)
            .border(1.dp, tint, RoundedCornerShape(GitHubControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = tint,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkflowRunRow(run: GHWorkflowRun, nowMs: Long, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val badge = aiModuleStatusBadge(run.status, run.conclusion, palette)
    val duration = if (isRunActive(run)) "running" else calcRunDuration(run, nowMs).ifBlank { "\u2014" }
    val sha = if (run.headSha.length >= 7) run.headSha.take(7) else "\u2014"
    val branch = run.branch.ifBlank { "\u2014" }
    val name = run.name.ifBlank { "workflow" }
    val actor = run.actor.ifBlank { "\u2014" }
    val ago = formatTimeAgoMono(run.updatedAt, nowMs)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            badge.glyph,
            color = badge.color.copy(alpha = badge.alpha),
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            modifier = Modifier.width(20.dp),
        )
        Text(
            "#${run.runNumber}",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.width(58.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            name,
            color = palette.textPrimary,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            modifier = Modifier.width(110.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            duration,
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.width(72.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            branch,
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.width(108.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            sha,
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.width(72.dp),
            maxLines = 1,
        )
        Text(
            actor,
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            ago,
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

private fun formatTimeAgoMono(iso: String, nowMs: Long): String {
    val ms = parseIsoMs(iso) ?: return "\u2014"
    val diff = (nowMs - ms).coerceAtLeast(0L)
    val sec = diff / 1000
    return when {
        sec < 60 -> "${sec}s"
        sec < 3600 -> "${sec / 60}m"
        sec < 86400 -> "${sec / 3600}h"
        sec < 604800 -> "${sec / 86400}d"
        else -> "${sec / 604800}w"
    }
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

@Composable
private fun DynamicDispatchInputs(
    schema: GHWorkflowDispatchSchema?,
    values: Map<String, String>,
    missingRequiredInputs: List<String>,
    onValueChange: (String, String) -> Unit
) {
    val inputs = schema?.inputs.orEmpty()
    val palette = AiModuleTheme.colors
    if (schema == null) {
        Text("This workflow has no workflow_dispatch trigger", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
        return
    }
    if (inputs.isEmpty()) {
        Text("This workflow has no workflow_dispatch inputs", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (missingRequiredInputs.isNotEmpty()) {
            Text(
                "Required inputs missing: ${missingRequiredInputs.joinToString(", ")}",
                fontSize = 11.sp,
                color = palette.warning,
                fontFamily = JetBrainsMono
            )
        }
        inputs.forEach { input ->
            WorkflowDispatchInputField(
                input = input,
                value = values[input.key].orEmpty(),
                onValueChange = { onValueChange(input.key, it) }
            )
        }
    }
}

@Composable
private fun WorkflowDispatchInputField(
    input: GHWorkflowDispatchInput,
    value: String,
    onValueChange: (String) -> Unit
) {
    val choices = dispatchInputChoices(input)
    val missingRequired = input.required && dispatchInputValue(input, mapOf(input.key to value)).isBlank()
    val palette = AiModuleTheme.colors
    val isDefault = value == input.defaultValue
    
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(input.key, fontSize = 12.sp, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            if (input.required) MiniActionsBadge("required", palette.warning)
            if (input.type.isNotBlank()) MiniActionsBadge(input.type, palette.textSecondary)
            Spacer(modifier = Modifier.weight(1f))
            if (!isDefault) {
                Text(
                    text = "reset to default",
                    fontSize = 10.sp,
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    modifier = Modifier.clickable { onValueChange(input.defaultValue) }
                )
            }
        }
        if (input.description.isNotBlank()) {
            Text(input.description, fontSize = 10.sp, color = palette.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        
        if (input.type.lowercase() == "boolean") {
            val isTrue = value.lowercase() == "true"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surfaceElevated)
                    .border(1.dp, if (isTrue) palette.accent.copy(alpha = 0.5f) else palette.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onValueChange((!isTrue).toString()) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isTrue) "Enabled (true)" else "Disabled (false)",
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMono,
                    color = if (isTrue) palette.accent else palette.textSecondary
                )
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTrue) palette.accent.copy(alpha = 0.2f) else palette.border.copy(alpha = 0.3f))
                        .border(1.dp, if (isTrue) palette.accent else palette.border, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    contentAlignment = if (isTrue) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isTrue) palette.accent else palette.textMuted)
                    )
                }
            }
        } else if (choices.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value.ifBlank { "Select option..." },
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMono,
                        color = if (value.isNotBlank()) palette.textPrimary else palette.textMuted
                    )
                    Text(
                        text = "▼",
                        fontSize = 8.sp,
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                ) {
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    choice, 
                                    fontFamily = JetBrainsMono, 
                                    fontSize = 12.sp, 
                                    color = if (value == choice) palette.accent else palette.textPrimary 
                                ) 
                            },
                            onClick = {
                                onValueChange(choice)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = palette.textPrimary,
                                leadingIconColor = palette.accent
                            )
                        )
                    }
                }
            }
        } else {
            AiModuleTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = input.key,
                modifier = Modifier.fillMaxWidth(),
                singleLine = input.type.lowercase() != "environment"
            )
        }
        if (missingRequired) {
            Text("Required value", fontSize = 10.sp, color = palette.warning)
        }
    }
}

private fun dispatchInputChoices(input: GHWorkflowDispatchInput): List<String> = when {
    input.options.isNotEmpty() -> input.options
    input.type.equals("boolean", ignoreCase = true) -> listOf("true", "false")
    else -> emptyList()
}

private fun missingDispatchInputs(schema: GHWorkflowDispatchSchema?, values: Map<String, String>): List<String> =
    schema?.inputs.orEmpty()
        .filter { it.required && dispatchInputValue(it, values).isBlank() }
        .map { it.key }

private fun dispatchInputValue(input: GHWorkflowDispatchInput, values: Map<String, String>): String =
    values[input.key].orEmpty().ifBlank { input.defaultValue }.trim()

private fun loadSavedDispatchInputValues(
    context: Context,
    repo: GHRepo,
    workflow: GHWorkflow,
    schema: GHWorkflowDispatchSchema
): Map<String, String> {
    val prefs = context.getSharedPreferences(ACTIONS_INPUT_PREFS, Context.MODE_PRIVATE)
    return schema.inputs.associate { input ->
        input.key to (prefs.getString(dispatchInputPrefKey(repo, workflow, input.key), null) ?: input.defaultValue)
    }
}

private fun saveDispatchInputValues(
    context: Context,
    repo: GHRepo,
    workflow: GHWorkflow,
    values: Map<String, String>
) {
    val editor = context.getSharedPreferences(ACTIONS_INPUT_PREFS, Context.MODE_PRIVATE).edit()
    values.forEach { (key, value) ->
        editor.putString(dispatchInputPrefKey(repo, workflow, key), value)
    }
    editor.apply()
}

private fun dispatchInputPrefKey(repo: GHRepo, workflow: GHWorkflow, inputKey: String): String =
    "${repo.owner}/${repo.name}/${workflow.id}/$inputKey"

@Composable
private fun ModernRunCard(
    run: GHWorkflowRun,
    nowMs: Long,
    canWrite: Boolean = true,
    onRunClick: () -> Unit,
    onCancel: () -> Unit,
    onRerun: () -> Unit
) {
    val statusColor = runStatusColor(run)
    val live = isRunActive(run)
    val elapsed = calcRunDuration(run, nowMs)
    val colors = AiModuleTheme.colors

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .ghGlassCard(12.dp)
            .clickable(onClick = onRunClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                if (run.actorAvatar.isNotBlank()) {
                    AsyncImage(
                        model = run.actorAvatar,
                        contentDescription = run.actor,
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(runStatusIcon(run), null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }
                }
                if (live) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .padding(1.dp)
                    ) {
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape).background(statusColor)
                        )
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    run.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (run.displayTitle.isNotBlank()) {
                    Text(
                        run.displayTitle,
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                "#${run.runNumber}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted,
                maxLines = 1
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniActionsBadge(displayRunStatus(run), statusColor)
            if (run.branch.isNotBlank()) MiniActionsBadge(run.branch, Blue)
            if (run.event.isNotBlank()) MiniActionsBadge(run.event, Purple)
            if (run.runAttempt > 1) MiniActionsBadge("attempt ${run.runAttempt}", Orange)
        }

        val footerParts = buildList {
            if (run.actor.isNotBlank()) add(run.actor)
            if (elapsed.isNotBlank()) add(elapsed)
            if (run.headSha.length >= 7) add(run.headSha.take(7))
        }
        if (footerParts.isNotEmpty()) {
            Text(
                footerParts.joinToString("  ·  "),
                fontSize = 11.sp,
                color = colors.textMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.10f))
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canWrite) {
                if (live) {
                    Chip(Icons.Rounded.Cancel, Strings.cancel, Red) { onCancel() }
                } else {
                    Chip(Icons.Rounded.Refresh, Strings.ghRerun) { onRerun() }
                }
            }
            Spacer(Modifier.weight(1f))
            Chip(Icons.Rounded.Article, "Open") { onRunClick() }
        }
    }
}

private fun githubStatusFilter(filter: ActionsRunFilter): String? = when (filter) {
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
private fun runStatusColor(run: GHWorkflowRun): Color = when {
    run.status == "in_progress" -> AiModuleTheme.colors.accent
    run.conclusion == "success" -> AiModuleTheme.colors.accent
    run.conclusion == "failure" -> AiModuleTheme.colors.error
    run.status in setOf("queued", "pending", "waiting", "requested") -> AiModuleTheme.colors.textMuted
    run.conclusion == "cancelled" -> AiModuleTheme.colors.textMuted
    run.conclusion == "skipped" || run.conclusion == "neutral" -> AiModuleTheme.colors.textMuted
    run.conclusion == "timed_out" -> AiModuleTheme.colors.error
    else -> AiModuleTheme.colors.textMuted
}

private fun runStatusIcon(run: GHWorkflowRun) = when {
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

