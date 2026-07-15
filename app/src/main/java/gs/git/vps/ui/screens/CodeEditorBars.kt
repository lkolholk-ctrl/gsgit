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
 * Панели CodeEditor: UpgradedEditorAccessoryBar (аксессуар-бар над клавиатурой) и
 * EditorStatusBar (статус-строка). Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

@Composable
internal fun UpgradedEditorAccessoryBar(
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
internal fun EditorStatusBar(
    palette: gs.git.vps.ui.theme.AiModuleColors,
    branch: String,
    ext: String,
    linesCount: Int,
    charsCount: Int,
    currentLine: Int,
    currentColumn: Int,
    mode: GitHubEditorMode,
    indentLabel: String,
    diagnosticErrors: Int,
    diagnosticWarnings: Int,
    diagnosticsSkipped: Boolean,
    onBranchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(palette.surface)
            .border(0.5.dp, palette.border, RoundedCornerShape(0.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                text = indentLabel,
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

            Text(
                text = when {
                    diagnosticsSkipped -> "analysis off >1 MB"
                    diagnosticErrors > 0 -> "× $diagnosticErrors"
                    diagnosticWarnings > 0 -> "! $diagnosticWarnings"
                    else -> "✓ 0 problems"
                },
                color = when {
                    diagnosticsSkipped -> palette.textMuted
                    diagnosticErrors > 0 -> palette.error
                    diagnosticWarnings > 0 -> palette.warning
                    else -> palette.syntaxString
                },
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
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
