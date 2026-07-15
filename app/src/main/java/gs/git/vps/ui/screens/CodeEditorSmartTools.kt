package gs.git.vps.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.json.JSONArray
import org.json.JSONObject

internal enum class EditorDiagnosticSeverity { ERROR, WARNING }

internal data class EditorDiagnostic(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: EditorDiagnosticSeverity,
)

internal data class EditorFormatResult(
    val text: String,
    val label: String,
    val error: String? = null,
)

private data class CommentSyntax(
    val line: String? = null,
    val blockOpen: String? = null,
    val blockClose: String? = null,
)

private val PAIRS = mapOf('(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'', '`' to '`')
private val CLOSERS = PAIRS.values.toSet() + '>'
private val BRACE_LANGUAGE_EXTS = setOf(
    "kt", "kts", "java", "js", "jsx", "ts", "tsx", "dart", "c", "cc", "cpp", "h", "hpp",
    "cs", "swift", "go", "rs", "scala", "gradle", "groovy", "php", "css", "scss", "less",
)
private val MARKUP_EXTS = setOf("xml", "html", "htm", "xhtml", "svg")

/**
 * Applies one keyboard edit and enriches it with IDE-like pair handling and indentation.
 * The function is deterministic and never modifies more than the single edit around the cursor.
 */
internal fun applySmartEditorInput(
    old: TextFieldValue,
    incoming: TextFieldValue,
    ext: String,
    indentUnit: String,
): TextFieldValue {
    if (!old.selection.collapsed && incoming.selection.collapsed) {
        wrapSelectionIfPair(old, incoming, ext)?.let { return it }
    }

    if (old.selection.collapsed && incoming.selection.collapsed) {
        deleteMatchingPair(old, incoming, ext)?.let { return it }
        skipExistingCloser(old, incoming)?.let { return it }
        autoCloseTypedPair(old, incoming, ext)?.let { return it }
        indentTypedCloser(old, incoming, indentUnit)?.let { return it }
        smartNewLine(old, incoming, ext, indentUnit)?.let { return it }
    }
    return incoming
}

private fun wrapSelectionIfPair(old: TextFieldValue, incoming: TextFieldValue, ext: String): TextFieldValue? {
    val start = minOf(old.selection.start, old.selection.end)
    val end = maxOf(old.selection.start, old.selection.end)
    val selected = old.text.substring(start, end)
    val expectedLength = old.text.length - selected.length + 1
    if (incoming.text.length != expectedLength) return null
    val typedIndex = incoming.selection.start - 1
    val typed = incoming.text.getOrNull(typedIndex) ?: return null
    val close = pairFor(typed, ext) ?: return null
    val wrapped = old.text.replaceRange(start, end, "$typed$selected$close")
    return TextFieldValue(wrapped, TextRange(start + 1, start + 1 + selected.length))
}

private fun deleteMatchingPair(old: TextFieldValue, incoming: TextFieldValue, ext: String): TextFieldValue? {
    if (old.text.length != incoming.text.length + 1) return null
    val oldCursor = old.selection.start
    val newCursor = incoming.selection.start
    if (oldCursor <= 0 || newCursor != oldCursor - 1) return null
    val deleted = old.text.getOrNull(oldCursor - 1) ?: return null
    val expectedClose = pairFor(deleted, ext) ?: return null
    if (old.text.getOrNull(oldCursor) != expectedClose) return null
    val closeIndex = newCursor
    if (incoming.text.getOrNull(closeIndex) != expectedClose) return null
    val text = incoming.text.removeRange(closeIndex, closeIndex + 1)
    return TextFieldValue(text, TextRange(newCursor))
}

private fun skipExistingCloser(old: TextFieldValue, incoming: TextFieldValue): TextFieldValue? {
    if (incoming.text.length != old.text.length + 1) return null
    val cursor = old.selection.start
    val typed = incoming.text.getOrNull(incoming.selection.start - 1) ?: return null
    if (typed !in CLOSERS || old.text.getOrNull(cursor) != typed) return null
    if (incoming.selection.start != cursor + 1) return null
    return old.copy(selection = TextRange(cursor + 1))
}

private fun autoCloseTypedPair(old: TextFieldValue, incoming: TextFieldValue, ext: String): TextFieldValue? {
    if (incoming.text.length != old.text.length + 1) return null
    val insertedAt = incoming.selection.start - 1
    if (insertedAt < 0 || insertedAt != old.selection.start) return null
    val typed = incoming.text[insertedAt]
    val close = pairFor(typed, ext) ?: return null
    val next = old.text.getOrNull(old.selection.start)
    if (typed == '"' || typed == '\'' || typed == '`') {
        if (isEscaped(old.text, old.selection.start)) return null
        if (next?.isLetterOrDigit() == true) return null
    } else if (next != null && !next.isWhitespace() && next !in CLOSERS && next !in setOf(',', ';', ':', '.')) {
        return null
    }
    val text = incoming.text.substring(0, incoming.selection.start) + close + incoming.text.substring(incoming.selection.start)
    return TextFieldValue(text, TextRange(incoming.selection.start))
}

private fun indentTypedCloser(old: TextFieldValue, incoming: TextFieldValue, indentUnit: String): TextFieldValue? {
    if (incoming.text.length != old.text.length + 1 || indentUnit.isEmpty()) return null
    val typedAt = incoming.selection.start - 1
    val typed = incoming.text.getOrNull(typedAt) ?: return null
    if (typed !in setOf('}', ']', ')')) return null
    val lineStart = incoming.text.lastIndexOf('\n', typedAt - 1).let { if (it < 0) 0 else it + 1 }
    val prefix = incoming.text.substring(lineStart, typedAt)
    if (prefix.isEmpty() || prefix.any { !it.isWhitespace() }) return null
    val reduced = when {
        prefix.endsWith(indentUnit) -> prefix.dropLast(indentUnit.length)
        prefix.lastOrNull() == '\t' -> prefix.dropLast(1)
        else -> return null
    }
    val text = incoming.text.replaceRange(lineStart, typedAt, reduced)
    return TextFieldValue(text, TextRange(lineStart + reduced.length + 1))
}

private fun smartNewLine(old: TextFieldValue, incoming: TextFieldValue, ext: String, indentUnit: String): TextFieldValue? {
    if (incoming.text.length != old.text.length + 1) return null
    val cursor = incoming.selection.start
    if (cursor <= 0 || incoming.text.getOrNull(cursor - 1) != '\n') return null
    val oldCursor = old.selection.start
    val lineStart = if (oldCursor <= 0) 0 else {
        old.text.lastIndexOf('\n', oldCursor - 1).let { if (it < 0) 0 else it + 1 }
    }
    val beforeCursor = old.text.substring(lineStart, oldCursor)
    val baseIndent = beforeCursor.takeWhile { it == ' ' || it == '\t' }
    val trimmedBefore = beforeCursor.trimEnd()
    val opener = trimmedBefore.lastOrNull()
    val matchingClose = opener?.let { PAIRS[it] }
    val next = old.text.getOrNull(oldCursor)
    val opensBlock = matchingClose != null || when (ext) {
        "py", "python", "yaml", "yml" -> trimmedBefore.endsWith(":")
        "sh", "bash" -> trimmedBefore.endsWith("then") || trimmedBefore.endsWith("do")
        else -> false
    }
    val innerIndent = if (opensBlock) baseIndent + indentUnit else baseIndent
    if (matchingClose != null && next == matchingClose) {
        val insert = innerIndent + "\n" + baseIndent
        val text = incoming.text.substring(0, cursor) + insert + incoming.text.substring(cursor)
        return TextFieldValue(text, TextRange(cursor + innerIndent.length))
    }
    if (innerIndent.isEmpty()) return null
    val text = incoming.text.substring(0, cursor) + innerIndent + incoming.text.substring(cursor)
    return TextFieldValue(text, TextRange(cursor + innerIndent.length))
}

private fun pairFor(char: Char, ext: String): Char? = when {
    char == '<' && ext in MARKUP_EXTS -> '>'
    char == '`' && ext !in setOf("kt", "kts", "js", "jsx", "ts", "tsx", "md", "markdown") -> null
    else -> PAIRS[char]
}

private fun isEscaped(text: String, index: Int): Boolean {
    var slashes = 0
    var cursor = index - 1
    while (cursor >= 0 && text[cursor] == '\\') { slashes++; cursor-- }
    return slashes % 2 == 1
}

internal fun toggleEditorComment(state: TextFieldValue, ext: String): TextFieldValue {
    val syntax = commentSyntax(ext) ?: return state
    val text = state.text
    val rawStart = minOf(state.selection.start, state.selection.end).coerceIn(0, text.length)
    val rawEnd = maxOf(state.selection.start, state.selection.end).coerceIn(0, text.length)

    if (syntax.line != null) {
        val start = if (rawStart <= 0) 0 else {
            text.lastIndexOf('\n', rawStart - 1).let { if (it < 0) 0 else it + 1 }
        }
        val adjustedEnd = if (rawEnd > rawStart && rawEnd > 0 && text.getOrNull(rawEnd - 1) == '\n') rawEnd - 1 else rawEnd
        val end = text.indexOf('\n', adjustedEnd).let { if (it < 0) text.length else it }
        val block = text.substring(start, end)
        val lines = block.split('\n')
        val prefix = syntax.line
        val uncomment = lines.filter { it.isNotBlank() }.all { it.trimStart().startsWith(prefix) }
        val transformed = lines.joinToString("\n") { line ->
            if (line.isBlank()) line else {
                val indent = line.takeWhile { it == ' ' || it == '\t' }
                val body = line.removePrefix(indent)
                if (uncomment) indent + body.removePrefix(prefix).removePrefix(" ")
                else "$indent$prefix $body"
            }
        }
        val newText = text.replaceRange(start, end, transformed)
        val newSelection = if (state.selection.collapsed) {
            val delta = transformed.length - block.length
            TextRange((state.selection.start + delta).coerceIn(start, start + transformed.length))
        } else TextRange(start, start + transformed.length)
        return TextFieldValue(newText, newSelection)
    }

    val open = syntax.blockOpen ?: return state
    val close = syntax.blockClose ?: return state
    val start = if (state.selection.collapsed) {
        if (rawStart <= 0) 0 else {
            text.lastIndexOf('\n', rawStart - 1).let { if (it < 0) 0 else it + 1 }
        }
    } else rawStart
    val end = if (state.selection.collapsed) text.indexOf('\n', rawEnd).let { if (it < 0) text.length else it } else rawEnd
    val selected = text.substring(start, end)
    val trimmed = selected.trim()
    val transformed = if (trimmed.startsWith(open) && trimmed.endsWith(close)) {
        val leading = selected.indexOf(trimmed).coerceAtLeast(0)
        val content = trimmed.removePrefix(open).removeSuffix(close).trim()
        selected.substring(0, leading) + content
    } else "$open $selected $close"
    val newText = text.replaceRange(start, end, transformed)
    return TextFieldValue(newText, TextRange(start, start + transformed.length))
}

private fun commentSyntax(ext: String): CommentSyntax? = when (ext) {
    "kt", "kts", "java", "js", "jsx", "ts", "tsx", "dart", "c", "cc", "cpp", "h", "hpp",
    "cs", "swift", "go", "rs", "scala", "gradle", "groovy", "php" -> CommentSyntax("//", "/*", "*/")
    "py", "python", "sh", "bash", "yaml", "yml", "toml", "ini", "properties", "rb", "pl" -> CommentSyntax("#")
    "sql", "lua" -> CommentSyntax("--")
    "css", "scss", "sass", "less" -> CommentSyntax(blockOpen = "/*", blockClose = "*/")
    "xml", "html", "htm", "xhtml", "svg", "md", "markdown" -> CommentSyntax(blockOpen = "<!--", blockClose = "-->")
    else -> null
}

internal fun formatEditorDocument(text: String, ext: String, indentUnit: String): EditorFormatResult {
    if (text.isBlank()) return EditorFormatResult(text, "empty file")
    if (ext == "json") {
        return try {
            val trimmed = text.trim()
            val formatted = when {
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(indentUnit.length.coerceAtLeast(2))
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(indentUnit.length.coerceAtLeast(2))
                else -> return EditorFormatResult(text, "JSON", "JSON root must be an object or array")
            }
            EditorFormatResult(formatted + if (text.endsWith('\n')) "\n" else "", "JSON")
        } catch (e: Exception) {
            EditorFormatResult(text, "JSON", e.message ?: "Invalid JSON")
        }
    }
    val normalized = when {
        ext in BRACE_LANGUAGE_EXTS -> formatBraceLanguage(text, indentUnit)
        ext in MARKUP_EXTS -> formatMarkupLines(text, indentUnit)
        else -> text.lines().joinToString("\n") { it.trimEnd() }
    }
    val label = when {
        ext in BRACE_LANGUAGE_EXTS -> "${ext.ifBlank { "code" }.uppercase()} indentation"
        ext in MARKUP_EXTS -> "${ext.uppercase()} indentation"
        else -> "trailing whitespace"
    }
    return EditorFormatResult(normalized, label)
}

private fun formatBraceLanguage(text: String, indentUnit: String): String {
    var depth = 0
    var inBlockComment = false
    var inTripleQuote = false
    var inBacktick = false
    return text.lines().joinToString("\n") { original ->
        val preserveLiteralLine = inTripleQuote || inBacktick
        val trimmed = original.trim()
        if (trimmed.isEmpty() && !preserveLiteralLine) return@joinToString ""
        val structure = structuralChars(
            if (preserveLiteralLine) original else trimmed,
            inBlockComment,
            inTripleQuote,
            inBacktick,
        )
        inBlockComment = structure.blockComment
        inTripleQuote = structure.tripleQuote
        inBacktick = structure.backtick
        val chars = structure.chars
        if (preserveLiteralLine) return@joinToString original
        val leadingClosers = chars.takeWhile { it == '}' || it == ']' || it == ')' }.size
        val lineDepth = (depth - leadingClosers).coerceAtLeast(0)
        val result = indentUnit.repeat(lineDepth) + trimmed
        val opens = chars.count { it == '{' || it == '[' || it == '(' }
        val closes = chars.count { it == '}' || it == ']' || it == ')' }
        depth = (depth + opens - closes).coerceAtLeast(0)
        result
    }
}

private data class StructuralScan(
    val chars: List<Char>,
    val blockComment: Boolean,
    val tripleQuote: Boolean,
    val backtick: Boolean,
)

private fun structuralChars(
    line: String,
    initialBlockComment: Boolean,
    initialTripleQuote: Boolean,
    initialBacktick: Boolean,
): StructuralScan {
    val result = mutableListOf<Char>()
    var quote: Char? = null
    var escaped = false
    var blockComment = initialBlockComment
    var tripleQuote = initialTripleQuote
    var backtick = initialBacktick
    var i = 0
    while (i < line.length) {
        val c = line[i]
        val next = line.getOrNull(i + 1)
        if (tripleQuote) {
            if (line.startsWith("\"\"\"", i)) { tripleQuote = false; i += 3 } else i++
            continue
        }
        if (backtick) {
            if (!escaped && c == '`') backtick = false
            escaped = !escaped && c == '\\'
            if (c != '\\') escaped = false
            i++
            continue
        }
        if (blockComment) {
            if (c == '*' && next == '/') { blockComment = false; i += 2 } else i++
            continue
        }
        if (quote != null) {
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == quote) quote = null
            i++
            continue
        }
        if (c == '/' && next == '/') break
        if (c == '/' && next == '*') { blockComment = true; i += 2; continue }
        if (line.startsWith("\"\"\"", i)) { tripleQuote = true; i += 3; continue }
        if (c == '`') { backtick = true; escaped = false; i++; continue }
        if (c == '"' || c == '\'') { quote = c; i++; continue }
        if (c in setOf('{', '}', '[', ']', '(', ')')) result += c
        i++
    }
    return StructuralScan(result, blockComment, tripleQuote, backtick)
}

