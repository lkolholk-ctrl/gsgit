package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import coil.compose.AsyncImage
import gs.git.vps.R
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHActionSecret
import gs.git.vps.data.github.model.GHActionVariable
import gs.git.vps.data.github.model.GHCodespace
import gs.git.vps.data.github.model.GHEnvironment
import gs.git.vps.data.github.model.GHEnvironmentProtectionRule
import gs.git.vps.data.github.model.GHDeploymentBranchPolicy
import gs.git.vps.data.github.model.GHEnvironmentSecret
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.data.github.canWrite
import gs.git.vps.data.github.KernelErrorCatalog
import gs.git.vps.data.github.KernelErrorPatterns
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHActionRunner
import gs.git.vps.data.github.model.GHActionRunnerGroup
import gs.git.vps.data.github.model.GHActionsCacheEntry
import gs.git.vps.data.github.model.GHActionsCacheUsage
import gs.git.vps.data.github.model.GHActionsPermissions
import gs.git.vps.data.github.model.GHActionsRetention
import gs.git.vps.data.github.model.GHActionsUsage
import gs.git.vps.data.github.model.GHArtifact
import gs.git.vps.data.github.model.GHCheckAnnotation
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHDeployment
import gs.git.vps.data.github.model.GHJob
import gs.git.vps.data.github.model.GHPendingDeployment
import gs.git.vps.data.github.model.GHStep
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowDispatchInput
import gs.git.vps.data.github.model.GHWorkflowDispatchSchema
import gs.git.vps.data.github.model.GHWorkflowPermissions
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHWorkflowRunReview
import gs.git.vps.ui.theme.Blue
import gs.git.vps.ui.theme.Green
import gs.git.vps.ui.theme.Orange
import gs.git.vps.ui.theme.Purple
import gs.git.vps.ui.theme.Red
import gs.git.vps.ui.theme.Teal
import gs.git.vps.ui.components.AiModuleDestructiveButton
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.components.AiModulePrimaryButton
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.aiModuleStatusBadge
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.ui.theme.SeparatorColor
import gs.git.vps.ui.theme.SurfaceLight
import gs.git.vps.ui.theme.SurfaceWhite
import gs.git.vps.ui.theme.TextPrimary
import gs.git.vps.ui.theme.TextSecondary
import gs.git.vps.ui.theme.TextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Actions-панели окружений репо: DeploymentsPanel, EnvironmentsPanel, CodespacesPanel.
 * Вынесено из GitHubActionsModule.kt (Фаза 1 UI-декомпозиции, чистое перемещение).
 */

