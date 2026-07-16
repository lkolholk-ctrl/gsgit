package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import gs.git.vps.data.github.model.GHBlameRange
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

internal enum class GitHubEditorMode { EDIT, READ, PREVIEW, DIFF }

internal data class EditorSearchMatch(val start: Int, val end: Int, val line: Int)

internal data class EditorSymbol(val name: String, val kind: String, val line: Int)

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
    previewBytes: ByteArray? = null,
    initialLine: Int? = null,
    readOnly: Boolean = false,
    lite: Boolean = false,
    workspaceTabs: List<GHContent> = emptyList(),
    workspaceDirtyPaths: Set<String> = emptySet(),
    canNavigateWorkspaceBack: Boolean = false,
    canNavigateWorkspaceForward: Boolean = false,
    initialHasDraft: Boolean = false,
    workspaceOfflineCache: Boolean = false,
    onSelectWorkspaceTab: ((GHContent) -> Unit)? = null,
    onCloseWorkspaceTab: ((GHContent) -> Unit)? = null,
    onWorkspaceBack: (() -> Unit)? = null,
    onWorkspaceForward: (() -> Unit)? = null,
    onQuickOpen: (() -> Unit)? = null,
    onGlobalSearch: (() -> Unit)? = null,
    onOpenBlame: ((GHContent) -> Unit)? = null,
    onOpenHistory: ((GHContent) -> Unit)? = null,
    onDraftChanged: ((path: String, content: String, changedFromInitial: Boolean) -> Unit)? = null,
    onSaveDraft: ((path: String, content: String) -> Unit)? = null,
    onBack: () -> Unit,
    onAskAi: ((prompt: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

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
                    mode = if (readOnly) GitHubEditorMode.READ else GitHubEditorMode.EDIT,
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            )
        }
    }

    val prefs = remember(context) { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }
    val editorIndentUnit = remember(file.path, branch) {
        if (prefs.getBoolean("editor_use_tabs", false)) "\t"
        else " ".repeat(prefs.getInt("editor_tab_size", 4).coerceIn(1, 8))
    }
    var textState by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var savedContent by remember { mutableStateOf(initialContent) }
    var savedSha by remember { mutableStateOf(file.sha) }
    val initialFilePolicy = remember(file.path) { codeFilePolicy(file.name) }
    var mode by remember(file.path, branch) {
        mutableStateOf(
            when {
                readOnly && initialFilePolicy.previewable -> GitHubEditorMode.PREVIEW
                readOnly -> GitHubEditorMode.READ
                else -> GitHubEditorMode.EDIT
            },
        )
    }
    var workspaceHasSavedDraft by remember(file.path, branch) { mutableStateOf(initialHasDraft) }
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
    var fileBlame by remember { mutableStateOf<List<GHBlameRange>>(emptyList()) }
    var showBlame by rememberSaveable(file.path) { mutableStateOf(false) }

    val currentFile = tabs.getOrNull(activeTabIndex)?.file ?: file

    LaunchedEffect(currentBranch, currentFile.path, showBlame) {
        if (!showBlame) return@LaunchedEffect
        fileBlame = try {
            GitHubManager.getFileBlame(context, repoOwner, repoName, currentFile.path, currentBranch)
        } catch (_: java.lang.Exception) {
            emptyList()
        }
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
    val isImage = ext in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico")
    val isMarkdown = ext in listOf("md", "markdown")
    val isJson = ext == "json"
    val isSvg = ext == "svg"
    val supportsPreview = isMarkdown || isJson || isSvg
    val text = textState.text
    val lines = remember(text) { text.lines() }
    val hasChanges = text != savedContent
    val diagnosticsSkipped = text.length > 1_000_000
    var diagnostics by remember(currentFile.path, branch) { mutableStateOf<List<EditorDiagnostic>>(emptyList()) }
    var showDiagnosticsDialog by rememberSaveable(currentFile.path, branch) { mutableStateOf(false) }
    LaunchedEffect(text, ext) {
        kotlinx.coroutines.delay(280)
        diagnostics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            analyzeEditorText(text, ext)
        }
    }
    // Workspace tabs must not lose the latest keystrokes when switching through Quick Open/history.
    // The parent owns the typed draft and decides whether a reverted remote file should be removed.
    val latestDraftChanged by rememberUpdatedState(onDraftChanged)
    LaunchedEffect(currentFile.path, text) {
        if (latestDraftChanged != null) {
            kotlinx.coroutines.delay(350)
            latestDraftChanged?.invoke(currentFile.path, text, hasChanges || workspaceHasSavedDraft)
        }
    }
    // Авто-сейв черновика при уходе в фон (ON_STOP) — правки не теряются при смерти процесса.
    // Спека: «авто-сохраняется молча на уход в фон». Только когда есть onSaveDraft (Code-таб draft-режим).
    if (onSaveDraft != null) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val latestText by rememberUpdatedState(text)
        val latestPath by rememberUpdatedState(currentFile.path)
        val latestHasChanges by rememberUpdatedState(hasChanges)
        DisposableEffect(lifecycleOwner) {
            val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && latestHasChanges) {
                    onSaveDraft(latestPath, latestText)
                }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }
    }
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

    val activeBlame = remember(fileBlame, currentLine) {
        fileBlame.firstOrNull { currentLine in it.startLine..it.endLine }
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
            onSaveDraft != null && hasChanges -> { focusManager.clearFocus(); onSaveDraft(currentFile.path, text); onBack() }
            onDraftChanged != null -> {
                focusManager.clearFocus()
                onDraftChanged(currentFile.path, text, hasChanges || workspaceHasSavedDraft)
                onBack()
            }
            hasChanges && !isImage -> showDiscardDialog = true
            else -> { focusManager.clearFocus(); onBack() }
        }
    }
    BackHandler(enabled = true, onBack = ::handleEditorBack)

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
        applyState(applySmartEditorInput(textState, newState, ext, editorIndentUnit))
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
        applyState(toggleEditorComment(textState, ext))
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

    fun formatCodeDocument() {
        val result = formatEditorDocument(textState.text, ext, editorIndentUnit)
        if (result.error != null) {
            Toast.makeText(context, result.error, Toast.LENGTH_SHORT).show()
            return
        }
        if (result.text == textState.text) {
            Toast.makeText(context, "Already formatted", Toast.LENGTH_SHORT).show()
            return
        }
        val cursor = textState.selection.start.coerceIn(0, result.text.length)
        applyState(TextFieldValue(result.text, TextRange(cursor)))
        Toast.makeText(context, "Formatted: ${result.label}", Toast.LENGTH_SHORT).show()
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

    if (showDiagnosticsDialog) {
        EditorDiagnosticsDialog(
            diagnostics = diagnostics,
            onSelect = { diagnostic ->
                goToLine(diagnostic.line)
                showDiagnosticsDialog = false
            },
            onDismiss = { showDiagnosticsDialog = false },
        )
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
                    EditorCompactField("stash message", msgInput) { msgInput = it }
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
        val onSaveAction: () -> Unit = {
            if (onSaveDraft != null) {
                onSaveDraft(currentFile.path, text)
                savedContent = text
                workspaceHasSavedDraft = true
                Toast.makeText(context, "saved to draft", Toast.LENGTH_SHORT).show()
            } else { showCommitDialog = true }
        }
        // Lite-chrome (Code-таб): тонкий канонный топбар вместо всего тяжёлого chrome (топбар + tabs +
        // search + autocomplete — всё внутри ветки !zenMode). Ядро редактирования (ModernEditCanvas) и
        // accessory гейтятся отдельно. См. docs/code-tab-ui-plan.md (Phase-2 редактора).
        if (lite) {
            val flushDraft = {
                onDraftChanged?.invoke(currentFile.path, text, hasChanges || workspaceHasSavedDraft)
                Unit
            }
            LiteEditorTopBar(
                file = currentFile,
                ext = ext,
                hasChanges = hasChanges,
                offlineCache = workspaceOfflineCache,
                tabs = workspaceTabs,
                dirtyPaths = workspaceDirtyPaths,
                canGoBack = canNavigateWorkspaceBack,
                canGoForward = canNavigateWorkspaceForward,
                onSave = onSaveAction,
                onBack = ::handleEditorBack,
                onSelectTab = { selected -> flushDraft(); onSelectWorkspaceTab?.invoke(selected) },
                onCloseTab = { selected -> flushDraft(); onCloseWorkspaceTab?.invoke(selected) },
                onHistoryBack = { flushDraft(); onWorkspaceBack?.invoke() },
                onHistoryForward = { flushDraft(); onWorkspaceForward?.invoke() },
                onQuickOpen = { flushDraft(); onQuickOpen?.invoke() },
                onGlobalSearch = { flushDraft(); onGlobalSearch?.invoke() },
                onOpenBlame = onOpenBlame?.let { action -> { flushDraft(); action(currentFile) } },
                onOpenHistory = onOpenHistory?.let { action -> { flushDraft(); action(currentFile) } },
            )
        } else if (!zenMode) {
            GitHubEditorTopBar(
                fileName = currentFile.name,
                subtitle = buildEditorSubtitle(ext, lines.size, text.length, hasChanges),
                isImage = isImage,
                showMoreMenu = showMoreMenu,
                hasChanges = hasChanges,
                onToggleMoreMenu = { showMoreMenu = !showMoreMenu },
                onSave = onSaveAction,
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
                fontSize = fontSize,
                onToggleLineNumbers = { lineNumbers = !lineNumbers },
                onToggleWrap = { wrapLines = !wrapLines },
                onToggleZen = { zenMode = !zenMode; showMoreMenu = false },
                onToggleSearch = { showSearch = !showSearch; showMoreMenu = false },
                onToggleBlame = { showBlame = !showBlame; showMoreMenu = false },
                onCycleMode = {
                    mode = when (mode) {
                        GitHubEditorMode.EDIT -> if (supportsPreview) GitHubEditorMode.PREVIEW else GitHubEditorMode.READ
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
                supportsPreview = supportsPreview,
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

        AnimatedVisibility(diagnostics.isNotEmpty() && !isImage && !zenMode) {
            EditorDiagnosticsBar(
                diagnostics = diagnostics,
                onOpen = { showDiagnosticsDialog = true },
            )
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
                        isImage -> ModernImageCanvas(currentFile, previewBytes)
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
                        isJson && mode == GitHubEditorMode.PREVIEW -> ModernJsonPreviewCanvas(text, fontSize)
                        isSvg && mode == GitHubEditorMode.PREVIEW -> ModernImageCanvas(
                            currentFile,
                            text.toByteArray(Charsets.UTF_8),
                        )
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

        AnimatedVisibility(showBlame && activeBlame != null && !zenMode) {
            activeBlame?.let { blame ->
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
                            text = "${blame.author} · ${formatRelativeTime(blame.date)} · L${blame.startLine}-${blame.endLine}",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "· ${blame.message}",
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = blame.sha.take(7),
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
                indentLabel = if (editorIndentUnit == "\t") "Tabs" else "Spaces: ${editorIndentUnit.length}",
                diagnosticErrors = diagnostics.count { it.severity == EditorDiagnosticSeverity.ERROR },
                diagnosticWarnings = diagnostics.count { it.severity == EditorDiagnosticSeverity.WARNING },
                diagnosticsSkipped = diagnosticsSkipped,
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

/**
 * Лёгкий топбар Code-таб-редактора (Phase-2): тонкий канонный chrome вместо тяжёлого
 * GitHubEditorTopBar+tabs+accessory. Глиф-back + путь файла + индикатор dirty + save
 * (GitHubTerminalButton). Ядро редактирования (ModernEditCanvas) переиспользуется без изменений.
 */
@Composable
private fun LiteEditorTopBar(
    file: GHContent,
    ext: String,
    hasChanges: Boolean,
    offlineCache: Boolean,
    tabs: List<GHContent>,
    dirtyPaths: Set<String>,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onSelectTab: (GHContent) -> Unit,
    onCloseTab: (GHContent) -> Unit,
    onHistoryBack: () -> Unit,
    onHistoryForward: () -> Unit,
    onQuickOpen: () -> Unit,
    onGlobalSearch: () -> Unit,
    onOpenBlame: (() -> Unit)?,
    onOpenHistory: (() -> Unit)?,
) {
    val colors = AiModuleTheme.colors
    val sub = file.path.substringBeforeLast('/', "").ifEmpty { ext }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GitHubTopBarAction(glyph = GhGlyphs.BACK, onClick = onBack, tint = colors.textPrimary, contentDescription = "back")
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = colors.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (sub.isNotEmpty()) {
                    Text(
                        text = sub,
                        color = colors.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (hasChanges) {
                Text(text = "●", color = colors.warning, fontFamily = JetBrainsMono, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
            }
            if (offlineCache) {
                Text(
                    text = "cache",
                    color = colors.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                )
                Spacer(Modifier.width(6.dp))
            }
            onOpenBlame?.let {
                GitHubTopBarAction(glyph = "B", onClick = it, tint = colors.textSecondary, contentDescription = "blame")
            }
            onOpenHistory?.let {
                GitHubTopBarAction(glyph = "H", onClick = it, tint = colors.textSecondary, contentDescription = "file history")
            }
            GitHubTerminalButton(label = "save", onClick = onSave, color = colors.accent)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
    CodeWorkspaceToolbar(
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        onGoBack = onHistoryBack,
        onGoForward = onHistoryForward,
        onQuickOpen = onQuickOpen,
        onGlobalSearch = onGlobalSearch,
    )
    CodeWorkspaceTabsRow(
        tabs = tabs,
        activePath = file.path,
        dirtyPaths = dirtyPaths,
        onSelect = onSelectTab,
        onClose = onCloseTab,
    )
}
