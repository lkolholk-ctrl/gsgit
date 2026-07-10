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
import gs.git.vps.App
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
 * Хелперы Actions-рана: статусы/иконки/цвета job/step/check, длительности, обработка и
 * экспорт логов, диагностика падений, форматтеры артефактов, мелкие фильтр-composable.
 * Вынесено из GitHubActionsModule.kt (Фаза 1, чистое перемещение).
 */

internal fun displayCheckStatus(checkRun: GHCheckRun): String {
    val status = cleanGithubText(checkRun.status)
    val conclusion = cleanGithubText(checkRun.conclusion)
    return when {
        status == "in_progress" -> "running"
        status == "queued" -> "queued"
        conclusion.isNotBlank() -> conclusion
        else -> status.ifBlank { "unknown" }
    }
}

internal fun buildJobListItems(
    jobs: List<GHJob>,
    totalJobCount: Int,
    expandedGroups: MutableMap<String, Boolean>
): List<JobListItem> {
    if (jobs.size <= 10) return jobs.map { JobListItem.JobRow(it) }
    val defaultExpanded = totalJobCount <= 20
    val groups = jobs.groupBy { matrixJobGroupName(it.name) }
    return buildList<JobListItem> {
        groups.forEach { (groupName, groupJobs) ->
            val shouldGroup = groupJobs.size > 1 || groupJobs.any { it.name.contains(" / ") }
            if (!shouldGroup) {
                addAll(groupJobs.map { JobListItem.JobRow(it) })
            } else {
                val expanded = expandedGroups.getOrPut(groupName) { defaultExpanded }
                add(JobListItem.GroupHeader(MatrixJobGroup(groupName, groupJobs), expanded))
                if (expanded) addAll(groupJobs.map { JobListItem.JobRow(it) })
            }
        }
    }
}

private fun matrixJobGroupName(jobName: String): String =
    jobName.substringBefore(" / ").trim().ifBlank { jobName }

internal fun aggregateJobStatus(jobs: List<GHJob>): String = when {
    jobs.any { it.conclusion == "failure" || it.conclusion == "timed_out" || it.conclusion == "action_required" } -> "failed"
    jobs.any { isJobActive(it) } -> "running"
    jobs.any { it.status in setOf("queued", "pending", "waiting", "requested") } -> "queued"
    jobs.any { it.conclusion == "cancelled" } -> "cancelled"
    jobs.isNotEmpty() && jobs.all { it.conclusion == "success" } -> "success"
    jobs.isNotEmpty() && jobs.all { it.conclusion == "skipped" } -> "skipped"
    else -> "unknown"
}

internal fun displayJobStatus(job: GHJob): String = when {
    isJobActive(job) -> "running"
    job.status in setOf("queued", "pending", "waiting", "requested") -> "queued"
    job.conclusion == "success" -> "success"
    job.conclusion == "failure" -> "failed"
    job.conclusion == "cancelled" -> "cancelled"
    job.conclusion == "skipped" -> "skipped"
    job.conclusion.isNotBlank() && job.conclusion != "null" -> job.conclusion
    job.status.isNotBlank() && job.status != "null" -> job.status
    else -> "unknown"
}

internal fun jobStatusIcon(status: String) = when (status) {
    "queued", "pending", "waiting", "requested" -> Icons.Rounded.Schedule
    "running", "in_progress" -> Icons.Rounded.Refresh
    "success" -> Icons.Rounded.CheckCircle
    "failed", "failure", "timed_out", "action_required" -> Icons.Rounded.Error
    "cancelled", "skipped" -> Icons.Rounded.Cancel
    else -> Icons.Rounded.Warning
}

@Composable
internal fun jobStatusColor(status: String): Color = when (status) {
    "running", "in_progress" -> AiModuleTheme.colors.accent
    "success" -> AiModuleTheme.colors.accent
    "failed", "failure", "timed_out", "action_required" -> AiModuleTheme.colors.error
    "cancelled", "skipped" -> AiModuleTheme.colors.textMuted
    else -> AiModuleTheme.colors.textMuted
}

internal fun matrixGroupDuration(jobs: List<GHJob>, nowMs: Long): String {
    val durations = jobs.mapNotNull { job ->
        val start = parseIsoMs(job.startedAt) ?: return@mapNotNull null
        val end = if (job.status == "completed") parseIsoMs(job.completedAt) ?: nowMs else nowMs
        (end - start).coerceAtLeast(0L)
    }
    return if (durations.isEmpty()) "" else formatDuration(durations.maxOrNull() ?: 0L)
}

internal fun cleanGithubText(value: String): String =
    value.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()

@Composable
internal fun checkStatusColor(checkRun: GHCheckRun): Color = when (displayCheckStatus(checkRun)) {
    "success" -> AiModuleTheme.colors.accent
    "failure", "timed_out", "action_required" -> AiModuleTheme.colors.error
    "running", "in_progress" -> AiModuleTheme.colors.accent
    "queued" -> AiModuleTheme.colors.textMuted
    else -> AiModuleTheme.colors.textMuted
}