@Composable
internal fun DeploymentsPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deployments by remember { mutableStateOf<List<GHDeployment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var showStatus by remember { mutableStateOf<GHDeployment?>(null) }

    fun load() {
        scope.launch {
            loading = true
            deployments = GitHubManager.getDeployments(context, repo.owner, repo.name)
            loading = false
        }
    }

    LaunchedEffect(repo) { load() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionsPanelHeader("Deployments", "Repository deployment history.", loading) { load() }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubTerminalButton("+ deploy", onClick = { showCreate = true }, color = Blue)
        }
        if (deployments.isEmpty() && !loading) {
            EmptyActionsText("No deployments found")
        } else {
            deployments.forEach { dep ->
                Row(
                    Modifier.fillMaxWidth().ghGlassCard(10.dp).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(dep.environment, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                        Text("${dep.ref.take(12)} · ${dep.task.takeIf { it.isNotBlank() } ?: "deploy"}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                        Text(dep.createdAt.take(19).replace('T', ' '), fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                    }
                    AiModulePillButton(label = "status", onClick = { showStatus = dep })
                }
            }
        }
    }

    if (showCreate) {
        var ref by remember { mutableStateOf("") }
        var env by remember { mutableStateOf("production") }
        var desc by remember { mutableStateOf("") }
        AiModuleAlertDialog(
            onDismissRequest = { showCreate = false },
            title = "create deployment",
            confirmButton = {
                AiModuleTextAction(label = "deploy", enabled = ref.isNotBlank(), onClick = {
                    scope.launch {
                        val ok = GitHubManager.createDeployment(context, repo.owner, repo.name, ref, env, desc)
                        Toast.makeText(context, if (ok != null) "Deployed" else "Failed", Toast.LENGTH_SHORT).show()
                        if (ok != null) load()
                        showCreate = false
                    }
                }, tint = Blue)
            },
            dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showCreate = false }) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalTextField(value = ref, onValueChange = { ref = it }, placeholder = "Ref (branch/tag/sha) *", singleLine = true)
                GitHubTerminalTextField(value = env, onValueChange = { env = it }, placeholder = "Environment", singleLine = true)
                GitHubTerminalTextField(value = desc, onValueChange = { desc = it }, placeholder = "Description", singleLine = true)
            }
        }
    }
    showStatus?.let { dep ->
        var state by remember { mutableStateOf("success") }
        AiModuleAlertDialog(
            onDismissRequest = { showStatus = null },
            title = "set status · ${dep.environment}",
            confirmButton = {
                AiModuleTextAction(label = "set", onClick = {
                    scope.launch {
                        GitHubManager.createDeploymentStatus(context, repo.owner, repo.name, dep.id, state)
                        Toast.makeText(context, "Status set", Toast.LENGTH_SHORT).show()
                        showStatus = null
                    }
                }, tint = Blue)
            },
            dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showStatus = null }) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("success", "failure", "error", "pending", "in_progress", "queued", "inactive").forEach { s ->
                    GitHubTerminalTab(label = s, selected = state == s, onClick = { state = s })
                }
            }
        }
    }
}