private fun formatMarkupLines(text: String, indentUnit: String): String {
    var depth = 0
    return text.lines().joinToString("\n") { original ->
        val trimmed = original.trim()
        if (trimmed.isEmpty()) return@joinToString ""
        val closes = trimmed.startsWith("</")
        if (closes) depth = (depth - 1).coerceAtLeast(0)
        val formatted = indentUnit.repeat(depth) + trimmed
        val declaration = trimmed.startsWith("<!") || trimmed.startsWith("<?")
        val opens = trimmed.startsWith("<") && trimmed.contains('>') && !closes && !declaration &&
            !trimmed.endsWith("/>") && !trimmed.contains("</")
        if (opens) depth++
        formatted
    }
}

internal fun analyzeEditorText(text: String, ext: String): List<EditorDiagnostic> {
    if (text.length > 1_000_000) return emptyList()
    val diagnostics = mutableListOf<EditorDiagnostic>()
    text.lines().forEachIndexed { index, line ->
        if (line.endsWith(' ') || line.endsWith('\t')) diagnostics += EditorDiagnostic(
            index + 1, line.length, "Trailing whitespace", EditorDiagnosticSeverity.WARNING,
        )
        if (ext in setOf("yaml", "yml") && '\t' in line.takeWhile(Char::isWhitespace)) diagnostics += EditorDiagnostic(
            index + 1, 1, "YAML indentation must use spaces", EditorDiagnosticSeverity.ERROR,
        )
        if (line.startsWith("<<<<<<<") || line.startsWith("=======") || line.startsWith(">>>>>>>")) diagnostics += EditorDiagnostic(
            index + 1, 1, "Unresolved merge conflict marker", EditorDiagnosticSeverity.ERROR,
        )
    }
    if (ext == "json" && text.isNotBlank()) {
        try {
            val trimmed = text.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> diagnostics += EditorDiagnostic(1, 1, "JSON root must be an object or array", EditorDiagnosticSeverity.ERROR)
            }
        } catch (e: Exception) {
            val position = Regex("character (\\d+)").find(e.message.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            val location = position?.let { offsetToLineColumn(text, it) } ?: (1 to 1)
            diagnostics += EditorDiagnostic(location.first, location.second, e.message ?: "Invalid JSON", EditorDiagnosticSeverity.ERROR)
        }
    } else if (ext in BRACE_LANGUAGE_EXTS) {
        diagnostics += analyzeBalancedBraces(text)
    } else if (ext in MARKUP_EXTS) {
        diagnostics += analyzeMarkupTags(text, ext == "html" || ext == "htm")
    }
    return diagnostics.distinctBy { Triple(it.line, it.column, it.message) }
        .sortedWith(compareBy<EditorDiagnostic> { it.line }.thenBy { it.column })
        .take(50)
}