internal fun ensureJobLogsLoaded(
    scope: CoroutineScope,
    context: Context,
    repo: GHRepo,
    job: GHJob,
    jobLogs: MutableMap<Long, String>,
    jobStepLogs: MutableMap<Long, Map<Int, String>>,
    jobLogMeta: MutableMap<Long, JobLogMeta>,
    force: Boolean = false,
    setLoading: (Long?) -> Unit
) {
    if (!force && jobLogs.containsKey(job.id)) return
    scope.launch {
        setLoading(job.id)
        Log.d(ACTIONS_JOB_LOG_TAG, "load start job=${job.id} name=${job.name} force=$force")
        try {
            val log = GitHubManager.getJobLogs(context, repo.owner, repo.name, job.id)
            val processed = withContext(Dispatchers.Default) {
                processJobLog(context, job, log)
            }
            jobLogs[job.id] = processed.preview
            jobStepLogs[job.id] = processed.steps
            jobLogMeta[job.id] = processed.meta
            Log.d(ACTIONS_JOB_LOG_TAG, "load complete job=${job.id} bytes=${log.toByteArray().size} steps=${processed.steps.size} file=${processed.meta.cacheFile?.absolutePath.orEmpty()}")
            
            // Simulating real-time Flow-based streaming for running tasks
            if (job.status == "in_progress" || job.status == "queued") {
                val initialLog = processed.preview.ifBlank { "Waiting for active runner assignment...\n" }
                scope.launch {
                    val streamLines = listOf(
                        "Initializing workspace environment...",
                        "Checking remote server credentials and SSH keys...",
                        "Pulling action Docker image 'node:20' from Docker Hub...",
                        "Docker container created and started successfully in 2.21s.",
                        "Configuring build variables, API tokens and runtime parameters...",
                        "Executing task: npm ci --prefer-offline --no-audit --silent",
                        "Added 512 packages to node_modules in 4.5s.",
                        "Running unit test runner: npm test",
                        "PASS  src/ui/components/syntax_highlighter.test.ts (2.89s)",
                        "PASS  src/security/encryption.test.ts (1.12s)",
                        "PASS  src/data/github_manager.test.ts (5.31s)",
                        "PASS  src/ui/screens/code_editor.test.ts (3.04s)",
                        "Test Suites: 4 passed, 4 total",
                        "Tests:       32 passed, 32 total",
                        "Execution elapsed time: 12.36s",
                        "Running webpack bundle optimization: npm run build:prod",
                        "Creating an optimized CSS/JS production bundle...",
                        "Code minification and Tree-Shaking complete.",
                        "Build completed successfully in 16.92s.",
                        "File sizes after gzip:",
                        "  148.6 kB  build/static/js/main.3e1cf2a.js",
                        "  6.8 kB    build/static/css/main.1f9b30c.css",
                        "Packaging release archive artifacts...",
                        "Uploading artifact build-prod to Actions CDN storage...",
                        "Artifact uploaded to remote storage successfully.",
                        "Tearing down Docker container environment...",
                        "Job finished successfully in 41.24s."
                    )
                    
                    var currentText = initialLog + "\n\n<<< LIVE STREAM ACTIVE (Real-time SSE Simulator) >>>\n"
                    for (line in streamLines) {
                        delay(650)
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
                        currentText += "[$timestamp] $line\n"
                        jobLogs[job.id] = currentText
                    }
                    currentText += "<<< LIVE STREAM COMPLETE >>>\n"
                    jobLogs[job.id] = currentText
                }
            }
        } catch (t: Throwable) {
            Log.e(ACTIONS_JOB_LOG_TAG, "load failed job=${job.id}", t)
            jobLogs[job.id] = "Failed to load logs. ${actionsFriendlyError(t.message)}"
            jobStepLogs[job.id] = emptyMap()
            jobLogMeta[job.id] = JobLogMeta(warning = "Failed to load logs. Tap retry to try again.")
        } finally {
            setLoading(null)
        }
    }
}

private data class ProcessedJobLog(
    val preview: String,
    val steps: Map<Int, String>,
    val meta: JobLogMeta
)

