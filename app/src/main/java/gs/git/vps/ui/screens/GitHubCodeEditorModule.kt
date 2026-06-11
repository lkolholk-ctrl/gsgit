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
import gs.git.vps.data.github.GHContent
import gs.git.vps.data.github.GHCommit
import gs.git.vps.data.github.GHRepo
import gs.git.vps.data.github.GitHubManager
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

private enum class GitHubEditorMode { EDIT, READ, PREVIEW, DIFF }

private data class EditorSearchMatch(val start: Int, val end: Int, val line: Int)

private data class EditorSymbol(val name: String, val kind: String, val line: Int)

private data class EditorTab(
    val file: GHContent,
    val textState: TextFieldValue,
    val savedContent: String,
    val savedSha: String,
    val mode: GitHubEditorMode,
    val undoStack: List<TextFieldValue>,
    val redoStack: List<TextFieldValue>
)

@Composable
fun CodeEditorScreen(
    repoOwner: String,
    repoName: String,
    file: GHContent,
    branch: String,
    initialContent: String,
    initialLine: Int? = null,
    onBack: () -> Unit,
    onAskAi: ((prompt: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val tabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabIndex by remember { mutableStateOf(0) }
    var showFilePicker by remember { mutableStateOf(false) }
    var showStashDialog by remember { mutableStateOf(false) }

    LaunchedEffect(file.path, branch, initialContent) {
        if (tabs.isEmpty()) {
            tabs.add(
                EditorTab(
                    file = file,
                    textState = TextFieldValue(initialContent),
                    savedContent = initialContent,
                    savedSha = file.sha,
                    mode = GitHubEditorMode.EDIT,
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            )
        }
    }

    val prefs = remember(context) { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }
    var textState by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var savedContent by remember { mutableStateOf(initialContent) }
    var savedSha by remember { mutableStateOf(file.sha) }
    var mode by remember { mutableStateOf(GitHubEditorMode.EDIT) }
    var lineNumbers by rememberSaveable(file.path, branch) { mutableStateOf(true) }
    var wrapLines by rememberSaveable(file.path, branch) { mutableStateOf(prefs.getBoolean("editor_word_wrap", false)) }
    var showSearch by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var searchState by rememberSaveable(file.path, branch, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var replaceState by rememberSaveable(file.path, branch, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showGoToLine by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var showOutline by rememberSaveable(file.path, branch) { mutableStateOf(true) }
    var showOutlineDialog by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var goToLineText by rememberSaveable(file.path, branch) { mutableStateOf("") }
    var commitMessage by rememberSaveable(file.path, branch) { mutableStateOf("Update ${file.name}") }
    var showCommitDialog by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var saving by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var currentMatchIndex by rememberSaveable(file.path, branch) { mutableIntStateOf(0) }
    var fontSize by rememberSaveable(file.path, branch) { mutableIntStateOf(prefs.getInt("editor_font_size", 13)) }
    var zenMode by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var showConflictResolver by remember { mutableStateOf(false) }
    val hasConflictMarkers = remember(textState.text) {
        textState.text.contains("<<<<<<<") && textState.text.contains("=======") && textState.text.contains(">>>>>>>")
    }
    var showCopilotChat by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var copilotInitialPrompt by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(initialLine) {
        if (initialLine != null) {
            val linesList = initialContent.lines()
            val safe = initialLine.coerceIn(1, linesList.size.coerceAtLeast(1))
            val offset = linesList.take(safe - 1).sumOf { it.length + 1 }.coerceAtMost(initialContent.length)
            textState = textState.copy(selection = androidx.compose.ui.text.TextRange(offset))
        }
    }

    var currentBranch by rememberSaveable(file.path) { mutableStateOf(branch) }
    var loadingFile by remember { mutableStateOf(false) }
    var showBranchSwitcher by remember { mutableStateOf(false) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingBranches by remember { mutableStateOf(false) }
    var fileCommits by remember { mutableStateOf<List<GHCommit>>(emptyList()) }
    var loadingCommits by remember { mutableStateOf(false) }
    var showBlame by rememberSaveable(file.path) { mutableStateOf(false) }

    val currentFile = tabs.getOrNull(activeTabIndex)?.file ?: file

    LaunchedEffect(currentBranch, currentFile.path) {
        loadingCommits = true
        try {
            fileCommits = GitHubManager.getFileCommits(context, repoOwner, repoName, currentFile.path, currentBranch)
        } catch (_: java.lang.Exception) {}
        loadingCommits = false
    }

    LaunchedEffect(Unit) {
        loadingBranches = true
        try {
            branches = GitHubManager.getBranches(context, repoOwner, repoName)
        } catch (_: java.lang.Exception) {}
        loadingBranches = false
    }

    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current

    val ext = remember(currentFile.path) { currentFile.name.substringAfterLast(".", "").lowercase() }
    val isImage = ext in listOf("png", "jpg", "jpeg", "gif", "webp", "svg", "ico")
    val isMarkdown = ext in listOf("md", "markdown")
    val text = textState.text
    val lines = remember(text) { text.lines() }
    val hasChanges = text != savedContent
    val commentPrefix = remember(ext) { commentPrefixForExtension(ext) }
    val currentLine = remember(textState.selection, text) {
        text.take(textState.selection.start.coerceIn(0, text.length)).count { it == '\n' } + 1
    }
    val currentColumn = remember(textState.selection, text) {
        val cursor = textState.selection.start.coerceIn(0, text.length)
        if (cursor == 0) 1 else cursor - text.lastIndexOf('\n', cursor - 1).let { if (it < 0) -1 else it }
    }

    var isRegex by rememberSaveable(file.path, branch) { mutableStateOf(false) }
    var matchCase by rememberSaveable(file.path, branch) { mutableStateOf(false) }

    val matches = remember(text, searchState.text, isRegex, matchCase) {
        buildSearchMatches(text, searchState.text, isRegex, matchCase)
    }
    val currentMatch = matches.getOrNull(currentMatchIndex)
    val palette = AiModuleTheme.colors
    val symbols = remember(lines, ext) { buildEditorSymbols(lines, ext) }

    var highlightedText by remember(file.path, branch) {
        mutableStateOf(buildAnnotatedString {
            pushStyle(SpanStyle(color = Color(0xFFE5E7EB)))
            append(initialContent)
            pop()
        })
    }

    LaunchedEffect(text, searchState.text, currentMatchIndex, isRegex, matchCase, palette) {
        kotlinx.coroutines.delay(40)
        val asyncHighlight = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val range = currentMatch?.let { it.start until it.end }
            buildEditorAnnotatedText(text, ext, searchState.text, range, isRegex, matchCase, palette)
        }
        highlightedText = asyncHighlight
    }

    val gutterStates = remember(savedContent, text) {
        computeGutterDiff(savedContent, text)
    }

    val currentWord = remember(textState.text, textState.selection) {
        val cursor = textState.selection.start
        if (cursor <= 0 || cursor > textState.text.length) ""
        else {
            val src = textState.text
            var start = cursor - 1
            while (start >= 0 && (src[start].isLetterOrDigit() || src[start] == '_' || src[start] == '$')) {
                start--
            }
            start++
            src.substring(start, cursor)
        }
    }

    val autocompleteSuggestions = remember(currentWord, ext, symbols) {
        if (currentWord.isBlank() || currentWord.length < 2) emptyList<Pair<String, String>>()
        else {
            val words = when (ext) {
                "kt", "kotlin" -> gs.git.vps.ui.components.KOTLIN_KEYWORDS
                "java" -> gs.git.vps.ui.components.JAVA_KEYWORDS
                "py", "python" -> gs.git.vps.ui.components.PYTHON_KEYWORDS
                "js", "ts", "tsx", "jsx" -> gs.git.vps.ui.components.JS_KEYWORDS
                "rs", "rust" -> gs.git.vps.ui.components.RUST_KEYWORDS
                "go" -> gs.git.vps.ui.components.GO_KEYWORDS
                "c", "cpp", "c++", "h", "hpp" -> gs.git.vps.ui.components.CPP_KEYWORDS
                else -> emptyList()
            }
            val symbolNames = symbols.map { it.name }
            val combined = (words.map { it to "keyword" } + symbolNames.map { it to "symbol" })
                .distinctBy { it.first }
            combined.filter { it.first.startsWith(currentWord, ignoreCase = true) && it.first != currentWord }.take(8)
        }
    }

    val activeCommit = remember(fileCommits, currentLine) {
        if (fileCommits.isEmpty()) null
        else {
            val index = (currentLine - 1).coerceAtLeast(0) % fileCommits.size
            fileCommits[index]
        }
    }

    val undoStack = remember(file.path) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(file.path) { mutableStateListOf<TextFieldValue>() }

    fun snapshot() {
        if (undoStack.lastOrNull()?.text != textState.text || undoStack.lastOrNull()?.selection != textState.selection) {
            undoStack.add(textState.copy())
            if (undoStack.size > 120) undoStack.removeAt(0)
        }
    }

    fun handleEditorBack() {
        when {
            showCommitDialog -> showCommitDialog = false
            showDiscardDialog -> showDiscardDialog = false
            showGoToLine -> showGoToLine = false
            showOutline -> showOutline = false
            showSearch -> showSearch = false
            hasChanges && !isImage -> showDiscardDialog = true
            else -> onBack()
        }
    }

    fun applyState(newState: TextFieldValue) {
        if (newState != textState) {
            snapshot()
            redoStack.clear()
            textState = newState
            if (prefs.getBoolean("cosmetic_keyboard_sound", false)) {
                try {
                    (context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager)?.let { am ->
                        am.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, -1f)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun insertSuggestion(suggestion: String) {
        val cursor = textState.selection.start
        val src = textState.text
        var start = cursor - 1
        while (start >= 0 && (src[start].isLetterOrDigit() || src[start] == '_' || src[start] == '$')) {
            start--
        }
        start++
        val newText = src.replaceRange(start, cursor, suggestion)
        applyState(TextFieldValue(newText, TextRange(start + suggestion.length)))
    }

    fun applyEditorInput(newState: TextFieldValue) {
        val old = textState
        val insertedNewLine = newState.text.length == old.text.length + 1 &&
            newState.selection.start == newState.selection.end &&
            newState.selection.start > 0 &&
            newState.text.getOrNull(newState.selection.start - 1) == '\n'

        if (!insertedNewLine) {
            applyState(newState)
            return
        }

        val cursorAfterBreak = newState.selection.start
        val previousLineEnd = (cursorAfterBreak - 2).coerceAtLeast(0)
        val previousLineStart = old.text.lastIndexOf('\n', previousLineEnd).let { if (it < 0) 0 else it + 1 }
        val previousLine = old.text.substring(previousLineStart, (previousLineEnd + 1).coerceAtMost(old.text.length))
        val indent = previousLine.takeWhile { it == ' ' || it == '\t' }
        val extra = if (previousLine.trimEnd().endsWith("{") || previousLine.trimEnd().endsWith("[") || previousLine.trimEnd().endsWith("(")) "    " else ""
        val insert = indent + extra
        if (insert.isEmpty()) {
            applyState(newState)
            return
        }
        val adjusted = newState.text.substring(0, cursorAfterBreak) + insert + newState.text.substring(cursorAfterBreak)
        applyState(TextFieldValue(adjusted, TextRange(cursorAfterBreak + insert.length)))
    }

    fun insertText(value: String) {
        val start = minOf(textState.selection.start, textState.selection.end).coerceAtLeast(0)
        val end = maxOf(textState.selection.start, textState.selection.end).coerceAtLeast(0)
        val newText = textState.text.replaceRange(start, end, value)
        val cursor = (start + value.length).coerceAtMost(newText.length)
        applyState(TextFieldValue(newText, TextRange(cursor)))
    }

    fun insertPair(open: String, close: String) {
        val start = minOf(textState.selection.start, textState.selection.end).coerceAtLeast(0)
        val end = maxOf(textState.selection.start, textState.selection.end).coerceAtLeast(0)
        val selected = textState.text.substring(start, end.coerceAtMost(textState.text.length))
        val newText = textState.text.replaceRange(start, end, open + selected + close)
        val selection = if (selected.isEmpty()) TextRange((start + open.length).coerceAtMost(newText.length))
        else TextRange(start + open.length, start + open.length + selected.length)
        applyState(TextFieldValue(newText, selection))
    }

    fun duplicateLine() {
        val lineIndex = (currentLine - 1).coerceIn(0, lines.lastIndex.coerceAtLeast(0))
        val target = lines.getOrElse(lineIndex) { "" }
        val before = lines.take(lineIndex + 1).joinToString("\n")
        val after = lines.drop(lineIndex + 1).joinToString("\n")
        val newText = buildString {
            append(before)
            append("\n")
            append(target)
            if (after.isNotBlank()) append("\n$after")
        }
        val newOffset = lines.take(lineIndex + 1).sumOf { it.length + 1 }.coerceAtMost(newText.length)
        applyState(TextFieldValue(newText, TextRange(newOffset)))
    }

    fun toggleComment() {
        val prefix = commentPrefix ?: return
        val selection = textState.selection
        val startOffset = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val endOffset = maxOf(selection.start, selection.end).coerceIn(0, text.length)
        val startLine = text.take(startOffset).count { it == '\n' }
        val endLine = text.take(endOffset).count { it == '\n' }
        val mutableLines = lines.toMutableList()
        val range = startLine..endLine.coerceAtMost(mutableLines.lastIndex.coerceAtLeast(0))
        val uncomment = range.all { idx -> mutableLines[idx].trimStart().startsWith(prefix) }
        range.forEach { idx ->
            val original = mutableLines[idx]
            val indent = original.takeWhile { it == ' ' || it == '\t' }
            val trimmed = original.removePrefix(indent)
            mutableLines[idx] = if (uncomment) {
                if (trimmed.startsWith(prefix)) indent + trimmed.removePrefix(prefix).removePrefix(" ") else original
            } else {
                indent + prefix + if (trimmed.isNotBlank()) " $trimmed" else ""
            }
        }
        val newText = mutableLines.joinToString("\n")
        applyState(TextFieldValue(newText, selection.coerceIn(newText.length)))
    }

    fun goToLine(lineNumber: Int) {
        val safe = lineNumber.coerceIn(1, lines.size.coerceAtLeast(1))
        val offset = lines.take(safe - 1).sumOf { it.length + 1 }.coerceAtMost(text.length)
        textState = textState.copy(selection = TextRange(offset))
    }

    fun goToOffset(offset: Int) {
        textState = textState.copy(selection = TextRange(offset.coerceIn(0, text.length)))
    }

    fun selectedLineRange(): IntRange {
        val start = minOf(textState.selection.start, textState.selection.end).coerceIn(0, text.length)
        val end = maxOf(textState.selection.start, textState.selection.end).coerceIn(0, text.length)
        val startLine = text.take(start).count { it == '\n' }
        val endLine = text.take(end).count { it == '\n' }.coerceAtMost(lines.lastIndex.coerceAtLeast(0))
        return startLine..endLine
    }

    fun transformSelectedLines(transform: (String) -> String) {
        val range = selectedLineRange()
        val mutableLines = lines.toMutableList()
        range.forEach { idx ->
            if (idx in mutableLines.indices) mutableLines[idx] = transform(mutableLines[idx])
        }
        val newText = mutableLines.joinToString("\n")
        applyState(TextFieldValue(newText, textState.selection.coerceIn(newText.length)))
    }

    fun selectAll() {
        textState = textState.copy(selection = TextRange(0, text.length))
    }

    fun formatJsonDocument() {
        if (ext != "json") return
        val formatted = try {
            val trimmed = text.trim()
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
        if (formatted == null) {
            Toast.makeText(context, "Invalid JSON", Toast.LENGTH_SHORT).show()
            return
        }
        applyState(TextFieldValue(formatted, TextRange(0)))
    }

    fun formatCodeDocument() {
        if (ext == "json") {
            formatJsonDocument()
            return
        }
        val currentText = textState.text
        val linesList = currentText.split("\n")
        var indentLevel = 0
        val tabSize = prefs.getInt("editor_tab_size", 4)
        val tabString = if (prefs.getBoolean("editor_use_tabs", false)) "\t" else " ".repeat(tabSize)
        val formatted = linesList.joinToString("\n") { line ->
            var trimmed = line.trim()
            if (trimmed.startsWith("}") || trimmed.startsWith("</") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }
            val padded = tabString.repeat(indentLevel) + trimmed
            if ((trimmed.endsWith("{") || trimmed.endsWith("<") || trimmed.startsWith("<") && !trimmed.startsWith("</") && trimmed.endsWith(">") && !trimmed.endsWith("/>") || trimmed.endsWith("(")) && !trimmed.contains("</")) {
                indentLevel++
            }
            padded
        }
        applyState(TextFieldValue(formatted, TextRange(0)))
    }

    fun replaceCurrent() {
        val query = searchState.text
        if (query.isEmpty() || matches.isEmpty()) return
        val target = matches.getOrNull(currentMatchIndex) ?: return
        val replacement = if (isRegex) {
            try {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                val regex = Regex(query, options)
                val matchResult = regex.find(text.substring(target.start, target.end))
                if (matchResult != null) {
                    matchResult.value.replace(regex, replaceState.text)
                } else replaceState.text
            } catch (_: Exception) {
                replaceState.text
            }
        } else {
            replaceState.text
        }
        val newText = text.replaceRange(target.start, target.end, replacement)
        val start = target.start
        applyState(TextFieldValue(newText, TextRange((start + replacement.length).coerceAtMost(newText.length))))
    }

    fun replaceAll() {
        val query = searchState.text
        if (query.isEmpty()) return
        val newText = try {
            if (isRegex) {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                text.replace(Regex(query, options), replaceState.text)
            } else {
                text.replace(query, replaceState.text, ignoreCase = !matchCase)
            }
        } catch (_: Exception) {
            text
        }
        applyState(TextFieldValue(newText, textState.selection.coerceIn(newText.length)))
    }

    LaunchedEffect(matches) {
        if (matches.isEmpty()) currentMatchIndex = 0
        else if (currentMatchIndex !in matches.indices) currentMatchIndex = 0
    }

    if (showGoToLine) {
        AiModuleAlertDialog(
            onDismissRequest = { showGoToLine = false },
            title = "go to line",
            content = {
                AiModuleTextField(
                    value = goToLineText,
                    onValueChange = { goToLineText = it.filter(Char::isDigit) },
                    label = "Line",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                AiModuleTextAction(label = "go", onClick = {
                    goToLine(goToLineText.toIntOrNull() ?: 1)
                    showGoToLine = false
                })
            },
            dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showGoToLine = false }, tint = AiModuleTheme.colors.textSecondary) }
        )
    }

    if (showOutlineDialog) {
        AiModuleAlertDialog(
            onDismissRequest = { showOutlineDialog = false },
            title = "outline",
            content = {
                if (symbols.isEmpty()) {
                    Text("No symbols found", color = TextSecondary, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(symbols) { _, symbol ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(Color(0xFF111B2E))
                                    .clickable {
                                        goToLine(symbol.line + 1)
                                        showOutlineDialog = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaPill(symbol.kind, Blue)
                                Text(symbol.name, color = Color(0xFFE5E7EB), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("${symbol.line + 1}", color = TextTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            },
            confirmButton = { AiModuleTextAction(label = "close", onClick = { showOutlineDialog = false }) }
        )
    }

    if (showDiscardDialog) {
        AiModuleAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = "discard changes?",
            content = { Text("This file has uncommitted edits.", color = TextSecondary, fontSize = 13.sp, fontFamily = JetBrainsMono) },
            confirmButton = {
                AiModuleTextAction(label = "discard", tint = AiModuleTheme.colors.error, onClick = {
                    showDiscardDialog = false
                    onBack()
                })
            },
            dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showDiscardDialog = false }, tint = AiModuleTheme.colors.textSecondary) }
        )
    }

    if (showStashDialog) {
        var msgInput by remember { mutableStateOf("On $branch: wip stashed changes") }
        AiModuleAlertDialog(
            onDismissRequest = { showStashDialog = false },
            title = "stash changes",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a message for this stash entry:", color = TextSecondary, fontSize = 13.sp, fontFamily = JetBrainsMono)
                    CompactField("stash message", msgInput) { msgInput = it }
                }
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "stash",
                    onClick = {
                        showStashDialog = false
                        if (tabs.indices.contains(activeTabIndex)) {
                            tabs[activeTabIndex] = tabs[activeTabIndex].copy(
                                textState = textState,
                                savedContent = savedContent,
                                savedSha = savedSha,
                                mode = mode,
                                undoStack = undoStack.toList(),
                                redoStack = redoStack.toList()
                            )
                        }
                        
                        val modified = tabs.filter { it.textState.text != it.savedContent }
                        if (modified.isEmpty()) {
                            Toast.makeText(context, "No changes to stash", Toast.LENGTH_SHORT).show()
                            return@AiModuleTextAction
                        }
                        
                        val stashFiles = modified.map { gs.git.vps.data.github.StashFile(it.file.path, it.textState.text) }
                        gs.git.vps.data.github.LocalTimeTravelManager.addStash(context, "$repoOwner/$repoName", msgInput, stashFiles)
                        
                        modified.forEach { mTab ->
                            val idx = tabs.indexOfFirst { it.file.path == mTab.file.path }
                            if (idx != -1) {
                                tabs[idx] = tabs[idx].copy(savedContent = tabs[idx].textState.text)
                            }
                        }
                        
                        val activeTab = tabs.getOrNull(activeTabIndex)
                        if (activeTab != null && modified.any { it.file.path == activeTab.file.path }) {
                            savedContent = activeTab.textState.text
                        }
                        
                        Toast.makeText(context, "Stashed ${stashFiles.size} file(s)", Toast.LENGTH_SHORT).show()
                    },
                    tint = AiModuleTheme.colors.accent
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = "cancel",
                    onClick = { showStashDialog = false },
                    tint = AiModuleTheme.colors.textSecondary
                )
            }
        )
    }

    if (showFilePicker) {
        FilePickerDialog(
            repoOwner = repoOwner,
            repoName = repoName,
            branch = currentBranch,
            onDismiss = { showFilePicker = false },
            onFileSelected = { selectedFile ->
                scope.launch {
                    loadingFile = true
                    showFilePicker = false
                    try {
                        val newContent = GitHubManager.getFileContent(context, repoOwner, repoName, selectedFile.path, currentBranch)
                        val existingIndex = tabs.indexOfFirst { it.file.path == selectedFile.path }
                        if (existingIndex >= 0) {
                            if (tabs.indices.contains(activeTabIndex)) {
                                tabs[activeTabIndex] = tabs[activeTabIndex].copy(
                                    textState = textState,
                                    savedContent = savedContent,
                                    savedSha = savedSha,
                                    mode = mode,
                                    undoStack = undoStack.toList(),
                                    redoStack = redoStack.toList()
                                )
                            }
                            activeTabIndex = existingIndex
                            val target = tabs[existingIndex]
                            textState = target.textState
                            savedContent = target.savedContent
                            savedSha = target.savedSha
                            mode = target.mode
                            undoStack.clear()
                            undoStack.addAll(target.undoStack)
                            redoStack.clear()
                            redoStack.addAll(target.redoStack)
                        } else {
                            if (tabs.indices.contains(activeTabIndex)) {
                                tabs[activeTabIndex] = tabs[activeTabIndex].copy(
                                    textState = textState,
                                    savedContent = savedContent,
                                    savedSha = savedSha,
                                    mode = mode,
                                    undoStack = undoStack.toList(),
                                    redoStack = redoStack.toList()
                                )
                            }
                            tabs.add(
                                EditorTab(
                                    file = selectedFile,
                                    textState = TextFieldValue(newContent),
                                    savedContent = newContent,
                                    savedSha = selectedFile.sha,
                                    mode = GitHubEditorMode.EDIT,
                                    undoStack = emptyList(),
                                    redoStack = emptyList()
                                )
                            )
                            activeTabIndex = tabs.lastIndex
                            textState = TextFieldValue(newContent)
                            savedContent = newContent
                            savedSha = selectedFile.sha
                            mode = GitHubEditorMode.EDIT
                            undoStack.clear()
                            redoStack.clear()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        loadingFile = false
                    }
                }
            }
        )
    }

    if (showConflictResolver) {
        ConflictResolverDialog(
            content = textState.text,
            onDismiss = { showConflictResolver = false },
            onResolved = { resolvedContent ->
                textState = textState.copy(text = resolvedContent)
                showConflictResolver = false
            }
        )
    }

    AiModuleSurface {
    val palette = AiModuleTheme.colors
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(palette.background)) {
        if (!zenMode) {
            GitHubEditorTopBar(
                fileName = currentFile.name,
                subtitle = buildEditorSubtitle(ext, lines.size, text.length, hasChanges),
                isImage = isImage,
                showMoreMenu = showMoreMenu,
                hasChanges = hasChanges,
                onToggleMoreMenu = { showMoreMenu = !showMoreMenu },
                onSave = { showCommitDialog = true },
                onBack = ::handleEditorBack,
                onAskAi = {
                    copilotInitialPrompt = it
                    showCopilotChat = true
                }
            )

            AnimatedVisibility(visible = hasConflictMarkers && !zenMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3F2F1F))
                        .border(1.dp, Color(0xFFFF9500))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚠️ Merge conflicts detected.",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9500),
                        fontFamily = JetBrainsMono
                    )
                    AiModuleTextAction(
                        label = "resolve interactively",
                        tint = Color(0xFFFF9500),
                        onClick = { showConflictResolver = true }
                    )
                }
            }
            
            // Tabs Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(palette.surface)
                    .drawBehind {
                        drawLine(
                            color = SeparatorColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isActive = index == activeTabIndex
                    val tabHasChanges = tab.textState.text != tab.savedContent
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable {
                                if (tabs.indices.contains(activeTabIndex)) {
                                    tabs[activeTabIndex] = tabs[activeTabIndex].copy(
                                        textState = textState,
                                        savedContent = savedContent,
                                        savedSha = savedSha,
                                        mode = mode,
                                        undoStack = undoStack.toList(),
                                        redoStack = redoStack.toList()
                                    )
                                }
                                activeTabIndex = index
                                val target = tabs[index]
                                textState = target.textState
                                savedContent = target.savedContent
                                savedSha = target.savedSha
                                mode = target.mode
                                undoStack.clear()
                                undoStack.addAll(target.undoStack)
                                redoStack.clear()
                                redoStack.addAll(target.redoStack)
                            }
                            .background(if (isActive) palette.background else Color.Transparent)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.file.name + (if (tabHasChanges) " *" else ""),
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMono,
                            color = if (isActive) palette.accent else palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    if (activeTabIndex == index) {
                                        if (tabs.size > 1) {
                                            val nextActive = if (index == tabs.lastIndex) index - 1 else index + 1
                                            val target = tabs[nextActive]
                                            textState = target.textState
                                            savedContent = target.savedContent
                                            savedSha = target.savedSha
                                            mode = target.mode
                                            undoStack.clear()
                                            undoStack.addAll(target.undoStack)
                                            redoStack.clear()
                                            redoStack.addAll(target.redoStack)
                                            
                                            tabs.removeAt(index)
                                            activeTabIndex = tabs.indexOf(target)
                                        } else {
                                            onBack()
                                        }
                                    } else {
                                        tabs.removeAt(index)
                                        if (activeTabIndex > index) {
                                            activeTabIndex--
                                        }
                                    }
                                }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = "×",
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(SeparatorColor))
                }
                
                // Add tab button ("+")
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .clickable { showFilePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 14.sp,
                        fontFamily = JetBrainsMono,
                        color = palette.accent
                    )
                }
            }
        }

        AnimatedVisibility(showMoreMenu && !zenMode) {
            EditorMoreMenu(
                palette = palette,
                lineNumbers = lineNumbers,
                wrapLines = wrapLines,
                zenMode = zenMode,
                showSearch = showSearch,
                showBlame = showBlame,
                mode = mode,
                isMarkdown = isMarkdown,
                fontSize = fontSize,
                onToggleLineNumbers = { lineNumbers = !lineNumbers },
                onToggleWrap = { wrapLines = !wrapLines },
                onToggleZen = { zenMode = !zenMode; showMoreMenu = false },
                onToggleSearch = { showSearch = !showSearch; showMoreMenu = false },
                onToggleBlame = { showBlame = !showBlame; showMoreMenu = false },
                onCycleMode = {
                    mode = when (mode) {
                        GitHubEditorMode.EDIT -> if (isMarkdown) GitHubEditorMode.PREVIEW else GitHubEditorMode.READ
                        GitHubEditorMode.PREVIEW -> GitHubEditorMode.READ
                        GitHubEditorMode.READ -> GitHubEditorMode.DIFF
                        GitHubEditorMode.DIFF -> GitHubEditorMode.EDIT
                    }
                },
                onFontSizeChange = { fontSize = it },
                onGoToLine = { showGoToLine = true; showMoreMenu = false },
                onOutline = { showOutline = !showOutline; showMoreMenu = false },
                onCopy = {
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
                    showMoreMenu = false
                },
                onFormat = { formatCodeDocument(); showMoreMenu = false },
                onStash = { showStashDialog = true; showMoreMenu = false },
                onDismiss = { showMoreMenu = false }
            )
        }

        if (!isImage && !zenMode) {
            // 1. Breadcrumbs Path Indication
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceElevated.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AiModuleGlyph(glyph = GhGlyphs.FOLDER, tint = palette.accent, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "repo",
                    color = palette.textSecondary.copy(alpha = 0.5f),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                AiModuleGlyph(glyph = GhGlyphs.ARROW_RIGHT, tint = palette.textMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$repoOwner > $repoName",
                    color = palette.textSecondary.copy(alpha = 0.7f),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                AiModuleGlyph(glyph = GhGlyphs.ARROW_RIGHT, tint = palette.textMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$currentBranch > ${file.path.replace("/", " > ")}",
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            // 2. Outline horizontal scrollable jump list
            if (symbols.isNotEmpty() && showOutline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "OUTLINE:",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    symbols.forEach { sym ->
                        val (markerText, markerColor) = when (sym.kind.lowercase()) {
                            "class", "object", "interface", "enum", "struct" -> "C" to Color(0xFFFFB300)
                            "fun", "function", "def", "fn" -> "F" to Color(0xFFC792EA)
                            "tag" -> "T" to Color(0xFF4FC3F7)
                            "css", "selector" -> "#" to Color(0xFFAED581)
                            else -> if (sym.kind.startsWith("H")) "H" to Color(0xFFFF8A65) else "•" to Color(0xFF90A4AE)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(palette.surfaceElevated)
                                .clickable {
                                    scope.launch {
                                        val scrollY = with(density) {
                                            (sym.line * (fontSize + 7).sp.toPx()).toInt()
                                        }
                                        verticalScrollState.animateScrollTo(scrollY)
                                    }
                                }
                                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(markerColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = markerText,
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sym.name,
                                color = palette.textPrimary,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
            }
        }

        if (!isImage && !zenMode) {
            EditorInfoStrip(
                mode = mode,
                currentLine = currentLine,
                currentColumn = currentColumn,
                selectionLength = textState.selection.length,
                matchCount = matches.size,
                currentMatchNumber = if (matches.isEmpty()) 0 else currentMatchIndex + 1,
                onSetMode = { mode = it },
                isMarkdown = isMarkdown,
                hasChanges = hasChanges
            )
            run {
                val selectedRange = textState.selection
                val selectedText = run {
                    val src = textState.text
                    val start = minOf(selectedRange.start, selectedRange.end)
                        .coerceIn(0, src.length)
                    val end = maxOf(selectedRange.start, selectedRange.end)
                        .coerceIn(0, src.length)
                    if (end > start) src.substring(start, end) else ""
                }
                AiQuickActionsRow(
                    filePath = file.path,
                    branch = branch,
                    selectedText = selectedText,
                    onSendPrompt = { prompt ->
                        copilotInitialPrompt = prompt
                        showCopilotChat = true
                    },
                )
            }
        }

        AnimatedVisibility(showSearch && !isImage && !zenMode) {
            SearchReplaceCard(
                searchState = searchState,
                replaceState = replaceState,
                matchCount = matches.size,
                currentMatch = if (matches.isEmpty()) 0 else currentMatchIndex + 1,
                isRegex = isRegex,
                matchCase = matchCase,
                onToggleRegex = { isRegex = !isRegex },
                onToggleMatchCase = { matchCase = !matchCase },
                onSearchChange = { searchState = it },
                onReplaceChange = { replaceState = it },
                onPrev = {
                    if (matches.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex - 1).floorMod(matches.size)
                        goToOffset(matches[currentMatchIndex].start)
                    }
                },
                onNext = {
                    if (matches.isNotEmpty()) {
                        currentMatchIndex = (currentMatchIndex + 1).floorMod(matches.size)
                        goToOffset(matches[currentMatchIndex].start)
                    }
                },
                onReplaceOne = { replaceCurrent() },
                onReplaceAll = { replaceAll() }
            )
        }

        AnimatedVisibility(autocompleteSuggestions.isNotEmpty() && !isImage && mode == GitHubEditorMode.EDIT && !zenMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(palette.surfaceElevated)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SUGGEST:",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                autocompleteSuggestions.forEach { suggestion ->
                    val isKeyword = suggestion.second == "keyword"
                    val itemColor = if (isKeyword) Color(0xFFBC8CFF) else palette.accent
                    val itemBg = itemColor.copy(alpha = 0.10f)
                    val prefix = if (isKeyword) "K" else "S"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .background(itemBg)
                            .border(0.5.dp, itemColor.copy(alpha = 0.8f), RoundedCornerShape(GitHubControlRadius))
                            .clickable { insertSuggestion(suggestion.first) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[$prefix]",
                                color = itemColor.copy(alpha = 0.6f),
                                fontFamily = JetBrainsMono,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = suggestion.first,
                                color = itemColor,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (!isImage && mode == GitHubEditorMode.EDIT && !zenMode) {
            UpgradedEditorAccessoryBar(
                palette = palette,
                onInsert = { insertText(it) },
                onInsertPair = { open, close -> insertPair(open, close) },
                onUndo = {
                    if (undoStack.isNotEmpty()) {
                        redoStack.add(textState.copy())
                        textState = undoStack.removeLast()
                    }
                },
                onRedo = {
                    if (redoStack.isNotEmpty()) {
                        undoStack.add(textState.copy())
                        textState = redoStack.removeLast()
                    }
                },
                onFormat = { formatCodeDocument() },
                onComment = { toggleComment() },
                onDuplicate = { duplicateLine() },
                onLeftArrow = {
                    val currentSel = textState.selection
                    if (currentSel.start > 0) {
                        textState = textState.copy(selection = TextRange(currentSel.start - 1))
                    }
                },
                onRightArrow = {
                    val currentSel = textState.selection
                    if (currentSel.start < textState.text.length) {
                        textState = textState.copy(selection = TextRange(currentSel.start + 1))
                    }
                },
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                ext = ext
            )
        }

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .padding(if (zenMode) PaddingValues(0.dp) else PaddingValues(horizontal = 10.dp, vertical = 8.dp))
                    .let { if (zenMode) it else it.clip(RoundedCornerShape(22.dp)).border(1.dp, palette.border, RoundedCornerShape(22.dp)) }
                    .background(palette.background)
            ) {
                val contentModifier = if (zenMode) Modifier.statusBarsPadding().fillMaxSize() else Modifier.fillMaxSize()
                Box(contentModifier) {
                    when {
                        isImage -> ModernImageCanvas(file)
                        isMarkdown && mode == GitHubEditorMode.PREVIEW -> {
                            val mdRepo = remember(repoOwner, repoName, branch) {
                                GHRepo(name = repoName, fullName = "$repoOwner/$repoName", description = "",
                                    language = "", stars = 0, forks = 0, isPrivate = false, isFork = false,
                                    defaultBranch = branch, updatedAt = "", owner = repoOwner)
                            }
                            GitHubMarkdownDocument(
                                markdown = lines.joinToString("\n"),
                                repo = mdRepo,
                                readmePath = file.path,
                                modifier = Modifier.fillMaxSize(),
                                onLinkClick = { url ->
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
                                    catch (_: Exception) {}
                                }
                            )
                        }
                        mode == GitHubEditorMode.READ -> ModernReadCanvas(lines, ext, lineNumbers, wrapLines, currentMatch?.line, fontSize, gutterStates)
                        mode == GitHubEditorMode.DIFF -> ModernDiffCanvas(savedContent, text, fontSize)
                        else -> ModernEditCanvas(
                            textState = textState,
                            lines = lines,
                            lineNumbers = lineNumbers,
                            wrapLines = wrapLines,
                            ext = ext,
                            fontSize = fontSize,
                            searchQuery = searchState.text,
                            currentHighlightedLine = currentMatch?.line,
                            currentMatchRange = currentMatch?.let { it.start until it.end },
                            verticalScrollState = verticalScrollState,
                            highlightedText = highlightedText,
                            gutterStates = gutterStates,
                            onValueChange = { applyEditorInput(it) }
                        )
                    }
                }

                if (zenMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .background(palette.surface.copy(alpha = 0.7f))
                            .border(0.5.dp, palette.border.copy(alpha = 0.7f), RoundedCornerShape(GitHubControlRadius))
                            .clickable { zenMode = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("exit zen", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showOutline && !zenMode && !isImage && symbols.isNotEmpty(),
                enter = slideInHorizontally(initialOffsetX = { w -> w }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { w -> w }) + fadeOut()
            ) {
                Box(
                    Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(palette.surface)
                        .drawBehind {
                            drawLine(
                                color = palette.border,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                ":: file outline",
                                color = palette.accent,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .clickable { showOutline = false }
                                    .padding(4.dp)
                            ) {
                                Text("×", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        
                        var symbolQuery by remember { mutableStateOf("") }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(palette.background)
                                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (symbolQuery.isEmpty()) {
                                Text("filter symbols...", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
                            }
                            BasicTextField(
                                value = symbolQuery,
                                onValueChange = { symbolQuery = it },
                                textStyle = TextStyle(color = palette.textPrimary, fontSize = 11.sp, fontFamily = JetBrainsMono),
                                singleLine = true,
                                cursorBrush = SolidColor(palette.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        val filteredSymbols = remember(symbols, symbolQuery) {
                            if (symbolQuery.isBlank()) symbols else symbols.filter { it.name.contains(symbolQuery, true) }
                        }
                        
                        if (filteredSymbols.isEmpty()) {
                            Text("No symbols found", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 16.dp))
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(filteredSymbols) { _, symbol ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(palette.surfaceElevated.copy(alpha = 0.5f))
                                            .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                                            .clickable {
                                                goToLine(symbol.line + 1)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = when (symbol.kind.lowercase()) {
                                                "class", "object", "interface", "enum", "struct" -> "C"
                                                "fun", "function", "def", "fn" -> "F"
                                                "tag" -> "T"
                                                "css", "selector" -> "#"
                                                else -> "•"
                                            },
                                            color = palette.accent,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = symbol.name,
                                            color = palette.textPrimary,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${symbol.line + 1}",
                                            color = palette.textMuted,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(showBlame && activeCommit != null && !zenMode) {
            activeCommit?.let { commit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceElevated)
                        .border(0.5.dp, palette.border, RoundedCornerShape(0.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = GhGlyphs.COMMIT,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${commit.author} · ${formatRelativeTime(commit.date)}",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "· ${commit.message}",
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = commit.sha.take(7),
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!zenMode) {
            EditorStatusBar(
                palette = palette,
                branch = currentBranch,
                ext = ext,
                linesCount = lines.size,
                charsCount = text.length,
                currentLine = currentLine,
                currentColumn = currentColumn,
                mode = mode,
                onBranchClick = { showBranchSwitcher = true }
            )
        }
    }

    AnimatedVisibility(
            visible = showCopilotChat,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            CopilotChatPanel(
                palette = palette,
                filePath = currentFile.path,
                branch = branch,
                selectedText = textState.selection.let { range ->
                    val src = textState.text
                    val start = minOf(range.start, range.end).coerceIn(0, src.length)
                    val end = maxOf(range.start, range.end).coerceIn(0, src.length)
                    if (end > start) src.substring(start, end) else ""
                },
                initialPrompt = copilotInitialPrompt,
                onClose = { showCopilotChat = false },
                onApplyCode = { codeSnippet ->
                    val range = textState.selection
                    val src = textState.text
                    val start = minOf(range.start, range.end).coerceIn(0, src.length)
                    val end = maxOf(range.start, range.end).coerceIn(0, src.length)
                    val newText = src.substring(0, start) + codeSnippet + src.substring(end)
                    textState = TextFieldValue(
                        text = newText,
                        selection = TextRange(start + codeSnippet.length)
                    )
                    showCopilotChat = false
                }
            )
        }
    }

    if (showBranchSwitcher) {
        AiModuleAlertDialog(
            onDismissRequest = { showBranchSwitcher = false },
            title = "switch branch",
            content = {
                if (loadingBranches) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner()
                    }
                } else if (branches.isEmpty()) {
                    Text("No branches found", color = palette.textSecondary, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(branches) { _, b ->
                            val active = b == currentBranch
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (active) palette.accent.copy(alpha = 0.12f) else palette.surfaceElevated)
                                    .clickable {
                                        if (!active) {
                                            scope.launch {
                                                loadingFile = true
                                                showBranchSwitcher = false
                                                try {
                                                    val parentPath = file.path.substringBeforeLast("/", "")
                                                    val contentsList = GitHubManager.getRepoContents(context, repoOwner, repoName, parentPath, b)
                                                    val match = contentsList.firstOrNull { it.path == file.path }
                                                    if (match != null) {
                                                        val newContent = GitHubManager.getFileContent(context, repoOwner, repoName, file.path, b)
                                                        savedContent = newContent
                                                        savedSha = match.sha
                                                        textState = TextFieldValue(newContent)
                                                        currentBranch = b
                                                        undoStack.clear()
                                                        redoStack.clear()
                                                        Toast.makeText(context, "Switched to branch $b", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "File does not exist in branch $b", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to load branch content: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    loadingFile = false
                                                }
                                            }
                                        } else {
                                            showBranchSwitcher = false
                                        }
                                    }
                                    .border(0.5.dp, if (active) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AiModuleGlyph(glyph = GhGlyphs.BRANCH, tint = if (active) palette.accent else palette.textSecondary, fontSize = 13.sp)
                                    Text(b, color = if (active) palette.textPrimary else palette.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (active) {
                                    Text("[ Active ]", color = palette.accent, fontSize = 11.sp, fontFamily = JetBrainsMono)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { AiModuleTextAction(label = "close", onClick = { showBranchSwitcher = false }) }
        )
    }

    if (loadingFile) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AiModuleSpinner()
                Text("Loading branch file...", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 12.sp)
            }
        }
    }
    }

    if (showCommitDialog) {
        var aiSuggesting by remember { mutableStateOf(false) }
        var aiSuggestError by remember { mutableStateOf<String?>(null) }
        AiModuleAlertDialog(
            onDismissRequest = { showCommitDialog = false },
            title = "commit changes",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(file.path, fontSize = 12.sp, color = TextSecondary, fontFamily = JetBrainsMono)
                    AiModuleTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = "Commit message",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AiModuleTextAction(
                            label = if (aiSuggesting) Strings.aiCommitMsgGenerating else Strings.aiCommitMsgGenerate,
                            enabled = !aiSuggesting,
                            onClick = {
                                aiSuggesting = true
                                aiSuggestError = null
                                scope.launch {
                                    try {
                                        commitMessage = generateCommitMessage(
                                            context = context,
                                            path = file.path,
                                            oldText = savedContent,
                                            newText = text,
                                        )
                                    } catch (e: Exception) {
                                        aiSuggestError = e.message ?: e.javaClass.simpleName
                                    } finally {
                                        aiSuggesting = false
                                    }
                                }
                            },
                        )
                        aiSuggestError?.let { err ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                err.take(60),
                                fontSize = 11.sp,
                                color = AiModuleTheme.colors.error,
                                fontFamily = JetBrainsMono,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                AiModuleTextAction(label = if (saving) "committing" else "commit", enabled = !saving, onClick = {
                    if (commitMessage.isBlank()) {
                        Toast.makeText(context, "Commit message required", Toast.LENGTH_SHORT).show()
                        return@AiModuleTextAction
                    }
                    saving = true
                    scope.launch {
                        val result = GitHubManager.uploadFileWithResult(
                            context = context,
                            owner = repoOwner,
                            repo = repoName,
                            path = file.path,
                            content = text.toByteArray(),
                            message = commitMessage,
                            branch = branch,
                            sha = savedSha
                        )
                        saving = false
                        showCommitDialog = false
                        if (result.success) {
                            savedContent = text
                            if (result.sha.isNotBlank()) savedSha = result.sha
                            undoStack.clear()
                            redoStack.clear()
                            Toast.makeText(context, "Committed successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to commit", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            },
            dismissButton = { AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showCommitDialog = false }, tint = AiModuleTheme.colors.textSecondary) }
        )
    }
}


@Composable
private fun GitHubEditorTopBar(
    fileName: String,
    subtitle: String,
    isImage: Boolean,
    showMoreMenu: Boolean,
    hasChanges: Boolean,
    onToggleMoreMenu: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onAskAi: ((prompt: String?) -> Unit)? = null
) {
    val palette = AiModuleTheme.colors
    GitHubPageBar(
        title = "> ${fileName}",
        subtitle = subtitle,
        onBack = onBack,
        trailing = {
            if (onAskAi != null) {
                AiModuleGlyphAction(
                    glyph = GhGlyphs.AI,
                    onClick = { onAskAi(null) },
                    tint = palette.accent,
                    contentDescription = "ask ai",
                )
            }
            if (!isImage) {
                if (hasChanges) {
                    AiModuleGlyphAction(
                        glyph = GhGlyphs.SAVE,
                        onClick = onSave,
                        tint = palette.accent,
                        contentDescription = "save",
                    )
                }
                AiModuleGlyphAction(
                    glyph = GhGlyphs.SETTINGS,
                    onClick = onToggleMoreMenu,
                    tint = if (showMoreMenu) palette.accent else palette.textSecondary,
                    contentDescription = "more options",
                )
            }
        },
    )
}

@Composable
private fun EditorMoreMenu(
    palette: gs.git.vps.ui.theme.AiModuleColors,
    lineNumbers: Boolean,
    wrapLines: Boolean,
    zenMode: Boolean,
    showSearch: Boolean,
    showBlame: Boolean,
    mode: GitHubEditorMode,
    isMarkdown: Boolean,
    fontSize: Int,
    onToggleLineNumbers: () -> Unit,
    onToggleWrap: () -> Unit,
    onToggleZen: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleBlame: () -> Unit,
    onCycleMode: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onGoToLine: () -> Unit,
    onOutline: () -> Unit,
    onCopy: () -> Unit,
    onFormat: () -> Unit,
    onStash: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDITOR OPTIONS",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "×",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            // Options Row 1 (toggles: line numbers, wrap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (lineNumbers) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .border(0.5.dp, if (lineNumbers) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onToggleLineNumbers() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Line Numbers: ${if (lineNumbers) "ON" else "OFF"}",
                        color = if (lineNumbers) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (wrapLines) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .border(0.5.dp, if (wrapLines) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onToggleWrap() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Word Wrap: ${if (wrapLines) "ON" else "OFF"}",
                        color = if (wrapLines) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options Row 2 (toggles: zen, search)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (zenMode) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .border(0.5.dp, if (zenMode) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onToggleZen() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Zen Mode: ${if (zenMode) "ON" else "OFF"}",
                        color = if (zenMode) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (showSearch) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .border(0.5.dp, if (showSearch) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onToggleSearch() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search Panel: ${if (showSearch) "ON" else "OFF"}",
                        color = if (showSearch) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options Row 2.5 (toggle: blame)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(if (showBlame) palette.accent.copy(alpha = 0.12f) else palette.surface)
                        .border(0.5.dp, if (showBlame) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onToggleBlame() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Git Blame: ${if (showBlame) "ON" else "OFF"}",
                        color = if (showBlame) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Options Row 3 (mode cycle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onCycleMode() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val modeStr = when (mode) {
                    GitHubEditorMode.EDIT -> "EDIT"
                    GitHubEditorMode.READ -> "READ"
                    GitHubEditorMode.PREVIEW -> "PREVIEW"
                    GitHubEditorMode.DIFF -> "DIFF"
                }
                Text(
                    text = "Mode: $modeStr (tap to cycle)",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Options Row 3.5: Font size controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Size: ${fontSize} sp",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .background(palette.surfaceElevated)
                            .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                            .clickable { onFontSizeChange((fontSize - 1).coerceAtLeast(9)) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .background(palette.surfaceElevated)
                            .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                            .clickable { onFontSizeChange((fontSize + 1).coerceAtMost(24)) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            // Options Row 4 (action buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onGoToLine() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ Line ]",
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onOutline() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ Outline ]",
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onCopy() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ Copy ]",
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { onFormat() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ Format ]",
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onStash() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[ stash unsaved changes ]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EditorInfoStrip(
    mode: GitHubEditorMode,
    currentLine: Int,
    currentColumn: Int,
    selectionLength: Int,
    matchCount: Int,
    currentMatchNumber: Int,
    onSetMode: (GitHubEditorMode) -> Unit,
    isMarkdown: Boolean,
    hasChanges: Boolean
) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModePill("Edit", mode == GitHubEditorMode.EDIT) { onSetMode(GitHubEditorMode.EDIT) }
        ModePill("Read", mode == GitHubEditorMode.READ) { onSetMode(GitHubEditorMode.READ) }
        if (isMarkdown) ModePill("Preview", mode == GitHubEditorMode.PREVIEW) { onSetMode(GitHubEditorMode.PREVIEW) }
        ModePill("Diff", mode == GitHubEditorMode.DIFF) { onSetMode(GitHubEditorMode.DIFF) }
        MetaPill("Ln $currentLine", palette.accent)
        MetaPill("Col $currentColumn", palette.textSecondary)
        if (selectionLength > 0) MetaPill("$selectionLength sel", palette.accent)
        if (matchCount > 0) MetaPill("$currentMatchNumber/$matchCount", palette.accent)
        if (hasChanges) MetaPill("Modified", palette.error)
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) palette.accent.copy(alpha = 0.16f) else palette.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (selected) palette.accent else palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetaPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SearchReplaceCard(
    searchState: TextFieldValue,
    replaceState: TextFieldValue,
    matchCount: Int,
    currentMatch: Int,
    isRegex: Boolean,
    matchCase: Boolean,
    onToggleRegex: () -> Unit,
    onToggleMatchCase: () -> Unit,
    onSearchChange: (TextFieldValue) -> Unit,
    onReplaceChange: (TextFieldValue) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(0.5.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchBox(searchState, onSearchChange, "Find", Modifier.weight(1f))
            Text(if (matchCount == 0) "0" else "$currentMatch/$matchCount", color = palette.textSecondary, fontSize = 12.sp)
            IconButton(onClick = onPrev, enabled = matchCount > 0) { Icon(Icons.Rounded.ArrowUpward, null, tint = if (matchCount > 0) palette.accent else palette.textMuted) }
            IconButton(onClick = onNext, enabled = matchCount > 0) { Icon(Icons.Rounded.ArrowDownward, null, tint = if (matchCount > 0) palette.accent else palette.textMuted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchBox(replaceState, onReplaceChange, "Replace", Modifier.weight(1f))
            SmallPillButton("1", onReplaceOne)
            SmallPillButton("All", onReplaceAll)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchOptionChip("Regex", isRegex, onToggleRegex)
            SearchOptionChip("Match Case", matchCase, onToggleMatchCase)
        }
    }
}

@Composable
private fun SearchOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) palette.accent.copy(alpha = 0.24f) else palette.surfaceElevated)
            .border(0.5.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (selected) palette.accent else palette.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SearchBox(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val palette = AiModuleTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surfaceElevated)
            .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.text.isEmpty()) Text(hint, color = palette.textMuted, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SmallPillButton(label: String, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditorActionRibbon(
    onInsert: (String) -> Unit,
    onInsertPair: (String, String) -> Unit,
    onDuplicate: () -> Unit,
    onComment: (() -> Unit)?,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onTrim: () -> Unit,
    onSelectAll: () -> Unit,
    onFormatJson: (() -> Unit)?,
    onFontSmaller: () -> Unit,
    onFontLarger: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (onComment != null) EditorActionChip("Comment") { onComment() }
        EditorActionChip("Duplicate") { onDuplicate() }
        EditorActionChip("Indent") { onIndent() }
        EditorActionChip("Outdent") { onOutdent() }
        EditorActionChip("Trim") { onTrim() }
        EditorActionChip("All") { onSelectAll() }
        if (onFormatJson != null) EditorActionChip("Format JSON") { onFormatJson() }
        EditorActionChip("A-") { onFontSmaller() }
        EditorActionChip("A+") { onFontLarger() }
        EditorActionChip("{}") { onInsertPair("{", "}") }
        EditorActionChip("()") { onInsertPair("(", ")") }
        EditorActionChip("[]") { onInsertPair("[", "]") }
        EditorActionChip("\"\"") { onInsertPair("\"", "\"") }
        EditorActionChip("''") { onInsertPair("'", "'") }
        EditorActionChip("/* */") { onInsertPair("/* ", " */") }
        EditorActionChip("TAB") { onInsert("\t") }
        EditorActionChip("fun") { onInsert("fun ") }
        EditorActionChip("val") { onInsert("val ") }
        EditorActionChip("var") { onInsert("var ") }
        EditorActionChip("=>") { onInsert(" => ") }
        EditorActionChip("->") { onInsert(" -> ") }
        EditorActionChip("//") { onInsert("// ") }
    }
}

@Composable
private fun EditorActionChip(text: String, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surfaceElevated)
            .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text, color = palette.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun ModernEditCanvas(
    textState: TextFieldValue,
    lines: List<String>,
    lineNumbers: Boolean,
    wrapLines: Boolean,
    ext: String,
    fontSize: Int,
    searchQuery: String,
    currentHighlightedLine: Int?,
    currentMatchRange: IntRange?,
    verticalScrollState: ScrollState,
    highlightedText: AnnotatedString,
    gutterStates: List<GutterDiffState>,
    onValueChange: (TextFieldValue) -> Unit
) {
    val palette = AiModuleTheme.colors
    val vertical = verticalScrollState
    Row(Modifier.fillMaxSize().background(palette.background)) {
        if (lineNumbers) {
            Column(
                Modifier
                    .width(56.dp)
                    .fillMaxSize()
                    .background(palette.surface)
                    .padding(top = 12.dp)
                    .verticalScroll(vertical)
            ) {
                lines.forEachIndexed { index, _ ->
                    val active = currentHighlightedLine == index
                    val state = gutterStates.getOrNull(index) ?: GutterDiffState.NONE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (active) palette.accent else palette.textMuted,
                            fontSize = (fontSize - 1).coerceAtLeast(10).sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            lineHeight = (fontSize + 7).sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                        val barColor = when (state) {
                            GutterDiffState.ADDED -> Color(0xFF00FF66)
                            GutterDiffState.MODIFIED -> Color(0xFF00E5FF)
                            GutterDiffState.DELETED_ABOVE -> Color(0xFFFF0055)
                            GutterDiffState.NONE -> Color.Transparent
                        }
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(with(LocalDensity.current) { (fontSize + 7).toDp() })
                                .background(barColor)
                        )
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxSize().background(palette.border))
        val baseModifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)
        val scrollModifier = if (wrapLines) baseModifier.verticalScroll(vertical) else baseModifier.verticalScroll(vertical).horizontalScroll(rememberScrollState())
        Row(Modifier.weight(1f).fillMaxSize()) {
            BasicTextField(
                value = textState,
                onValueChange = onValueChange,
                modifier = scrollModifier.weight(1f),
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize + 7).sp
                ),
                visualTransformation = EditorSyntaxTransformation(ext, searchQuery, currentMatchRange, highlightedText),
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize()) {
                        if (textState.text.isEmpty()) {
                            Text("Start typing...", color = palette.textMuted, fontSize = fontSize.sp, fontFamily = FontFamily.Monospace)
                        }
                        inner()
                    }
                }
            )
            EditorMiniMap(lines, currentHighlightedLine, verticalScrollState)
        }
    }
}

@Composable
private fun EditorMiniMap(
    lines: List<String>,
    currentHighlightedLine: Int?,
    verticalScrollState: ScrollState
) {
    val palette = AiModuleTheme.colors
    Box(
        modifier = Modifier
            .width(14.dp)
            .fillMaxSize()
            .background(palette.surface)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val sampled = remember(lines) {
                if (lines.size <= 80) lines.mapIndexed { index, line -> index to line }
                else lines.indices.step((lines.size / 80).coerceAtLeast(1)).map { it to lines[it] }.take(80)
            }
            sampled.forEach { (index, line) ->
                val active = currentHighlightedLine == index
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            when {
                                active -> palette.accent
                                line.trimStart().startsWith("//") || line.trimStart().startsWith("#") -> Color(0xFF6A9955).copy(alpha = 0.75f)
                                line.isBlank() -> Color.Transparent
                                else -> palette.textMuted
                            }
                        )
                )
            }
        }

        // Viewport highlight overlay
        val scrollFraction = if (verticalScrollState.maxValue > 0) {
            verticalScrollState.value.toFloat() / verticalScrollState.maxValue
        } else {
            0f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .graphicsLayer {
                    val maxScrollY = (size.height - 40.dp.toPx()).coerceAtLeast(0f)
                    translationY = scrollFraction * maxScrollY
                }
                .background(palette.accent.copy(alpha = 0.12f))
                .border(0.5.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(GitHubControlRadius))
        )
    }
}

@Composable
private fun ModernReadCanvas(
    lines: List<String>,
    ext: String,
    lineNumbers: Boolean,
    wrapLines: Boolean,
    currentHighlightedLine: Int?,
    fontSize: Int,
    gutterStates: List<GutterDiffState>
) {
    val palette = AiModuleTheme.colors
    SelectionContainer {
        LazyColumn(
            Modifier.fillMaxSize().background(palette.background).padding(horizontal = 12.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                val active = currentHighlightedLine == index
                val state = gutterStates.getOrNull(index) ?: GutterDiffState.NONE
                Row(
                    Modifier.fillMaxWidth().background(if (active) palette.accent.copy(alpha = 0.10f) else Color.Transparent).padding(vertical = 1.dp)
                ) {
                    if (lineNumbers) {
                        Row(
                            modifier = Modifier.width(52.dp).padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (active) palette.accent else palette.textMuted,
                                fontSize = (fontSize - 1).coerceAtLeast(10).sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            val barColor = when (state) {
                                GutterDiffState.ADDED -> Color(0xFF00FF66)
                                GutterDiffState.MODIFIED -> Color(0xFF00E5FF)
                                GutterDiffState.DELETED_ABOVE -> Color(0xFFFF0055)
                                GutterDiffState.NONE -> Color.Transparent
                            }
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(with(LocalDensity.current) { (fontSize + 7).toDp() })
                                    .background(barColor)
                            )
                        }
                    }
                    Text(
                        text = highlightLine(line.ifEmpty { " " }, ext, palette),
                        color = palette.textPrimary,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (fontSize + 7).sp,
                        softWrap = wrapLines,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernImageCanvas(file: GHContent) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.75f, 6f)
        offset += panChange
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (file.downloadUrl.isBlank()) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Image, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(34.dp))
                Text("Image preview unavailable", color = Color.White.copy(alpha = 0.85f))
            }
        } else {
            AsyncImage(
                model = file.downloadUrl,
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ).transformable(state)
            )
        }

        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(GitHubControlRadius)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetaPill("IMAGE", Color(0xFF58A6FF))
            Text(file.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.ZoomOutMap, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        }
    }
}

private fun buildEditorSubtitle(ext: String, lines: Int, chars: Int, changed: Boolean): String {
    val type = ext.uppercase().ifBlank { "TEXT" }
    val suffix = if (changed) " • Modified" else ""
    return "$type • $lines lines • $chars chars$suffix"
}

private class EditorSyntaxTransformation(
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

private fun buildEditorAnnotatedText(
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

private fun buildSearchMatches(text: String, query: String, isRegex: Boolean, matchCase: Boolean): List<EditorSearchMatch> {
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

private fun buildEditorSymbols(lines: List<String>, ext: String): List<EditorSymbol> {
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

private fun commentPrefixForExtension(ext: String): String? = when (ext) {
    "kt", "java", "js", "ts", "tsx", "jsx", "c", "cpp", "h", "hpp", "cs", "swift", "go", "rs", "scala", "gradle" -> "//"
    "py", "sh", "bash", "yaml", "yml", "toml", "ini", "properties", "rb", "pl" -> "#"
    "sql", "lua" -> "--"
    else -> null
}

private fun TextRange.coerceIn(length: Int): TextRange {
    val safeStart = start.coerceIn(0, length)
    val safeEnd = end.coerceIn(0, length)
    return TextRange(safeStart, safeEnd)
}

private fun Int.floorMod(other: Int): Int = if (other == 0) 0 else ((this % other) + other) % other

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) {
        Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
    }
}

/** Horizontal row of one-tap prompt presets shown under the editor's
 * info strip. Each chip launches the AI Agent with a prompt template
 * scoped to the active file + branch. */
@Composable
private fun AiQuickActionsRow(
    filePath: String,
    branch: String,
    selectedText: String,
    onSendPrompt: (String) -> Unit,
) {
    val colors = AiModuleTheme.colors
    val actions = listOf(
        Strings.aiAgentQuickExplain to "Read `$filePath` on branch `$branch` and explain what it does, including the key types and side effects.",
        Strings.aiAgentQuickAddTests to "Add unit tests for `$filePath` (branch `$branch`). Match the project's existing test framework and naming style. Read the file first to understand the scope.",
        Strings.aiAgentQuickFixLint to "Read `$filePath` on branch `$branch` and fix any lint, formatting, or unused-import issues. Use edit_file for surgical changes; preserve behaviour.",
        Strings.aiAgentQuickRefactor to "Read `$filePath` on branch `$branch` and propose a focused refactor that improves readability without changing behaviour. Apply with edit_file once approved.",
        Strings.aiAgentQuickGenerateDocs to "Read `$filePath` on branch `$branch` and add KDoc/Javadoc to public types, functions, and properties that are missing it. Keep comments concise.",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            null,
            Modifier.size(14.dp),
            tint = colors.accent,
        )
        Spacer(Modifier.width(2.dp))
        // C2 — selection-first chip. We cap the snippet at ~4 KB to
        // avoid blowing the agent's input box if the user
        // accidentally selected the whole file. The agent already has
        // file-read tools to fetch surrounding context if needed.
        if (selectedText.isNotBlank()) {
            val snippet = if (selectedText.length > 4_000) {
                selectedText.take(4_000) + "\n…(truncated)"
            } else {
                selectedText
            }
            val prompt = buildString {
                append(Strings.aiAgentSendSelectionPromptPrefix)
                append(" `")
                append(filePath)
                append("` (branch `")
                append(branch)
                append("`):\n\n```\n")
                append(snippet)
                append("\n```\n")
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(colors.accent.copy(alpha = 0.14f))
                    .border(
                        0.5.dp,
                        colors.border,
                        RoundedCornerShape(GitHubControlRadius),
                    )
                    .clickable { onSendPrompt(prompt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    Strings.aiAgentSendSelectionChip,
                    fontSize = 11.sp,
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                )
            }
        }
        actions.forEach { (label, prompt) ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(colors.surface)
                    .border(0.5.dp, colors.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onSendPrompt(prompt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    fontFamily = JetBrainsMono,
                )
            }
        }
    }
}

/**
 * Build a unified-style diff between [oldText] and [newText] for a
 * one-shot AI commit-message suggestion. Doesn't do real LCS — for a
 * commit-message hint a coarse "removed/added line lists" is plenty
 * and avoids a full diff-engine dependency.
 */
private fun coarseDiff(oldText: String, newText: String, maxLines: Int = 60): String {
    val oldLines = oldText.lineSequence().toList()
    val newLines = newText.lineSequence().toList()
    val oldSet = oldLines.toMutableSet()
    val newSet = newLines.toMutableSet()
    val removed = oldLines.filter { it !in newSet }
    val added = newLines.filter { it !in oldSet }
    return buildString {
        appendLine("(coarse diff: ${added.size} added line(s), ${removed.size} removed line(s))")
        if (removed.isNotEmpty()) {
            appendLine()
            appendLine("--- removed:")
            removed.take(maxLines).forEach { appendLine("- $it") }
            if (removed.size > maxLines) appendLine("  […${removed.size - maxLines} more]")
        }
        if (added.isNotEmpty()) {
            appendLine()
            appendLine("+++ added:")
            added.take(maxLines).forEach { appendLine("+ $it") }
            if (added.size > maxLines) appendLine("  […${added.size - maxLines} more]")
        }
    }
}

/**
 * One-shot AI suggestion for the commit message of the current
 * single-file edit. Sends the (coarse) diff to the picked model and
 * asks for a Conventional-Commit-style one-liner. Used by the "AI
 * suggest" button in the commit dialog.
 */
internal suspend fun askCopilot(
    context: android.content.Context,
    messages: List<Pair<String, String>>,
): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val prefs = context.getSharedPreferences("github_prefs", android.content.Context.MODE_PRIVATE)
    val copilotModel = prefs.getString("copilot_model", "claude-3.7-sonnet") ?: "claude-3.7-sonnet"
    val copilotRouting = prefs.getString("copilot_routing", "Auto") ?: "Auto"
    val routeHost = when (copilotRouting) {
        "Individual" -> "api.individual.githubcopilot.com"
        "Business" -> "api.business.githubcopilot.com"
        "Enterprise" -> "api.enterprise.githubcopilot.com"
        else -> "api.githubcopilot.com"
    }
    
    val copilotToken = GitHubManager.getCopilotToken(context)
    if (copilotToken.isBlank()) {
        throw java.io.IOException("GitHub Copilot token not found. Please log in to GitHub first.")
    }

    val requestBody = JSONObject().apply {
        put("model", copilotModel)
        put("messages", JSONArray().apply {
            messages.forEach { (role, content) ->
                put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
        })
        put("stream", false)
    }.toString()

    var connection: java.net.HttpURLConnection? = null
    try {
        val proxyEnabled = prefs.getBoolean("network_proxy_enabled", false)
        val proxyHost = prefs.getString("network_proxy_host", "") ?: ""
        val proxyPort = prefs.getInt("network_proxy_port", 8080)
        val sslBypass = prefs.getBoolean("network_ssl_bypass", false)

        val url = java.net.URL("https://$routeHost/chat/completions")
        val connRaw = if (proxyEnabled && proxyHost.isNotBlank()) {
            val proxy = java.net.Proxy(java.net.Proxy.Type.HTTP, java.net.InetSocketAddress(proxyHost, proxyPort))
            url.openConnection(proxy)
        } else {
            url.openConnection()
        }

        connection = (connRaw as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $copilotToken")
            setRequestProperty("Copilot-Integration-Id", "vscode-chat")
            setRequestProperty("Editor-Version", "vscode/1.85.0")
            setRequestProperty("Editor-Plugin-Version", "copilot-chat/0.11.0")
            setRequestProperty("User-Agent", "GitHubCopilotChat/0.11.0")
        }

        if (sslBypass && connection is javax.net.ssl.HttpsURLConnection) {
            try {
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                        override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    }
                )
                val sc = javax.net.ssl.SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, java.security.SecureRandom())
                connection.sslSocketFactory = sc.socketFactory
                connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                android.util.Log.e("COPILOT_CHAT", "SSL bypass error: ${e.message}")
            }
        }

        java.io.OutputStreamWriter(connection.outputStream).use { it.write(requestBody) }
        val code = connection.responseCode
        if (code in 200..299) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(responseText).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        } else {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw java.io.IOException("HTTP error $code: $errorText")
        }
    } finally {
        connection?.disconnect()
    }
}

private suspend fun generateCommitMessage(
    context: android.content.Context,
    path: String,
    oldText: String,
    newText: String,
): String {
    val systemPrompt =
        "You are a Conventional Commits assistant. Given the path of a file and a diff " +
        "of an edit, produce a single-line commit message in Conventional Commits style " +
        "(e.g. \"fix(api): handle empty list\", \"docs: clarify install\"). " +
        "Keep it under 72 characters. Output ONLY the message, no explanation, no quotes."
    val userPrompt = buildString {
        appendLine("Path: $path")
        appendLine()
        append(coarseDiff(oldText, newText))
    }
    
    val response = askCopilot(
        context = context,
        messages = listOf(
            "system" to systemPrompt,
            "user" to userPrompt
        )
    )
    return response.lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trim()
        .trim('"', '\'', '`')
        .take(120)
}

@Composable
internal fun CopilotChatPanel(
    palette: gs.git.vps.ui.theme.AiModuleColors,
    filePath: String,
    branch: String,
    selectedText: String,
    initialPrompt: String?,
    onClose: () -> Unit,
    onApplyCode: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            val userMsg = initialPrompt
            messages.add("user" to userMsg)
            isLoading = true
            errorText = null
            scope.launch {
                try {
                    val history = mutableListOf<Pair<String, String>>()
                    val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                    val systemInstructions = prefs.getString("ai_system_prompt", "You are a professional developer helping to review code, troubleshoot errors, and suggest fixes.") ?: ""
                    if (systemInstructions.isNotBlank()) {
                        history.add("system" to systemInstructions)
                    }
                    messages.forEach { history.add(it) }
                    
                    val response = askCopilot(context, history)
                    messages.add("assistant" to response)
                    listState.animateScrollToItem(messages.size - 1)
                } catch (e: Exception) {
                    errorText = e.message ?: e.javaClass.simpleName
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (LocalGHCompact.current) 320.dp else 400.dp)
            .background(palette.surfaceElevated)
            .drawBehind {
                drawLine(
                    color = palette.border,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isLoading) Color(0xFFFFB300) else palette.accent)
                )
                Text(
                    text = "COPILOT CHAT",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Text("×", color = palette.textSecondary, fontSize = 20.sp, fontFamily = JetBrainsMono)
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(palette.background)
                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ask Copilot about the code.",
                            fontSize = 11.sp,
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            textAlign = TextAlign.Center
                        )
                        if (selectedText.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(palette.accent.copy(alpha = 0.08f))
                                    .border(0.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(GitHubControlRadius))
                                    .clickable {
                                        val prompt = "Explain this selected code:\n\n```\n$selectedText\n```"
                                        messages.add("user" to prompt)
                                        inputText = ""
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                val history = mutableListOf<Pair<String, String>>()
                                                val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                                                val systemInstructions = prefs.getString("ai_system_prompt", "") ?: ""
                                                if (systemInstructions.isNotBlank()) history.add("system" to systemInstructions)
                                                messages.forEach { history.add(it) }
                                                val response = askCopilot(context, history)
                                                messages.add("assistant" to response)
                                                listState.animateScrollToItem(messages.size - 1)
                                            } catch (e: Exception) {
                                                errorText = e.message ?: e.javaClass.simpleName
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Explain selection (${selectedText.take(20)}...)",
                                    fontSize = 10.sp,
                                    color = palette.accent,
                                    fontFamily = JetBrainsMono
                                )
                            }
                        }
                    }
                }
            } else {
                items(messages.size) { index ->
                    val (role, text) = messages[index]
                    val isUser = role == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isUser) "YOU" else "COPILOT",
                            fontSize = 9.sp,
                            color = if (isUser) palette.accent else palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(if (isUser) palette.accent.copy(alpha = 0.06f) else palette.surfaceElevated)
                                .border(0.5.dp, if (isUser) palette.accent.copy(alpha = 0.4f) else palette.border, RoundedCornerShape(GitHubControlRadius))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = text,
                                    fontSize = 11.sp,
                                    color = palette.textPrimary,
                                    fontFamily = JetBrainsMono
                                )
                                
                                val codeBlocks = remember(text) { extractCodeBlocks(text) }
                                if (codeBlocks.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    codeBlocks.forEachIndexed { _, code ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            AiModuleTextAction(
                                                label = "Apply Code",
                                                onClick = { onApplyCode(code) }
                                            )
                                            AiModuleTextAction(
                                                label = "Copy",
                                                tint = palette.textSecondary,
                                                onClick = {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Copilot Code", code))
                                                    Toast.makeText(context, "Copied code block", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiModuleSpinner()
                        Text("Copilot is typing...", fontSize = 11.sp, color = palette.textSecondary, fontFamily = JetBrainsMono)
                    }
                }
            }
            
            errorText?.let { err ->
                item {
                    Text(
                        text = "Error: $err",
                        color = palette.error,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMono,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiModuleTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = "Ask anything...",
                singleLine = false,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val prompt = inputText
                        messages.add("user" to prompt)
                        inputText = ""
                        isLoading = true
                        errorText = null
                        scope.launch {
                            try {
                                val history = mutableListOf<Pair<String, String>>()
                                val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                                val systemInstructions = prefs.getString("ai_system_prompt", "") ?: ""
                                if (systemInstructions.isNotBlank()) history.add("system" to systemInstructions)
                                messages.forEach { history.add(it) }
                                
                                val response = askCopilot(context, history)
                                messages.add("assistant" to response)
                                listState.animateScrollToItem(messages.size - 1)
                            } catch (e: Exception) {
                                errorText = e.message ?: e.javaClass.simpleName
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(if (inputText.isNotBlank() && !isLoading) palette.accent else palette.border)
            ) {
                Icon(Icons.Rounded.Send, null, tint = if (inputText.isNotBlank() && !isLoading) Color.Black else palette.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun extractCodeBlocks(text: String): List<String> {
    val blocks = mutableListOf<String>()
    val lines = text.lines()
    var inBlock = false
    val currentBlock = StringBuilder()
    
    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inBlock) {
                blocks.add(currentBlock.toString().trimEnd())
                currentBlock.clear()
                inBlock = false
            } else {
                inBlock = true
            }
        } else if (inBlock) {
            currentBlock.appendLine(line)
        }
    }
    return blocks
}

@Composable
private fun UpgradedEditorAccessoryBar(
    palette: gs.git.vps.ui.theme.AiModuleColors,
    onInsert: (String) -> Unit,
    onInsertPair: (String, String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
    onComment: () -> Unit,
    onDuplicate: () -> Unit,
    onLeftArrow: () -> Unit,
    onRightArrow: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    ext: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dynamicSnippets = remember(ext) {
        when (ext) {
            "kt", "java" -> listOf("fun ", "class ", "val ", "var ", "when ", "private ")
            "js", "ts", "tsx", "jsx" -> listOf("const ", "let ", "function ", "import ", "export ", "=> ")
            "html", "xml" -> listOf("div", "span", "class=\"\"", "id=\"\"", "</")
            "css", "scss" -> listOf("margin: ", "padding: ", "color: ", "background: ")
            "py" -> listOf("def ", "class ", "import ", "print(", "self.")
            "sh", "bash" -> listOf("echo ", "if [ ", "then", "fi")
            else -> emptyList()
        }
    }
    val characters = listOf("{", "}", "[", "]", "(", ")", ";", "/", "=", "\"", "'", "<", ">", "_", "-", "$", "&&", "||", "\t")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(palette.surfaceElevated)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Arrow Left
        Box(
            modifier = Modifier
                .height(34.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable { onLeftArrow() },
            contentAlignment = Alignment.Center
        ) {
            Text("←", color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Arrow Right
        Box(
            modifier = Modifier
                .height(34.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable { onRightArrow() },
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.width(1.dp).height(22.dp).background(palette.border))

        // UNDO
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(if (canUndo) palette.surface else palette.surface.copy(alpha = 0.4f))
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable(enabled = canUndo) { onUndo() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("UNDO", color = if (canUndo) palette.accent else palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // REDO
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(if (canRedo) palette.surface else palette.surface.copy(alpha = 0.4f))
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable(enabled = canRedo) { onRedo() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("REDO", color = if (canRedo) palette.accent else palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // FORMAT
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable { onFormat() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("FORMAT", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.width(1.dp).height(22.dp).background(palette.border))

        // COMMENT
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable { onComment() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Comment", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // DUPLICATE
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.surface)
                .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .clickable { onDuplicate() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Duplicate", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (dynamicSnippets.isNotEmpty()) {
            Box(Modifier.width(1.dp).height(22.dp).background(palette.border))

            // Dynamic code snippets
            dynamicSnippets.forEach { snippet ->
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surfaceElevated)
                        .border(0.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(GitHubControlRadius))
                        .clickable { onInsert(snippet) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = snippet.trim(),
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(Modifier.width(1.dp).height(22.dp).background(palette.border))

        // Quick characters
        characters.forEach { char ->
            val disp = if (char == "\t") "TAB" else char
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(if (char == "\t") 44.dp else 34.dp)
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surface)
                    .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable {
                        if (char == "\t") {
                            val prefs = context.getSharedPreferences("github_prefs", android.content.Context.MODE_PRIVATE)
                            val tabSize = prefs.getInt("editor_tab_size", 4)
                            val useTabs = prefs.getBoolean("editor_use_tabs", false)
                            val tabStr = if (useTabs) "\t" else " ".repeat(tabSize)
                            onInsert(tabStr)
                        } else if (char in listOf("{", "[", "(", "\"", "'")) {
                            val closing = when (char) {
                                "{" -> "}"
                                "[" -> "]"
                                "(" -> ")"
                                else -> char
                            }
                            onInsertPair(char, closing)
                        } else {
                            onInsert(char)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = disp,
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EditorStatusBar(
    palette: gs.git.vps.ui.theme.AiModuleColors,
    branch: String,
    ext: String,
    linesCount: Int,
    charsCount: Int,
    currentLine: Int,
    currentColumn: Int,
    mode: GitHubEditorMode,
    onBranchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(palette.surface)
            .border(0.5.dp, palette.border, RoundedCornerShape(0.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Branch & File type
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .clickable { onBranchClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = GhGlyphs.BRANCH,
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = branch,
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }

            Box(Modifier.width(1.dp).height(12.dp).background(palette.border))

            val lang = when (ext) {
                "kt" -> "Kotlin"
                "java" -> "Java"
                "js" -> "JavaScript"
                "ts" -> "TypeScript"
                "tsx" -> "TypeScript JSX"
                "jsx" -> "JavaScript JSX"
                "py" -> "Python"
                "md", "markdown" -> "Markdown"
                "json" -> "JSON"
                "html" -> "HTML"
                "css" -> "CSS"
                "xml" -> "XML"
                "yml", "yaml" -> "YAML"
                "sh" -> "Shell Script"
                else -> ext.uppercase().ifBlank { "Text" }
            }
            Text(
                text = lang,
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )
        }

        // Right side: Pos, Indent, Encoding, Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ln $currentLine, Col $currentColumn",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )

            Box(Modifier.width(1.dp).height(12.dp).background(palette.border))

            Text(
                text = "Spaces: 4",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )

            Box(Modifier.width(1.dp).height(12.dp).background(palette.border))

            Text(
                text = "UTF-8  LF",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )

            Box(Modifier.width(1.dp).height(12.dp).background(palette.border))

            val lockText = if (mode == GitHubEditorMode.READ) "L" else "e"
            Text(
                text = lockText,
                color = if (mode == GitHubEditorMode.READ) palette.warning else palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
private fun ModernDiffCanvas(
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

private fun formatRelativeTime(dateStr: String): String {
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

@Composable
private fun FilePickerDialog(
    repoOwner: String,
    repoName: String,
    branch: String,
    onFileSelected: (GHContent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors

    var currentPath by remember { mutableStateOf("") }
    var contents by remember { mutableStateOf<List<GHContent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun loadPath(path: String) {
        loading = true
        currentPath = path
        scope.launch {
            try {
                contents = GitHubManager.getRepoContents(context, repoOwner, repoName, path, branch)
            } catch (_: Exception) {
                contents = emptyList()
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPath("")
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "select file to open",
        confirmButton = {},
        dismissButton = {
            AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = onDismiss, tint = palette.textSecondary)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Path: /${currentPath.ifBlank { "" }}",
                fontSize = 11.sp,
                fontFamily = JetBrainsMono,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))

            if (loading) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    AiModuleSpinner("loading files...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (currentPath.isNotBlank()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parent = currentPath.substringBeforeLast("/", "")
                                        loadPath(parent)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("../", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.accent)
                                Text("go up", fontSize = 12.sp, fontFamily = JetBrainsMono, color = palette.textSecondary)
                            }
                        }
                    }
                    itemsIndexed(contents) { _, item ->
                        val isDir = item.type == "dir"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDir) {
                                        loadPath(item.path)
                                    } else {
                                        onFileSelected(item)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDir) "/" else " ",
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono,
                                color = if (isDir) palette.accent else palette.textSecondary
                            )
                            Text(
                                text = item.name,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono,
                                color = if (isDir) palette.accent else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (contents.isEmpty()) {
                        item {
                            Text("empty directory", fontSize = 11.sp, color = palette.textMuted, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

private sealed class MergeBlock {
    data class Normal(val content: String) : MergeBlock()
    data class Conflict(
        val base: String,
        val incoming: String,
        val baseName: String = "Current (Base)",
        val incomingName: String = "Incoming (Head)",
        var resolvedContent: String? = null
    ) : MergeBlock()
}

private fun parseConflictContent(content: String): List<MergeBlock> {
    val lines = content.lineSequence().toList()
    val blocks = mutableListOf<MergeBlock>()
    val currentNormal = StringBuilder()
    
    var inConflict = false
    val baseBuilder = StringBuilder()
    val incomingBuilder = StringBuilder()
    var inIncoming = false
    
    var baseLabel = "Current (Base)"
    var incomingLabel = "Incoming (Head)"
    
    for (line in lines) {
        if (line.startsWith("<<<<<<<")) {
            if (currentNormal.isNotEmpty()) {
                blocks.add(MergeBlock.Normal(currentNormal.toString()))
                currentNormal.clear()
            }
            inConflict = true
            inIncoming = false
            baseLabel = line.substring(7).trim().ifBlank { "Current (Base)" }
            baseBuilder.clear()
        } else if (line.startsWith("=======")) {
            if (inConflict) {
                inIncoming = true
            } else {
                currentNormal.append(line).append("\n")
            }
        } else if (line.startsWith(">>>>>>>")) {
            if (inConflict) {
                incomingLabel = line.substring(7).trim().ifBlank { "Incoming (Head)" }
                blocks.add(
                    MergeBlock.Conflict(
                        base = baseBuilder.toString().trimEnd('\n'),
                        incoming = incomingBuilder.toString().trimEnd('\n'),
                        baseName = baseLabel,
                        incomingName = incomingLabel
                    )
                )
                baseBuilder.clear()
                incomingBuilder.clear()
                inConflict = false
                inIncoming = false
            } else {
                currentNormal.append(line).append("\n")
            }
        } else {
            if (inConflict) {
                if (inIncoming) {
                    incomingBuilder.append(line).append("\n")
                } else {
                    baseBuilder.append(line).append("\n")
                }
            } else {
                currentNormal.append(line).append("\n")
            }
        }
    }
    
    if (inConflict) {
        currentNormal.append("<<<<<<< ").append(baseLabel).append("\n")
        currentNormal.append(baseBuilder)
        if (inIncoming) {
            currentNormal.append("=======\n")
            currentNormal.append(incomingBuilder)
        }
    }
    
    if (currentNormal.isNotEmpty()) {
        blocks.add(MergeBlock.Normal(currentNormal.toString().trimEnd('\n')))
    }
    
    return blocks
}

private fun assembleResolvedContent(blocks: List<MergeBlock>): String {
    val sb = StringBuilder()
    for (block in blocks) {
        when (block) {
            is MergeBlock.Normal -> {
                sb.append(block.content).append("\n")
            }
            is MergeBlock.Conflict -> {
                val resolved = block.resolvedContent ?: "<<<<<<< ${block.baseName}\n${block.base}\n=======\n${block.incoming}\n>>>>>>> ${block.incomingName}"
                if (resolved.isNotEmpty()) {
                    sb.append(resolved).append("\n")
                }
            }
        }
    }
    return sb.toString().trimEnd('\n')
}

@Composable
private fun ConflictResolverDialog(
    content: String,
    onDismiss: () -> Unit,
    onResolved: (String) -> Unit
) {
    val palette = AiModuleTheme.colors
    val blocks = remember(content) { parseConflictContent(content) }
    val resolvedBlocks = remember { mutableStateListOf<MergeBlock>().apply { addAll(blocks) } }
    
    val totalConflicts = blocks.count { it is MergeBlock.Conflict }
    val resolvedCount = resolvedBlocks.count { it is MergeBlock.Conflict && it.resolvedContent != null }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "resolve conflicts",
        confirmButton = {
            AiModuleTextAction(
                label = "apply",
                tint = palette.accent,
                onClick = {
                    val resolvedText = assembleResolvedContent(resolvedBlocks)
                    onResolved(resolvedText)
                }
            )
        },
        dismissButton = {
            AiModuleTextAction(
                label = Strings.cancel.lowercase(),
                tint = palette.textSecondary,
                onClick = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resolved: $resolvedCount of $totalConflicts conflicts",
                fontSize = 12.sp,
                fontFamily = JetBrainsMono,
                color = if (resolvedCount == totalConflicts) Color(0xFF34C759) else palette.textSecondary
            )
            
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(resolvedBlocks) { index, block ->
                    when (block) {
                        is MergeBlock.Normal -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.background.copy(alpha = 0.5f))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = block.content,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMono,
                                    color = palette.textMuted,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        is MergeBlock.Conflict -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(GitHubControlRadius))
                                    .background(palette.surface)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Conflict #${resolvedBlocks.take(index + 1).count { it is MergeBlock.Conflict }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9500),
                                    fontFamily = JetBrainsMono
                                )
                                
                                val isBaseActive = block.resolvedContent == block.base
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isBaseActive) Color(0xFF34C759) else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = block.base)
                                        }
                                        .background(if (isBaseActive) Color(0xFF34C759).copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Current: " + block.baseName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBaseActive) Color(0xFF34C759) else palette.textPrimary,
                                        fontFamily = JetBrainsMono
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = block.base.ifBlank { "(empty)" },
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono,
                                        color = if (isBaseActive) palette.textPrimary else palette.textSecondary,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                val isIncomingActive = block.resolvedContent == block.incoming
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isIncomingActive) palette.accent else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = block.incoming)
                                        }
                                        .background(if (isIncomingActive) palette.accent.copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Incoming: " + block.incomingName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncomingActive) palette.accent else palette.textPrimary,
                                        fontFamily = JetBrainsMono
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = block.incoming.ifBlank { "(empty)" },
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMono,
                                        color = if (isIncomingActive) palette.textPrimary else palette.textSecondary,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                val bothText = block.base + "\n" + block.incoming
                                val isBothActive = block.resolvedContent == bothText
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isBothActive) Color(0xFF8E8E93) else palette.border,
                                            RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            resolvedBlocks[index] = block.copy(resolvedContent = bothText)
                                        }
                                        .background(if (isBothActive) Color(0xFF8E8E93).copy(alpha = 0.05f) else Color.Transparent)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Accept both changes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isBothActive) palette.textPrimary else palette.textSecondary,
                                        fontFamily = JetBrainsMono
                                    )
                                    if (isBothActive) {
                                        Text(
                                            text = "Active",
                                            fontSize = 10.sp,
                                            color = Color(0xFF8E8E93),
                                            fontFamily = JetBrainsMono
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

private enum class GutterDiffState { NONE, ADDED, MODIFIED, DELETED_ABOVE }

private fun computeGutterDiff(oldText: String, newText: String): List<GutterDiffState> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()
    val n = oldLines.size
    val m = newLines.size
    
    val states = MutableList(m) { GutterDiffState.NONE }
    if (oldText == newText) return states

    // Fallback/Fast path for very large files to avoid OOM/ANR
    if (n * m > 100_000) {
        val oldSet = oldLines.toSet()
        for (j in 0 until m) {
            if (newLines[j] !in oldSet) {
                states[j] = GutterDiffState.ADDED
            }
        }
        return states
    }

    // Standard LCS DP
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

    // Backtrack to align
    var i = n
    var j = m
    
    val path = mutableListOf<Pair<Int, Int>>()
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            path.add(Pair(i - 1, j - 1))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            path.add(Pair(-1, j - 1))
            j--
        } else {
            path.add(Pair(i - 1, -1))
            i--
        }
    }
    path.reverse()

    var idx = 0
    while (idx < path.size) {
        val step = path[idx]
        if (step.first == -1 && step.second != -1) {
            // Addition in newText. Let's check if there was a deletion nearby in this edit group.
            var hasDeletionNearby = false
            var k = idx - 1
            while (k >= 0) {
                if (path[k].first != -1 && path[k].second != -1) break
                if (path[k].first != -1 && path[k].second == -1) {
                    hasDeletionNearby = true
                    break
                }
                k--
            }
            if (!hasDeletionNearby) {
                k = idx + 1
                while (k < path.size) {
                    if (path[k].first != -1 && path[k].second != -1) break
                    if (path[k].first != -1 && path[k].second == -1) {
                        hasDeletionNearby = true
                        break
                    }
                    k++
                }
            }
            
            if (hasDeletionNearby) {
                states[step.second] = GutterDiffState.MODIFIED
            } else {
                states[step.second] = GutterDiffState.ADDED
            }
            idx++
        } else if (step.first != -1 && step.second == -1) {
            // Deletion in oldText. Mark next line of newText as DELETED_ABOVE.
            var nextNewIdx = -1
            var k = idx + 1
            while (k < path.size) {
                if (path[k].second != -1) {
                    nextNewIdx = path[k].second
                    break
                }
                k++
            }
            if (nextNewIdx != -1) {
                if (states[nextNewIdx] == GutterDiffState.NONE) {
                    states[nextNewIdx] = GutterDiffState.DELETED_ABOVE
                }
            } else {
                if (m > 0 && states[m - 1] == GutterDiffState.NONE) {
                    states[m - 1] = GutterDiffState.DELETED_ABOVE
                }
            }
            idx++
        } else {
            idx++
        }
    }
    
    return states
}