private fun analyzeBalancedBraces(text: String): List<EditorDiagnostic> {
    data class Open(val char: Char, val line: Int, val column: Int)
    val stack = mutableListOf<Open>()
    val diagnostics = mutableListOf<EditorDiagnostic>()
    var blockComment = false
    var tripleQuote = false
    var backtick = false
    text.lines().forEachIndexed { lineIndex, line ->
        val chars = structuralChars(line, blockComment, tripleQuote, backtick)
        blockComment = chars.blockComment
        tripleQuote = chars.tripleQuote
        backtick = chars.backtick
        var searchFrom = 0
        chars.chars.forEach { char ->
            val column = line.indexOf(char, searchFrom).let { if (it < 0) searchFrom else it }
            searchFrom = column + 1
            if (char == '{' || char == '[' || char == '(') stack += Open(char, lineIndex + 1, column + 1)
            else {
                val expected = when (char) { '}' -> '{'; ']' -> '['; else -> '(' }
                val last = stack.lastOrNull()
                if (last?.char == expected) stack.removeAt(stack.lastIndex)
                else diagnostics += EditorDiagnostic(lineIndex + 1, column + 1, "Unexpected '$char'", EditorDiagnosticSeverity.ERROR)
            }
        }
    }
    stack.takeLast(20).forEach { open -> diagnostics += EditorDiagnostic(
        open.line, open.column, "Unclosed '${open.char}'", EditorDiagnosticSeverity.ERROR,
    ) }
    return diagnostics
}

