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
 * Chrome CodeEditor: топ-бар (GitHubEditorTopBar, EditorMoreMenu), инфо-полоса, поиск/замена,
 * action-ribbon, mode/meta-pills и мелкие кнопки. Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

@Composable
internal fun GitHubEditorTopBar(
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
internal fun EditorMoreMenu(
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
internal fun EditorInfoStrip(
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
internal fun MetaPill(text: String, color: Color) {
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
internal fun SearchReplaceCard(
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

