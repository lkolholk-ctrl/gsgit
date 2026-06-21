package gs.git.vps.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModulePrimaryButton
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Шит сообщения для батч-коммита черновика Code-таба (Стадия 4). Собирает сообщение и запускает
 * один коммит на [fileCount] файлов в текущую ветку. Пока [committing] — кнопки заблокированы.
 */
@Composable
internal fun CodeCommitSheet(
    fileCount: Int,
    branch: String,
    committing: Boolean,
    onCommit: (message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val plural = if (fileCount == 1) "file" else "files"
    var message by remember { mutableStateOf("Update $fileCount $plural") }

    AiModuleAlertDialog(
        onDismissRequest = { if (!committing) onDismiss() },
        title = "commit $fileCount $plural",
        confirmButton = {
            AiModulePrimaryButton(
                label = if (committing) "committing…" else "commit",
                enabled = !committing && message.isNotBlank(),
                onClick = { onCommit(message.trim()) },
            )
        },
        dismissButton = {
            AiModuleSecondaryButton(label = "cancel", enabled = !committing, onClick = onDismiss)
        },
        content = {
            Column {
                AiModuleText(
                    "One commit, $fileCount $plural → $branch.",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                AiModuleTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = "commit message",
                    enabled = !committing,
                )
            }
        },
    )
}
