package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.codeFilePolicy
import gs.git.vps.data.github.formatCodeFileBytes
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

@Composable
internal fun CodeFileGuardScreen(
    file: GHContent,
    message: String,
    onBack: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val policy = codeFilePolicy(file.name)
    AiModuleSurface {
        Column(
            Modifier.fillMaxSize().background(palette.background).padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.warning.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                    .background(palette.surface, RoundedCornerShape(18.dp))
                    .padding(18.dp),
            ) {
                AiModuleText(
                    "[ safe file guard ]",
                    color = palette.warning,
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                AiModuleText(file.path, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                AiModuleText(
                    "kind: ${policy.kind.name.lowercase()}  ·  size: ${if (file.size > 0) formatCodeFileBytes(file.size) else "unknown"}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(18.dp))
                AiModuleText(message, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                AiModuleText(
                    "GsGit stopped before editor allocation to avoid corrupted text, ANR, or excessive memory use.",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GitHubTerminalButton("back", onBack, color = palette.textSecondary)
                    GitHubTerminalButton("open on github", onOpenGitHub, color = palette.accent)
                }
            }
        }
    }
}