private fun analyzeMarkupTags(text: String, html: Boolean): List<EditorDiagnostic> {
    val voidTags = if (html) setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr") else emptySet()
    val stack = mutableListOf<Pair<String, Int>>()
    val diagnostics = mutableListOf<EditorDiagnostic>()
    val tagRegex = Regex("<(/?)([A-Za-z][A-Za-z0-9:_-]*)(?:\\s[^<>]*?)?(/?)>", RegexOption.DOT_MATCHES_ALL)
    tagRegex.findAll(text).forEach { match ->
        val closing = match.groupValues[1] == "/"
        val name = match.groupValues[2].lowercase()
        val selfClosing = match.groupValues[3] == "/" || name in voidTags
        if (selfClosing) return@forEach
        val location = offsetToLineColumn(text, match.range.first)
        if (!closing) stack += name to location.first
        else if (stack.lastOrNull()?.first == name) stack.removeAt(stack.lastIndex)
        else diagnostics += EditorDiagnostic(location.first, location.second, "Unexpected closing tag </$name>", EditorDiagnosticSeverity.ERROR)
    }
    stack.takeLast(20).forEach { (name, line) -> diagnostics += EditorDiagnostic(
        line, 1, "Unclosed tag <$name>", EditorDiagnosticSeverity.ERROR,
    ) }
    return diagnostics
}

private fun offsetToLineColumn(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    val before = text.substring(0, safe)
    val line = before.count { it == '\n' } + 1
    val column = safe - before.lastIndexOf('\n')
    return line to column
}