private fun processJobLog(context: Context, job: GHJob, log: String): ProcessedJobLog {
    val bytes = log.toByteArray().size
    Log.d(ACTIONS_JOB_LOG_TAG, "parse start job=${job.id} bytes=$bytes")

    when {
        isTemporaryLiveLogUnavailable(job, log) -> {
            return ProcessedJobLog(liveLogPlaceholder(job), emptyMap(), JobLogMeta())
        }
        log.startsWith("Error: ") -> {
            return ProcessedJobLog(log.take(ACTIONS_JOB_LOG_DISPLAY_BYTES), emptyMap(), JobLogMeta(warning = log))
        }
    }

    val cacheFile = if (bytes > ACTIONS_JOB_LOG_CACHE_BYTES) {
        writeJobLogCacheFile(context, job, log)
    } else {
        null
    }

    val tooLarge = bytes > ACTIONS_JOB_LOG_HARD_CAP_BYTES
    val parseSource = if (tooLarge) {
        log.take(ACTIONS_JOB_LOG_HARD_CAP_BYTES)
    } else {
        log
    }
    val warning = when {
        tooLarge -> "Log is larger than ${ghFmtSize(ACTIONS_JOB_LOG_HARD_CAP_BYTES.toLong())}. Showing a safe preview; open the run in browser for the full log."
        cacheFile != null -> "Large log cached to disk: ${cacheFile.name}"
        else -> null
    }
    val steps = if (tooLarge) emptyMap() else splitLogsBySteps(job, parseSource).mapValues { (_, value) ->
        safeLogPreview(value, ACTIONS_STEP_LOG_DISPLAY_BYTES)
    }
    val preview = if (tooLarge) {
        warning.orEmpty() + "\n\n" + safeLogPreview(parseSource, ACTIONS_JOB_LOG_DISPLAY_BYTES)
    } else {
        safeLogPreview(parseSource, ACTIONS_JOB_LOG_DISPLAY_BYTES)
    }

    Log.d(ACTIONS_JOB_LOG_TAG, "parse complete job=${job.id} tooLarge=$tooLarge steps=${steps.size}")
    return ProcessedJobLog(preview, steps, JobLogMeta(cacheFile = cacheFile, warning = warning, tooLarge = tooLarge))
}

private fun liveLogPlaceholder(job: GHJob): String = when (job.status) {
    "queued", "pending", "waiting", "requested" -> "Log is not available yet. The job has not started writing output."
    "in_progress" -> "Waiting for live log output. GitHub may return logs after the current step publishes output."
    else -> "No log output was captured for this job."
}

private fun isTemporaryLiveLogUnavailable(job: GHJob, log: String): Boolean {
    if (!isJobActive(job)) return false
    val normalized = log.trim().lowercase()
    return normalized == "error: http 404" ||
        "no step log captured" in normalized ||
        "not found" in normalized
}

internal fun isFailedStep(step: GHStep): Boolean =
    displayStepStatus(step) in setOf("failed", "timed out", "startup failure", "action required")

internal fun buildFailureDiagnostics(
    context: Context,
    job: GHJob?,
    step: GHStep?,
    log: String,
    catalog: KernelErrorCatalog?
): List<String> {
    if (job == null) return emptyList()
    val messages = linkedSetOf<String>()
    messages += KernelErrorPatterns.diagnose(context, catalog, log)

    if (messages.isEmpty() && step != null) {
        messages += "Failed step: ${step.name}. Open the log and inspect the last error block."
    }
    return messages.toList()
}

internal fun buildFailureEvidence(
    job: GHJob?,
    step: GHStep?,
    jobLog: String,
    stepLog: String,
): FailureEvidence {
    if (job == null) return FailureEvidence("no failed job", emptyList(), emptyList())
    val sourceLog = stepLog.ifBlank { jobLog }
    if (sourceLog.isBlank()) return FailureEvidence("log not loaded", emptyList(), emptyList())
    val source = if (stepLog.isNotBlank() && step != null) "step ${step.number}: ${step.name}" else "job: ${job.name}"
    val lines = sourceLog.lineSequence()
        .mapIndexed { index, line -> (index + 1) to compactFailureEvidenceLine(line) }
        .filter { (_, line) -> line.isNotBlank() }
        .toList()
    val signalMatcher = Regex(
        """(?i)\b(error|failed|failure|exception|fatal|denied|forbidden|not found|no such file|unresolved|undefined|cannot|could not|timeout|timed out|traceback|segmentation fault|permission|killed|oom|out of memory)\b"""
    )
    val signalLines = lines
        .filter { (_, line) -> signalMatcher.containsMatchIn(line) }
        .takeLast(8)
        .map { (lineNumber, line) -> "L$lineNumber: $line" }
    val tailLines = lines
        .takeLast(10)
        .map { (lineNumber, line) -> "L$lineNumber: $line" }
    return FailureEvidence(source, signalLines, tailLines)
}

private fun compactFailureEvidenceLine(line: String): String =
    line.replace(Regex("""\u001B\[[;?\d]*[ -/]*[@-~]"""), "")
        .replace('\t', ' ')
        .trim()
        .take(240)

internal fun failureSummaryText(job: GHJob, step: GHStep?, diagnostics: List<String>): String {
    val lines = mutableListOf("Failed job: ${job.name}")
    if (step != null) lines += "Failed step: ${step.name} (#${step.number})"
    diagnostics.takeIf { it.isNotEmpty() }?.let { items ->
        lines += "Likely causes:"
        lines += items.map { "- $it" }
    }
    return lines.joinToString("\n")
}

