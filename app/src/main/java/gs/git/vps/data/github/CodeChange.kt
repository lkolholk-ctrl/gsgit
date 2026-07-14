package gs.git.vps.data.github

/**
 * One uncommitted Code-workspace operation. The key used by the UI/store is always [path], which is
 * the final visible path. A rename additionally keeps [CodeRename.oldPath] so one batch commit can
 * remove the old tree entry and add the new one atomically.
 */
internal sealed interface CodeChange {
    val path: String
    val kind: CodeChangeKind
}

internal enum class CodeChangeKind(val marker: String) {
    ADD("A"),
    MODIFY("M"),
    DELETE("D"),
    RENAME("R"),
}

/** New file, or a copied file whose blob can be reused until it is edited. */
internal data class CodeAdd(
    override val path: String,
    val content: String? = "",
    val sourcePath: String? = null,
    val sourceSha: String? = null,
) : CodeChange {
    override val kind: CodeChangeKind = CodeChangeKind.ADD
}

internal data class CodeModify(
    override val path: String,
    val content: String,
) : CodeChange {
    override val kind: CodeChangeKind = CodeChangeKind.MODIFY
}

internal data class CodeDelete(
    override val path: String,
    /** Previous draft operation restored when this deletion is discarded. */
    val restore: CodeChange? = null,
) : CodeChange {
    override val kind: CodeChangeKind = CodeChangeKind.DELETE
}

/** Existing file moved from [oldPath] to [path]. [sourceSha] avoids downloading unchanged blobs. */
internal data class CodeRename(
    val oldPath: String,
    override val path: String,
    val content: String? = null,
    val sourceSha: String? = null,
) : CodeChange {
    override val kind: CodeChangeKind = CodeChangeKind.RENAME
}

internal fun CodeChange.withEditedContent(newContent: String): CodeChange = when (this) {
    is CodeAdd -> copy(content = newContent)
    is CodeModify -> copy(content = newContent)
    is CodeRename -> copy(content = newContent)
    is CodeDelete -> restore?.withEditedContent(newContent) ?: CodeModify(path, newContent)
}

/** Remote path to read when an add/rename still reuses the original blob. */
internal fun CodeChange.contentSourcePath(): String = when (this) {
    is CodeAdd -> sourcePath ?: path
    is CodeRename -> oldPath
    else -> path
}

internal fun CodeChange.draftContentOrNull(): String? = when (this) {
    is CodeAdd -> content
    is CodeModify -> content
    is CodeRename -> content
    is CodeDelete -> null
}