@Composable
internal fun EnvironmentsPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var environments by remember { mutableStateOf<List<GHEnvironment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var selectedEnv by remember { mutableStateOf<GHEnvironment?>(null) }
    var envSecrets by remember { mutableStateOf<List<GHEnvironmentSecret>>(emptyList()) }
    var showAddSecret by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            environments = GitHubManager.getEnvironments(context, repo.owner, repo.name)
            loading = false
        }
    }

    LaunchedEffect(repo) { load() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionsPanelHeader("Environments", "Repository deployment environments.", loading) { load() }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (repo.canWrite()) {
                GitHubTerminalButton("+ env", onClick = { showCreate = true }, color = Blue)
            }
        }
        if (environments.isEmpty() && !loading) {
            EmptyActionsText("No environments configured")
        } else {
            environments.forEach { env ->
                Column(
                    Modifier.fillMaxWidth().ghGlassCard(10.dp).clickable { selectedEnv = env }.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(env.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, modifier = Modifier.weight(1f))
                        if (env.protectionRules.isNotEmpty()) {
                            Text("[${env.protectionRules.size} rules]", fontSize = 10.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                        }
                        if (repo.canWrite()) {
                            IconButton(onClick = {
                                scope.launch {
                                    val ok = GitHubManager.deleteEnvironment(context, repo.owner, repo.name, env.name)
                                    Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                                    if (ok) load()
                                }
                            }) { Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.textMuted) }
                        }
                    }
                    Text(env.createdAt.take(19).replace('T', ' '), fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                }
            }
        }
    }

    if (showCreate) {
        var envName by remember { mutableStateOf("") }
        var waitTimer by remember { mutableStateOf("0") }
        AiModuleAlertDialog(
            onDismissRequest = { showCreate = false },
            title = "create environment",
            confirmButton = {
                AiModuleTextAction(label = "create", enabled = envName.isNotBlank(), onClick = {
                    scope.launch {
                        val ok = GitHubManager.createOrUpdateEnvironment(context, repo.owner, repo.name, envName, waitTimer.toIntOrNull() ?: 0)
                        Toast.makeText(context, if (ok != null) "Created" else "Failed", Toast.LENGTH_SHORT).show()
                        if (ok != null) load()
                        showCreate = false
                    }
                }, tint = Blue)
            },
            dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showCreate = false }) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalTextField(value = envName, onValueChange = { envName = it }, placeholder = "Environment name *", singleLine = true)
                GitHubTerminalTextField(value = waitTimer, onValueChange = { waitTimer = it }, placeholder = "Wait timer (minutes)", singleLine = true)
            }
        }
    }

    selectedEnv?.let { env ->
        LaunchedEffect(env.name) {
            envSecrets = GitHubManager.getEnvironmentSecrets(context, repo.owner, repo.name, env.name)
        }
        GitHubScreenFrame(title = "> env/${env.name}", onBack = { selectedEnv = null }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (repo.canWrite()) {
                        GitHubTerminalButton("+ secret", onClick = { showAddSecret = true }, color = Blue)
                    }
                }
                if (env.protectionRules.isNotEmpty()) {
                    Text("protection rules", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                    env.protectionRules.forEach { rule ->
                        Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(rule.type, fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, modifier = Modifier.weight(1f))
                            Text(if (rule.enabled) "[on]" else "[off]", fontSize = 10.sp, color = if (rule.enabled) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                        }
                    }
                }
                env.deploymentBranchPolicy?.let { policy ->
                    Text("branch policy", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                    Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(8.dp)) {
                        if (policy.protectedBranches) Text("protected branches only", fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                        if (policy.customBranchPolicies) Text("custom branch policies", fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                    }
                }
                Text("secrets", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                if (envSecrets.isEmpty()) {
                    EmptyActionsText("No environment secrets")
                } else {
                    envSecrets.forEach { secret ->
                        Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(secret.name, fontSize = 12.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono, modifier = Modifier.weight(1f))
                            if (repo.canWrite()) {
                                IconButton(onClick = {
                                    scope.launch {
                                        val ok = GitHubManager.deleteEnvironmentSecret(context, repo.owner, repo.name, env.name, secret.name)
                                        Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                                        if (ok) envSecrets = GitHubManager.getEnvironmentSecrets(context, repo.owner, repo.name, env.name)
                                    }
                                }) { Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.textMuted) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSecret && selectedEnv != null) {
        var secretName by remember { mutableStateOf("") }
        var secretValue by remember { mutableStateOf("") }
        AiModuleAlertDialog(
            onDismissRequest = { showAddSecret = false },
            title = "add secret · ${selectedEnv!!.name}",
            confirmButton = {
                AiModuleTextAction(label = "save", enabled = secretName.isNotBlank() && secretValue.isNotBlank(), onClick = {
                    scope.launch {
                        val ok = GitHubManager.createOrUpdateEnvironmentSecret(context, repo.owner, repo.name, selectedEnv!!.name, secretName, secretValue)
                        Toast.makeText(context, if (ok) "Saved" else "Failed", Toast.LENGTH_SHORT).show()
                        if (ok) envSecrets = GitHubManager.getEnvironmentSecrets(context, repo.owner, repo.name, selectedEnv!!.name)
                        showAddSecret = false
                    }
                }, tint = Blue)
            },
            dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showAddSecret = false }) },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalTextField(value = secretName, onValueChange = { secretName = it }, placeholder = "Secret name *", singleLine = true)
                GitHubTerminalTextField(value = secretValue, onValueChange = { secretValue = it }, placeholder = "Secret value *", singleLine = true)
            }
        }
    }
}

@Composable
internal fun CodespacesPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var codespaces by remember { mutableStateOf<List<GHCodespace>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            loading = true
            codespaces = GitHubManager.getCodespaces(context)
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiModuleSectionLabel("> codespaces")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiModulePillButton("refresh", onClick = { load() }, accent = false)
        }
        if (loading) {
            AiModuleSpinner(label = "loading")
        } else if (codespaces.isEmpty()) {
            Text("No codespaces found", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
        } else {
            codespaces.forEach { cs ->
                Row(Modifier.fillMaxWidth().ghGlassCard(8.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cs.displayName.ifBlank { cs.name }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                        Text("${cs.owner}/${cs.repo}  ${cs.state}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val ok = GitHubManager.deleteCodespace(context, cs.name)
                            Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            if (ok) load()
                        }
                    }) { Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = AiModuleTheme.colors.textMuted) }
                }
            }
        }
    }
}
