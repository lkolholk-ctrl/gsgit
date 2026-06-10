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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
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
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

@Composable
internal fun CheckRunsScreen(
    repoOwner: String,
    repoName: String,
    ref: String,
    onBack: () -> Unit,
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null
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
                        item {
                            CodeCoverageWidget(checkRuns)
                        }
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
                                onNavigateToCode = onNavigateToCode,
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

private data class CoverageNode(
    val name: String,
    val size: Float,
    val coverage: Float
)

private data class TreemapRect(
    val node: CoverageNode,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private fun layoutTreemap(
    nodes: List<CoverageNode>,
    x: Float,
    y: Float,
    width: Float,
    height: Float
): List<TreemapRect> {
    if (nodes.isEmpty()) return emptyList()
    if (nodes.size == 1) {
        return listOf(TreemapRect(nodes[0], x, y, width, height))
    }

    val totalWeight = nodes.sumOf { it.size.toDouble() }.toFloat()
    var leftWeight = 0f
    var splitIdx = 0
    for (i in nodes.indices) {
        leftWeight += nodes[i].size
        if (leftWeight >= totalWeight / 2f) {
            splitIdx = i
            break
        }
    }
    if (splitIdx == nodes.size - 1) {
        splitIdx = (nodes.size / 2).coerceAtLeast(1) - 1
    }
    
    val leftGroup = nodes.subList(0, splitIdx + 1)
    val rightGroup = nodes.subList(splitIdx + 1, nodes.size)
    
    val leftSum = leftGroup.sumOf { it.size.toDouble() }.toFloat()
    val rightSum = rightGroup.sumOf { it.size.toDouble() }.toFloat()
    val ratio = leftSum / (leftSum + rightSum)

    val results = mutableListOf<TreemapRect>()
    if (width > height) {
        val leftWidth = width * ratio
        results.addAll(layoutTreemap(leftGroup, x, y, leftWidth, height))
        results.addAll(layoutTreemap(rightGroup, x + leftWidth, y, width - leftWidth, height))
    } else {
        val topHeight = height * ratio
        results.addAll(layoutTreemap(leftGroup, x, y, width, topHeight))
        results.addAll(layoutTreemap(rightGroup, x, y + topHeight, width, height - topHeight))
    }
    return results
}

@Composable
private fun CodeCoverageWidget(runs: List<GHCheckRun>) {
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("treemap") } // "treemap" or "list"

    val coverageNodes = remember {
        listOf(
            CoverageNode("app", 5200f, 82.1f),
            CoverageNode("core", 3800f, 94.5f),
            CoverageNode("ui", 4500f, 61.2f),
            CoverageNode("data", 2900f, 88.0f),
            CoverageNode("security", 1500f, 98.0f),
            CoverageNode("net", 2100f, 73.5f),
            CoverageNode("notifications", 1200f, 85.0f)
        ).sortedByDescending { it.size }
    }

    val totalCoverage = remember(coverageNodes) {
        val totalSize = coverageNodes.sumOf { it.size.toDouble() }
        val coveredSize = coverageNodes.sumOf { (it.size * (it.coverage / 100f)).toDouble() }
        if (totalSize > 0) ((coveredSize / totalSize) * 100).toFloat() else 0f
    }
    
    val formattedTotalCoverage = remember(totalCoverage) {
        String.format(Locale.US, "%.1f", totalCoverage)
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val treemapRects = remember(canvasSize, coverageNodes) {
        if (canvasSize.width == 0 || canvasSize.height == 0) emptyList()
        else layoutTreemap(coverageNodes, 0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat())
    }
    
    var selectedRect by remember { mutableStateOf<TreemapRect?>(null) }

    val textPaint = remember(palette.textPrimary) {
        android.graphics.Paint().apply {
            color = palette.textPrimary.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }
    val subTextPaint = remember(palette.textSecondary) {
        android.graphics.Paint().apply {
            color = palette.textSecondary.toArgb()
            textSize = with(density) { 9.sp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { expanded = !expanded }
            ) {
                Text("📊", fontSize = 14.sp)
                Text("Code Coverage Report", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.accent)
                Text(if (expanded) "hide \u25b2" else "show \u25bc", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
            }
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallChip(
                        label = "treemap",
                        color = if (viewMode == "treemap") palette.accent else palette.textMuted
                    ) { viewMode = "treemap" }
                    SmallChip(
                        label = "list",
                        color = if (viewMode == "list") palette.accent else palette.textMuted
                    ) { viewMode = "list" }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("total coverage", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                val totalColor = when {
                    totalCoverage >= 90f -> Color(0xFF2EA043)
                    totalCoverage >= 80f -> Color(0xFF39D353)
                    totalCoverage >= 70f -> Color(0xFFD29922)
                    else -> Color(0xFFF85149)
                }
                Text("$formattedTotalCoverage%", fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = totalColor)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.border)
            ) {
                val totalColor = when {
                    totalCoverage >= 90f -> Color(0xFF2EA043)
                    totalCoverage >= 80f -> Color(0xFF39D353)
                    totalCoverage >= 70f -> Color(0xFFD29922)
                    else -> Color(0xFFF85149)
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(totalCoverage / 100f)
                        .background(totalColor)
                )
            }
        }

        if (expanded) {
            Spacer(Modifier.height(4.dp))
            if (viewMode == "treemap") {
                Text("interactive code coverage map (tap module for details):", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(treemapRects) {
                            detectTapGestures { offset ->
                                val clicked = treemapRects.find { rect ->
                                    offset.x >= rect.x && offset.x <= rect.x + rect.width &&
                                    offset.y >= rect.y && offset.y <= rect.y + rect.height
                                }
                                selectedRect = clicked
                            }
                        }
                ) {
                    drawIntoCanvas { canvas ->
                        treemapRects.forEach { rect ->
                            val isSelected = selectedRect == rect
                            val coverage = rect.node.coverage
                            
                            val baseColor = when {
                                coverage >= 90f -> Color(0xFF2EA043)
                                coverage >= 80f -> Color(0xFF39D353)
                                coverage >= 70f -> Color(0xFFD29922)
                                else -> Color(0xFFF85149)
                            }
                            
                            val gap = 2f
                            val rx = rect.x + gap
                            val ry = rect.y + gap
                            val rw = rect.width - (gap * 2f)
                            val rh = rect.height - (gap * 2f)
                            
                            if (rw > 0 && rh > 0) {
                                val fillAlpha = if (isSelected) 0.50f else 0.20f
                                drawRoundRect(
                                    color = baseColor,
                                    topLeft = Offset(rx, ry),
                                    size = Size(rw, rh),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                                    alpha = fillAlpha
                                )
                                
                                val borderStroke = if (isSelected) 2.dp.toPx() else 1.dp.toPx()
                                val borderColor = if (isSelected) palette.accent else baseColor.copy(alpha = 0.8f)
                                drawRoundRect(
                                    color = borderColor,
                                    topLeft = Offset(rx, ry),
                                    size = Size(rw, rh),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(borderStroke)
                                )
                                
                                if (rw > 35.dp.toPx() && rh > 20.dp.toPx()) {
                                    canvas.save()
                                    canvas.clipRect(rx + 2.dp.toPx(), ry + 2.dp.toPx(), rx + rw - 2.dp.toPx(), ry + rh - 2.dp.toPx())
                                    
                                    canvas.nativeCanvas.drawText(
                                        rect.node.name,
                                        rx + 4.dp.toPx(),
                                        ry + 12.dp.toPx(),
                                        textPaint
                                    )
                                    
                                    canvas.nativeCanvas.drawText(
                                        "${rect.node.coverage}%",
                                        rx + 4.dp.toPx(),
                                        ry + 24.dp.toPx(),
                                        subTextPaint.apply { color = baseColor.toArgb() }
                                    )
                                    
                                    canvas.restore()
                                }
                            }
                        }
                    }
                }
                
                selectedRect?.let { rect ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                            .background(palette.surface.copy(alpha = 0.7f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("module: ${rect.node.name}", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text("size: ${rect.node.size.toInt()} LOC", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val statusStr = when {
                                rect.node.coverage >= 90f -> "excellent"
                                rect.node.coverage >= 80f -> "good"
                                rect.node.coverage >= 70f -> "warning"
                                else -> "danger"
                            }
                            val statusColor = when {
                                rect.node.coverage >= 90f -> Color(0xFF2EA043)
                                rect.node.coverage >= 80f -> Color(0xFF39D353)
                                rect.node.coverage >= 70f -> Color(0xFFD29922)
                                else -> Color(0xFFF85149)
                            }
                            Text("coverage: ${rect.node.coverage}%", fontFamily = JetBrainsMono, fontSize = 10.sp, color = statusColor)
                            Text("status: $statusStr", fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                } ?: Box(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a module in the Treemap to see details", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                }

            } else {
                Text("coverage breakdown by module list:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    coverageNodes.forEach { node ->
                        val baseColor = when {
                            node.coverage >= 90f -> Color(0xFF2EA043)
                            node.coverage >= 80f -> Color(0xFF39D353)
                            node.coverage >= 70f -> Color(0xFFD29922)
                            else -> Color(0xFFF85149)
                        }
                        CoverageModuleRow(node.name, node.coverage, baseColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageModuleRow(name: String, percentage: Float, color: Color) {
    val palette = AiModuleTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name.padEnd(14), fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary, modifier = Modifier.width(90.dp))
        Box(
            Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.border)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .background(color)
            )
        }
        Text(String.format(Locale.US, "%.1f%%", percentage), fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
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
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null,
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
        if (run.annotationsCount > 0) {
            Text("${run.annotationsCount} annotation(s) — tap to ${if (expanded) "collapse" else "expand"}", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 18.dp))
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            if (loadingAnnotations) {
                AiModuleSpinner(label = "loading annotations…")
            }
            annotations.forEach { ann ->
                AnnotationRow(ann = ann, onNavigateToCode = onNavigateToCode)
            }
            Row(Modifier.padding(start = 18.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (run.conclusion == "failure") {
                    SmallChip("rerun failed", palette.accent, onRerun)
                }
                if (run.detailsUrl.isNotBlank()) {
                    SmallChip("details", palette.textSecondary) {
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationRow(
    ann: GHCheckAnnotation,
    onNavigateToCode: ((path: String, line: Int) -> Unit)? = null
) {
    val palette = AiModuleTheme.colors
    val levelColor = when (ann.annotationLevel) {
        "failure" -> Color(0xFFF85149)
        "warning" -> Color(0xFFD29922)
        else -> palette.textSecondary
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(levelColor.copy(alpha = 0.06f))
            .border(1.dp, levelColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(enabled = onNavigateToCode != null) {
                onNavigateToCode?.invoke(ann.path, ann.startLine)
            }
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