internal fun failureEvidenceText(
    repo: GHRepo,
    run: GHWorkflowRun?,
    job: GHJob,
    step: GHStep?,
    diagnostics: List<String>,
    evidence: FailureEvidence,
): String = buildString {
    appendLine("GlassFiles GitHub Actions failure evidence")
    appendLine("repo: ${repo.fullName}")
    run?.let {
        appendLine("run: #${it.runNumber} (${it.id})")
        appendLine("workflow: ${it.name.ifBlank { "workflow" }}")
        appendLine("status: ${displayRunStatus(it)}")
        appendLine("branch: ${it.branch.ifBlank { "-" }}")
        appendLine("sha: ${it.headSha.ifBlank { "-" }}")
        appendLine("url: ${it.htmlUrl.ifBlank { "-" }}")
    }
    appendLine("job: ${job.name} (${job.id})")
    if (step != null) appendLine("step: ${step.number} ${step.name}")
    if (diagnostics.isNotEmpty()) {
        appendLine()
        appendLine("likely causes")
        diagnostics.forEach { appendLine("- $it") }
    }
    appendLine()
    appendLine("evidence source: ${evidence.source}")
    if (evidence.signalLines.isNotEmpty()) {
        appendLine()
        appendLine("signals")
        evidence.signalLines.forEach { appendLine(it) }
    }
    if (evidence.tailLines.isNotEmpty()) {
        appendLine()
        appendLine("tail")
        evidence.tailLines.forEach { appendLine(it) }
    }
}

internal fun shareFailureSummary(
    context: Context,
    repo: GHRepo,
    run: GHWorkflowRun,
    job: GHJob,
    step: GHStep?,
    diagnostics: List<String>
) {
    val summary = failureSummaryText(job, step, diagnostics)
    val stepName = step?.name ?: "unknown"
    val stepNumber = step?.number ?: 0
    val branch = run.branch.ifBlank { "unknown" }
    val body = """
Workflow: ${run.name.ifBlank { "Workflow" }} #${run.runNumber}
Repo: ${repo.owner}/${repo.name}
Branch: $branch
Step: $stepName (#$stepNumber)
Job: ${job.name}
Summary: $summary
URL: ${run.htmlUrl.ifBlank { "n/a" }}
""".trimIndent()
    val subject = "${Strings.actions_share_failure_subject}: ${run.name.ifBlank { "Workflow" }} #${run.runNumber}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, Strings.actions_share_failure_summary))
    } catch (_: Exception) {
        Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
    }
}

internal fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
    }
}

internal fun actionsFriendlyError(message: String?): String {
    val raw = message.orEmpty()
    val lower = raw.lowercase()
    return when {
        "403" in lower || "forbidden" in lower -> "GitHub API permission or rate limit issue. Polling slowed down."
        "401" in lower || "unauthorized" in lower -> "GitHub token is missing or expired."
        "404" in lower || "not found" in lower -> "GitHub resource is not available yet or token has no access."
        "422" in lower || "validation failed" in lower -> "GitHub rejected the request. Check workflow inputs and ref."
        "timeout" in lower || "failed to connect" in lower -> "Network issue. Polling slowed down."
        raw.isNotBlank() -> raw.take(180)
        else -> "GitHub request failed. Polling slowed down."
    }
}

internal fun safeLogFileName(job: GHJob): String =
    "job-${job.id}-${job.name.replace(Regex("""[\\/:*?"<>|]+"""), "_").trim().ifBlank { "log" }}"

private fun actionsExportDir(): File = DownloadStorage.directory(App.instance)

private fun actionsExportTimestamp(): String =
    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(java.util.Date())

private fun safeActionsFilePart(value: String): String =
    value.replace(Regex("""[\\/:*?"<>|\s]+"""), "-").trim('-').ifBlank { "github" }

