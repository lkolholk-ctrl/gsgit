package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

@Composable
internal fun EditorDiagnosticsBar(
    diagnostics: List<EditorDiagnostic>,
    onOpen: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val errors = diagnostics.count { it.severity == EditorDiagnosticSeverity.ERROR }
    val warnings = diagnostics.size - errors
    val first = diagnostics.firstOrNull()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (errors > 0) palette.error.copy(alpha = 0.10f) else palette.warning.copy(alpha = 0.08f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AiModuleText(
            text = if (errors > 0) "× $errors" else "! $warnings",
            color = if (errors > 0) palette.error else palette.warning,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
        AiModuleText(
            text = first?.let { "Ln ${it.line}:${it.column} · ${it.message}" }.orEmpty(),
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AiModuleText("details", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp)
    }
}

@Composable
internal fun EditorDiagnosticsDialog(
    diagnostics: List<EditorDiagnostic>,
    onSelect: (EditorDiagnostic) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val errors = diagnostics.count { it.severity == EditorDiagnosticSeverity.ERROR }
    val warnings = diagnostics.size - errors
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "problems · $errors errors · $warnings warnings",
        confirmButton = {},
        dismissButton = { AiModuleTextAction("close", onDismiss, tint = palette.textSecondary) },
    ) {
        LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
            items(diagnostics, key = { "${it.line}:${it.column}:${it.message}" }) { diagnostic ->
                val color = if (diagnostic.severity == EditorDiagnosticSeverity.ERROR) palette.error else palette.warning
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(diagnostic) }
                        .padding(horizontal = 4.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AiModuleText(
                        if (diagnostic.severity == EditorDiagnosticSeverity.ERROR) "×" else "!",
                        color = color,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        AiModuleText(
                            diagnostic.message,
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                        AiModuleText(
                            "Ln ${diagnostic.line}, Col ${diagnostic.column}",
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border))
            }
        }
    }
}
