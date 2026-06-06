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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GHCheckAnnotation
import gs.git.vps.data.github.GHCheckRun
import gs.git.vps.data.github.GHCheckSuite
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
internal fun CheckRunsScreen(
    repoOwner: String,
    repoName: String,
    ref: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkRuns by remember { mutableStateOf<List<GHCheckRun>>(emptyList()) }
    var checkSuites by remember { mutableStateOf<List<GHCheckSuite>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var expandedRunId by remember { mutableStateOf<Long?>(null) }
    var annotations by remember { mutableStateOf<List<GHCheckAnnotation>>(emptyList()) }
    var loadingAnnotations by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("") }

    LaunchedEffect(repoOwner, repoName, ref) {
        checkSuites = GitHubManager.getPullRequestCheckSuites(context, repoOwner, repoName, ref)
        checkRuns = GitHubManager.getPullRequestCheckRuns(context, repoOwner, repoName, ref)
        loading = false
    }

    val filteredRuns = if (filterStatus.isBlank()) checkRuns else checkRuns.filter { run ->
        when (filterStatus) {
            "success" -> run.conclusion == "success"
            "fail" -> run.conclusion == "failure"
            "running" -> run.status == "in_progress" || run.status == "queued"
            else -> true
        }
    }

    GitHubScreenFrame(
        title = "> checks",
        onBack = onBack,
        subtitle = "$repoOwner/$repoName · ${ref.take(7)}",
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading checks…")
            }
            checkRuns.isEmpty() && checkSuites.isEmpty() -> GitHubMonoEmpty(
                title = "no checks",
                subtitle = "no CI activity reported for this ref",
            )
            else -> {
                Column(Modifier.fillMaxSize()) {
                    // Status filter chips
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("" to "all", "success" to "ok", "fail" to "fail", "running" to "running")
                        val palette = AiModuleTheme.colors
                        filters.forEach { (value, label) ->
                            val sel = filterStatus == value
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                                    .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                                    .clickable { filterStatus = value }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
                            }
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    ) {
                        if (checkSuites.isNotEmpty()) {
                            item {
                                Text(
                                    text = "check suites",
                                    color = AiModuleTheme.colors.textMuted,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                            items(checkSuites) { suite ->
                                CheckSuiteRow(suite)
                                AiModuleHairline()
                            }
                        }
                        if (filteredRuns.isNotEmpty()) {
                            item {
                                Text(
                                    text = "check runs (${filteredRuns.size})",
                                    color = AiModuleTheme.colors.textMuted,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                        items(filteredRuns, key = { it.id }) { run ->
                            CheckRunRowExpanded(
                                run = run,
                                expanded = expandedRunId == run.id,
                                annotations = if (expandedRunId == run.id) annotations else emptyList(),
                                loadingAnnotations = loadingAnnotations && expandedRunId == run.id,
                                onClick = {
                                    if (expandedRunId == run.id) {
                                        expandedRunId = null
                                    } else {
                                        expandedRunId = run.id
                                        if (run.annotationsCount > 0) {
                                            scope.launch {
                                                loadingAnnotations = true
                                                annotations = GitHubManager.getCheckRunAnnotations(context, repoOwner, repoName, run.id)
                                                loadingAnnotations = false
                                            }
                                        } else {
                                            annotations = emptyList()
                                        }
                                    }
                                },
                                onRerun = {
                                    scope.launch {
                                        val ok = GitHubManager.rerunFailedJobs(context, repoOwner, repoName, run.id)
                                        Toast.makeText(context, if (ok) "rerun triggered" else "rerun failed", Toast.LENGTH_SHORT).show()
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
}

@Composable
private fun CheckSuiteRow(suite: GHCheckSuite) {
    val palette = AiModuleTheme.colors
    val badge = aiModuleStatusBadge(suite.status, suite.conclusion, palette)
    val label = aiModuleStatusLabel(suite.status, suite.conclusion)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(badge.glyph, color = badge.color, fontFamily = JetBrainsMono, fontSize = 14.sp, modifier = Modifier.width(18.dp))
            Text(suite.app.ifBlank { "check suite ${suite.id}" }, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(label, color = badge.color, fontFamily = JetBrainsMono, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("${suite.latestCheckRunsCount} runs · ${suite.headSha.take(7)} · ${suite.updatedAt.take(10)}", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(start = 18.dp))
    }
}

@Composable
private fun CheckRunRowExpanded(
    run: GHCheckRun,
    expanded: Boolean,
    annotations: List<GHCheckAnnotation>,
    loadingAnnotations: Boolean,
    onClick: () -> Unit,
    onRerun: () -> Unit
) {
    val palette = AiModuleTheme.colors
    val badge = aiModuleStatusBadge(run.status, run.conclusion, palette)
    val label = aiModuleStatusLabel(run.status, run.conclusion)

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(badge.glyph, color = badge.color, fontFamily = JetBrainsMono, fontSize = 14.sp, modifier = Modifier.width(18.dp))
            Text(run.name, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(label, color = badge.color, fontFamily = JetBrainsMono, fontSize = 11.sp)
        }
        // Summary line
        Row(Modifier.padding(start = 18.dp)) {
            if (run.startedAt.isNotBlank()) {
                Text("started: ${run.startedAt.take(16).replace('T', ' ')}", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
            }
            if (run.completedAt.isNotBlank()) {
                Text("completed: ${run.completedAt.take(16).replace('T', ' ')}", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            }
        }
        if (run.outputTitle.isNotBlank()) {
            Text(run.outputTitle, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp, modifier = Modifier.padding(start = 18.dp))
        }
        if (run.outputSummary.isNotBlank()) {
            Text(run.outputSummary, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = if (expanded) Int.MAX_VALUE else 3, modifier = Modifier.padding(start = 18.dp))
        }
        // Annotations badge
        if (run.annotationsCount > 0) {
            Text("${run.annotationsCount} annotation(s) — tap to ${if (expanded) "collapse" else "expand"}", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 18.dp))
        }
        // Expanded: annotations + actions
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            if (loadingAnnotations) {
                AiModuleSpinner(label = "loading annotations…")
            }
            annotations.forEach { ann ->
                AnnotationRow(ann)
            }
            Row(Modifier.padding(start = 18.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (run.conclusion == "failure") {
                    SmallChip("rerun failed", palette.accent, onRerun)
                }
                if (run.detailsUrl.isNotBlank()) {
                    SmallChip("details", palette.textSecondary) {
                        // Open URL would go here
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationRow(ann: GHCheckAnnotation) {
    val palette = AiModuleTheme.colors
    val levelColor = when (ann.annotationLevel) {
        "failure" -> GitHubErrorRed
        "warning" -> GitHubWarningAmber()
        else -> palette.textSecondary
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(levelColor.copy(alpha = 0.06f))
            .border(1.dp, levelColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ann.annotationLevel, color = levelColor, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Text("${ann.path}:${ann.startLine}", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
        }
        if (ann.title.isNotBlank()) {
            Text(ann.title, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Text(ann.message, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp)
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
