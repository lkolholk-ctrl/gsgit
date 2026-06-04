package gs.git.vps.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.text.TextStyle

// Compact mode — propagates through all sub-screens automatically

sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val lang: String, val codeText: String) : MarkdownBlock()
    data class ListItem(val level: Int, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0
    val n = lines.size
    
    while (i < n) {
        val line = lines[i]
        val trimmed = line.trim()
        
        when {
            trimmed.startsWith("```") -> {
                val lang = trimmed.substring(3).trim()
                val codeBuilder = StringBuilder()
                i++
                while (i < n && !lines[i].trim().startsWith("```")) {
                    codeBuilder.append(lines[i]).append("\n")
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(lang, codeBuilder.toString().trimEnd()))
                i++
            }
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length
                if (level in 1..6 && trimmed.getOrNull(level) == ' ') {
                    blocks.add(MarkdownBlock.Heading(level, trimmed.substring(level + 1).trim()))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(line))
                }
                i++
            }
            trimmed.startsWith(">") -> {
                val quoteBuilder = StringBuilder()
                while (i < n && (lines[i].trim().startsWith(">") || lines[i].trim().isEmpty())) {
                    val content = lines[i].trim().removePrefix(">").trim()
                    if (content.isNotEmpty()) {
                        quoteBuilder.append(content).append("\n")
                    }
                    i++
                }
                blocks.add(MarkdownBlock.BlockQuote(quoteBuilder.toString().trimEnd()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") || (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(". ")) -> {
                val leadingSpaces = line.takeWhile { it == ' ' || it == '\t' }.length
                val level = leadingSpaces / 2
                val listText = if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                    trimmed.substring(2)
                } else {
                    trimmed.substring(trimmed.indexOf(". ") + 2)
                }
                blocks.add(MarkdownBlock.ListItem(level, listText))
                i++
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
            }
            trimmed.isEmpty() -> {
                i++
            }
            else -> {
                val paraBuilder = StringBuilder(line.trim())
                i++
                while (i < n) {
                    val nextLine = lines[i]
                    val nextTrim = nextLine.trim()
                    if (nextTrim.isEmpty() || nextTrim.startsWith("#") || nextTrim.startsWith("```") || nextTrim.startsWith(">") || nextTrim.startsWith("- ") || nextTrim.startsWith("* ") || nextTrim.startsWith("+ ") || nextTrim == "---") {
                        break
                    }
                    paraBuilder.append(" ").append(nextTrim)
                    i++
                }
                blocks.add(MarkdownBlock.Paragraph(paraBuilder.toString()))
            }
        }
    }
    return blocks
}

@Composable
fun MarkdownCanvas(text: String, modifier: Modifier = Modifier, repo: String = "") {
    val palette = AiModuleTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var useGithubRender by remember { mutableStateOf(false) }
    var githubHtml by remember { mutableStateOf("") }
    var rendering by remember { mutableStateOf(false) }

    LaunchedEffect(useGithubRender, text) {
        if (useGithubRender && text.isNotBlank()) {
            rendering = true
            githubHtml = GitHubManager.renderMarkdown(context, text, if (repo.isNotBlank()) "gfm" else "markdown", repo)
            rendering = false
        }
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            AiModulePillButton(
                label = if (useGithubRender) "local" else "gfm",
                onClick = { useGithubRender = !useGithubRender },
            )
        }
        if (rendering) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "rendering…")
            }
        } else if (useGithubRender && githubHtml.isNotBlank()) {
            val htmlBlocks = remember(githubHtml) { parseGithubHtml(githubHtml) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                htmlBlocks.forEach { block -> RenderMarkdownBlock(block) }
            }
        } else {
            val blocks = remember(text) { parseMarkdown(text) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                blocks.forEach { block -> RenderMarkdownBlock(block) }
            }
        }
    }
}

