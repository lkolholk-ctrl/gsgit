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

/**
 * История ранов Actions: ActionsHistoryTab, ActionsRunsHistoryScreen (список с фильтрами),
 * WorkflowRunRow, фильтр-chips, formatTimeAgo. Вынесено из GitHubActionsModule.kt (Фаза 1).
 */

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

internal suspend fun findNewActionsDispatchRun(
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
internal fun ActionsTerminalFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

internal fun formatTimeAgoMono(iso: String, nowMs: Long): String {
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

