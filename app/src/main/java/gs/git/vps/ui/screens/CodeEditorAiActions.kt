package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.Strings
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Быстрые AI-действия в CodeEditor: AiQuickActionsRow — чипы с готовыми промптами,
 * улетающими в AI Agent через onSendPrompt. Copilot-клиент удалён вместе с PAT.
 */

/** Horizontal row of one-tap prompt presets shown under the editor's
 * info strip. Each chip launches the AI Agent with a prompt template
 * scoped to the active file + branch. */
@Composable
internal fun AiQuickActionsRow(
    filePath: String,
    branch: String,
    selectedText: String,
    onSendPrompt: (String) -> Unit,
) {
    val colors = AiModuleTheme.colors
    val actions = listOf(
        Strings.aiAgentQuickExplain to "Read `$filePath` on branch `$branch` and explain what it does, including the key types and side effects.",
        Strings.aiAgentQuickAddTests to "Add unit tests for `$filePath` (branch `$branch`). Match the project's existing test framework and naming style. Read the file first to understand the scope.",
        Strings.aiAgentQuickFixLint to "Read `$filePath` on branch `$branch` and fix any lint, formatting, or unused-import issues. Use edit_file for surgical changes; preserve behaviour.",
        Strings.aiAgentQuickRefactor to "Read `$filePath` on branch `$branch` and propose a focused refactor that improves readability without changing behaviour. Apply with edit_file once approved.",
        Strings.aiAgentQuickGenerateDocs to "Read `$filePath` on branch `$branch` and add KDoc/Javadoc to public types, functions, and properties that are missing it. Keep comments concise.",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            null,
            Modifier.size(14.dp),
            tint = colors.accent,
        )
        Spacer(Modifier.width(2.dp))
        // C2 — selection-first chip. We cap the snippet at ~4 KB to
        // avoid blowing the agent's input box if the user
        // accidentally selected the whole file. The agent already has
        // file-read tools to fetch surrounding context if needed.
        if (selectedText.isNotBlank()) {
            val snippet = if (selectedText.length > 4_000) {
                selectedText.take(4_000) + "\n…(truncated)"
            } else {
                selectedText
            }
            val prompt = buildString {
                append(Strings.aiAgentSendSelectionPromptPrefix)
                append(" `")
                append(filePath)
                append("` (branch `")
                append(branch)
                append("`):\n\n```\n")
                append(snippet)
                append("\n```\n")
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(colors.accent.copy(alpha = 0.14f))
                    .border(
                        0.5.dp,
                        colors.border,
                        RoundedCornerShape(GitHubControlRadius),
                    )
                    .clickable { onSendPrompt(prompt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    Strings.aiAgentSendSelectionChip,
                    fontSize = 11.sp,
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                )
            }
        }
        actions.forEach { (label, prompt) ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(colors.surface)
                    .border(0.5.dp, colors.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onSendPrompt(prompt) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    fontFamily = JetBrainsMono,
                )
            }
        }
    }
}