private fun parseGithubHtml(html: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    val len = html.length
    while (i < len) {
        when {
            i + 4 <= len && html.substring(i, i + 4) == "<h1>" -> {
                val end = html.indexOf("</h1>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.Heading(1, stripTags(html.substring(i + 4, to)))); i = if (end > i) end + 5 else len
            }
            i + 4 <= len && html.substring(i, i + 4) == "<h2>" -> {
                val end = html.indexOf("</h2>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.Heading(2, stripTags(html.substring(i + 4, to)))); i = if (end > i) end + 5 else len
            }
            i + 4 <= len && html.substring(i, i + 4) == "<h3>" -> {
                val end = html.indexOf("</h3>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.Heading(3, stripTags(html.substring(i + 4, to)))); i = if (end > i) end + 5 else len
            }
            i + 4 <= len && html.substring(i, i + 4) == "<h4>" -> {
                val end = html.indexOf("</h4>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.Heading(4, stripTags(html.substring(i + 4, to)))); i = if (end > i) end + 5 else len
            }
            i + 3 <= len && html.substring(i, i + 3) == "<p>" -> {
                val end = html.indexOf("</p>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.Paragraph(stripTags(html.substring(i + 3, to)))); i = if (end > i) end + 4 else len
            }
            i + 5 <= len && html.substring(i, i + 5) == "<pre>" -> {
                val end = html.indexOf("</pre>", i); val to = if (end > i) end else len
                val raw = html.substring(i + 5, to)
                val codeStart = raw.indexOf(">"); val codeEnd = raw.lastIndexOf("<")
                val code = if (codeStart in 0 until codeEnd) raw.substring(codeStart + 1, codeEnd) else raw
                blocks.add(MarkdownBlock.CodeBlock("", code.trimIndent())); i = if (end > i) end + 6 else len
            }
            i + 4 <= len && html.substring(i, i + 4) == "<ul>" -> {
                val end = html.indexOf("</ul>", i); val to = if (end > i) end else len
                val inner = html.substring(i + 4, to)
                val liRx = Regex("<li>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
                liRx.findAll(inner).forEach { m -> blocks.add(MarkdownBlock.ListItem(0, stripTags(m.groupValues[1]))) }
                i = if (end > i) end + 5 else len
            }
            i + 4 <= len && html.substring(i, i + 4) == "<ol>" -> {
                val end = html.indexOf("</ol>", i); val to = if (end > i) end else len
                val inner = html.substring(i + 4, to)
                val liRx = Regex("<li>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
                liRx.findAll(inner).forEach { m -> blocks.add(MarkdownBlock.ListItem(0, stripTags(m.groupValues[1]))) }
                i = if (end > i) end + 5 else len
            }
            i + 8 <= len && html.substring(i, i + 8) == "<hr>" -> {
                blocks.add(MarkdownBlock.HorizontalRule); i += 4
            }
            i + 10 <= len && html.substring(i, i + 10) == "<hr />" -> {
                blocks.add(MarkdownBlock.HorizontalRule); i += 5
            }
            i + 10 <= len && html.substring(i, i + 10) == "<blockquote>" -> {
                val end = html.indexOf("</blockquote>", i); val to = if (end > i) end else len
                blocks.add(MarkdownBlock.BlockQuote(stripTags(html.substring(i + 10, to)).trim())); i = if (end > i) end + 13 else len
            }
            else -> i++
        }
    }
    return blocks
}

private fun stripTags(html: String): String {
    return html.replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
}

@Composable
private fun RenderMarkdownBlock(block: MarkdownBlock) {
    val palette = AiModuleTheme.colors
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    
    when (block) {
        is MarkdownBlock.Heading -> {
            val fontSize = when (block.level) {
                1 -> 28.sp
                2 -> 20.sp
                3 -> 18.sp
                4 -> 14.sp
                else -> 14.sp
            }
            val fontWeight = if (block.level <= 2) FontWeight.ExtraBold else FontWeight.Bold
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
                Text(
                    text = buildMdAnnotated(block.text, palette),
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono
                )
                if (block.level <= 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
                }
            }
        }
        is MarkdownBlock.Paragraph -> {
            androidx.compose.material3.Text(
                text = buildMdAnnotated(block.text, palette),
                style = TextStyle(
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            )
        }
        is MarkdownBlock.CodeBlock -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surfaceElevated)
                    .border(0.5.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = block.lang.ifBlank { "code" }.lowercase(),
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .clickable {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(block.codeText))
                                Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "cp",
                            color = palette.accent,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(palette.border))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    block.codeText.lines().forEach { line ->
                        Text(
                            text = highlightLine(line, block.lang),
                            fontSize = 14.sp,
                            fontFamily = JetBrainsMono,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
        is MarkdownBlock.ListItem -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (block.level * 16).dp, top = 1.dp, bottom = 1.dp)
            ) {
                Text(
                    text = "  \u2022  ",
                    fontSize = 14.sp,
                    color = palette.accent,
                    fontFamily = JetBrainsMono
                )
                androidx.compose.material3.Text(
                    text = buildMdAnnotated(block.text, palette),
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is MarkdownBlock.BlockQuote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surfaceElevated.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Max)
                        .background(palette.accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    block.text.lines().forEach { line ->
                        Text(
                            text = buildMdAnnotated(line, palette),
                            fontSize = 14.sp,
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
        is MarkdownBlock.HorizontalRule -> {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(1.dp)
                    .background(palette.border)
            )
        }
    }
}

@Composable
internal fun MarkdownLine(line: String) {
    val palette = AiModuleTheme.colors
    val block = remember(line) {
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("# ") -> MarkdownBlock.Heading(1, trimmed.removePrefix("# "))
            trimmed.startsWith("## ") -> MarkdownBlock.Heading(2, trimmed.removePrefix("## "))
            trimmed.startsWith("### ") -> MarkdownBlock.Heading(3, trimmed.removePrefix("### "))
            trimmed.startsWith("```") -> MarkdownBlock.HorizontalRule
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> MarkdownBlock.ListItem(0, trimmed.substring(2))
            trimmed.startsWith("> ") -> MarkdownBlock.BlockQuote(trimmed.substring(2))
            trimmed.startsWith("---") || trimmed.startsWith("***") -> MarkdownBlock.HorizontalRule
            trimmed.isEmpty() -> MarkdownBlock.Paragraph(" ")
            else -> MarkdownBlock.Paragraph(line)
        }
    }
    RenderMarkdownBlock(block)
}

private fun openUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

internal fun buildMdAnnotated(text: String, palette: gs.git.vps.ui.theme.AiModuleColors): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        val len = text.length
        val defColor = palette.textPrimary
        val codeColor = palette.accent
        val codeBg = palette.surfaceElevated
        val boldColor = palette.textPrimary
        val linkColor = Color(0xFF58A6FF)
        
        while (i < len) {
            when {
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = codeColor, fontFamily = JetBrainsMono, background = codeBg, fontSize = 12.sp))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = boldColor, fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < len) {
                        if (text[closeBracket + 1] == '(') closeBracket + 1 else -1
                    } else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (closeParen > 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(openParen + 1, closeParen)
                        val start = length
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = linkColor, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                        append(linkText)
                        pop()
                        addLink(LinkAnnotation.Url(url), start, length)
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

internal fun buildMdAnnotated(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildMdAnnotated(text, AiModuleDarkColors)
}

// ═══════════════════════════════════
// Syntax Highlighting (fast, safe)
// ═══════════════════════════════════

internal val defaultKeywords = setOf(
    "fun", "val", "var", "class", "object", "interface", "enum", "data", "sealed", "abstract",
    "override", "private", "public", "protected", "internal", "open", "final", "companion",
    "import", "package", "return", "if", "else", "when", "for", "while", "do", "break", "continue",
    "try", "catch", "finally", "throw", "is", "as", "in", "by", "init", "constructor", "suspend",
    "function", "const", "let", "def", "self", "this", "super", "new", "delete", "typeof", "instanceof",
    "static", "void", "int", "long", "float", "double", "boolean", "char", "string", "byte",
    "true", "false", "null", "nil", "None", "True", "False",
    "struct", "impl", "trait", "pub", "fn", "mut", "use", "mod", "crate", "extern",
    "from", "with", "yield", "async", "await", "lambda", "raise", "except", "pass",
    "switch", "case", "default", "goto", "volatile", "register", "typedef", "sizeof"
)

internal val htmlKeywords = setOf(
    "div", "span", "html", "head", "body", "script", "style", "link", "meta", "title",
    "p", "a", "img", "input", "button", "form", "table", "tr", "td", "th", "ul", "ol", "li",
    "h1", "h2", "h3", "h4", "h5", "h6", "br", "hr", "section", "header", "footer", "nav",
    "class", "id", "src", "href", "type", "value", "name", "content", "rel", "width", "height"
)

internal fun highlightLine(line: String, ext: String): androidx.compose.ui.text.AnnotatedString {
    val defColor = AiModuleDarkColors.textPrimary

    // Safety: very long lines → no highlighting (prevents OOM on minified files)
    if (line.length > 500) {
        return androidx.compose.ui.text.buildAnnotatedString {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor))
            append(line.take(500) + "...")
            pop()
        }
    }

    return try {
        doHighlightLine(line, ext)
    } catch (_: Exception) {
        // Fallback: plain text if highlighting crashes
        androidx.compose.ui.text.buildAnnotatedString {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(line); pop()
        }
    }
}

internal fun doHighlightLine(line: String, ext: String): androidx.compose.ui.text.AnnotatedString {
    val palette = AiModuleDarkColors
    val kwColor = palette.syntaxKeyword
    val strColor = palette.syntaxString
    val commentColor = palette.syntaxComment
    val numColor = palette.syntaxNumber
    val typeColor = palette.accent
    val tagColor = palette.accent
    val attrColor = palette.syntaxArg
    val defColor = palette.textPrimary

    val isHtml = ext in listOf("html", "xml", "svg", "xaml", "xhtml")
    val isCss = ext in listOf("css", "scss", "sass", "less")
    val isJson = ext in listOf("json")
    val isYaml = ext in listOf("yaml", "yml", "toml")
    val isPy = ext in listOf("py")

    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0; val len = line.length
        if (len == 0) return@buildAnnotatedString

        // JSON/YAML
        if (isJson || isYaml) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0 && (line.trimStart().startsWith("\"") || isYaml)) {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = attrColor)); append(line.substring(0, colonIdx)); pop()
                pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(":"); pop()
                val rest = line.substring(colonIdx + 1)
                val trimRest = rest.trimStart()
                val c = when {
                    trimRest.startsWith("\"") -> strColor
                    trimRest.firstOrNull()?.isDigit() == true || trimRest.startsWith("-") -> numColor
                    trimRest.startsWith("true") || trimRest.startsWith("false") || trimRest.startsWith("null") -> kwColor
                    else -> defColor
                }
                pushStyle(androidx.compose.ui.text.SpanStyle(color = c)); append(rest); pop()
                return@buildAnnotatedString
            }
        }

        // HTML/XML
        if (isHtml) {
            while (i < len) {
                when {
                    i + 3 < len && line[i] == '<' && line[i + 1] == '!' && line[i + 2] == '-' && line[i + 3] == '-' -> {
                        val end = line.indexOf("-->", i)
                        val to = if (end >= 0) minOf(end + 3, len) else len
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = commentColor)); append(line.substring(i, to)); pop(); i = to
                    }
                    line[i] == '<' -> {
                        val end = line.indexOf('>', i)
                        val to = if (end >= 0) minOf(end + 1, len) else len
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = tagColor)); append(line.substring(i, to)); pop(); i = to
                    }
                    line[i] == '"' || line[i] == '\'' -> {
                        val q = line[i]; val start = i; i++
                        while (i < len && line[i] != q) i++
                        if (i < len) i++
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = strColor)); append(line.substring(start, minOf(i, len))); pop()
                    }
                    else -> { pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(line[i]); pop(); i++ }
                }
            }
            return@buildAnnotatedString
        }

        // CSS
        if (isCss) {
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("/*") || trimmed.startsWith("*") -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = commentColor)); append(line); pop()
                }
                trimmed.startsWith("//") -> {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = commentColor)); append(line); pop()
                }
                trimmed.contains(":") && trimmed.contains(";") -> {
                    val colon = line.indexOf(':')
                    if (colon in 0 until len) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = attrColor)); append(line.substring(0, colon)); pop()
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(":"); pop()
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = numColor)); append(line.substring(colon + 1)); pop()
                    } else { pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(line); pop() }
                }
                else -> { pushStyle(androidx.compose.ui.text.SpanStyle(color = tagColor)); append(line); pop() }
            }
            return@buildAnnotatedString
        }

        // General code
        val commentStart = findSafeCommentStart(line, isPy)

        while (i < len) {
            if (commentStart >= 0 && i >= commentStart) {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = commentColor)); append(line.substring(i, len)); pop(); break
            }
            when {
                line[i] == '"' || line[i] == '\'' -> {
                    val q = line[i]; val start = i; i++
                    while (i < len && line[i] != q) {
                        if (line[i] == '\\' && i + 1 < len) i++ // skip escaped char safely
                        i++
                    }
                    if (i < len) i++ // closing quote
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = strColor)); append(line.substring(start, minOf(i, len))); pop()
                }
                line[i].isDigit() && (i == 0 || !line[i - 1].isLetterOrDigit()) -> {
                    val start = i
                    while (i < len && (line[i].isDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'f' || line[i] == 'L')) i++
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = numColor)); append(line.substring(start, minOf(i, len))); pop()
                }
                line[i].isLetter() || line[i] == '_' -> {
                    val start = i
                    while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    val word = line.substring(start, minOf(i, len))
                    val color = when {
                        word in defaultKeywords -> kwColor
                        word.firstOrNull()?.isUpperCase() == true && word.length > 1 -> typeColor
                        else -> defColor
                    }
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = color)); append(word); pop()
                }
                line[i] == '@' -> {
                    val start = i; i++
                    while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = palette.syntaxFlag)); append(line.substring(start, minOf(i, len))); pop()
                }
                else -> { pushStyle(androidx.compose.ui.text.SpanStyle(color = defColor)); append(line[i]); pop(); i++ }
            }
        }
    }
}

internal fun findSafeCommentStart(line: String, isPython: Boolean): Int {
    var i = 0; var inStr = false; var q = ' '
    val len = line.length
    while (i < len) {
        val c = line[i]
        if (!inStr && (c == '"' || c == '\'')) { inStr = true; q = c }
        else if (inStr && c == q && (i == 0 || line[i - 1] != '\\')) inStr = false
        else if (!inStr) {
            if (i + 1 < len && c == '/' && line[i + 1] == '/') return i
            if (isPython && c == '#') return i
        }
        i++
    }
    return -1
}
