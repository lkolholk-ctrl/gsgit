package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.*
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun BranchProtectionScreen(
    repoOwner: String,
    repoName: String,
    branches: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBranch by remember { mutableStateOf(branches.firstOrNull() ?: "main") }
    var protection by remember { mutableStateOf<GHBranchProtection?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var loadedState by remember { mutableStateOf<BranchProtectionEditState?>(null) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var disableConfirmed by remember { mutableStateOf(false) }

    // Editable fields
    var enabled by remember { mutableStateOf(false) }
    var requireStatusChecks by remember { mutableStateOf(false) }
    var statusChecksStrict by remember { mutableStateOf(false) }
    var statusCheckContexts by remember { mutableStateOf<List<String>>(emptyList()) }
    var newContext by remember { mutableStateOf("") }

    var requirePRReviews by remember { mutableStateOf(false) }
    var requiredApprovalCount by remember { mutableStateOf(1) }
    var dismissStaleReviews by remember { mutableStateOf(false) }
    var requireCodeOwnerReviews by remember { mutableStateOf(false) }

    var allowForcePushes by remember { mutableStateOf(true) }
    var allowDeletions by remember { mutableStateOf(true) }
    var requireConversationResolution by remember { mutableStateOf(false) }
    var enforceAdmins by remember { mutableStateOf(false) }
    var requireSignatures by remember { mutableStateOf(false) }
    var requiredLinearHistory by remember { mutableStateOf(false) }
    var blockCreations by remember { mutableStateOf(false) }
    var lockBranch by remember { mutableStateOf(false) }
    var requiredDeployments by remember { mutableStateOf<List<String>>(emptyList()) }
    var newDeployment by remember { mutableStateOf("") }

    fun loadProtection() {
        loading = true
        scope.launch {
            val p = GitHubManager.getBranchProtection(context, repoOwner, repoName, selectedBranch)
            protection = p
            if (p != null) {
                val signatures = GitHubManager.getBranchRequiredSignatures(context, repoOwner, repoName, selectedBranch)
                enabled = p.enabled
                requireStatusChecks = p.requiredStatusChecks != null
                statusChecksStrict = p.requiredStatusChecks?.strict ?: false
                statusCheckContexts = p.requiredStatusChecks?.contexts ?: emptyList()
                requirePRReviews = p.requiredPRReviews != null
                requiredApprovalCount = p.requiredPRReviews?.requiredApprovingReviewCount ?: 1
                dismissStaleReviews = p.requiredPRReviews?.dismissStaleReviews ?: false
                requireCodeOwnerReviews = p.requiredPRReviews?.requireCodeOwnerReviews ?: false
                allowForcePushes = p.allowForcePushes
                allowDeletions = p.allowDeletions
                requireConversationResolution = p.requiredConversationResolution
                enforceAdmins = p.enforceAdmins
                requireSignatures = p.requiredSignatures || signatures
                requiredLinearHistory = p.requiredLinearHistory
                blockCreations = p.blockCreations
                lockBranch = p.lockBranch
                requiredDeployments = p.requiredDeployments
            } else {
                enabled = false
                requireStatusChecks = false
                statusChecksStrict = false
                statusCheckContexts = emptyList()
                requirePRReviews = false
                requiredApprovalCount = 1
                dismissStaleReviews = false
                requireCodeOwnerReviews = false
                allowForcePushes = true
                allowDeletions = true
                requireConversationResolution = false
                enforceAdmins = false
                requireSignatures = false
                requiredLinearHistory = false
                blockCreations = false
                lockBranch = false
                requiredDeployments = emptyList()
            }
            loadedState = BranchProtectionEditState(
                enabled = enabled,
                requireStatusChecks = requireStatusChecks,
                statusChecksStrict = statusChecksStrict,
                statusCheckContexts = statusCheckContexts,
                requirePRReviews = requirePRReviews,
                requiredApprovalCount = requiredApprovalCount,
                dismissStaleReviews = dismissStaleReviews,
                requireCodeOwnerReviews = requireCodeOwnerReviews,
                allowForcePushes = allowForcePushes,
                allowDeletions = allowDeletions,
                requireConversationResolution = requireConversationResolution,
                enforceAdmins = enforceAdmins,
                requireSignatures = requireSignatures,
                requiredLinearHistory = requiredLinearHistory,
                blockCreations = blockCreations,
                lockBranch = lockBranch,
                requiredDeployments = requiredDeployments
            )
            loading = false
        }
    }

    LaunchedEffect(selectedBranch) { loadProtection() }

    val currentState = BranchProtectionEditState(
        enabled = enabled,
        requireStatusChecks = requireStatusChecks,
        statusChecksStrict = statusChecksStrict,
        statusCheckContexts = statusCheckContexts,
        requirePRReviews = requirePRReviews,
        requiredApprovalCount = requiredApprovalCount,
        dismissStaleReviews = dismissStaleReviews,
        requireCodeOwnerReviews = requireCodeOwnerReviews,
        allowForcePushes = allowForcePushes,
        allowDeletions = allowDeletions,
        requireConversationResolution = requireConversationResolution,
        enforceAdmins = enforceAdmins,
        requireSignatures = requireSignatures,
        requiredLinearHistory = requiredLinearHistory,
        blockCreations = blockCreations,
        lockBranch = lockBranch,
        requiredDeployments = requiredDeployments
    )
    val hasUnsavedChanges = loadedState != null && currentState != loadedState

    fun saveProtection() {
        if (!enabled && protection?.enabled == true && !disableConfirmed) {
            showDisableConfirm = true
            return
        }
        saving = true
        scope.launch {
            if (!enabled) {
                // Delete protection
                val ok = GitHubManager.deleteBranchProtection(context, repoOwner, repoName, selectedBranch)
                Toast.makeText(context, if (ok) "Protection removed" else "Failed", Toast.LENGTH_SHORT).show()
            } else {
                val protectionOk = GitHubManager.updateBranchProtection(
                    context = context,
                    owner = repoOwner,
                    repo = repoName,
                    branch = selectedBranch,
                    requiredStatusChecks = if (requireStatusChecks) GHRequiredStatusChecks(
                        strict = statusChecksStrict,
                        contexts = statusCheckContexts
                    ) else null,
                    requiredPRReviews = if (requirePRReviews) GHRequiredPRReviews(
                        requiredApprovingReviewCount = requiredApprovalCount,
                        dismissStaleReviews = dismissStaleReviews,
                        requireCodeOwnerReviews = requireCodeOwnerReviews
                    ) else null,
                    restrictions = null,
                    allowForcePushes = allowForcePushes,
                    allowDeletions = allowDeletions,
                    requiredConversationResolution = requireConversationResolution,
                    enforceAdmins = enforceAdmins,
                    requiredLinearHistory = requiredLinearHistory,
                    blockCreations = blockCreations,
                    lockBranch = lockBranch,
                    requiredDeployments = requiredDeployments
                )
                val signatureChanged = loadedState?.requireSignatures != requireSignatures
                val signaturesOk = if (signatureChanged) {
                    if (requireSignatures) {
                        GitHubManager.enableBranchRequiredSignatures(context, repoOwner, repoName, selectedBranch)
                    } else {
                        GitHubManager.disableBranchRequiredSignatures(context, repoOwner, repoName, selectedBranch)
                    }
                } else {
                    true
                }
                val ok = protectionOk && signaturesOk
                Toast.makeText(context, if (ok) "Protection saved" else "Failed to save", Toast.LENGTH_SHORT).show()
            }
            disableConfirmed = false
            saving = false
            loadProtection()
        }
    }

    GitHubScreenFrame(
        title = "> branch protection",
        subtitle = "$repoOwner/$repoName",
        onBack = { if (showDisableConfirm) showDisableConfirm = false else onBack() },
        trailing = {
                if (saving) {
                    AiModuleSpinner()
                } else {
                    GitHubTopBarTextAction(
                        label = if (hasUnsavedChanges) "save" else "saved",
                        onClick = { saveProtection() },
                        enabled = hasUnsavedChanges,
                        tint = if (hasUnsavedChanges) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted,
                    )
                }
        },
    ) {

        Column(Modifier.fillMaxSize()) {
            // Branch selector
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                branches.forEach { branch ->
                    BranchChip(
                        name = branch,
                        selected = branch == selectedBranch,
                        protected = branch == selectedBranch && enabled
                    ) {
                        selectedBranch = branch
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading…")
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                    BranchProtectionSummaryCard(
                        branch = selectedBranch,
                        state = currentState,
                        hasUnsavedChanges = hasUnsavedChanges
                    )
                }

                // Enable/disable protection
                item {
                    SettingsCard {
                        ProtectionToggleRow(
                            tag = "bp",
                            label = if (enabled) "Protection enabled" else "Protection disabled",
                            description = if (enabled) "Rules are enforced on this branch" else "No rules enforced",
                            checked = enabled,
                            onToggle = { enabled = !enabled },
                            tagColor = Color(0xFF34C759)
                        )
                    }
                }

                if (enabled) {
                    // Status Checks
                    item { SectionHeader("Status Checks") }
                    item {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProtectionToggleRow(
                                    tag = "ci",
                                    label = "Require status checks",
                                    description = "Status checks must pass before merging",
                                    checked = requireStatusChecks,
                                    onToggle = { requireStatusChecks = it },
                                    icon = Icons.Rounded.CheckCircle
                                )

                                if (requireStatusChecks) {
                                    ProtectionToggleRow(
                                        tag = "up",
                                        label = "Require branches to be up to date",
                                        description = "Branch must be up to date with base",
                                        checked = statusChecksStrict,
                                        onToggle = { statusChecksStrict = it },
                                        icon = Icons.Rounded.Update
                                    )

                                    // Contexts
                                    if (statusCheckContexts.isNotEmpty()) {
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            statusCheckContexts.forEach { ctx ->
                                                ContextChip(ctx) {
                                                    statusCheckContexts = statusCheckContexts - ctx
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AiModuleTextField(
                                            value = newContext,
                                            onValueChange = { newContext = it },
                                            label = "Status check name",
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        AiModulePillButton(
                                            label = "add",
                                            onClick = {
                                                if (newContext.isNotBlank() && newContext !in statusCheckContexts) {
                                                    statusCheckContexts = statusCheckContexts + newContext
                                                    newContext = ""
                                                }
                                            },
                                            enabled = newContext.isNotBlank()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // PR Reviews
                    item { SectionHeader("Pull Request Reviews") }
                    item {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProtectionToggleRow(
                                    tag = "rv",
                                    label = "Require pull request reviews",
                                    description = "PRs must be reviewed before merging",
                                    checked = requirePRReviews,
                                    onToggle = { requirePRReviews = it },
                                    icon = Icons.Rounded.Reviews
                                )

                                if (requirePRReviews) {
                                    // Approval count
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("approvals:", fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textSecondary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            (1..6).forEach { count ->
                                                val sel = count == requiredApprovalCount
                                                Box(
                                                    Modifier.clip(RoundedCornerShape(GitHubControlRadius))
                                                        .border(1.dp, if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius))
                                                        .background(if (sel) AiModuleTheme.colors.accent.copy(0.12f) else Color.Transparent)
                                                        .clickable { requiredApprovalCount = count }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "$count",
                                                        fontSize = 12.sp,
                                                        fontFamily = JetBrainsMono,
                                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (sel) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ProtectionToggleRow(
                                        tag = "ds",
                                        label = "Dismiss stale reviews",
                                        description = "Dismiss approved reviews on new pushes",
                                        checked = dismissStaleReviews,
                                        onToggle = { dismissStaleReviews = it },
                                        icon = Icons.Rounded.AutoDelete
                                    )

                                    ProtectionToggleRow(
                                        tag = "co",
                                        label = "Require code owner reviews",
                                        description = "Code owners must review changes",
                                        checked = requireCodeOwnerReviews,
                                        onToggle = { requireCodeOwnerReviews = it },
                                        icon = Icons.Rounded.VerifiedUser
                                    )
                                }
                            }
                        }
                    }

                    // Additional rules
                    item { SectionHeader("Additional Rules") }
                    item {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProtectionToggleRow(
                                    tag = "fp",
                                    label = "Allow force pushes",
                                    description = "Permit force pushes to this branch",
                                    checked = allowForcePushes,
                                    onToggle = { allowForcePushes = it },
                                    icon = Icons.Rounded.FlashOn
                                )

                                ProtectionToggleRow(
                                    tag = "dl",
                                    label = "Allow deletions",
                                    description = "Permit branch deletions",
                                    checked = allowDeletions,
                                    onToggle = { allowDeletions = it },
                                    icon = Icons.Rounded.Delete
                                )

                                ProtectionToggleRow(
                                    tag = "cr",
                                    label = "Require conversation resolution",
                                    description = "All conversations must be resolved before merge",
                                    checked = requireConversationResolution,
                                    onToggle = { requireConversationResolution = it },
                                    icon = Icons.Rounded.MarkChatRead
                                )

                                ProtectionToggleRow(
                                    tag = "ad",
                                    label = "Enforce for admins",
                                    description = "Rules apply to repository admins too",
                                    checked = enforceAdmins,
                                    onToggle = { enforceAdmins = it },
                                    icon = Icons.Rounded.AdminPanelSettings
                                )

                                ProtectionToggleRow(
                                    tag = "sig",
                                    label = "Require signed commits",
                                    description = "Reject unsigned commits on this protected branch",
                                    checked = requireSignatures,
                                    onToggle = { requireSignatures = !requireSignatures },
                                    icon = Icons.Rounded.Verified
                                )

                                ProtectionToggleRow(
                                    tag = "lh",
                                    label = "Require linear history",
                                    description = "Only allow merge commits or squash merging",
                                    checked = requiredLinearHistory,
                                    onToggle = { requiredLinearHistory = it },
                                    icon = Icons.Rounded.MergeType
                                )

                                ProtectionToggleRow(
                                    tag = "bc",
                                    label = "Block creations",
                                    description = "Prevent new branch creations matching patterns",
                                    checked = blockCreations,
                                    onToggle = { blockCreations = it },
                                    icon = Icons.Rounded.Block
                                )

                                ProtectionToggleRow(
                                    tag = "lk",
                                    label = "Lock branch",
                                    description = "Branch is read-only, no pushes allowed",
                                    checked = lockBranch,
                                    onToggle = { lockBranch = it },
                                    icon = Icons.Rounded.Lock
                                )
                            }
                        }
                    }

                    // Required Deployments
                    item { SectionHeader("Required Deployments") }
                    item {
                        SettingsCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (requiredDeployments.isNotEmpty()) {
                                    Row(
                                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        requiredDeployments.forEach { env ->
                                            ContextChip(env) {
                                                requiredDeployments = requiredDeployments - env
                                            }
                                        }
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AiModuleTextField(
                                        value = newDeployment,
                                        onValueChange = { newDeployment = it },
                                        label = "Environment name",
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    AiModulePillButton(
                                        label = "add",
                                        onClick = {
                                            if (newDeployment.isNotBlank() && newDeployment !in requiredDeployments) {
                                                requiredDeployments = requiredDeployments + newDeployment
                                                newDeployment = ""
                                            }
                                        },
                                        enabled = newDeployment.isNotBlank()
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
            }
        }
    }

    if (showDisableConfirm) {
        AiModuleAlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = "Disable branch protection?",
            content = {
                Text(
                    "This removes protection rules from $selectedBranch. Required reviews, status checks, and admin enforcement will no longer apply.",
                    fontSize = 13.sp,
                    color = AiModuleTheme.colors.textSecondary
                )
            },
            confirmButton = {
                AiModuleTextAction(label = "disable", onClick = {
                    disableConfirmed = true
                    showDisableConfirm = false
                    saveProtection()
                }, tint = Color(0xFFFF3B30))
            },
            dismissButton = {
                AiModuleTextAction(label = "cancel", onClick = {
                    showDisableConfirm = false
                    disableConfirmed = false
                    enabled = true
                }, tint = AiModuleTheme.colors.textSecondary)
            }
        )
    }
}

private data class BranchProtectionEditState(
    val enabled: Boolean,
    val requireStatusChecks: Boolean,
    val statusChecksStrict: Boolean,
    val statusCheckContexts: List<String>,
    val requirePRReviews: Boolean,
    val requiredApprovalCount: Int,
    val dismissStaleReviews: Boolean,
    val requireCodeOwnerReviews: Boolean,
    val allowForcePushes: Boolean,
    val allowDeletions: Boolean,
    val requireConversationResolution: Boolean,
    val enforceAdmins: Boolean,
    val requireSignatures: Boolean,
    val requiredLinearHistory: Boolean,
    val blockCreations: Boolean,
    val lockBranch: Boolean,
    val requiredDeployments: List<String>
)

@Composable
private fun BranchProtectionSummaryCard(branch: String, state: BranchProtectionEditState, hasUnsavedChanges: Boolean) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "bp",
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                color = if (state.enabled) Color(0xFF34C759) else AiModuleTheme.colors.textSecondary,
                modifier = Modifier.width(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(branch, fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                Text(
                    if (state.enabled) "${state.statusCheckContexts.size} checks · ${if (state.requirePRReviews) "${state.requiredApprovalCount} approvals" else "no review requirement"}"
                    else "No branch protection enabled",
                    fontSize = 11.sp,
                    color = AiModuleTheme.colors.textSecondary
                )
            }
            if (hasUnsavedChanges) {
                Text(
                    "unsaved",
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono,
                    color = Color(0xFFFF9500),
                    modifier = Modifier.clip(RoundedCornerShape(GitHubControlRadius)).border(1.dp, Color(0xFFFF9500), RoundedCornerShape(GitHubControlRadius)).background(Color(0xFFFF9500).copy(0.10f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (state.enabled) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.requireStatusChecks) MiniProtectionBadge("Status checks", AiModuleTheme.colors.accent)
                if (state.requirePRReviews) MiniProtectionBadge("Reviews", Color(0xFF34C759))
                if (state.requireConversationResolution) MiniProtectionBadge("Conversations", Color(0xFFFF9500))
                if (state.enforceAdmins) MiniProtectionBadge("Admins", Color(0xFFFF3B30))
                if (state.requireSignatures) MiniProtectionBadge("Signatures", AiModuleTheme.colors.accent)
                if (state.requiredLinearHistory) MiniProtectionBadge("Linear", AiModuleTheme.colors.accent)
                if (state.blockCreations) MiniProtectionBadge("Blocked", Color(0xFFFF3B30))
                if (state.lockBranch) MiniProtectionBadge("Locked", Color(0xFFFF9500))
                if (state.requiredDeployments.isNotEmpty()) MiniProtectionBadge("Deployments", Color(0xFF5856D6))
            }
        }
    }
}

@Composable
private fun MiniProtectionBadge(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, color, RoundedCornerShape(GitHubControlRadius))
            .background(color.copy(0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, fontFamily = JetBrainsMono, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BranchChip(name: String, selected: Boolean, protected: Boolean, onClick: () -> Unit) {
    val colors = AiModuleTheme.colors
    val tint = if (selected) colors.accent else colors.textSecondary
    Row(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, tint, RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) tint.copy(0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(name, fontSize = 12.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, color = tint)
        if (protected) {
            Text("●", fontSize = 8.sp, color = Color(0xFF34C759))
        }
    }
}

@Composable
private fun ContextChip(name: String, onRemove: () -> Unit) {
    val colors = AiModuleTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, colors.accent, RoundedCornerShape(GitHubControlRadius))
            .background(colors.accent.copy(0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, fontSize = 11.sp, fontFamily = JetBrainsMono, color = colors.accent, fontWeight = FontWeight.Medium)
        Text("×", fontSize = 12.sp, fontFamily = JetBrainsMono, color = colors.accent, modifier = Modifier.clickable { onRemove() })
    }
}

@Composable
private fun ProtectionToggleRow(
    tag: String,
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    tagColor: Color = AiModuleTheme.colors.accent,
    icon: ImageVector? = null
) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius))
            .clickable { onToggle(!checked) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = if (checked) tagColor else palette.textSecondary)
        } else {
            Text(
                tag,
                fontSize = 12.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                color = if (checked) tagColor else palette.textSecondary,
                modifier = Modifier.width(24.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = palette.textPrimary)
            Text(description, fontSize = 11.sp, color = palette.textMuted)
        }
        TerminalToggleIndicator(checked = checked, tint = if (checked) tagColor else palette.accent)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).padding(16.dp)
    ) { content() }
}

@Composable
private fun SectionHeader(title: String, color: Color = AiModuleTheme.colors.textPrimary) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(bottom = 8.dp))
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
