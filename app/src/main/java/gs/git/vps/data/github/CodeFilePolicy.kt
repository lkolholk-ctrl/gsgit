package gs.git.vps.data.github

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal const val CODE_TEXT_MAX_BYTES = 1_000_000L
internal const val CODE_IMAGE_MAX_BYTES = 8_000_000L

internal enum class CodeFileKind {
    TEXT,
    MARKDOWN,
    JSON,
    SVG,
    IMAGE,
    BINARY,
}

internal data class CodeFilePolicy(
    val kind: CodeFileKind,
    val editable: Boolean,
    val previewable: Boolean,
    val maxBytes: Long,
)

internal class CodeFileRejectedException(
    message: String,
    val actualBytes: Long? = null,
) : IllegalStateException(message)

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico")
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown")
private val BINARY_EXTENSIONS = setOf(
    "tiff", "heic", "psd", "ai", "sketch", "fig", "pdf",
    "zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar", "jar", "apk", "aab", "war", "aar",
    "exe", "dll", "so", "o", "a", "class", "dex", "bin", "dat", "db", "sqlite", "realm", "lock",
    "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "mp4", "mkv", "mov", "avi", "webm", "m4v",
    "ttf", "otf", "woff", "woff2", "eot", "glb", "gltf", "obj", "fbx", "blend", "wasm",
)

internal fun codeFilePolicy(name: String): CodeFilePolicy {
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        extension in IMAGE_EXTENSIONS -> CodeFilePolicy(CodeFileKind.IMAGE, false, true, CODE_IMAGE_MAX_BYTES)
        extension == "svg" -> CodeFilePolicy(CodeFileKind.SVG, true, true, CODE_TEXT_MAX_BYTES)
        extension in MARKDOWN_EXTENSIONS -> CodeFilePolicy(CodeFileKind.MARKDOWN, true, true, CODE_TEXT_MAX_BYTES)
        extension == "json" -> CodeFilePolicy(CodeFileKind.JSON, true, true, CODE_TEXT_MAX_BYTES)
        extension in BINARY_EXTENSIONS -> CodeFilePolicy(CodeFileKind.BINARY, false, false, 0L)
        else -> CodeFilePolicy(CodeFileKind.TEXT, true, false, CODE_TEXT_MAX_BYTES)
    }
}

internal fun codeFileGuardMessage(name: String, size: Long): String? {
    val policy = codeFilePolicy(name)
    if (policy.kind == CodeFileKind.BINARY) {
        return "Binary preview is not supported. The file was not loaded into memory."
    }
    if (size > 0L && size > policy.maxBytes) {
        return "File is ${formatCodeFileBytes(size)}; safe ${if (policy.kind == CodeFileKind.IMAGE) "preview" else "editor"} limit is ${formatCodeFileBytes(policy.maxBytes)}. The file was not loaded into memory."
    }
    return null
}

internal fun decodeCodeText(bytes: ByteArray): String {
    if (bytes.any { it == 0.toByte() }) {
        throw CodeFileRejectedException("Binary content detected (NUL byte). The file was not opened as text.", bytes.size.toLong())
    }
    val controls = bytes.count { byte ->
        val value = byte.toInt() and 0xff
        value < 0x20 && value != 0x09 && value != 0x0a && value != 0x0c && value != 0x0d
    }
    if (bytes.isNotEmpty() && controls * 100 > bytes.size * 2) {
        throw CodeFileRejectedException("Binary content detected (control bytes). The file was not opened as text.", bytes.size.toLong())
    }
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        throw CodeFileRejectedException("The file is not valid UTF-8 and cannot be edited safely.", bytes.size.toLong())
    }
}

internal fun formatCodeFileBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
