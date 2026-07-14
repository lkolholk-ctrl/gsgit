package gs.git.vps.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModulePrimaryButton
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

private enum class CodeCreateKind { FILE, FOLDER }

@Composable
internal fun CodeCreateEntryDialog(
    parentPath: String,
    existingNames: Set<String>,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    var kind by remember { mutableStateOf(CodeCreateKind.FILE) }
    var value by remember { mutableStateOf("") }
    val target = joinCodePath(parentPath, value)
    val error = when {
        value.isBlank() -> null
        target == null -> "invalid path"
        '/' !in value.trim().trim('/') && value.trim().trim('/') in existingNames -> "name already exists"
        else -> null
    }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "new entry",
        confirmButton = {
            AiModulePrimaryButton(
                label = if (kind == CodeCreateKind.FILE) "create file" else "create folder",
                enabled = target != null && error == null,
                onClick = {
                    val safeTarget = target ?: return@AiModulePrimaryButton
                    if (kind == CodeCreateKind.FILE) onCreateFile(safeTarget) else onCreateFolder(safeTarget)
                },
            )
        },
        dismissButton = { AiModuleSecondaryButton(label = "cancel", onClick = onDismiss) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GitHubTerminalButton(
                        label = "file",
                        onClick = { kind = CodeCreateKind.FILE },
                        color = if (kind == CodeCreateKind.FILE) palette.accent else palette.textMuted,
                    )
                    GitHubTerminalButton(
                        label = "folder",
                        onClick = { kind = CodeCreateKind.FOLDER },
                        color = if (kind == CodeCreateKind.FOLDER) palette.accent else palette.textMuted,
                    )
                }
                GitHubTerminalTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = if (kind == CodeCreateKind.FILE) "name or relative/path.kt" else "folder name",
                    singleLine = true,
                    minHeight = 42.dp,
                )
                AiModuleText(
                    when {
                        error != null -> error
                        target != null -> "/$target"
                        parentPath.isBlank() -> "repository root"
                        else -> "/$parentPath",
                    color = if (error != null) palette.error else palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                if (kind == CodeCreateKind.FOLDER) {
                    AiModuleText(
                        "Git does not store empty folders; a .gitkeep file will be added.",
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                }
            }
        },
    )
}

@Composable
internal fun CodeEntryActionsDialog(
    item: GHContent,
    onRenameOrMove: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    var target by remember(item.path) { mutableStateOf(item.path) }
    val normalized = normalizeCodePath(target)
    val unchanged = normalized == item.path
    val duplicateTarget = if (!unchanged && normalized != null) normalized else suggestCodeCopyPath(item.path)

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = item.name,
        confirmButton = {
            AiModulePrimaryButton(
                label = "rename / move",
                enabled = normalized != null && !unchanged,
                onClick = { normalized?.let(onRenameOrMove) },
            )
        },
        dismissButton = { AiModuleSecondaryButton(label = "close", onClick = onDismiss) },
        content = {
            Column {
                AiModuleText(
                    "repository path",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(6.dp))
                GitHubTerminalTextField(
                    value = target,
                    onValueChange = { target = it },
                    placeholder = "path/to/${item.name}",
                    singleLine = true,
                    minHeight = 42.dp,
                )
                if (target.isNotBlank() && normalized == null) {
                    AiModuleText(
                        "invalid path",
                        color = palette.error,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GitHubTerminalButton(
                        label = "duplicate",
                        onClick = { onDuplicate(duplicateTarget) },
                        color = palette.accent,
                    )
                    GitHubTerminalButton(label = "delete", onClick = onRequestDelete, color = palette.error)
                }
            }
        },
    )
}

@Composable
internal fun CodeDeleteEntryDialog(item: GHContent, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val palette = AiModuleTheme.colors
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "delete ${item.name}?",
        confirmButton = { AiModulePrimaryButton(label = "delete", onClick = onDelete) },
        dismissButton = { AiModuleSecondaryButton(label = "cancel", onClick = onDismiss) },
        content = {
            AiModuleText(
                "The deletion stays in the Code draft until commit and can be discarded.",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
            )
        },
    )
}

internal fun normalizeCodePath(raw: String): String? {
    val clean = raw.trim().replace('\\', '/').trim('/')
    if (clean.isBlank() || clean.any { it == '\n' || it == '\r' || it == '\u0000' }) return null
    val segments = clean.split('/')
    if (segments.any { it.isBlank() || it == "." || it == ".." }) return null
    return segments.joinToString("/")
}

private fun joinCodePath(parent: String, child: String): String? {
    val normalizedChild = normalizeCodePath(child) ?: return null
    return normalizeCodePath(listOf(parent.trim('/'), normalizedChild).filter { it.isNotBlank() }.joinToString("/"))
}

private fun suggestCodeCopyPath(path: String): String {
    val parent = path.substringBeforeLast('/', "")
    val name = path.substringAfterLast('/')
    val dot = name.lastIndexOf('.')
    val copyName = if (dot > 0) "${name.substring(0, dot)}_copy${name.substring(dot)}" else "${name}_copy"
    return listOf(parent, copyName).filter { it.isNotBlank() }.joinToString("/")
}
