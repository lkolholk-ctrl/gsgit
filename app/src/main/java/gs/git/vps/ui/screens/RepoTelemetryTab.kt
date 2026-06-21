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

/**
 * Telemetry-таб репо-модуля: аналитика коммитов, граф добавлений/удалений, grep по истории.
 * Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun TelemetryTab(repo: GHRepo, commits: List<GHCommit>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    
    var telemetryLoading by remember { mutableStateOf(true) }
    var progressMessage by remember { mutableStateOf("Initializing scanner...") }
    
    var additionsHistory by remember { mutableStateOf<List<Int>>(emptyList()) }
    var deletionsHistory by remember { mutableStateOf<List<Int>>(emptyList()) }
    var commitShas by remember { mutableStateOf<List<String>>(emptyList()) }
    var codeChurn by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var contributors by remember { mutableStateOf<List<GHContributor>>(emptyList()) }
    
    var grepQuery by remember { mutableStateOf("") }
    var isSearchingGrep by remember { mutableStateOf(false) }
    var grepResults by remember { mutableStateOf<List<GrepResult>>(emptyList()) }
    val commitDetailsCache = remember { mutableStateMapOf<String, GHCommitDetail>() }
    var activeCommitsState by remember { mutableStateOf<List<GHCommit>>(emptyList()) }

    LaunchedEffect(repo.fullName, commits) {
        try {
            telemetryLoading = true
            val activeCommits = if (commits.isEmpty()) {
                progressMessage = "Fetching commits..."
                withContext(Dispatchers.IO) {
                    GitHubManager.getCommits(context, repo.owner, repo.name, 1)
                }
            } else {
                commits
            }
            activeCommitsState = activeCommits
            
            if (activeCommits.isEmpty()) {
                telemetryLoading = false
                return@LaunchedEffect
            }
            
            progressMessage = "Fetching contributors..."
            contributors = withContext(Dispatchers.IO) {
                GitHubManager.getContributors(context, repo.owner, repo.name)
            }
            
            val targetCommits = activeCommits.take(8)
            val adds = mutableListOf<Int>()
            val dels = mutableListOf<Int>()
            val shas = mutableListOf<String>()
            val churnMap = mutableMapOf<String, Int>()
            
            targetCommits.forEachIndexed { index, commit ->
                progressMessage = "Analyzing commit ${index + 1}/${targetCommits.size}..."
                val details = withContext(Dispatchers.IO) {
                    GitHubManager.getCommitDiff(context, repo.owner, repo.name, commit.sha)
                }
                if (details != null) {
                    commitDetailsCache[commit.sha] = details
                    adds.add(details.totalAdditions)
                    dels.add(details.totalDeletions)
                    shas.add(commit.sha.take(5))
                    
                    details.files.forEach { file ->
                        val total = file.additions + file.deletions
                        churnMap[file.filename] = (churnMap[file.filename] ?: 0) + total
                    }
                } else {
                    adds.add(0)
                    dels.add(0)
                    shas.add(commit.sha.take(5))
                }
            }
            
            additionsHistory = adds.reversed()
            deletionsHistory = dels.reversed()
            commitShas = shas.reversed()
            codeChurn = churnMap.toList().sortedByDescending { it.second }.take(5)
            
            telemetryLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            telemetryLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (telemetryLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(0.5.dp, palette.border, RoundedCornerShape(8.dp))
                        .background(palette.surface.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiModuleSpinner(label = progressMessage)
                    }
                }
            }
        } else {
            // --- ADDITIONS / DELETIONS CURVE ---
            item {
                Text(
                    text = "CODE BASE FREQUENCY (LAST ${additionsHistory.size} COMMITS)",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                TelemetryGraph(additionsHistory, deletionsHistory, commitShas)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFF00FF66)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Additions", color = palette.textSecondary, fontSize = 10.sp, fontFamily = JetBrainsMono)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFFFF0055)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deletions", color = palette.textSecondary, fontSize = 10.sp, fontFamily = JetBrainsMono)
                    }
                }
            }

            // --- CODE CHURN SECTION ---
            item {
                Text(
                    text = "CODE CHURN TOP FILES",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (codeChurn.isEmpty()) {
                    Text("No file changes recorded.", color = palette.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val maxChurn = (codeChurn.firstOrNull()?.second ?: 1).toFloat()
                        codeChurn.forEach { (filename, count) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        filename.substringAfterLast("/"),
                                        color = palette.textPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMono,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "$count lines changed",
                                        color = palette.textSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(palette.border.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(count / maxChurn)
                                            .background(palette.accent)
                                    )
                                }
                                Text(
                                    filename,
                                    color = palette.textMuted,
                                    fontSize = 8.sp,
                                    fontFamily = JetBrainsMono,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // --- CONTRIBUTOR ACTIVITY ---
            item {
                Text(
                    text = "CONTRIBUTOR RANKINGS",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (contributors.isEmpty()) {
                    Text("No contributor information available.", color = palette.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val totalContr = contributors.sumOf { it.contributions }.toFloat().coerceAtLeast(1f)
                        contributors.take(5).forEach { contr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (contr.avatarUrl.isNotBlank()) {
                                    AsyncImage(contr.avatarUrl, contr.login, Modifier.size(24.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(palette.border))
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        contr.login,
                                        color = palette.textPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val percent = ((contr.contributions / totalContr) * 100).toInt()
                                    Text(
                                        "${contr.contributions} contributions ($percent%)",
                                        color = palette.textSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- INTERACTIVE COMMIT GREP SCANNER ---
            item {
                Text(
                    text = "COMMIT GREP CODESCANNER",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Search commits and file diff patches using regex",
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CompactField("grep regex (e.g. TODO|fun|const)", grepQuery) { grepQuery = it }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.accent.copy(alpha = 0.1f))
                            .border(0.5.dp, palette.accent, RoundedCornerShape(4.dp))
                            .clickable {
                                if (grepQuery.isBlank()) {
                                    Toast.makeText(context, "Please enter a search regex", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                isSearchingGrep = true
                                val regex = try { Regex(grepQuery) } catch (e: Exception) { null }
                                if (regex == null) {
                                    Toast.makeText(context, "Invalid regular expression", Toast.LENGTH_SHORT).show()
                                    isSearchingGrep = false
                                    return@clickable
                                }
                                
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        val results = mutableListOf<GrepResult>()
                                        activeCommitsState.take(15).forEach { commit ->
                                            if (regex.containsMatchIn(commit.message) || regex.containsMatchIn(commit.author)) {
                                                results.add(GrepResult(commit.sha, commit.message, "commit-meta", ""))
                                            }
                                            
                                            var details = commitDetailsCache[commit.sha]
                                            if (details == null) {
                                                val fetchedDetails = withContext(Dispatchers.IO) {
                                                    try {
                                                        GitHubManager.getCommitDiff(context, repo.owner, repo.name, commit.sha)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        null
                                                    }
                                                }
                                                if (fetchedDetails != null) {
                                                    withContext(Dispatchers.Main) {
                                                        commitDetailsCache[commit.sha] = fetchedDetails
                                                    }
                                                    details = fetchedDetails
                                                }
                                            }
                                            
                                            if (details != null) {
                                                details.files.forEach { file ->
                                                    val patch = file.patch ?: ""
                                                    if (regex.containsMatchIn(patch)) {
                                                        val matchedLines = patch.lines().filter { regex.containsMatchIn(it) }
                                                        matchedLines.forEach { line ->
                                                            results.add(GrepResult(commit.sha, commit.message, file.filename, line))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            grepResults = results
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Scan error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            isSearchingGrep = false
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isSearchingGrep) {
                            AiModuleSpinner()
                        } else {
                            Text("scan", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (grepResults.isNotEmpty()) {
                item {
                    Text(
                        "SCAN RESULTS [${grepResults.size}]",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(grepResults) { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                            .background(palette.surface)
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    res.sha.take(7),
                                    color = palette.accent,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    res.location,
                                    color = palette.textSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = JetBrainsMono
                                )
                            }
                            Text(
                                res.message.lines().firstOrNull().orEmpty(),
                                color = palette.textPrimary,
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMono
                            )
                            if (res.line.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.surface.copy(alpha = 0.5f))
                                        .border(0.2.dp, palette.border)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        res.line.trim(),
                                        color = palette.textMuted,
                                        fontSize = 9.sp,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (grepQuery.isNotBlank() && !isSearchingGrep) {
                item {
                    Text("No scan matches found.", color = palette.textMuted, fontSize = 10.sp, fontFamily = JetBrainsMono)
                }
            }
        }
    }
}

data class GrepResult(
    val sha: String,
    val message: String,
    val location: String,
    val line: String
)

@Composable
fun TelemetryGraph(additions: List<Int>, deletions: List<Int>, commitShas: List<String>) {
    val palette = AiModuleTheme.colors
    
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
            .background(palette.surface.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height
        
        if (additions.isEmpty() || additions.size < 2) {
            drawContext.canvas.nativeCanvas.drawText(
                "INSUFFICIENT TELEMETRY DATA",
                width / 2f,
                height / 2f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
            return@Canvas
        }
        
        val maxVal = maxOf(additions.maxOrNull() ?: 1, deletions.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
        val pointsCount = additions.size
        val stepX = width / (pointsCount - 1)
        
        for (i in 1..4) {
            val y = height * (i / 5f)
            drawLine(
                color = palette.border.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        val addPath = androidx.compose.ui.graphics.Path()
        val delPath = androidx.compose.ui.graphics.Path()
        
        addPath.moveTo(0f, height - (additions[0] / maxVal) * height)
        delPath.moveTo(0f, height - (deletions[0] / maxVal) * height)
        
        for (i in 1 until pointsCount) {
            val x = i * stepX
            val addY = height - (additions[i] / maxVal) * height
            val delY = height - (deletions[i] / maxVal) * height
            
            addPath.lineTo(x, addY)
            delPath.lineTo(x, delY)
        }
        
        drawPath(
            path = addPath,
            color = Color(0xFF00FF66),
            style = Stroke(width = 3f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        
        drawPath(
            path = delPath,
            color = Color(0xFFFF0055),
            style = Stroke(width = 3f, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        
        for (i in 0 until pointsCount) {
            val x = i * stepX
            val addY = height - (additions[i] / maxVal) * height
            val delY = height - (deletions[i] / maxVal) * height
            
            drawCircle(
                color = Color(0xFF00FF66),
                radius = 6f,
                center = Offset(x, addY)
            )
            drawCircle(
                color = Color(0xFFFF0055),
                radius = 6f,
                center = Offset(x, delY)
            )
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "> ${label.lowercase()}",
            color = AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
        )
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AiModuleTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AiModuleTheme.colors.accent),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
    }
}