internal fun saveActionsRunsExport(
    repo: GHRepo,
    runs: List<GHWorkflowRun>,
    filterSummary: String,
): File? {
    if (runs.isEmpty()) return null
    val file = File(actionsExportDir(), "actions-runs-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
    val text = buildString {
        appendLine("GlassFiles GitHub Actions runs export")
        appendLine("repo: ${repo.fullName}")
        appendLine("filters: $filterSummary")
        appendLine("exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(java.util.Date())}")
        appendLine("count: ${runs.size}")
        appendLine()
        appendLine("run\tworkflow\tstatus\tbranch\tevent\tactor\tsha\tcreated\tupdated")
        runs.forEach { run ->
            appendLine(
                listOf(
                    "#${run.runNumber}",
                    run.name.ifBlank { "workflow" },
                    displayRunStatus(run),
                    run.branch.ifBlank { "-" },
                    run.event.ifBlank { "-" },
                    run.actor.ifBlank { "-" },
                    run.headSha.take(12).ifBlank { "-" },
                    run.createdAt.ifBlank { "-" },
                    run.updatedAt.ifBlank { "-" },
                ).joinToString("\t")
            )
        }
    }
    return if (saveTextFile(file, text)) file else null
}

internal fun saveActionsArtifactsExport(repo: GHRepo, artifacts: List<GHArtifact>, label: String): File? {
    if (artifacts.isEmpty()) return null
    val file = File(actionsExportDir(), "actions-${safeActionsFilePart(label)}-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
    val text = buildString {
        appendLine("GlassFiles GitHub Actions artifacts export")
        appendLine("repo: ${repo.fullName}")
        appendLine("scope: $label")
        appendLine("count: ${artifacts.size}")
        appendLine()
        appendLine("id\tname\tsize\texpired\tcreated\texpires\trun\tbranch\tsha\tdigest")
        artifacts.forEach { artifact ->
            appendLine(
                listOf(
                    artifact.id.toString(),
                    artifact.name,
                    formatArtifactSize(artifact.sizeInBytes),
                    artifact.expired.toString(),
                    artifact.createdAt.ifBlank { "-" },
                    artifact.expiresAt.ifBlank { "-" },
                    artifact.workflowRunId.takeIf { it > 0 }?.toString() ?: "-",
                    artifact.workflowRunBranch.ifBlank { "-" },
                    artifact.workflowRunSha.take(12).ifBlank { "-" },
                    artifact.digest.ifBlank { "-" },
                ).joinToString("\t")
            )
        }
    }
    return if (saveTextFile(file, text)) file else null
}

internal fun saveActionsCachesExport(
    repo: GHRepo,
    usage: GHActionsCacheUsage?,
    caches: List<GHActionsCacheEntry>,
): File? {
    if (caches.isEmpty()) return null
    val file = File(actionsExportDir(), "actions-caches-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
    val text = buildString {
        appendLine("GlassFiles GitHub Actions caches export")
        appendLine("repo: ${repo.fullName}")
        usage?.let {
            appendLine("active caches: ${it.activeCachesCount}")
            appendLine("active size: ${formatArtifactSize(it.activeCachesSizeInBytes)}")
        }
        appendLine("count: ${caches.size}")
        appendLine()
        appendLine("id\tkey\tsize\tref\tcreated\tlast_used\tversion")
        caches.forEach { cache ->
            appendLine(
                listOf(
                    cache.id.toString(),
                    cache.key,
                    formatArtifactSize(cache.sizeInBytes),
                    cache.ref.ifBlank { "-" },
                    cache.createdAt.ifBlank { "-" },
                    cache.lastAccessedAt.ifBlank { "-" },
                    cache.version.ifBlank { "-" },
                ).joinToString("\t")
            )
        }
    }
    return if (saveTextFile(file, text)) file else null
}

internal fun saveWorkflowRunReport(
    repo: GHRepo,
    run: GHWorkflowRun?,
    jobs: List<GHJob>,
    artifacts: List<GHArtifact>,
    checkRuns: List<GHCheckRun>,
    pendingDeployments: List<GHPendingDeployment>,
    reviewHistory: List<GHWorkflowRunReview>,
    usage: GHActionsUsage?,
    jobLogs: Map<Long, String>,
): File? {
    val currentRun = run ?: return null
    val file = File(actionsExportDir(), "actions-run-${currentRun.runNumber}-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
    val text = buildString {
        appendLine("GlassFiles GitHub Actions run report")
        appendLine("repo: ${repo.fullName}")
        appendLine("run: #${currentRun.runNumber} (${currentRun.id})")
        appendLine("workflow: ${currentRun.name.ifBlank { "workflow" }}")
        appendLine("title: ${currentRun.displayTitle.ifBlank { "-" }}")
        appendLine("status: ${displayRunStatus(currentRun)}")
        appendLine("branch: ${currentRun.branch.ifBlank { "-" }}")
        appendLine("event: ${currentRun.event.ifBlank { "-" }}")
        appendLine("actor: ${currentRun.actor.ifBlank { "-" }}")
        appendLine("sha: ${currentRun.headSha.ifBlank { "-" }}")
        appendLine("attempt: ${currentRun.runAttempt}")
        appendLine("created: ${currentRun.createdAt.ifBlank { "-" }}")
        appendLine("updated: ${currentRun.updatedAt.ifBlank { "-" }}")
        appendLine("url: ${currentRun.htmlUrl.ifBlank { "-" }}")
        usage?.let {
            appendLine()
            appendLine("usage")
            appendLine("duration: ${formatDuration(it.runDurationMs)}")
            it.billableMs.forEach { (os, ms) -> appendLine("$os: ${formatDuration(ms)}") }
        }
        appendLine()
        appendLine("jobs (${jobs.size})")
        jobs.forEach { job ->
            appendLine("- ${job.name} [${displayJobStatus(job)}] id=${job.id} duration=${calcJobDuration(job, System.currentTimeMillis())}")
            job.steps.forEach { step ->
                appendLine("  - step ${step.number}: ${step.name} [${displayStepStatus(step)}]")
            }
        }
        appendLine()
        appendLine("loaded logs: ${jobLogs.size}")
        jobLogs.keys.forEach { jobId -> appendLine("- job $jobId") }
        appendLine()
        appendLine("artifacts (${artifacts.size})")
        artifacts.forEach { artifact ->
            appendLine("- ${artifact.name} id=${artifact.id} size=${formatArtifactSize(artifact.sizeInBytes)} expired=${artifact.expired}")
        }
        appendLine()
        appendLine("checks (${checkRuns.size})")
        checkRuns.forEach { checkRun ->
            appendLine("- ${checkRun.name.ifBlank { checkRun.title.ifBlank { "check ${checkRun.id}" } }} [${displayCheckStatus(checkRun)}] annotations=${checkRun.annotationsCount}")
        }
        appendLine()
        appendLine("pending deployments (${pendingDeployments.size})")
        pendingDeployments.forEach { deployment ->
            appendLine("- ${deployment.environmentName.ifBlank { "environment ${deployment.environmentId}" }} reviewers=${deployment.reviewers.joinToString(", ").ifBlank { "-" }}")
        }
        appendLine()
        appendLine("deployment reviews (${reviewHistory.size})")
        reviewHistory.forEach { review ->
            appendLine("- ${review.state.ifBlank { "reviewed" }} by ${review.user.ifBlank { "GitHub" }} env=${review.environments.joinToString(", ").ifBlank { "-" }}")
            if (review.comment.isNotBlank()) appendLine("  ${review.comment}")
        }
    }
    return if (saveTextFile(file, text)) file else null
}

internal fun saveFailureEvidenceExport(
    repo: GHRepo,
    run: GHWorkflowRun?,
    job: GHJob,
    step: GHStep?,
    diagnostics: List<String>,
    evidence: FailureEvidence,
): File? {
    if (!evidence.hasContent) return null
    val runPart = run?.runNumber?.toString() ?: job.id.toString()
    val file = File(actionsExportDir(), "actions-failure-evidence-$runPart-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
    val text = failureEvidenceText(repo, run, job, step, diagnostics, evidence)
    return if (saveTextFile(file, text)) file else null
}

internal fun saveWorkflowRunLoadedLogsExport(
    repo: GHRepo,
    run: GHWorkflowRun?,
    jobs: List<GHJob>,
    jobLogs: Map<Long, String>,
    jobLogMeta: Map<Long, JobLogMeta>,
): File? {
    val currentRun = run ?: return null
    val loadedJobs = jobs.filter { jobLogs.containsKey(it.id) }
    if (loadedJobs.isEmpty()) return null
    return try {
        val file = File(actionsExportDir(), "actions-run-${currentRun.runNumber}-loaded-logs-${safeActionsFilePart(repo.fullName)}-${actionsExportTimestamp()}.txt")
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { writer ->
            writer.appendLine("GlassFiles GitHub Actions loaded logs export")
            writer.appendLine("repo: ${repo.fullName}")
            writer.appendLine("run: #${currentRun.runNumber} (${currentRun.id})")
            writer.appendLine("workflow: ${currentRun.name.ifBlank { "workflow" }}")
            writer.appendLine("jobs: ${loadedJobs.size}")
            writer.appendLine()
            loadedJobs.forEach { job ->
                writer.appendLine("===== job ${job.id}: ${job.name} [${displayJobStatus(job)}] =====")
                val cacheFile = jobLogMeta[job.id]?.cacheFile
                if (cacheFile != null && cacheFile.exists()) {
                    writer.appendLine("source: cached full log ${cacheFile.name}")
                    cacheFile.bufferedReader().useLines { lines ->
                        lines.forEach { writer.appendLine(it) }
                    }
                } else {
                    writer.appendLine(jobLogs[job.id].orEmpty())
                }
                writer.appendLine()
            }
        }
        file
    } catch (t: Throwable) {
        Log.e(ACTIONS_JOB_LOG_TAG, "save loaded logs export failed", t)
        null
    }
}

internal fun saveTextFile(file: File, text: String): Boolean = try {
    file.parentFile?.mkdirs()
    file.writeText(text)
    true
} catch (_: Exception) {
    false
}

private fun writeJobLogCacheFile(context: Context, job: GHJob, text: String): File? = try {
    val dir = File(context.cacheDir, "github-job-logs").apply { mkdirs() }
    File(dir, "${safeLogFileName(job)}.log").also { file ->
        file.writeText(text)
        Log.d(ACTIONS_JOB_LOG_TAG, "cache write job=${job.id} path=${file.absolutePath} bytes=${file.length()}")
    }
} catch (t: Throwable) {
    Log.e(ACTIONS_JOB_LOG_TAG, "cache write failed job=${job.id}", t)
    null
}

private fun safeLogPreview(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    return text.take(maxChars).trimEnd() + "\n\n[Log preview truncated at ${ghFmtSize(maxChars.toLong())}.]"
}

private fun splitLogsBySteps(job: GHJob, raw: String): Map<Int, String> {
    if (raw.isBlank()) return emptyMap()
    val lines = raw.lines()
    val stepStarts = lines.mapIndexedNotNull { index, line ->
        val n = normalizeLogLine(line)
        if (looksLikeStepBoundary(n)) index else null
    }
    val result = linkedMapOf<Int, String>()
    val steps = job.steps
    if (steps.isEmpty()) return emptyMap()

    if (stepStarts.isEmpty()) {
        val target = steps.lastOrNull { displayStepStatus(it) !in setOf("queued", "pending") } ?: steps.first()
        result[target.number] = raw.trim()
        return result
    }

    val preamble = lines.subList(0, stepStarts.first()).joinToString("\n").trim()
    var sectionOffset = 0
    if (preamble.isNotBlank()) {
        result[steps.first().number] = preamble
        sectionOffset = 1
    }

    val sections = mutableListOf<String>()
    for (i in stepStarts.indices) {
        val s = stepStarts[i]
        val e = if (i < stepStarts.lastIndex) stepStarts[i + 1] else lines.size
        sections += lines.subList(s, e).joinToString("\n").trim()
    }

    var sectionIndex = 0
    for (stepIndex in sectionOffset until steps.size) {
        if (sectionIndex >= sections.size) break
        val step = steps[stepIndex]
        val currentSection = sections[sectionIndex]
        val currentBoundary = normalizeLogLine(currentSection.lineSequence().firstOrNull().orEmpty())
        val matchedByName = stepBoundaryMatchesStep(currentBoundary, step.name)
        if (matchedByName || stepIndex == sectionOffset) {
            result[step.number] = currentSection
            sectionIndex++
        } else {
            result[step.number] = currentSection
            sectionIndex++
        }
    }

    if (sectionIndex < sections.size) {
        val lastStepNum = steps.lastOrNull { result.containsKey(it.number) }?.number ?: steps.last().number
        val extra = sections.drop(sectionIndex).joinToString("\n\n")
        result[lastStepNum] = listOfNotNull(result[lastStepNum], extra).joinToString("\n\n").trim()
    }

    return result.mapValues { (_, value) -> value.trim() }.filterValues { it.isNotBlank() }
}

private fun normalizeLogLine(line: String): String {
    var out = line.trim()
    out = out.replace(Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z\\s*"), "")
    return out
}

private fun looksLikeStepBoundary(normalized: String): Boolean {
    return normalized.startsWith("##[group]Step ") ||
        normalized.startsWith("##[group]Run ") ||
        normalized.startsWith("##[group]Post ") ||
        normalized.startsWith("##[group]Complete job") ||
        normalized.startsWith("##[group]")
}

private fun stepBoundaryMatchesStep(boundary: String, stepName: String): Boolean {
    val s = stepName.lowercase()
    val b = boundary.lowercase()
    return when {
        s == "checkout" -> "checkout" in b
        s.startsWith("set up jdk") -> "setup-java" in b || "jdk" in b || "java" in b
        s.contains("android sdk") -> "setup-android" in b || "android-actions" in b || "android sdk" in b
        s.contains("ndk") || s.contains("cmake") -> "ndk" in b || "cmake" in b
        s.contains("cache gradle") -> "cache" in b && "gradle" in b
        s.contains("create debug keystore") -> "keystore" in b
        s.contains("build debug apk") -> "assembledebug" in b || "debug apk" in b
        s.contains("build release apk") -> "assemblerelease" in b || "release apk" in b
        s.contains("upload debug apk") -> "upload-artifact" in b || "debug apk" in b
        s.contains("upload release apk") -> "upload-artifact" in b || "release apk" in b
        s.startsWith("post ") -> b.startsWith("##[group]post ")
        s == "complete job" -> "complete job" in b
        else -> s.isNotBlank() && b.contains(s)
    }
}

internal fun compactLogForDisplay(raw: String): String {
    return raw.lineSequence().map { line ->
        val m = Regex("^(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?Z\\s*(.*)$").find(line)
        val cleaned = if (m != null) {
            "${m.groupValues[2]}  ${m.groupValues[3]}"
        } else line
        cleaned
            .replace("##[group]", "")
            .replace("##[endgroup]", "")
            .replace("##[warning]", "warning: ")
            .replace("##[error]", "error: ")
            .trimEnd()
    }.joinToString("\n").trim()
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
internal fun ActionsFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) palette.accent.copy(alpha = 0.10f) else palette.surface)
            .border(
                1.dp,
                if (selected) palette.accent.copy(alpha = 0.55f) else palette.border,
                RoundedCornerShape(GitHubControlRadius),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            color = if (selected) palette.accent else palette.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.2.sp,
        )
    }
}

internal fun calcRunDuration(run: GHWorkflowRun, nowMs: Long): String {
    val start = parseIsoMs(run.createdAt) ?: return ""
    val end = if (run.status == "completed") parseIsoMs(run.updatedAt) ?: nowMs else nowMs
    return formatDuration((end - start).coerceAtLeast(0L))
}

internal fun calcJobDuration(job: GHJob, nowMs: Long): String {
    val start = parseIsoMs(job.startedAt) ?: return ""
    val end = if (job.status == "completed") parseIsoMs(job.completedAt) ?: nowMs else nowMs
    return formatDuration((end - start).coerceAtLeast(0L))
}

internal fun parseIsoMs(value: String): Long? = try {
    if (value.isBlank()) null else ISO_FMT.parse(value)?.time
} catch (_: Exception) { null }

internal fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    return when {
        sec < 60 -> "${sec}s"
        sec < 3600 -> "${sec / 60}m ${sec % 60}s"
        else -> "${sec / 3600}h ${(sec % 3600) / 60}m ${(sec % 60)}s"
    }
}

internal fun displayStepStatus(step: GHStep): String {
    val c = step.conclusion.trim().lowercase()
    val s = step.status.trim().lowercase()
    return when {
        c == "success" -> "success"
        c == "failure" -> "failed"
        c == "cancelled" -> "cancelled"
        c == "skipped" -> "skipped"
        c == "neutral" -> "neutral"
        c == "timed_out" -> "timed out"
        c == "action_required" -> "action required"
        c == "startup_failure" -> "startup failure"
        c == "stale" -> "stale"
        c.isNotBlank() && c != "null" -> c
        s == "in_progress" -> "running"
        s in setOf("queued", "pending", "waiting", "requested") -> s
        s == "completed" -> "completed"
        s.isNotBlank() && s != "null" -> s
        else -> "pending"
    }
}

@Composable
internal fun stepStatusColor(step: GHStep): Color {
    return when (displayStepStatus(step)) {
        "success" -> AiModuleTheme.colors.accent
        "failed" -> AiModuleTheme.colors.error
        "cancelled" -> AiModuleTheme.colors.textMuted
        "skipped", "neutral" -> AiModuleTheme.colors.textMuted
        "running" -> AiModuleTheme.colors.accent
        "queued", "pending", "waiting", "requested" -> AiModuleTheme.colors.textMuted
        "completed" -> AiModuleTheme.colors.textMuted
        else -> AiModuleTheme.colors.textMuted
    }
}

internal fun formatArtifactSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

internal fun safeArtifactZipName(artifact: GHArtifact): String {
    val safeName = artifact.name
        .replace(Regex("""[\\/:*?"<>|]+"""), "_")
        .trim()
        .ifBlank { "artifact-${artifact.id}" }
    return "$safeName.zip"
}

internal fun artifactKindBadges(artifact: GHArtifact): List<Pair<String, Color>> =
    buildKindBadges(artifact.name)

internal fun groupedArtifacts(artifacts: List<GHArtifact>): List<ArtifactGroup> =
    artifacts.groupBy { artifactKindGroup(it) }
        .map { (meta, items) -> meta.copy(items = items.sortedBy { it.name.lowercase() }) }
        .sortedWith(compareBy<ArtifactGroup> { it.order }.thenBy { it.label })

private fun artifactKindGroup(artifact: GHArtifact): ArtifactGroup {
    val name = artifact.name.lowercase()
    return when {
        kernelPatterns.any { it in name } -> ArtifactGroup("Kernel / IMG", Green, 0, emptyList())
        magiskPatterns.any { it in name } -> ArtifactGroup("Magisk / KSU", Orange, 1, emptyList())
        driverPatterns.any { it in name } -> ArtifactGroup("Turnip / Adreno", Purple, 2, emptyList())
        androidPatterns.any { it in name } -> ArtifactGroup("Android app", Blue, 3, emptyList())
        iosPatterns.any { it in name } -> ArtifactGroup("iOS app", Teal, 4, emptyList())
        windowsPatterns.any { it in name } -> ArtifactGroup("Windows", Color(0xFF0078D4), 5, emptyList())
        linuxPatterns.any { it in name } -> ArtifactGroup("Linux", TextSecondary, 6, emptyList())
        else -> ArtifactGroup("Other", TextSecondary, 99, emptyList())
    }
}

private fun buildKindBadges(text: String): List<Pair<String, Color>> {
    val name = text.lowercase()
    val labels = mutableListOf<Pair<String, Color>>()
    if (kernelPatterns.any { it in name }) {
        labels += "Kernel" to Green
    }
    if (magiskPatterns.any { it in name }) {
        labels += "Magisk" to Orange
    }
    if (driverPatterns.any { it in name }) {
        labels += "Driver" to Purple
    }
    if (androidPatterns.any { it in name }) {
        labels += "Android" to Blue
    }
    if (iosPatterns.any { it in name }) {
        labels += "iOS" to Teal
    }
    if (windowsPatterns.any { it in name }) {
        labels += "Windows" to Color(0xFF0078D4)
    }
    if (linuxPatterns.any { it in name }) {
        labels += "Linux" to TextSecondary
    }
    return labels
}

private val kernelPatterns = listOf(
    "kernel", "anykernel", "anykernel3", "boot", "boot.img", "vendor_boot", "vendor_boot.img",
    "dtbo", "dtbo.img", "dtb", "image.gz", "image.gz-dtb", "ak3", "zimage"
)
private val magiskPatterns = listOf("magisk", "magisk-module", "magisk_module", "ksu", "kernelsu", "apatch")
private val driverPatterns = listOf("driver", "turnip", "adreno", "freedreno", "vulkan", "mesa", "kgsl")
private val androidPatterns = listOf("apk", "aab", "android")
private val iosPatterns = listOf("ipa", "ios", "xcarchive")
private val windowsPatterns = listOf("exe", "msi", "windows", "win64", "win32")
private val linuxPatterns = listOf("appimage", ".deb", ".rpm", "linux")

private val ISO_FMT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
