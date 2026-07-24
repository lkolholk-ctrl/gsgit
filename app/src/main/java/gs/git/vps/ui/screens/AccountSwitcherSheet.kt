package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.github.AccountInfo
import gs.git.vps.data.github.GitHubAuth
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Gmail-style переключатель аккаунтов: bottom-sheet со списком аккаунтов, «Добавить аккаунт»
 * и удалением. Stateless — данные и события приходят/уходят через параметры (UDF).
 */
@Composable
internal fun AccountSwitcherSheet(
    accounts: List<AccountInfo>,
    canAdd: Boolean,
    onSwitch: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    var confirmRemove by remember { mutableStateOf<Int?>(null) }
    val noRipple = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = noRipple, indication = null) { onDismiss() }
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(palette.surface)
                // Клик по самому листу не должен закрывать sheet.
                .clickable(interactionSource = noRipple, indication = null) {}
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Accounts",
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            accounts.forEach { acc ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSwitch(acc.slot) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(palette.surfaceElevated),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (acc.avatarUrl.isNotBlank()) {
                            AsyncImage(acc.avatarUrl, acc.login, Modifier.size(40.dp).clip(CircleShape))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            if (acc.login.isNotBlank()) "@${acc.login}" else "Signed out",
                            color = if (acc.active) palette.accent else palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                        Text(
                            modeLabel(acc.mode),
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                    }
                    if (acc.active) {
                        Icon(Icons.Rounded.Check, contentDescription = "Active account", tint = palette.accent)
                    }
                    if (confirmRemove == acc.slot) {
                        Text(
                            "Remove?",
                            color = Color(0xFFE5534B),
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { confirmRemove = null; onRemove(acc.slot) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove account",
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { confirmRemove = acc.slot }
                                .padding(4.dp),
                        )
                    }
                }
            }

            if (canAdd) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAdd() }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(palette.surfaceElevated),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.accent)
                    }
                    Text(
                        "Add account",
                        color = palette.textPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

private fun modeLabel(mode: String): String = when (mode) {
    GitHubAuth.MODE_DEVICE -> "GitHub App · device flow"
    GitHubAuth.MODE_PAT -> "Personal access token"
    else -> "Guest"
}
