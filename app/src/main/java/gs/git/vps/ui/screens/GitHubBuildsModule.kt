package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.cancelWorkflowRunDetailed
import gs.git.vps.data.github.getWorkflowRuns
import gs.git.vps.data.github.rerunWorkflowDetailed
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.aiModuleStatusBadge
import gs.git.vps.ui.components.aiModuleStatusLabel
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@Composable
fun BuildsScreen(
    repo: GHRepo,
    branches: List<String>,
    workflows: List<GHWorkflow>,
    selectedBranch: String?,
    onRunSelected: (Long?) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    val owner = repo.owner
    val name = repo.name

    var runs by remember { mutableStateOf<List<GHWorkflowRun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var filterBranch by remember { mutableStateOf(selectedBranch ?: "") }
    var filterStatus by remember { mutableStateOf("") }

    fun loadRuns() {
        scope.launch {
            loading = true
            runs = GitHubManager.getWorkflowRuns(
                context, owner, name,
                branch = filterBranch.ifBlank { null },
                status = filterStatus.ifBlank { null },
                perPage = 30
            )
            loading = false
        }
    }

    LaunchedEffect(owner, name) { loadRuns() }

    GitHubScreenFrame(
        title = "> builds",
        subtitle = "$owner/$name",
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Filters
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status filter chips
                val statusFilters = listOf("" to "all", "success" to "ok", "failure" to "fail", "in_progress" to "running", "queued" to "queued")
                statusFilters.forEach { (value, label) ->
                    val sel = filterStatus == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                            .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                            .clickable { filterStatus = value; loadRuns() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
                    }
                }
            }

            // Branch filter
            if (branches.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("branch:", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filterBranch.isBlank()) palette.accent.copy(alpha = 0.15f) else palette.surface)
                            .border(1.dp, if (filterBranch.isBlank()) palette.accent else palette.border, RoundedCornerShape(6.dp))
                            .clickable { filterBranch = ""; loadRuns() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("all", fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (filterBranch.isBlank()) palette.accent else palette.textSecondary) }
                    branches.take(6).forEach { br ->
                        val sel = filterBranch == br
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                                .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                .clickable { filterBranch = br; loadRuns() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) { Text(br.take(12), fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading runs…") }
                runs.isEmpty() -> GitHubMonoEmpty(title = "no runs", subtitle = "no workflow runs found for this filter")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                ) {
                    item {
                        PerformanceInsightsWidget(runs)
                    }
                    items(runs, key = { it.id }) { run ->
                        WorkflowRunRow(
                            run = run,
                            onRerun = {
                                scope.launch {
                                    val result = GitHubManager.rerunWorkflowDetailed(context, owner, name, run.id)
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    if (result.success) loadRuns()
                                }
                            },
                            onCancel = {
                                scope.launch {
                                    val result = GitHubManager.cancelWorkflowRunDetailed(context, owner, name, run.id)
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    if (result.success) loadRuns()
                                }
                            }
                        )
                        AiModuleHairline()
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceInsightsWidget(runs: List<GHWorkflowRun>) {
    val palette = AiModuleTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val completedRuns = remember(runs) {
        runs.filter { it.status == "completed" && it.conclusion != "skipped" }
    }
    val avgSec = remember(completedRuns) {
        if (completedRuns.isEmpty()) 0L
        else completedRuns.map { calcRunDurationSeconds(it) }.filter { it > 0 }.average().toLong()
    }
    val successRate = remember(completedRuns) {
        if (completedRuns.isEmpty()) 0
        else (completedRuns.count { it.conclusion == "success" } * 100) / completedRuns.size
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "[insights] performance & tests stats",
                fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Text(
                if (expanded) "hide \u25b2" else "show \u25bc",
                fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formatAvg = if (avgSec == 0L) "n/a" else "${avgSec / 60}m ${avgSec % 60}s"
            Column {
                Text("avg build duration", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                Text(formatAvg, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Column {
                Text("success rate", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                Text("$successRate%", fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (successRate >= 80) Color(0xFF2EA043) else Color(0xFFF85149))
            }
        }

        if (expanded) {
            Spacer(Modifier.height(4.dp))
            BuildDurationTrendGraph(runs)
            Spacer(Modifier.height(4.dp))
            TestSummaryWidget(runs)
        }
    }
}

@Composable
private fun BuildDurationTrendGraph(runs: List<GHWorkflowRun>) {
    val palette = AiModuleTheme.colors
    val completedRuns = remember(runs) {
        runs.filter { it.status == "completed" && it.conclusion != "skipped" }
            .take(10)
            .reversed()
    }
    if (completedRuns.isEmpty()) return

    val durations = completedRuns.map { run ->
        val duration = calcRunDurationSeconds(run)
        if (duration <= 0) 120L else duration
    }

    val maxDuration = remember(durations) { (durations.maxOrNull() ?: 300).toFloat().coerceAtLeast(60f) }

    Column(
        Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.background)
            .padding(10.dp)
    ) {
        Text("duration trend (last ${completedRuns.size} runs)", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (durations.size - 1).coerceAtLeast(1)

            // Draw grid lines
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = height * i / gridLines
                drawLine(
                    color = palette.border.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw points & line
            val points = durations.mapIndexed { index, duration ->
                val x = index * spacing
                val y = height - (duration.toFloat() / maxDuration * height).coerceIn(0f, height)
                Offset(x, y)
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = palette.accent,
                    start = points[i],
                    end = points[i+1],
                    strokeWidth = 3f
                )
            }

            points.forEachIndexed { index, pt ->
                drawCircle(
                    color = if (completedRuns[index].conclusion == "success") Color(0xFF2EA043) else Color(0xFFF85149),
                    radius = 8f,
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun TestSummaryWidget(runs: List<GHWorkflowRun>) {
    val palette = AiModuleTheme.colors
    var showDetails by remember { mutableStateOf(false) }
    val completed = remember(runs) { runs.filter { it.status == "completed" } }
    val succeeded = remember(completed) { completed.count { it.conclusion == "success" } }
    val failed = remember(completed) { completed.count { it.conclusion == "failure" } }
    val other = remember(completed) { completed.size - succeeded - failed }
    val failedRuns = remember(completed) { completed.filter { it.conclusion == "failure" }.take(5) }

    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.background)
            .padding(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("workflow result summary", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(
                if (showDetails) "collapse" else "details",
                fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.accent,
                modifier = Modifier.clickable { showDetails = !showDetails }
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunMetricBox("success", succeeded.toString(), Color(0xFF2EA043))
            RunMetricBox("failure", failed.toString(), Color(0xFFF85149))
            RunMetricBox("other", other.toString(), palette.textSecondary)
        }

        if (showDetails) {
            Spacer(Modifier.height(8.dp))
            if (failedRuns.isEmpty()) {
                Text("no failed runs in this filter", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
            } else {
                failedRuns.forEach { run ->
                    Text(
                        "#${run.runNumber} ${run.name.ifBlank { "workflow" }} · ${run.branch}",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = palette.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RunMetricBox(title: String, value: String, color: Color) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .weight(1f)
            .background(palette.surface, RoundedCornerShape(4.dp))
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(title, fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontFamily = JetBrainsMono, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun calcRunDurationSeconds(run: GHWorkflowRun): Long {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val created = sdf.parse(run.createdAt)?.time ?: return 0L
        val updated = sdf.parse(run.updatedAt)?.time ?: return 0L
        ((updated - created) / 1000L).coerceAtLeast(0L)
    } catch (_: Exception) {
        0L
    }
}

@Composable
private fun WorkflowRunRow(run: GHWorkflowRun, onRerun: () -> Unit, onCancel: () -> Unit) {
    val palette = AiModuleTheme.colors
    val badge = aiModuleStatusBadge(run.status, run.conclusion, palette)
    val label = aiModuleStatusLabel(run.status, run.conclusion)

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(badge.glyph, color = badge.color, fontFamily = JetBrainsMono, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                run.name.ifBlank { "run #${run.runNumber}" },
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(label, color = badge.color, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("#${run.runNumber}", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            Text(run.branch, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            Text(run.event, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text(run.createdAt.take(16).replace('T', ' '), color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
        if (run.headSha.isNotBlank()) {
            Text("SHA: ${run.headSha.take(7)}", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (run.status == "completed") {
                SmallChip("rerun", palette.accent, onRerun)
            }
            if (run.status == "in_progress" || run.status == "queued") {
                SmallChip("cancel", palette.error, onCancel)
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = color)
    }
}
