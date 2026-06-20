package gs.git.vps.ui.screens

import android.widget.Toast
import android.content.Context
import gs.git.vps.security.BiometricHelper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.Strings
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GHAutolink
import gs.git.vps.data.github.GHRepositorySecuritySettings
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHDeployKey
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHRepoSettings
import gs.git.vps.data.github.model.GHTag
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun RepoSettingsScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit,
    onBranchProtection: () -> Unit = {},
    onCollaborators: () -> Unit = {},
    onTeams: () -> Unit = {},
    onWebhooks: () -> Unit = {},
    onDiscussions: () -> Unit = {},
    onRulesets: () -> Unit = {},
    onSecurity: () -> Unit = {},
    onAutolinks: () -> Unit = {},
    onLfs: () -> Unit = {},
    onInteractionLimits: () -> Unit = {},
    onDeleteRepo: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf<GHRepoSettings?>(null) }
    var tags by remember { mutableStateOf<List<GHTag>>(emptyList()) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var deployKeys by remember { mutableStateOf<List<GHDeployKey>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var adminActionInFlight by remember { mutableStateOf(false) }
    var deployKeyBusy by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteBusy by remember { mutableStateOf(false) }
    var deployKeyDeleteTarget by remember { mutableStateOf<GHDeployKey?>(null) }

    // Editable fields
    var description by remember { mutableStateOf("") }
    var homepage by remember { mutableStateOf("") }
    var hasIssues by remember { mutableStateOf(true) }
    var hasProjects by remember { mutableStateOf(true) }
    var hasWiki by remember { mutableStateOf(true) }
    var hasDiscussions by remember { mutableStateOf(false) }
    var allowForking by remember { mutableStateOf(true) }
    var isTemplate by remember { mutableStateOf(false) }
    var archived by remember { mutableStateOf(false) }
    var allowMergeCommit by remember { mutableStateOf(true) }
    var allowSquashMerge by remember { mutableStateOf(true) }
    var allowRebaseMerge by remember { mutableStateOf(true) }
    var deleteBranchOnMerge by remember { mutableStateOf(false) }
    var topics by remember { mutableStateOf<List<String>>(emptyList()) }
    var newTopic by remember { mutableStateOf("") }
    var mergeBase by remember { mutableStateOf("") }
    var mergeHead by remember { mutableStateOf("") }
    var mergeMessage by remember { mutableStateOf("") }
    var mergeConfirm by remember { mutableStateOf("") }
    var renameBranch by remember { mutableStateOf("") }
    var renameTo by remember { mutableStateOf("") }
    var renameConfirm by remember { mutableStateOf("") }
    var transferOwner by remember { mutableStateOf("") }
    var transferName by remember { mutableStateOf("") }
    var transferConfirm by remember { mutableStateOf("") }
    var deployKeyTitle by remember { mutableStateOf("") }
    var deployKeyValue by remember { mutableStateOf("") }
    var deployKeyReadOnly by remember { mutableStateOf(true) }
    var isWatching by remember { mutableStateOf(false) }
    var securitySettings by remember { mutableStateOf<GHRepositorySecuritySettings?>(null) }
    var watchBusy by remember { mutableStateOf(false) }

    LaunchedEffect(repoOwner, repoName) {
        val s = GitHubManager.getRepoSettings(context, repoOwner, repoName)
        val fetchedBranches = GitHubManager.getBranches(context, repoOwner, repoName)
        tags = GitHubManager.getRepoTags(context, repoOwner, repoName)
        branches = fetchedBranches
        deployKeys = GitHubManager.getRepoDeployKeys(context, repoOwner, repoName)
        isWatching = GitHubManager.isWatchingRepo(context, repoOwner, repoName)
        securitySettings = try { GitHubManager.getRepositorySecuritySettings(context, repoOwner, repoName) } catch (_: Exception) { null }
        settings = s
        if (s != null) {
            description = s.description
            homepage = s.homepage
            hasIssues = s.hasIssues
            hasProjects = s.hasProjects
            hasWiki = s.hasWiki
            hasDiscussions = s.hasDiscussions
            allowForking = s.allowForking
            isTemplate = s.isTemplate
            archived = s.archived
            allowMergeCommit = s.allowMergeCommit
            allowSquashMerge = s.allowSquashMerge
            allowRebaseMerge = s.allowRebaseMerge
            deleteBranchOnMerge = s.deleteBranchOnMerge
            topics = s.topics
            if (mergeBase.isBlank()) mergeBase = s.defaultBranch
            if (renameBranch.isBlank()) renameBranch = s.defaultBranch
        } else {
            val firstBranch = fetchedBranches.firstOrNull().orEmpty()
            if (mergeBase.isBlank()) mergeBase = firstBranch
            if (renameBranch.isBlank()) renameBranch = firstBranch
        }
        loading = false
    }

    fun saveChanges() {
        saving = true
        scope.launch {
            val cleanTopics = topics.map { normalizeRepoTopic(it) }.filter { it.isNotBlank() }.distinct().take(20)
            val ok = GitHubManager.updateRepoSettings(
                context = context,
                owner = repoOwner,
                repo = repoName,
                description = description,
                homepage = homepage,
                hasIssues = hasIssues,
                hasProjects = hasProjects,
                hasWiki = hasWiki,
                hasDiscussions = hasDiscussions,
                allowForking = allowForking,
                isTemplate = isTemplate,
                archived = archived,
                topics = null,
                allowMergeCommit = allowMergeCommit,
                allowSquashMerge = allowSquashMerge,
                allowRebaseMerge = allowRebaseMerge,
                deleteBranchOnMerge = deleteBranchOnMerge
            ) && GitHubManager.replaceRepoTopics(context, repoOwner, repoName, cleanTopics)
            Toast.makeText(context, if (ok) "Settings saved" else "Failed to save", Toast.LENGTH_SHORT).show()
            saving = false
            if (ok) {
                // Refresh
                val s = GitHubManager.getRepoSettings(context, repoOwner, repoName)
                settings = s
                topics = s?.topics ?: cleanTopics
                tags = GitHubManager.getRepoTags(context, repoOwner, repoName)
            }
        }
    }

    val hasUnsavedChanges = settings?.let { s ->
        description != s.description ||
            homepage != s.homepage ||
            hasIssues != s.hasIssues ||
            hasProjects != s.hasProjects ||
            hasWiki != s.hasWiki ||
            hasDiscussions != s.hasDiscussions ||
            allowForking != s.allowForking ||
            isTemplate != s.isTemplate ||
            archived != s.archived ||
            allowMergeCommit != s.allowMergeCommit ||
            allowSquashMerge != s.allowSquashMerge ||
            allowRebaseMerge != s.allowRebaseMerge ||
            deleteBranchOnMerge != s.deleteBranchOnMerge ||
            topics.map(::normalizeRepoTopic).filter { it.isNotBlank() }.distinct() != s.topics.map(::normalizeRepoTopic).filter { it.isNotBlank() }.distinct()
    } ?: false

    fun handleRepoSettingsBack() {
        when {
            deployKeyDeleteTarget != null -> deployKeyDeleteTarget = null
            showArchiveConfirm != null -> showArchiveConfirm = null
            showDeleteConfirm -> showDeleteConfirm = false
            else -> onBack()
        }
    }
    BackHandler(enabled = deployKeyDeleteTarget != null || showArchiveConfirm != null || showDeleteConfirm) {
        handleRepoSettingsBack()
    }

    GitHubScreenFrame(
        title = "> repo settings",
        subtitle = "$repoOwner/$repoName",
        onBack = ::handleRepoSettingsBack,
        trailing = {
                if (saving) {
                    AiModuleSpinner()
                } else {
                    GitHubTopBarTextAction(
                        label = if (hasUnsavedChanges) "save" else "saved",
                        onClick = { saveChanges() },
                        enabled = hasUnsavedChanges,
                        tint = if (hasUnsavedChanges) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted,
                    )
                }
        },
    ) {

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading…")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RepoSettingsSummaryCard(settings, tags, hasUnsavedChanges)
                }

                // General section
                item { SectionHeader("General") }

                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Description
                            AiModuleTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "Description",
                                maxLines = 3,
                            )

                            // Homepage
                            AiModuleTextField(
                                value = homepage,
                                onValueChange = { homepage = it },
                                label = "Homepage URL",
                            )
                        }
                    }
                }

                // Features section
                item { SectionHeader("Features") }

                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleRow("Issues", hasIssues, Icons.Rounded.BugReport) { hasIssues = it }
                            ToggleRow("Projects", hasProjects, Icons.Rounded.Dashboard) { hasProjects = it }
                            ToggleRow("Wiki", hasWiki, Icons.Rounded.MenuBook) { hasWiki = it }
                            ToggleRow("Discussions", hasDiscussions, Icons.Rounded.Forum) { hasDiscussions = it }
                        }
                    }
                }

                // Merge section
                item { SectionHeader("Pull Requests") }

                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleRow("Allow merge commits", allowMergeCommit, Icons.Rounded.MergeType) { allowMergeCommit = it }
                            ToggleRow("Allow squash merging", allowSquashMerge, Icons.Rounded.Compress) { allowSquashMerge = it }
                            ToggleRow("Allow rebase merging", allowRebaseMerge, Icons.Rounded.LinearScale) { allowRebaseMerge = it }
                            ToggleRow("Delete head branches on merge", deleteBranchOnMerge, Icons.Rounded.DeleteSweep) { deleteBranchOnMerge = it }
                        }
                    }
                }

                // Administration section
                item { SectionHeader("Administration") }

                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Branch protection button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onBranchProtection() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Shield, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Branch protection rules", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Require reviews, status checks, and more", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Collaborators button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onCollaborators() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Group, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Manage collaborators", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Add, remove, or change permissions", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onTeams() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Group, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Manage teams", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Org team access and permissions", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            ToggleRow("Allow forking", allowForking, Icons.Rounded.ForkRight) { allowForking = it }
                            ToggleRow("Template repository", isTemplate, Icons.Rounded.ContentCopy) { isTemplate = it }

                            // Webhooks button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onWebhooks() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Webhook, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Webhooks", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Manage repository webhooks", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Discussions button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onDiscussions() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Forum, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Discussions", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("View repository discussions", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Rulesets button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onRulesets() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Rule, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Rulesets", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("View repository rulesets", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Security button
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onSecurity() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Security, null, Modifier.size(22.dp), tint = Color(0xFFFF3B30))
                                Column(Modifier.weight(1f)) {
                                    Text("Security", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Dependabot alerts", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Autolinks
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onAutolinks() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Link, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Autolinks", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Reference autolink patterns", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // LFS
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onLfs() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Storage, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Git LFS", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Large File Storage settings", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Interaction Limits
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable { onInteractionLimits() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Block, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
                                Column(Modifier.weight(1f)) {
                                    Text("Interaction Limits", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text("Limit interactions for this repo", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.textMuted)
                            }

                            // Watch toggle
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.background)
                                    .clickable {
                                        if (!watchBusy) {
                                            watchBusy = true
                                            scope.launch {
                                                val ok = if (isWatching) GitHubManager.unwatchRepo(context, repoOwner, repoName) else GitHubManager.watchRepo(context, repoOwner, repoName)
                                                if (ok) isWatching = !isWatching
                                                watchBusy = false
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (isWatching) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    null, Modifier.size(22.dp),
                                    tint = if (isWatching) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("Watching", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    Text(if (isWatching) "You are watching this repository" else "Not watching this repository", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                                }
                                if (watchBusy) AiModuleSpinner() else TerminalToggleIndicator(checked = isWatching)
                            }

                            // Security settings toggles
                            if (securitySettings != null) {
                                val sec = securitySettings!!
                                SectionHeader("Security Settings")
                                SettingsCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SecurityToggleRow(
                                            label = "Dependabot security updates",
                                            checked = sec.automatedSecurityFixes,
                                            info = if (sec.automatedSecurityFixesPaused) "paused" else "active",
                                            onToggle = { enabled ->
                                                scope.launch {
                                                    val ok = GitHubManager.setAutomatedSecurityFixes(context, repoOwner, repoName, enabled)
                                                    if (ok) securitySettings = sec.copy(automatedSecurityFixes = enabled)
                                                    Toast.makeText(context, if (ok) "Updated" else "Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        SecurityToggleRow(
                                            label = "Vulnerability alerts",
                                            checked = sec.vulnerabilityAlerts,
                                            onToggle = { enabled ->
                                                scope.launch {
                                                    val ok = GitHubManager.setVulnerabilityAlerts(context, repoOwner, repoName, enabled)
                                                    if (ok) securitySettings = sec.copy(vulnerabilityAlerts = enabled)
                                                    Toast.makeText(context, if (ok) "Updated" else "Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        SecurityToggleRow(
                                            label = "Private vulnerability reporting",
                                            checked = sec.privateVulnerabilityReporting,
                                            onToggle = { enabled ->
                                                scope.launch {
                                                    val ok = GitHubManager.setPrivateVulnerabilityReporting(context, repoOwner, repoName, enabled)
                                                    if (ok) securitySettings = sec.copy(privateVulnerabilityReporting = enabled)
                                                    Toast.makeText(context, if (ok) "Updated" else "Failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Archive toggle
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(if (archived) Color(0xFFFF3B30).copy(0.1f) else AiModuleTheme.colors.background)
                                    .clickable { showArchiveConfirm = !archived }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    null,
                                    Modifier.size(22.dp),
                                    tint = if (archived) Color(0xFFFF3B30) else AiModuleTheme.colors.textSecondary
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (archived) "Unarchive this repository" else "Archive this repository",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (archived) Color(0xFFFF3B30) else AiModuleTheme.colors.textPrimary
                                    )
                                    Text(
                                        if (archived) "This repository is currently archived" else "Archive makes the repository read-only",
                                        fontSize = 12.sp,
                                        color = AiModuleTheme.colors.textMuted
                                    )
                                }
                                TerminalToggleIndicator(checked = archived, tint = Color(0xFFFF3B30))
                            }

                            // Delete repository
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(Color(0xFFFF3B30).copy(0.08f))
                                    .clickable { showDeleteConfirm = true }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteForever,
                                    null,
                                    Modifier.size(22.dp),
                                    tint = Color(0xFFFF3B30)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Delete this repository",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF3B30)
                                    )
                                    Text(
                                        "This action cannot be undone",
                                        fontSize = 12.sp,
                                        color = AiModuleTheme.colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionHeader("Deploy keys") }

                item {
                    DeployKeysSettingsCard(
                        keys = deployKeys,
                        busy = deployKeyBusy,
                        title = deployKeyTitle,
                        onTitleChange = { deployKeyTitle = it },
                        keyValue = deployKeyValue,
                        onKeyValueChange = { deployKeyValue = it },
                        readOnly = deployKeyReadOnly,
                        onReadOnlyChange = { deployKeyReadOnly = it },
                        onCreate = {
                            if (!deployKeyBusy) {
                                val title = deployKeyTitle.trim().ifBlank { "GlassFiles deploy key" }
                                val key = deployKeyValue.trim()
                                deployKeyBusy = true
                                scope.launch {
                                    val ok = GitHubManager.createRepoDeployKey(
                                        context = context,
                                        owner = repoOwner,
                                        repo = repoName,
                                        title = title,
                                        key = key,
                                        readOnly = deployKeyReadOnly,
                                    )
                                    Toast.makeText(context, if (ok) "Deploy key added" else "Deploy key add failed", Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        deployKeyTitle = ""
                                        deployKeyValue = ""
                                        deployKeys = GitHubManager.getRepoDeployKeys(context, repoOwner, repoName)
                                    }
                                    deployKeyBusy = false
                                }
                            }
                        },
                        onDelete = { key -> deployKeyDeleteTarget = key },
                    )
                }

                item { SectionHeader("Repository API") }

                item {
                    RepoAdminOperationsCard(
                        repoOwner = repoOwner,
                        repoName = repoName,
                        branches = branches,
                        busy = adminActionInFlight,
                        mergeBase = mergeBase,
                        onMergeBaseChange = { mergeBase = it },
                        mergeHead = mergeHead,
                        onMergeHeadChange = { mergeHead = it },
                        mergeMessage = mergeMessage,
                        onMergeMessageChange = { mergeMessage = it },
                        mergeConfirm = mergeConfirm,
                        onMergeConfirmChange = { mergeConfirm = it },
                        onMergeBranch = {
                            if (!adminActionInFlight) {
                                val base = mergeBase.trim()
                                val head = mergeHead.trim()
                                val message = mergeMessage.trim().takeIf { it.isNotBlank() }
                                adminActionInFlight = true
                                scope.launch {
                                    val ok = GitHubManager.mergeBranch(context, repoOwner, repoName, base, head, message)
                                    Toast.makeText(context, if (ok) "Branch merged" else "Merge failed", Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        mergeConfirm = ""
                                        mergeMessage = ""
                                    }
                                    adminActionInFlight = false
                                }
                            }
                        },
                        renameBranch = renameBranch,
                        onRenameBranchChange = { renameBranch = it },
                        renameTo = renameTo,
                        onRenameToChange = { renameTo = it },
                        renameConfirm = renameConfirm,
                        onRenameConfirmChange = { renameConfirm = it },
                        onRenameBranch = {
                            if (!adminActionInFlight) {
                                val oldBranch = renameBranch.trim()
                                val newBranch = renameTo.trim()
                                adminActionInFlight = true
                                scope.launch {
                                    val ok = GitHubManager.renameBranch(context, repoOwner, repoName, oldBranch, newBranch)
                                    Toast.makeText(context, if (ok) "Branch renamed" else "Rename failed", Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        val refreshedSettings = GitHubManager.getRepoSettings(context, repoOwner, repoName)
                                        val refreshedBranches = GitHubManager.getBranches(context, repoOwner, repoName)
                                        settings = refreshedSettings
                                        branches = refreshedBranches
                                        if (mergeBase.trim() == oldBranch) mergeBase = newBranch
                                        if (mergeHead.trim() == oldBranch) mergeHead = newBranch
                                        renameBranch = newBranch
                                        renameTo = ""
                                        renameConfirm = ""
                                    }
                                    adminActionInFlight = false
                                }
                            }
                        },
                        transferOwner = transferOwner,
                        onTransferOwnerChange = { transferOwner = it },
                        transferName = transferName,
                        onTransferNameChange = { transferName = it },
                        transferConfirm = transferConfirm,
                        onTransferConfirmChange = { transferConfirm = it },
                        onTransferRepo = {
                            if (!adminActionInFlight) {
                                verifyBiometricsAndRun(context, "transfer repository", "Confirm identity to transfer repository") {
                                    val newOwner = transferOwner.trim()
                                    val newName = transferName.trim().takeIf { it.isNotBlank() }
                                    adminActionInFlight = true
                                    scope.launch {
                                        val ok = GitHubManager.transferRepo(context, repoOwner, repoName, newOwner, newName)
                                        Toast.makeText(context, if (ok) "Transfer requested" else "Transfer failed", Toast.LENGTH_SHORT).show()
                                        if (ok) {
                                            transferOwner = ""
                                            transferName = ""
                                            transferConfirm = ""
                                        }
                                        adminActionInFlight = false
                                    }
                                }
                            }
                        },
                    )
                }

                // Topics section
                item { SectionHeader("Topics") }

                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Topic chips
                            if (topics.isNotEmpty()) {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    topics.forEach { topic ->
                                        TopicChip(topic) {
                                            topics = topics - topic
                                        }
                                    }
                                }
                            }

                            // Add topic
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.weight(1f)) {
                                    AiModuleTextField(
                                        value = newTopic,
                                        onValueChange = { newTopic = normalizeRepoTopic(it) },
                                        label = "Add topic",
                                    )
                                }
                                AiModulePillButton(
                                    label = "+ add",
                                    enabled = newTopic.isNotBlank() && topics.size < 20,
                                    onClick = {
                                        val normalized = normalizeRepoTopic(newTopic)
                                        if (normalized.isNotBlank() && normalized !in topics.map(::normalizeRepoTopic) && topics.size < 20) {
                                            topics = topics + normalized
                                            newTopic = ""
                                        }
                                    },
                                )
                            }
                            Text("${topics.size}/20 topics", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
                        }
                    }
                }

                item { SectionHeader("Tags") }

                item {
                    SettingsCard {
                        if (tags.isEmpty()) {
                            Text("No tags returned for this repository.", fontSize = 13.sp, color = AiModuleTheme.colors.textMuted)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tags.take(12).forEach { tag ->
                                    RepoTagRow(tag)
                                }
                                if (tags.size > 12) {
                                    Text("+${tags.size - 12} more tags", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    deployKeyDeleteTarget?.let { key ->
        AiModuleAlertDialog(
            onDismissRequest = { deployKeyDeleteTarget = null },
            title = "delete deploy key",
            confirmButton = {
                AiModuleTextAction(
                    label = "delete",
                    onClick = {
                        if (!deployKeyBusy) {
                            deployKeyBusy = true
                            scope.launch {
                                val ok = GitHubManager.deleteRepoDeployKey(context, repoOwner, repoName, key.id)
                                Toast.makeText(context, if (ok) "Deploy key deleted" else "Deploy key delete failed", Toast.LENGTH_SHORT).show()
                                if (ok) deployKeys = GitHubManager.getRepoDeployKeys(context, repoOwner, repoName)
                                deployKeyDeleteTarget = null
                                deployKeyBusy = false
                            }
                        }
                    },
                    tint = AiModuleTheme.colors.error,
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = Strings.cancel.lowercase(),
                    onClick = { deployKeyDeleteTarget = null },
                    tint = AiModuleTheme.colors.textSecondary,
                )
            },
        ) {
            Text(
                "Deploy keys are immutable. Delete `${key.title.ifBlank { key.id.toString() }}` from $repoOwner/$repoName?",
                color = AiModuleTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
            )
        }
    }

    val archiveTarget = showArchiveConfirm
    if (archiveTarget != null) {
        AiModuleAlertDialog(
            onDismissRequest = { showArchiveConfirm = null },
            title = if (archiveTarget) "archive repository" else "unarchive repository",
            confirmButton = {
                AiModuleTextAction(
                    label = if (archiveTarget) "archive" else "unarchive",
                    onClick = {
                        archived = archiveTarget
                        showArchiveConfirm = null
                    },
                    tint = if (archiveTarget) AiModuleTheme.colors.error else AiModuleTheme.colors.accent,
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = Strings.cancel.lowercase(),
                    onClick = { showArchiveConfirm = null },
                    tint = AiModuleTheme.colors.textSecondary,
                )
            },
        ) {
            Text(
                if (archiveTarget) "Archiving makes the repository read-only until it is unarchived. Save settings after confirming."
                else "Unarchiving restores normal repository writes after you save settings.",
                color = AiModuleTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
            )
        }
    }

    // Delete repository confirm dialog
    if (showDeleteConfirm) {
        var confirmName by remember { mutableStateOf("") }
        val expectedName = "$repoOwner/$repoName"
        AiModuleAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "delete repository",
            confirmButton = {
                AiModuleTextAction(
                    label = "delete permanently",
                    onClick = {
                        if (confirmName == expectedName && !deleteBusy) {
                            verifyBiometricsAndRun(context, "delete repository", "Confirm identity to delete repository") {
                                deleteBusy = true
                                scope.launch {
                                    val ok = GitHubManager.deleteRepo(context, repoOwner, repoName)
                                    if (ok) {
                                        Toast.makeText(context, "Repository deleted", Toast.LENGTH_SHORT).show()
                                        onDeleteRepo()
                                    } else {
                                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                    }
                                    deleteBusy = false
                                }
                            }
                        }
                    },
                    tint = if (confirmName == expectedName) AiModuleTheme.colors.error else AiModuleTheme.colors.textMuted,
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = Strings.cancel.lowercase(),
                    onClick = { showDeleteConfirm = false },
                    tint = AiModuleTheme.colors.textSecondary,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This will permanently delete $expectedName, including all issues, comments, and data. This action cannot be undone.",
                    color = AiModuleTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMono,
                )
                Text(
                    "Type $expectedName to confirm:",
                    color = Color(0xFFFF3B30),
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                )
                BasicTextField(
                    value = confirmName,
                    onValueChange = { confirmName = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        color = if (confirmName == expectedName) AiModuleTheme.colors.accent else Color(0xFFFF3B30)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(AiModuleTheme.colors.surface)
                        .border(1.dp, if (confirmName == expectedName) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                if (deleteBusy) AiModuleSpinner(label = "deleting…")
            }
        }
    }
}

@Composable
private fun RepoSettingsSummaryCard(settings: GHRepoSettings?, tags: List<GHTag>, hasUnsavedChanges: Boolean) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Settings, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text(settings?.name ?: "Repository", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                Text(
                    buildString {
                        append(settings?.defaultBranch ?: "default branch")
                        append(" · ")
                        append(if (settings?.isPrivate == true) "private" else "public")
                        append(" · ")
                        append("${settings?.topics?.size ?: 0} topics")
                        append(" · ")
                        append("${tags.size} tags")
                    },
                    fontSize = 11.sp,
                    color = AiModuleTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasUnsavedChanges) {
                Text(
                    "Unsaved",
                    fontSize = 10.sp,
                    color = Color(0xFFFF9500),
                    modifier = Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(Color(0xFFFF9500).copy(0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        if (settings?.archived == true) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Repository is archived and read-only.",
                fontSize = 12.sp,
                color = Color(0xFFFF3B30),
                modifier = Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(Color(0xFFFF3B30).copy(0.10f)).padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = AiModuleTheme.colors.textPrimary) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun RepoTagRow(tag: GHTag) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.background).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Label, null, Modifier.size(16.dp), tint = AiModuleTheme.colors.accent)
        Column(Modifier.weight(1f)) {
            Text(tag.name, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tag.commitSha.isNotBlank()) {
                Text(tag.commitSha.take(7), fontSize = 10.sp, color = AiModuleTheme.colors.textMuted)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(15.dp), tint = AiModuleTheme.colors.textMuted)
    }
}

@Composable
private fun DeployKeysSettingsCard(
    keys: List<GHDeployKey>,
    busy: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    keyValue: String,
    onKeyValueChange: (String) -> Unit,
    readOnly: Boolean,
    onReadOnlyChange: (Boolean) -> Unit,
    onCreate: () -> Unit,
    onDelete: (GHDeployKey) -> Unit,
) {
    val palette = AiModuleTheme.colors
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("> deploy keys", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${keys.size} keys", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
            Text(
                "GET/POST/DELETE /repos/{owner}/{repo}/keys",
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TerminalLabeledField("title", title, onTitleChange, "deploy key title")
            TerminalLabeledField("public_key", keyValue, onKeyValueChange, "ssh-rsa AAAA...", singleLine = false)
            GitHubTerminalCheckbox("read only", readOnly, onToggle = { onReadOnlyChange(!readOnly) }, enabled = !busy)
            GitHubTerminalButton(
                label = if (busy) "running" else "add deploy key",
                onClick = onCreate,
                color = palette.accent,
                enabled = !busy && keyValue.trim().isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            )

            DeployKeyDivider()

            if (keys.isEmpty()) {
                Text("No deploy keys returned, or token cannot read repository administration settings.", color = palette.textSecondary, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { key ->
                        DeployKeyRow(key, busy, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeployKeyRow(key: GHDeployKey, busy: Boolean, onDelete: (GHDeployKey) -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(palette.background).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(key.title.ifBlank { "deploy key ${key.id}" }, fontSize = 13.sp, color = palette.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(key.key.take(72).ifBlank { "key hidden" }, fontSize = 10.sp, color = palette.textMuted, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GitHubTerminalButton("delete", onClick = { onDelete(key) }, color = palette.error, enabled = !busy)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DeployKeyMiniTag(if (key.readOnly) "read-only" else "write")
            DeployKeyMiniTag(if (key.verified) "verified" else "unverified")
            DeployKeyMiniTag(if (key.enabled) "enabled" else "disabled")
            if (key.addedBy.isNotBlank()) DeployKeyMiniTag("by ${key.addedBy}")
            if (key.createdAt.isNotBlank()) DeployKeyMiniTag("created ${key.createdAt.take(10)}")
            if (key.lastUsed.isNotBlank()) DeployKeyMiniTag("used ${key.lastUsed.take(10)}")
        }
    }
}

@Composable
private fun DeployKeyDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AiModuleTheme.colors.border.copy(alpha = 0.45f))
    )
}

@Composable
private fun DeployKeyMiniTag(label: String) {
    Text(
        label,
        color = AiModuleTheme.colors.textMuted,
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(AiModuleTheme.colors.surfaceElevated)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RepoAdminOperationsCard(
    repoOwner: String,
    repoName: String,
    branches: List<String>,
    busy: Boolean,
    mergeBase: String,
    onMergeBaseChange: (String) -> Unit,
    mergeHead: String,
    onMergeHeadChange: (String) -> Unit,
    mergeMessage: String,
    onMergeMessageChange: (String) -> Unit,
    mergeConfirm: String,
    onMergeConfirmChange: (String) -> Unit,
    onMergeBranch: () -> Unit,
    renameBranch: String,
    onRenameBranchChange: (String) -> Unit,
    renameTo: String,
    onRenameToChange: (String) -> Unit,
    renameConfirm: String,
    onRenameConfirmChange: (String) -> Unit,
    onRenameBranch: () -> Unit,
    transferOwner: String,
    onTransferOwnerChange: (String) -> Unit,
    transferName: String,
    onTransferNameChange: (String) -> Unit,
    transferConfirm: String,
    onTransferConfirmChange: (String) -> Unit,
    onTransferRepo: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val repoFullName = "$repoOwner/$repoName"
    val mergeBaseClean = mergeBase.trim()
    val mergeHeadClean = mergeHead.trim()
    val mergeToken = if (mergeBaseClean.isNotBlank() && mergeHeadClean.isNotBlank()) "$mergeBaseClean<-$mergeHeadClean" else "base<-head"
    val renameBranchClean = renameBranch.trim()
    val renameToClean = renameTo.trim()
    val canMerge = !busy &&
        mergeBaseClean.isNotBlank() &&
        mergeHeadClean.isNotBlank() &&
        mergeBaseClean != mergeHeadClean &&
        mergeConfirm.trim() == mergeToken
    val canRename = !busy &&
        renameBranchClean.isNotBlank() &&
        renameToClean.isNotBlank() &&
        renameBranchClean != renameToClean &&
        renameConfirm.trim() == renameBranchClean
    val canTransfer = !busy &&
        transferOwner.trim().isNotBlank() &&
        transferConfirm.trim() == repoFullName

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TerminalAdminSection(
            title = "merge branch",
            endpoint = "POST /repos/$repoOwner/$repoName/merges",
        ) {
            TerminalLabeledField("base", mergeBase, onMergeBaseChange, "target branch")
            TerminalBranchQuickPick("base branches", branches, mergeBaseClean, onMergeBaseChange)
            TerminalLabeledField("head", mergeHead, onMergeHeadChange, "source branch")
            TerminalBranchQuickPick("head branches", branches, mergeHeadClean, onMergeHeadChange)
            TerminalLabeledField("commit_message", mergeMessage, onMergeMessageChange, "optional")
            TerminalLabeledField("confirm $mergeToken", mergeConfirm, onMergeConfirmChange, mergeToken)
            GitHubTerminalButton(
                label = if (busy) "running" else "merge",
                onClick = onMergeBranch,
                color = palette.accent,
                enabled = canMerge,
                modifier = Modifier.align(Alignment.End),
            )
        }

        TerminalAdminSection(
            title = "rename branch",
            endpoint = "POST /repos/$repoOwner/$repoName/branches/{branch}/rename",
        ) {
            TerminalLabeledField("branch", renameBranch, onRenameBranchChange, "current branch")
            TerminalBranchQuickPick("known branches", branches, renameBranchClean, onRenameBranchChange)
            TerminalLabeledField("new_name", renameTo, onRenameToChange, "new branch name")
            TerminalLabeledField("confirm $renameBranchClean", renameConfirm, onRenameConfirmChange, renameBranchClean.ifBlank { "branch" })
            GitHubTerminalButton(
                label = if (busy) "running" else "rename",
                onClick = onRenameBranch,
                color = palette.accent,
                enabled = canRename,
                modifier = Modifier.align(Alignment.End),
            )
        }

        TerminalAdminSection(
            title = "transfer repository",
            endpoint = "POST /repos/$repoOwner/$repoName/transfer",
            danger = true,
        ) {
            TerminalLabeledField("new_owner", transferOwner, onTransferOwnerChange, "user or organization")
            TerminalLabeledField("new_name", transferName, onTransferNameChange, "optional repository name")
            TerminalLabeledField("confirm $repoFullName", transferConfirm, onTransferConfirmChange, repoFullName)
            GitHubTerminalButton(
                label = if (busy) "running" else "transfer",
                onClick = onTransferRepo,
                color = palette.error,
                enabled = canTransfer,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun TerminalAdminSection(
    title: String,
    endpoint: String,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = AiModuleTheme.colors
    val borderColor = if (danger) palette.error else palette.textMuted
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "> $title",
                color = if (danger) palette.error else palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (danger) "[danger]" else "[write]",
                color = borderColor,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
        Text(
            endpoint,
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun TerminalLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            label,
            color = AiModuleTheme.colors.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
        GitHubTerminalTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            minHeight = 38.dp,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
        )
    }
}

@Composable
private fun TerminalBranchQuickPick(
    label: String,
    branches: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    if (branches.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                label,
                color = AiModuleTheme.colors.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                branches.take(24).forEach { branch ->
                    GitHubTerminalButton(
                        label = branch,
                        onClick = { onSelect(branch) },
                        color = if (branch == selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted,
                    )
                }
                if (branches.size > 24) {
                    Text(
                        "+${branches.size - 24}",
                        color = AiModuleTheme.colors.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToggleRow(
    label: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (checked) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary)
        Text(label, fontSize = 14.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        TerminalToggleIndicator(checked = checked)
    }
}

@Composable
private fun TerminalToggleIndicator(
    checked: Boolean,
    tint: Color = AiModuleTheme.colors.accent,
) {
    val color = if (checked) tint else AiModuleTheme.colors.textMuted
    Text(
        text = if (checked) "[on]" else "[off]",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private fun normalizeRepoTopic(value: String): String =
    value.lowercase()
        .replace(Regex("""[^a-z0-9-]+"""), "-")
        .replace(Regex("""-+"""), "-")
        .trim('-')
        .take(50)

@Composable
private fun TopicChip(topic: String, onRemove: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.accent.copy(0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(topic, fontSize = 12.sp, color = AiModuleTheme.colors.accent, fontWeight = FontWeight.Medium)
        Icon(
            Icons.Rounded.Close,
            null,
            Modifier.size(14.dp).clickable { onRemove() },
            tint = AiModuleTheme.colors.accent
        )
    }
}

@Composable
internal fun AutolinksPanel(owner: String, repo: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var autolinks by remember { mutableStateOf<List<GHAutolink>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            autolinks = GitHubManager.getAutolinks(context, owner, repo)
            loading = false
        }
    }

    LaunchedEffect(owner, repo) { load() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiModuleSectionLabel("> autolinks")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiModulePillButton("+ autolink", onClick = { showCreate = true })
            AiModulePillButton("refresh", onClick = { load() }, accent = false)
        }
        if (autolinks.isEmpty() && !loading) {
            Text("No autolinks configured", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
        } else {
            autolinks.forEach { link ->
                Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(link.keyPrefix, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                        Text(link.urlTemplate, fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val ok = GitHubManager.deleteAutolink(context, owner, repo, link.id)
                            Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            if (ok) load()
                        }
                    }) { Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.textMuted) }
                }
            }
        }
    }

    if (showCreate) {
        var prefix by remember { mutableStateOf("") }
        var template by remember { mutableStateOf("") }
        AiModuleAlertDialog(
            onDismissRequest = { showCreate = false },
            title = "create autolink",
            confirmButton = {
                AiModuleTextAction(label = "create", enabled = prefix.isNotBlank() && template.isNotBlank(), onClick = {
                    scope.launch {
                        val ok = GitHubManager.createAutolink(context, owner, repo, prefix, template)
                        Toast.makeText(context, if (ok) "Created" else "Failed", Toast.LENGTH_SHORT).show()
                        if (ok) load()
                        showCreate = false
                    }
                }, tint = Blue)
            },
            dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showCreate = false }) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalTextField(value = prefix, onValueChange = { prefix = it }, placeholder = "Key prefix (e.g. JIRA-)", singleLine = true)
                GitHubTerminalTextField(value = template, onValueChange = { template = it }, placeholder = "URL template (e.g. https://jira.com/\$id)", singleLine = true)
            }
        }
    }
}

@Composable
internal fun LfsPanel(owner: String, repo: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lfsEnabled by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(owner, repo) {
        lfsEnabled = null
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiModuleSectionLabel("> git lfs")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiModulePillButton("enable", onClick = {
                scope.launch {
                    val ok = GitHubManager.enableRepoLfs(context, owner, repo)
                    Toast.makeText(context, if (ok) "LFS enabled" else "Failed", Toast.LENGTH_SHORT).show()
                    if (ok) lfsEnabled = true
                }
            }, accent = true)
            AiModulePillButton("disable", onClick = {
                scope.launch {
                    val ok = GitHubManager.disableRepoLfs(context, owner, repo)
                    Toast.makeText(context, if (ok) "LFS disabled" else "Failed", Toast.LENGTH_SHORT).show()
                    if (ok) lfsEnabled = false
                }
            }, destructive = true)
        }
        Text("Git LFS lets you store large files outside the repository.", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
    }
}

@Composable
internal fun InteractionLimitsPanel(owner: String, repo: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentLimit: GHInteractionLimitEntry? by remember { mutableStateOf(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedLimit by remember { mutableStateOf("collaborators_only") }
    var selectedExpiry by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            loading = true
            currentLimit = GitHubManager.getRepoInteractionLimit(context, owner, repo)
            currentLimit?.let {
                selectedLimit = it.limit
                selectedExpiry = it.expiry ?: ""
            }
            loading = false
        }
    }

    LaunchedEffect(owner, repo) { load() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiModuleSectionLabel("> interaction limits")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiModulePillButton("refresh", onClick = { load() }, accent = false)
        }
        if (loading) {
            AiModuleSpinner(label = "loading")
        } else {
            if (currentLimit != null) {
                Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Active: ${currentLimit!!.limit}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                        currentLimit!!.expiry?.let {
                            Text("Expires: $it", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val ok = GitHubManager.removeRepoInteractionLimit(context, owner, repo)
                            Toast.makeText(context, if (ok) "Removed" else "Failed", Toast.LENGTH_SHORT).show()
                            if (ok) load()
                        }
                    }) { Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.textMuted) }
                }
            }
            Text("Limit:", fontSize = 11.sp, color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("collaborators_only", "contributors_only", "read_only").forEach { opt ->
                    val sel = selectedLimit == opt
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) AiModuleTheme.colors.accent.copy(0.15f) else AiModuleTheme.colors.surface).border(1.dp, if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(6.dp)).clickable { selectedLimit = opt }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(opt.replace("_", " "), fontSize = 10.sp, fontFamily = JetBrainsMono, color = if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary)
                    }
                }
            }
            Text("Expiry:", fontSize = 11.sp, color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("" to "none", "one_day" to "1 day", "one_week" to "1 week", "one_month" to "1 month", "six_months" to "6 months").forEach { (val_, lbl) ->
                    val sel = selectedExpiry == val_
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (sel) AiModuleTheme.colors.accent.copy(0.15f) else AiModuleTheme.colors.surface).border(1.dp, if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(6.dp)).clickable { selectedExpiry = val_ }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(lbl, fontSize = 10.sp, fontFamily = JetBrainsMono, color = if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary)
                    }
                }
            }
            AiModulePillButton("set limit", onClick = {
                scope.launch {
                    val ok = GitHubManager.setRepoInteractionLimit(context, owner, repo, selectedLimit, selectedExpiry.ifBlank { "one_month" })
                    Toast.makeText(context, if (ok) "Set" else "Failed", Toast.LENGTH_SHORT).show()
                    if (ok) load()
                }
            }, accent = true)
        }
        Text("Temporarily limit interactions for this repository.", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun SecurityToggleRow(
    label: String,
    checked: Boolean,
    info: String = "",
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            null, Modifier.size(20.dp),
            tint = if (checked) Color(0xFF34C759) else AiModuleTheme.colors.textMuted
        )
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
            if (info.isNotBlank()) Text(info, fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
        }
        TerminalToggleIndicator(checked = checked, tint = if (checked) Color(0xFF34C759) else AiModuleTheme.colors.textMuted)
    }
}

private fun verifyBiometricsAndRun(
    context: Context,
    title: String,
    subtitle: String,
    onVerified: () -> Unit
) {
    val activity = context as? androidx.fragment.app.FragmentActivity
    val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
    val biometricEnabled = prefs.getBoolean("biometric_lock_enabled", false)
    
    if (biometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(context)) {
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                onVerified()
            },
            onError = { err ->
                Toast.makeText(context, "Authentication required: $err", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        onVerified()
    }
}

