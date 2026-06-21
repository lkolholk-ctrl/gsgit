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
 * TimeTravel-таб репо-модуля: локальный reflog/тайм-машина по веткам.
 * Вынесено из GitHubRepoModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
internal fun TimeTravelTab(
    repo: GHRepo,
    selectedBranch: String,
    onOpenFileWithContent: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    
    var stashes by remember { mutableStateOf(LocalTimeTravelManager.getStashes(context, repo.fullName)) }
    var reflogs by remember { mutableStateOf(LocalTimeTravelManager.getReflog(context, repo.fullName)) }
    var viewingStash by remember { mutableStateOf<LocalStashItem?>(null) }
    var isResetting by remember { mutableStateOf(false) }

    fun refreshData() {
        stashes = LocalTimeTravelManager.getStashes(context, repo.fullName)
        reflogs = LocalTimeTravelManager.getReflog(context, repo.fullName)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- STASHES SECTION ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STASH STACK [${stashes.size}]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (stashes.isNotEmpty()) {
                    Text(
                        text = "[clear all]",
                        color = palette.error,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            context.getSharedPreferences("gsgit_stash_prefs", Context.MODE_PRIVATE)
                                .edit().remove(repo.fullName).apply()
                            refreshData()
                            Toast.makeText(context, "Stash stack cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (stashes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                        .background(palette.surface.copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No stashed changes in this repository.\nStash changes from the code editor menu.",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(stashes) { stash ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stash.id,
                                color = palette.accent,
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val dateStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", stash.timestamp).toString()
                            Text(
                                text = dateStr,
                                color = palette.textMuted,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = stash.message,
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Files: ${stash.files.joinToString { it.path.substringAfterLast("/") }}",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Apply button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (stash.files.isNotEmpty()) {
                                            if (stash.files.size == 1) {
                                                val first = stash.files.first()
                                                onOpenFileWithContent(first.path, first.content)
                                                Toast.makeText(context, "Stash applied: opened ${first.path.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewingStash = stash
                                            }
                                        } else {
                                            Toast.makeText(context, "Stash is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("apply", color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 10.sp)
                            }
                            
                            // Pop button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.accent.copy(alpha = 0.1f))
                                    .border(0.5.dp, palette.accent, RoundedCornerShape(4.dp))
                                    .clickable {
                                        if (stash.files.isNotEmpty()) {
                                            if (stash.files.size == 1) {
                                                val first = stash.files.first()
                                                onOpenFileWithContent(first.path, first.content)
                                                Toast.makeText(context, "Stash applied: opened ${first.path.substringAfterLast("/")}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewingStash = stash
                                            }
                                        }
                                        LocalTimeTravelManager.deleteStash(context, repo.fullName, stash.id)
                                        refreshData()
                                        Toast.makeText(context, "Stash popped (applied and deleted)", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("pop", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Drop button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .border(0.5.dp, palette.error, RoundedCornerShape(4.dp))
                                    .clickable {
                                        LocalTimeTravelManager.deleteStash(context, repo.fullName, stash.id)
                                        refreshData()
                                        Toast.makeText(context, "Stash entry dropped", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("drop", color = palette.error, fontFamily = JetBrainsMono, fontSize = 10.sp)
                            }

                            // View files button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                    .clickable {
                                        viewingStash = stash
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("view", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- REFLOGS SECTION ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GIT REFLOG [${reflogs.size}]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (reflogs.isNotEmpty()) {
                    Text(
                        text = "[clear all]",
                        color = palette.error,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            context.getSharedPreferences("gsgit_reflog_prefs", Context.MODE_PRIVATE)
                                .edit().remove(repo.fullName).apply()
                            refreshData()
                            Toast.makeText(context, "Reflog history cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        if (reflogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                        .background(palette.surface.copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No reflog entries found.\nBranch changes or pushed references will be logged here.",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(reflogs) { entry ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, palette.border, RoundedCornerShape(6.dp))
                        .background(palette.surface)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HEAD@{${entry.id}}",
                                color = palette.accent,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val dateStr = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", entry.timestamp).toString()
                            Text(
                                text = dateStr,
                                color = palette.textMuted,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp
                            )
                        }
                        
                        Text(
                            text = entry.action,
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "${entry.beforeSha.take(7)} -> ${entry.afterSha.take(7)}",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        if (entry.afterSha.startsWith("branch:") == false && isResetting == false) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.surface)
                                    .border(0.5.dp, palette.accent, RoundedCornerShape(4.dp))
                                    .clickable {
                                        isResetting = true
                                        scope.launch {
                                            val targetSha = entry.afterSha
                                            val ok = GitHubManager.updateGitRef(
                                                context,
                                                repo.owner,
                                                repo.name,
                                                "refs/heads/$selectedBranch",
                                                targetSha,
                                                force = true
                                            )
                                            isResetting = false
                                            if (ok != null) {
                                                Toast.makeText(context, "Soft reset: branch HEAD is now at ${targetSha.take(7)}", Toast.LENGTH_LONG).show()
                                                LocalTimeTravelManager.addReflogEntry(
                                                    context,
                                                    repo.fullName,
                                                    "refs/heads/$selectedBranch",
                                                    beforeSha = entry.beforeSha,
                                                    afterSha = targetSha,
                                                    action = "reset: moving to ${targetSha.take(7)}"
                                                )
                                                refreshData()
                                            } else {
                                                Toast.makeText(context, "Reset failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("[ soft reset to ${entry.afterSha.take(7)} ]", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    viewingStash?.let { stash ->
        AiModuleAlertDialog(
            onDismissRequest = { viewingStash = null },
            title = stash.id,
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Stashed changes for files:", color = palette.textSecondary, fontSize = 12.sp, fontFamily = JetBrainsMono)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stash.files) { sFile ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sFile.path, color = palette.accent, fontSize = 11.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(
                                        text = "[open]",
                                        color = palette.accent,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            onOpenFileWithContent(sFile.path, sFile.content)
                                            viewingStash = null
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                        .background(palette.surface)
                                        .padding(4.dp)
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        item {
                                            Text(sFile.content, color = palette.textSecondary, fontSize = 9.sp, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "close",
                    onClick = { viewingStash = null },
                    tint = palette.accent
                )
            }
        )
    }
}

