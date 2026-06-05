package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.ui.components.*
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

@Composable
internal fun RepoCreateScreen(
    userLogin: String,
    onBack: () -> Unit,
    onCreate: (GHRepoCreateParams) -> Unit
) {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var autoInit by remember { mutableStateOf(false) }
    var hasIssues by remember { mutableStateOf(true) }
    var hasProjects by remember { mutableStateOf(true) }
    var hasWiki by remember { mutableStateOf(true) }
    var gitignoreTemplate by remember { mutableStateOf("") }
    var licenseTemplate by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var gitignoreTemplates by remember { mutableStateOf<List<String>>(emptyList()) }
    var licenses by remember { mutableStateOf<List<GHLicense>>(emptyList()) }

    LaunchedEffect(Unit) {
        gitignoreTemplates = GitHubManager.getGitignoreTemplates(context)
        licenses = GitHubManager.getLicenses(context)
    }

    GitHubScreenFrame(
        title = "> create a new repository",
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // заголовок
            androidx.compose.material3.Text(
                "Repositories contain a project's files and version history.",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = palette.textMuted
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(
                "─".repeat(54),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.border
            )
            Spacer(Modifier.height(16.dp))

            // ═══ БЛОК 1: GENERAL ═══
            SectionNumber("1", "GENERAL")
            Spacer(Modifier.height(10.dp))

            // owner
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text(
                    "owner:",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(Modifier.width(6.dp))
                androidx.compose.material3.Text(
                    "@$userLogin",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent
                )
            }
            Spacer(Modifier.height(10.dp))

            // repository name
            AiModuleTextField(name, { name = it }, label = "Repository name *")
            Spacer(Modifier.height(2.dp))
            androidx.compose.material3.Text(
                "Great repository names are short and memorable.",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textMuted
            )
            Spacer(Modifier.height(12.dp))

            // description
            AiModuleTextField(description, { description = it }, label = "Description (optional)", maxLines = 3)
            Spacer(Modifier.height(16.dp))

            // ═══ БЛОК 2: CONFIGURATION ═══
            SectionNumber("2", "CONFIGURATION")
            Spacer(Modifier.height(10.dp))

            // visibility
            androidx.compose.material3.Text(
                "Choose visibility *",
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                color = palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            AiModuleCheckRow(
                label = "Public",
                checked = !isPrivate,
                onToggle = { isPrivate = false },
                description = "Anyone on the internet can see this repository."
            )
            AiModuleCheckRow(
                label = "Private",
                checked = isPrivate,
                onToggle = { isPrivate = true },
                description = "Choose who can see and commit to this repository."
            )
            Spacer(Modifier.height(14.dp))

            // initialize
            androidx.compose.material3.Text(
                "Initialize this repository with:",
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                color = palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            AiModuleCheckRow(
                label = "Add a README file",
                checked = autoInit,
                onToggle = { autoInit = !autoInit }
            )
            Spacer(Modifier.height(10.dp))

            // .gitignore selector
            if (gitignoreTemplates.isNotEmpty()) {
                androidx.compose.material3.Text(
                    "Add .gitignore",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChipOption("none", gitignoreTemplate.isEmpty()) { gitignoreTemplate = "" }
                    gitignoreTemplates.forEach { t ->
                        ChipOption(t, gitignoreTemplate == t) { gitignoreTemplate = t }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // license selector
            if (licenses.isNotEmpty()) {
                androidx.compose.material3.Text(
                    "Choose a license",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = palette.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChipOption("none", licenseTemplate.isEmpty()) { licenseTemplate = "" }
                    licenses.forEach { l ->
                        ChipOption(l.spdxId.ifBlank { l.key }, licenseTemplate == l.key) { licenseTemplate = l.key }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // feature toggles
            AiModuleCheckRow(label = "Issues", checked = hasIssues, onToggle = { hasIssues = !hasIssues })
            AiModuleCheckRow(label = "Projects", checked = hasProjects, onToggle = { hasProjects = !hasProjects })
            AiModuleCheckRow(label = "Wiki", checked = hasWiki, onToggle = { hasWiki = !hasWiki })
            Spacer(Modifier.height(16.dp))

            // ═══ ФИНАЛЬНАЯ ЗОНА ═══
            androidx.compose.material3.Text(
                "─".repeat(54),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.border
            )
            Spacer(Modifier.height(12.dp))

            if (creating) {
                AiModuleSpinner(label = "creating repository…")
            } else {
                BracketButton(
                    "[ Create repository ]",
                    accent = true,
                    enabled = name.isNotBlank()
                ) {
                    if (name.isBlank()) return@BracketButton
                    creating = true
                    onCreate(GHRepoCreateParams(
                        name = name, description = description, isPrivate = isPrivate,
                        autoInit = autoInit, gitignoreTemplate = gitignoreTemplate,
                        licenseTemplate = licenseTemplate, hasIssues = hasIssues,
                        hasProjects = hasProjects, hasWiki = hasWiki
                    ))
                }
            }
        }
    }
}

@Composable
private fun SectionNumber(num: String, title: String) {
    val palette = AiModuleTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(
            "[ $num ]",
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Text(
            title,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
    }
}

@Composable
private fun ChipOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) palette.accent.copy(alpha = 0.15f) else palette.surface,
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (selected) palette.accent else palette.border,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.material3.Text(
            label,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = if (selected) palette.accent else palette.textSecondary
        )
    }
}

@Composable
private fun BracketButton(label: String, accent: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val color = when {
        !enabled -> palette.textMuted
        accent -> palette.accent
        else -> palette.textSecondary
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .background(
                if (accent && enabled) palette.accent.copy(alpha = 0.08f) else palette.surface,
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            label,
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
