package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WrapText
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.model.GHCommit
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.getRepoContents
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.Blue
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.ui.theme.SeparatorColor
import gs.git.vps.ui.theme.TextSecondary
import gs.git.vps.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Diff CodeEditor: типы DiffType/DiffLine/DiffVisuals, generateDiff, ModernDiffCanvas,
 * formatRelativeTime. Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

private enum class DiffType { ADD, REMOVE, EQUAL }
private data class DiffLine(val type: DiffType, val text: String)

private fun generateDiff(oldText: String, newText: String): List<DiffLine> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val n = oldLines.size
    val m = newLines.size
    
    if (n * m > 100_000) {
        val result = mutableListOf<DiffLine>()
        val oldSet = oldLines.toSet()
        val newSet = newLines.toSet()
        oldLines.forEach { line ->
            if (line !in newSet) result.add(DiffLine(DiffType.REMOVE, line))
        }
        newLines.forEach { line ->
            if (line !in oldSet) {
                result.add(DiffLine(DiffType.ADD, line))
            } else {
                result.add(DiffLine(DiffType.EQUAL, line))
            }
        }
        return result
    }
    
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) {
        for (j in 1..m) {
            if (oldLines[i - 1] == newLines[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }
    
    val result = mutableListOf<DiffLine>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        when {
            i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1] -> {
                result.add(0, DiffLine(DiffType.EQUAL, oldLines[i - 1]))
                i--
                j--
            }
            j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j]) -> {
                result.add(0, DiffLine(DiffType.ADD, newLines[j - 1]))
                j--
            }
            else -> {
                result.add(0, DiffLine(DiffType.REMOVE, oldLines[i - 1]))
                i--
            }
        }
    }
    return result
}

private data class DiffVisuals(val bgColor: Color, val textColor: Color, val sign: String, val signColor: Color)

@Composable
internal fun ModernDiffCanvas(
    oldText: String,
    newText: String,
    fontSize: Int
) {
    val palette = AiModuleTheme.colors
    val diffLines = remember(oldText, newText) { generateDiff(oldText, newText) }
    
    SelectionContainer {
        LazyColumn(
            Modifier.fillMaxSize().background(palette.background).padding(horizontal = 12.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            itemsIndexed(diffLines) { _, diffLine ->
                val visuals = when (diffLine.type) {
                    DiffType.ADD -> DiffVisuals(Color(0xFF1E3A1E), Color(0xFFA8D982), "+", Color(0xFFA8D982))
                    DiffType.REMOVE -> DiffVisuals(Color(0xFF3A1E1E), Color(0xFFE57373), "-", Color(0xFFE57373))
                    DiffType.EQUAL -> DiffVisuals(Color.Transparent, palette.textPrimary, " ", palette.textMuted)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(visuals.bgColor)
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = visuals.sign,
                        color = visuals.signColor,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = diffLine.text.ifEmpty { " " },
                        color = visuals.textColor,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (fontSize + 7).sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

internal fun formatRelativeTime(dateStr: String): String {
    try {
        val clean = dateStr.replace("Z", "").replace("T", " ")
        val parts = clean.split(" ")
        if (parts.size < 2) return dateStr
        val dateParts = parts[0].split("-")
        if (dateParts.size < 3) return dateStr
        val year = dateParts[0].toIntOrNull() ?: return dateStr
        val month = dateParts[1].toIntOrNull() ?: return dateStr
        val day = dateParts[2].toIntOrNull() ?: return dateStr
        
        val currentYear = 2026
        val currentMonth = 5
        val currentDay = 30
        
        val diffYears = currentYear - year
        val diffMonths = currentMonth - month + (diffYears * 12)
        val diffDays = currentDay - day + (diffMonths * 30)
        
        return when {
            diffDays <= 0 -> "today"
            diffDays == 1 -> "yesterday"
            diffDays < 7 -> "$diffDays days ago"
            diffDays < 30 -> "${diffDays / 7} weeks ago"
            diffDays < 365 -> "${diffDays / 30} months ago"
            else -> "${diffDays / 365} years ago"
        }
    } catch (_: Exception) {
        return dateStr
    }
}

