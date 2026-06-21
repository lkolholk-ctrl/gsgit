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
 * AI/Copilot в CodeEditor: AiQuickActionsRow, askCopilot, generateCommitMessage,
 * CopilotChatPanel, coarseDiff, extractCodeBlocks. Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

/** Horizontal row of one-tap prompt presets shown under the editor's
 * info strip. Each chip launches the AI Agent with a prompt template
 * scoped to the active file + branch. */
@Composable
internal fun AiQuickActionsRow(
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
    val copilotModel = prefs.getString("copilot_model", "gpt-5.5") ?: "gpt-5.5"
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

internal suspend fun generateCommitMessage(
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
            .statusBarsPadding()
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

