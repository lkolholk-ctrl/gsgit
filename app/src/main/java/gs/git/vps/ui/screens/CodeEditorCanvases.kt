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
 * Канвасы CodeEditor: ModernEditCanvas (редактор), EditorMiniMap, ModernReadCanvas (чтение),
 * ModernImageCanvas (картинки). Вынесено из GitHubCodeEditorModule.kt (Фаза 1).
 */

@Composable
internal fun ModernEditCanvas(
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
internal fun ModernReadCanvas(
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
internal fun ModernImageCanvas(file: GHContent) {
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

