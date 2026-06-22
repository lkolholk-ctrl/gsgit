package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * Шит сообщения для батч-коммита черновика Code-таба (Стадия 4 + P1-полиш). Показывает свёрнутый
 * список изменённых файлов (M-чип + имя, первые [PREVIEW_LIMIT] + «+N more»), собирает сообщение и
 * запускает один коммит на [fileCount] файлов в [branch]. Пока [committing] — кнопки заблокированы.
 */
private const val PREVIEW_LIMIT = 5

@Composable
internal fun CodeCommitSheet(
    fileCount: Int,
    paths: Set<String>,
    branch: String,
    committing: Boolean,
    onCommit: (message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val plural = if (fileCount == 1) "file" else "files"
    var message by remember { mutableStateOf("Update $fileCount $plural") }
    val preview = remember(paths) { paths.sorted() }

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
                Spacer(Modifier.height(10.dp))
                // Свёрнутый список изменённых файлов (M-чип + имя).
                preview.take(PREVIEW_LIMIT).forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(5.dp)).background(palette.warning.copy(alpha = 0.16f)).padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            AiModuleText("M", color = palette.warning, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        AiModuleText(
                            p.substringAfterLast('/'),
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (preview.size > PREVIEW_LIMIT) {
                    AiModuleText(
                        "+${preview.size - PREVIEW_LIMIT} more",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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
