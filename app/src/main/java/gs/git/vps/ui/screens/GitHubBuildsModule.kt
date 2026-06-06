package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GHRepo
import gs.git.vps.data.github.GHWorkflow
import gs.git.vps.data.github.GHWorkflowRun
import gs.git.vps.data.github.GitHubManager
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
    onRunSelected: (Long?) -> Unit
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
        onBack = {},
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
                    items(runs, key = { it.id }) { run ->
                        WorkflowRunRow(
                            run = run,
                            onRerun = {
                                scope.launch {
                                    val ok = GitHubManager.rerunWorkflow(context, owner, name, run.id)
                                    Toast.makeText(context, if (ok) "rerun triggered" else "rerun failed", Toast.LENGTH_SHORT).show()
                                    if (ok) loadRuns()
                                }
                            },
                            onCancel = {
                                scope.launch {
                                    val ok = GitHubManager.cancelWorkflowRun(context, owner, name, run.id)
                                    Toast.makeText(context, if (ok) "cancelled" else "cancel failed", Toast.LENGTH_SHORT).show()
                                    if (ok) loadRuns()
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
                color = palette.textPrimary, fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
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
        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (run.conclusion == "failure" || run.status == "completed") {
                SmallChip("rerun", palette.accent) { onRerun() }
            }
            if (run.status == "in_progress" || run.status == "queued") {
                SmallChip("cancel", palette.error) { onCancel() }
            }
        }
    }
}

@Composable
private fun SmallChip(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
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
