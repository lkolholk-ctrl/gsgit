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
 * Текст/синтаксис CodeEditor: подсветка (EditorSyntaxTransformation, buildEditorAnnotatedText),
 * поиск (buildSearchMatches), символы (buildEditorSymbols), мелкие утилиты (coerceIn/floorMod/openUrl).
 * Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

internal fun buildEditorSubtitle(ext: String, lines: Int, chars: Int, changed: Boolean): String {
    val type = ext.uppercase().ifBlank { "TEXT" }
    val suffix = if (changed) " • Modified" else ""
    return "$type • $lines lines • $chars chars$suffix"
}

internal class EditorSyntaxTransformation(
    private val ext: String,
    private val searchQuery: String,
    private val currentMatchRange: IntRange?,
    private val precomputed: AnnotatedString
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val output = if (precomputed.text.length == text.text.length) {
            precomputed
        } else {
            buildEditorAnnotatedTextQuickFallback(text.text, ext, searchQuery, currentMatchRange)
        }
        return TransformedText(output, OffsetMapping.Identity)
    }
}

private fun buildEditorAnnotatedTextQuickFallback(
    text: String,
    ext: String,
    searchQuery: String,
    currentMatchRange: IntRange?
): AnnotatedString {
    return buildAnnotatedString {
        pushStyle(SpanStyle(color = Color(0xFFE5E7EB)))
        append(text)
        pop()
        buildSearchMatches(text, searchQuery, isRegex = false, matchCase = false).forEach { match ->
            addStyle(
                SpanStyle(background = Color(0xFFFFD166).copy(alpha = 0.20f)),
                match.start,
                match.end
            )
        }
        currentMatchRange?.let { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (end > start) {
                addStyle(SpanStyle(background = Color(0xFF58A6FF).copy(alpha = 0.34f)), start, end)
            }
        }
    }
}

internal fun buildEditorAnnotatedText(
    text: String,
    ext: String,
    searchQuery: String,
    currentMatchRange: IntRange?,
    isRegex: Boolean = false,
    matchCase: Boolean = false,
    palette: gs.git.vps.ui.theme.AiModuleColors = gs.git.vps.ui.theme.AiModuleDarkColors
): AnnotatedString {
    val syntax = buildAnnotatedString {
        if (text.length > 180_000) {
            pushStyle(SpanStyle(color = palette.textPrimary))
            append(text)
            pop()
            return@buildAnnotatedString
        }

        var start = 0
        while (start <= text.length) {
            val nextBreak = text.indexOf('\n', start)
            val end = if (nextBreak < 0) text.length else nextBreak
            val line = text.substring(start, end)
            if (line.length > 1_200) {
                pushStyle(SpanStyle(color = palette.textPrimary))
                append(line)
                pop()
            } else {
                append(doHighlightLine(line, ext, palette))
            }
            if (nextBreak < 0) break
            append('\n')
            start = nextBreak + 1
        }
    }

    return buildAnnotatedString {
        append(syntax)
        buildSearchMatches(text, searchQuery, isRegex, matchCase).forEach { match ->
            addStyle(
                SpanStyle(background = Color(0xFFFFD166).copy(alpha = 0.20f)),
                match.start,
                match.end
            )
        }
        currentMatchRange?.let { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (end > start) {
                addStyle(SpanStyle(background = Color(0xFF58A6FF).copy(alpha = 0.34f)), start, end)
            }
        }
    }
}

internal fun buildSearchMatches(text: String, query: String, isRegex: Boolean, matchCase: Boolean): List<EditorSearchMatch> {
    val q = query.trim()
    if (q.isBlank()) return emptyList()
    val matches = mutableListOf<EditorSearchMatch>()
    try {
        if (isRegex) {
            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = Regex(q, options)
            regex.findAll(text).take(2000).forEach { m ->
                val line = text.take(m.range.first).count { it == '\n' }
                matches += EditorSearchMatch(m.range.first, m.range.last + 1, line)
            }
        } else {
            var from = 0
            while (from <= text.length - q.length && matches.size < 2_000) {
                val idx = text.indexOf(q, from, ignoreCase = !matchCase)
                if (idx < 0) break
                val line = text.take(idx).count { it == '\n' }
                matches += EditorSearchMatch(idx, idx + q.length, line)
                from = idx + q.length.coerceAtLeast(1)
            }
        }
    } catch (_: Exception) {
        // Fallback for invalid regex
    }
    return matches
}

internal fun buildEditorSymbols(lines: List<String>, ext: String): List<EditorSymbol> {
    val symbols = mutableListOf<EditorSymbol>()
    val codeRegex = Regex("""^\s*(?:export\s+|public\s+|private\s+|protected\s+|internal\s+|open\s+|override\s+|suspend\s+|data\s+|sealed\s+|abstract\s+|final\s+)*(class|object|interface|enum|fun|function|def|fn|struct|trait|impl|const|let|var|val)\s+([A-Za-z_$][\w$]*)""")
    val markdownRegex = Regex("""^(#{1,6})\s+(.+)$""")
    val cssRegex = Regex("""^\s*([.#]?[A-Za-z0-9_-][^{]+)\s*\{""")
    val yamlRegex = Regex("""^([A-Za-z0-9_-][A-Za-z0-9_.-]*)\s*:""")

    lines.forEachIndexed { index, raw ->
        if (symbols.size >= 240) return@forEachIndexed
        val line = raw.trimEnd()
        when {
            ext in listOf("md", "markdown") -> {
                val match = markdownRegex.find(line) ?: return@forEachIndexed
                symbols += EditorSymbol(match.groupValues[2].trim().take(80), "H${match.groupValues[1].length}", index)
            }
            ext in listOf("css", "scss", "sass", "less") -> {
                val match = cssRegex.find(line) ?: return@forEachIndexed
                symbols += EditorSymbol(match.groupValues[1].trim().take(80), "CSS", index)
            }
            ext in listOf("yaml", "yml", "toml") && !raw.startsWith(" ") && !raw.startsWith("\t") -> {
                val match = yamlRegex.find(line) ?: return@forEachIndexed
                symbols += EditorSymbol(match.groupValues[1].trim().take(80), "KEY", index)
            }
            else -> {
                val match = codeRegex.find(line) ?: return@forEachIndexed
                val kind = match.groupValues[1].uppercase()
                symbols += EditorSymbol(match.groupValues[2].trim().take(80), kind, index)
            }
        }
    }
    return symbols
}

internal fun TextRange.coerceIn(length: Int): TextRange {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(0, length)
    return TextRange(safeStart, safeEnd)
}

internal fun Int.floorMod(other: Int): Int = if (other == 0) 0 else ((this % other) + other) % other

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) {
        Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
    }
}
