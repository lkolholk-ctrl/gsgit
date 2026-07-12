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
import gs.git.vps.util.DownloadStorage
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
 * Config-панели Actions репо: артефакты, кэши, variables, secrets, runners, settings +
 * инфраструктура панелей (header, rows, bulk-toolbar, empty/loading).
 * Вынесено из GitHubActionsModule.kt (Фаза 1, чистое перемещение).
 */

@Composable
private fun RepositoryArtifactsPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artifacts by remember { mutableStateOf<List<GHArtifact>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var busyArtifact by remember { mutableStateOf<Long?>(null) }
    var selectedArtifactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var bulkConfirm by remember { mutableStateOf("") }
    var bulkDeleting by remember { mutableStateOf(false) }

    suspend fun load(reset: Boolean = true) {
        loading = true
        val nextPage = if (reset) 1 else page + 1
        val fetched = GitHubManager.getRepositoryArtifacts(context, repo.owner, repo.name, nextPage, query)
        page = nextPage
        hasMore = fetched.size >= 100
        artifacts = if (reset) fetched else (artifacts + fetched).distinctBy { it.id }
        if (reset) {
            selectedArtifactIds = emptySet()
            bulkConfirm = ""
        }
        loading = false
    }

    LaunchedEffect(repo.owner, repo.name) { load(true) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ActionsPanelHeader(
                title = "Repository artifacts",
                subtitle = "All workflow artifacts across runs, with download and delete actions.",
                loading = loading,
                onRefresh = { scope.launch { load(true) } }
            )
            AiModuleTextField(
                value = query,
                onValueChange = { query = it },
                label = "Artifact name filter",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalButton("apply", onClick = { scope.launch { load(true) } }, enabled = !loading, color = Blue)
                GitHubTerminalButton(
                    "export visible",
                    onClick = {
                        val file = saveActionsArtifactsExport(repo, artifacts, "repository-artifacts")
                        Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                    },
                    enabled = artifacts.isNotEmpty() && !loading,
                    color = Blue,
                )
            }
            ActionsBulkToolbar(
                selectedCount = selectedArtifactIds.size,
                totalCount = artifacts.size,
                confirm = bulkConfirm,
                confirmTarget = "delete ${selectedArtifactIds.size}",
                deleting = bulkDeleting,
                onConfirmChange = { bulkConfirm = it },
                onSelectVisible = {
                    selectedArtifactIds = artifacts.map { it.id }.toSet()
                    bulkConfirm = ""
                },
                onSelectExpired = {
                    selectedArtifactIds = artifacts.filter { it.expired }.map { it.id }.toSet()
                    bulkConfirm = ""
                },
                onClear = { selectedArtifactIds = emptySet(); bulkConfirm = "" },
                onDelete = {
                    val ids = selectedArtifactIds
                    if (ids.isEmpty() || bulkConfirm.trim() != "delete ${ids.size}") return@ActionsBulkToolbar
                    bulkDeleting = true
                    scope.launch {
                        var deleted = 0
                        try {
                            ids.forEach { id ->
                                if (runCatching { GitHubManager.deleteArtifact(context, repo.owner, repo.name, id) }.getOrDefault(false)) deleted++
                            }
                            Toast.makeText(context, "Deleted $deleted/${ids.size}", Toast.LENGTH_SHORT).show()
                            load(true)
                        } finally {
                            bulkDeleting = false
                        }
                    }
                },
            )
        }
        if (artifacts.isEmpty() && !loading) {
            item { EmptyActionsText("No artifacts found") }
        }
        items(artifacts) { artifact ->
            ArtifactRow(
                artifact = artifact,
                busy = busyArtifact == artifact.id,
                disabled = bulkDeleting,
                selected = artifact.id in selectedArtifactIds,
                onToggleSelected = {
                    selectedArtifactIds = if (artifact.id in selectedArtifactIds) selectedArtifactIds - artifact.id else selectedArtifactIds + artifact.id
                    bulkConfirm = ""
                },
                onDownload = {
                    busyArtifact = artifact.id
                    scope.launch {
                        val dest = DownloadStorage.file(context, safeArtifactZipName(artifact))
                        val ok = GitHubManager.downloadArtifact(context, repo.owner, repo.name, artifact.id, dest)
                        Toast.makeText(context, if (ok) "${Strings.done}: ${dest.name}" else Strings.error, Toast.LENGTH_SHORT).show()
                        busyArtifact = null
                    }
                },
                onDelete = {
                    busyArtifact = artifact.id
                    scope.launch {
                        val ok = GitHubManager.deleteArtifact(context, repo.owner, repo.name, artifact.id)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        busyArtifact = null
                        if (ok) load(true)
                    }
                }
            )
        }
        if (hasMore) {
            item {
                GitHubTerminalButton("load more artifacts", onClick = { scope.launch { load(false) } }, enabled = !loading, color = Blue, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ActionsCachesPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf<GHActionsCacheUsage?>(null) }
    var caches by remember { mutableStateOf<List<GHActionsCacheEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Long?>(null) }
    var selectedCacheIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var bulkConfirm by remember { mutableStateOf("") }
    var bulkDeleting by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        usage = GitHubManager.getActionsCacheUsage(context, repo.owner, repo.name)
        caches = GitHubManager.getActionsCaches(context, repo.owner, repo.name)
        selectedCacheIds = emptySet()
        bulkConfirm = ""
        loading = false
    }

    LaunchedEffect(repo.owner, repo.name) { load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ActionsPanelHeader("Actions caches", "Repository cache usage and cache entries.", loading) { scope.launch { load() } }
            usage?.let {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Caches", it.activeCachesCount.toString(), Icons.Rounded.Timeline, Blue)
                    StatCard("Size", formatArtifactSize(it.activeCachesSizeInBytes), Icons.Rounded.Article, Green)
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubTerminalButton(
                    "export caches",
                    onClick = {
                        val file = saveActionsCachesExport(repo, usage, caches)
                        Toast.makeText(context, file?.let { "${Strings.done}: ${it.name}" } ?: Strings.error, Toast.LENGTH_SHORT).show()
                    },
                    enabled = caches.isNotEmpty() && !loading,
                    color = Blue,
                )
                var deleteKey by remember { mutableStateOf("") }
                var showDeleteByKey by remember { mutableStateOf(false) }
                GitHubTerminalButton("delete by key", onClick = { showDeleteByKey = true }, color = Red)
                if (showDeleteByKey) {
                    AiModuleAlertDialog(
                        onDismissRequest = { showDeleteByKey = false },
                        title = "delete caches by key",
                        confirmButton = {
                            AiModuleTextAction(label = "delete", enabled = deleteKey.isNotBlank(), onClick = {
                                scope.launch {
                                    val ok = GitHubManager.deleteActionsCacheByKey(context, repo.owner, repo.name, deleteKey)
                                    Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                                    if (ok) load()
                                    showDeleteByKey = false
                                    deleteKey = ""
                                }
                            }, tint = Red)
                        },
                        dismissButton = { AiModuleTextAction(label = "cancel", onClick = { showDeleteByKey = false }) },
                    ) {
                        GitHubTerminalTextField(value = deleteKey, onValueChange = { deleteKey = it }, placeholder = "Cache key", singleLine = true)
                    }
                }
            }
            ActionsBulkToolbar(
                selectedCount = selectedCacheIds.size,
                totalCount = caches.size,
                confirm = bulkConfirm,
                confirmTarget = "delete ${selectedCacheIds.size}",
                deleting = bulkDeleting,
                onConfirmChange = { bulkConfirm = it },
                onSelectVisible = {
                    selectedCacheIds = caches.map { it.id }.toSet()
                    bulkConfirm = ""
                },
                onSelectExpired = null,
                onClear = { selectedCacheIds = emptySet(); bulkConfirm = "" },
                onDelete = {
                    val ids = selectedCacheIds
                    if (ids.isEmpty() || bulkConfirm.trim() != "delete ${ids.size}") return@ActionsBulkToolbar
                    bulkDeleting = true
                    scope.launch {
                        var deleted = 0
                        try {
                            ids.forEach { id ->
                                if (runCatching { GitHubManager.deleteActionsCache(context, repo.owner, repo.name, id) }.getOrDefault(false)) deleted++
                            }
                            Toast.makeText(context, "Deleted $deleted/${ids.size}", Toast.LENGTH_SHORT).show()
                            load()
                        } finally {
                            bulkDeleting = false
                        }
                    }
                },
            )
        }
        if (caches.isEmpty() && !loading) item { EmptyActionsText("No caches found") }
        items(caches) { cache ->
            ActionsCacheRow(
                cache = cache,
                selected = cache.id in selectedCacheIds,
                deleting = deleting == cache.id,
                disabled = bulkDeleting,
                onToggleSelected = {
                    selectedCacheIds = if (cache.id in selectedCacheIds) selectedCacheIds - cache.id else selectedCacheIds + cache.id
                    bulkConfirm = ""
                },
                onDelete = {
                    deleting = cache.id
                    scope.launch {
                        val ok = GitHubManager.deleteActionsCache(context, repo.owner, repo.name, cache.id)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        deleting = null
                        if (ok) load()
                    }
                },
            )
        }
    }
}

@Composable
private fun ActionsVariablesPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var variables by remember { mutableStateOf<List<GHActionVariable>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        variables = GitHubManager.getRepoActionsVariables(context, repo.owner, repo.name)
        loading = false
    }

    LaunchedEffect(repo.owner, repo.name) { load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ActionsPanelHeader("Actions variables", "Create, update and delete repository variables.", loading) { scope.launch { load() } }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiModuleTextField(name, { name = it }, label = "Name", modifier = Modifier.fillMaxWidth(), singleLine = true)
                AiModuleTextField(value, { value = it }, label = "Value", modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 3)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GitHubTerminalButton("save variable", onClick = {
                        if (name.isBlank()) return@GitHubTerminalButton
                        scope.launch {
                            val existing = variables.any { it.name == name }
                            val ok = if (existing) GitHubManager.updateRepoActionsVariable(context, repo.owner, repo.name, name, value)
                            else GitHubManager.createRepoActionsVariable(context, repo.owner, repo.name, name, value)
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            if (ok) { name = ""; value = ""; load() }
                        }
                    }, color = Blue)
                }
            }
        }
        if (variables.isEmpty() && !loading) item { EmptyActionsText("No variables found") }
        items(variables) { variable ->
            ActionInfoCard(
                title = variable.name,
                subtitle = variable.value,
                meta = listOf("Updated ${variable.updatedAt.take(10)}"),
                actionLabel = "Delete",
                actionTint = Red,
                onAction = {
                    scope.launch {
                        val ok = GitHubManager.deleteRepoActionsVariable(context, repo.owner, repo.name, variable.name)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) load()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionsSecretsPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var secrets by remember { mutableStateOf<List<GHActionSecret>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val validName = remember(name) { normalizeActionsSecretName(name) }

    suspend fun load() {
        loading = true
        secrets = GitHubManager.getRepoActionsSecrets(context, repo.owner, repo.name)
        loading = false
    }

    LaunchedEffect(repo.owner, repo.name) { load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ActionsPanelHeader("Actions secrets", "Create, update and delete repository secrets.", loading || saving) { scope.launch { load() } }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AiModuleTextField(
                    value = name,
                    onValueChange = { input ->
                        name = input.uppercase(Locale.US).filter { it == '_' || it in 'A'..'Z' || it in '0'..'9' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Secret name",
                    singleLine = true
                )
                if (name.isNotBlank() && validName == null) {
                    Text(
                        "Name must start with a letter or _, and cannot start with GITHUB_.",
                        fontSize = 10.sp,
                        color = Red,
                    )
                }
                AiModuleTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Secret value",
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Encrypted on device with GitHub's public key. Fine-grained PAT: Secrets read/write.",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    GitHubTerminalButton("save secret", enabled = !saving && validName != null && value.isNotBlank(), onClick = {
                        scope.launch {
                            saving = true
                            val ok = GitHubManager.createOrUpdateRepoActionsSecret(context, repo.owner, repo.name, validName.orEmpty(), value)
                            saving = false
                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                            if (ok) { name = ""; value = ""; load() }
                        }
                    }, color = Blue)
                }
            }
        }
        if (secrets.isEmpty() && !loading) item { EmptyActionsText("No secrets found") }
        items(secrets) { secret ->
            ActionInfoCard(
                title = secret.name,
                subtitle = "Secret value is never returned by GitHub",
                meta = listOf("Updated ${secret.updatedAt.take(10)}"),
                actionLabel = "Delete",
                actionTint = Red,
                onAction = {
                    scope.launch {
                        val ok = GitHubManager.deleteRepoActionsSecret(context, repo.owner, repo.name, secret.name)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) load()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionsRunnersPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var runners by remember { mutableStateOf<List<GHActionRunner>>(emptyList()) }
    var runnerGroups by remember { mutableStateOf<List<GHActionRunnerGroup>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<GHActionRunnerGroup?>(null) }
    var groupRunners by remember { mutableStateOf<List<GHActionRunner>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingGroup by remember { mutableStateOf(false) }
    var runnerToken by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        runners = GitHubManager.getRepoSelfHostedRunners(context, repo.owner, repo.name)
        runnerGroups = GitHubManager.getOrgRunnerGroups(context, repo.owner)
        loading = false
    }

    suspend fun loadGroup(group: GHActionRunnerGroup) {
        selectedGroup = group
        loadingGroup = true
        groupRunners = GitHubManager.getOrgRunnerGroupRunners(context, repo.owner, group.id)
        loadingGroup = false
    }

    LaunchedEffect(repo.owner, repo.name) { load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ActionsPanelHeader("Self-hosted runners", "Repository self-hosted runner status, labels and registration tokens.", loading) { scope.launch { load() } }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(Icons.Rounded.PlayArrow, "Registration token") {
                        scope.launch {
                            val token = GitHubManager.createRepoRunnerRegistrationToken(context, repo.owner, repo.name)
                            runnerToken = token?.let { "registration: ${it.token}\nexpires: ${it.expiresAt}" }.orEmpty()
                            Toast.makeText(context, if (token != null) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    Chip(Icons.Rounded.Delete, "Remove token", Orange) {
                        scope.launch {
                            val token = GitHubManager.createRepoRunnerRemoveToken(context, repo.owner, repo.name)
                            runnerToken = token?.let { "remove: ${it.token}\nexpires: ${it.expiresAt}" }.orEmpty()
                            Toast.makeText(context, if (token != null) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                if (runnerToken.isNotBlank()) {
                    Text(runnerToken, fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Timeline, null, tint = Purple, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Runner groups", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Organization/Enterprise runner groups for ${repo.owner}. Requires eligible org access.", fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                    }
                }
                if (runnerGroups.isEmpty() && !loading) {
                    Text("// no runner groups returned", fontSize = 12.sp, color = TextTertiary, fontFamily = JetBrainsMono)
                } else {
                    runnerGroups.forEach { group ->
                        RunnerGroupRow(
                            group = group,
                            selected = selectedGroup?.id == group.id,
                            onOpen = { scope.launch { loadGroup(group) } },
                        )
                    }
                }
                selectedGroup?.let { group ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SeparatorColor))
                    Text("group:${group.name}", fontSize = 12.sp, color = Purple, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold)
                    when {
                        loadingGroup -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiModuleSpinner()
                            Text("loading group runners", fontSize = 12.sp, color = TextSecondary, fontFamily = JetBrainsMono)
                        }
                        groupRunners.isEmpty() -> Text("// no runners in this group", fontSize = 12.sp, color = TextTertiary, fontFamily = JetBrainsMono)
                        else -> groupRunners.forEach { runner ->
                            RunnerInlineRow(runner)
                        }
                    }
                }
            }
        }
        if (runners.isEmpty() && !loading) item { EmptyActionsText("No self-hosted runners found") }
        items(runners) { runner ->
            ActionInfoCard(
                title = runner.name,
                subtitle = "${runner.os} • ${runner.status}${if (runner.busy) " • busy" else ""}",
                meta = runner.labels,
                actionLabel = "Delete",
                actionTint = Red,
                onAction = {
                    scope.launch {
                        val ok = GitHubManager.deleteRepoSelfHostedRunner(context, repo.owner, repo.name, runner.id)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        if (ok) load()
                    }
                }
            )
        }
    }
}

@Composable
private fun RunnerGroupRow(group: GHActionRunnerGroup, selected: Boolean, onOpen: () -> Unit) {
    val tint = if (selected) Purple else TextSecondary
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(GitHubControlRadius))
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.name.ifBlank { "runner group #${group.id}" }, fontSize = 13.sp, color = TextPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            GitHubTerminalButton("runners", onClick = onOpen, color = tint)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniActionsBadge(group.visibility.ifBlank { "visibility?" }, Purple)
            if (group.isDefault) MiniActionsBadge("default", Blue)
            if (group.inherited) MiniActionsBadge("inherited", TextSecondary)
            if (group.allowsPublicRepositories) MiniActionsBadge("public repos", Green)
            if (group.restrictedToWorkflows) MiniActionsBadge("workflow-restricted", Orange)
        }
        if (group.selectedWorkflows.isNotEmpty()) {
            Text(
                group.selectedWorkflows.take(3).joinToString(" | "),
                fontSize = 10.sp,
                color = TextTertiary,
                fontFamily = JetBrainsMono,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RunnerInlineRow(runner: GHActionRunner) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, SeparatorColor, RoundedCornerShape(GitHubControlRadius)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (runner.busy) "●" else "○", color = if (runner.busy) Orange else Green, fontSize = 12.sp, fontFamily = JetBrainsMono)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(runner.name, fontSize = 12.sp, color = TextPrimary, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${runner.os} · ${runner.status}", fontSize = 10.sp, color = TextTertiary, fontFamily = JetBrainsMono)
        }
    }
}

@Composable
private fun ActionsSettingsPanel(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissions by remember { mutableStateOf<GHActionsPermissions?>(null) }
    var workflowPermissions by remember { mutableStateOf<GHWorkflowPermissions?>(null) }
    var retention by remember { mutableStateOf<GHActionsRetention?>(null) }
    var actionsEnabled by remember { mutableStateOf(false) }
    var allowedActions by remember { mutableStateOf("all") }
    var defaultWorkflowPermissions by remember { mutableStateOf("read") }
    var canApprovePullRequestReviews by remember { mutableStateOf(false) }
    var retentionDays by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        val loadedPermissions = GitHubManager.getRepoActionsPermissions(context, repo.owner, repo.name)
        val loadedWorkflowPermissions = GitHubManager.getRepoActionsWorkflowPermissions(context, repo.owner, repo.name)
        val loadedRetention = GitHubManager.getRepoActionsRetention(context, repo.owner, repo.name)
        permissions = loadedPermissions
        workflowPermissions = loadedWorkflowPermissions
        retention = loadedRetention
        actionsEnabled = loadedPermissions?.enabled ?: false
        allowedActions = normalizeActionsAllowedPolicy(loadedPermissions?.allowedActions)
        defaultWorkflowPermissions = normalizeWorkflowTokenPermission(loadedWorkflowPermissions?.defaultWorkflowPermissions)
        canApprovePullRequestReviews = loadedWorkflowPermissions?.canApprovePullRequestReviews ?: false
        retentionDays = loadedRetention?.days?.takeIf { it > 0 }?.toString().orEmpty()
        loading = false
    }

    LaunchedEffect(repo.owner, repo.name) { load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionsPanelHeader("Actions settings", "Repository Actions permissions, workflow token policy and retention.", loading || saving != null) { scope.launch { load() } }

        ActionsSettingsCard(
            title = "Actions permissions",
            subtitle = if (permissions?.enabled == true) "Enabled" else "Disabled or unavailable",
            meta = listOf("Allowed actions: ${permissions?.allowedActions.orEmpty().ifBlank { "unknown" }}")
        ) {
            GitHubTerminalCheckbox(
                "enabled",
                checked = actionsEnabled,
                onToggle = { actionsEnabled = !actionsEnabled },
                enabled = saving == null
            )
            Text("Allowed actions", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                actionsAllowedPolicies.forEach { policy ->
                    GitHubTerminalTab(policy, selected = allowedActions == policy) { allowedActions = policy }
                }
            }
            GitHubTerminalButton(
                if (saving == "actions") "saving..." else "save actions policy",
                enabled = saving == null,
                color = Blue,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = "actions"
                    scope.launch {
                        val ok = GitHubManager.setRepoActionsPermissions(context, repo.owner, repo.name, actionsEnabled, allowedActions)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        saving = null
                        if (ok) load()
                    }
                }
            )
        }

        ActionsSettingsCard(
            title = "Workflow permissions",
            subtitle = workflowPermissions?.defaultWorkflowPermissions.orEmpty().ifBlank { "Unavailable" },
            meta = listOf("Approve PR reviews: ${workflowPermissions?.canApprovePullRequestReviews ?: false}")
        ) {
            Text("Default GITHUB_TOKEN permission", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                workflowTokenPermissions.forEach { value ->
                    GitHubTerminalTab(value, selected = defaultWorkflowPermissions == value) {
                        defaultWorkflowPermissions = value
                    }
                }
            }
            GitHubTerminalCheckbox(
                "allow actions to approve pull requests",
                checked = canApprovePullRequestReviews,
                onToggle = { canApprovePullRequestReviews = !canApprovePullRequestReviews },
                enabled = saving == null
            )
            GitHubTerminalButton(
                if (saving == "workflow") "saving..." else "save workflow permissions",
                enabled = saving == null,
                color = Blue,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = "workflow"
                    scope.launch {
                        val ok = GitHubManager.setRepoActionsWorkflowPermissions(
                            context,
                            repo.owner,
                            repo.name,
                            defaultWorkflowPermissions,
                            canApprovePullRequestReviews
                        )
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        saving = null
                        if (ok) load()
                    }
                }
            )
        }

        ActionsSettingsCard(
            title = "Artifact and log retention",
            subtitle = retention?.days?.takeIf { it > 0 }?.let { "$it days" } ?: "Unavailable",
            meta = emptyList()
        ) {
            Text("Retention days", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
            GitHubTerminalTextField(
                value = retentionDays,
                onValueChange = { retentionDays = it.filter { char -> char.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "90",
                singleLine = true,
                minHeight = 38.dp
            )
            GitHubTerminalButton(
                if (saving == "retention") "saving..." else "save retention",
                enabled = saving == null && retentionDays.toIntOrNull()?.let { it > 0 } == true,
                color = Blue,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val days = retentionDays.toIntOrNull() ?: return@GitHubTerminalButton
                    saving = "retention"
                    scope.launch {
                        val ok = GitHubManager.setRepoActionsRetention(context, repo.owner, repo.name, days)
                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                        saving = null
                        if (ok) load()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionsSettingsCard(
    title: String,
    subtitle: String,
    meta: List<String>,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.textMuted.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title.lowercase(Locale.US), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary, fontFamily = JetBrainsMono)
        Text(subtitle, fontSize = 12.sp, color = palette.textSecondary, fontFamily = JetBrainsMono, lineHeight = 16.sp)
        if (meta.any { it.isNotBlank() }) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                meta.filter { it.isNotBlank() }.forEach { MiniActionsBadge(it, palette.textSecondary) }
            }
        }
        content()
    }
}

private val actionsAllowedPolicies = listOf("all", "local_only", "selected")
private val workflowTokenPermissions = listOf("read", "write")

private fun normalizeActionsAllowedPolicy(value: String?): String =
    value?.takeIf { it in actionsAllowedPolicies } ?: "all"

private fun normalizeWorkflowTokenPermission(value: String?): String =
    value?.takeIf { it in workflowTokenPermissions } ?: "read"

@Composable
internal fun ActionsPanelHeader(title: String, subtitle: String, loading: Boolean, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.Timeline, null, tint = Blue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
        IconButton(onClick = onRefresh) {
            if (loading) AiModuleSpinner()
            else Icon(Icons.Rounded.Refresh, null, tint = Blue)
        }
    }
}

@Composable
private fun ArtifactRow(
    artifact: GHArtifact,
    busy: Boolean,
    disabled: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GitHubTerminalCheckbox("", selected, onToggleSelected, enabled = !busy && !disabled)
        Icon(Icons.Rounded.Article, null, tint = if (artifact.expired) TextTertiary else Blue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(artifact.name, fontSize = 14.sp, color = if (artifact.expired) TextTertiary else TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                artifactKindBadges(artifact).forEach { (label, color) -> MiniActionsBadge(label, color) }
                MiniActionsBadge(formatArtifactSize(artifact.sizeInBytes), TextSecondary)
                if (artifact.expired) MiniActionsBadge("expired", Red)
                if (artifact.workflowRunId > 0) MiniActionsBadge("#${artifact.workflowRunId}", Blue)
                if (artifact.workflowRunBranch.isNotBlank()) MiniActionsBadge(artifact.workflowRunBranch, Blue)
                if (artifact.workflowRunSha.length >= 7) MiniActionsBadge(artifact.workflowRunSha.take(7), TextSecondary)
            }
            Text("Created ${artifact.createdAt.take(10)} • Expires ${artifact.expiresAt.take(10)}", fontSize = 11.sp, color = TextTertiary)
            if (artifact.digest.isNotBlank()) Text(artifact.digest, fontSize = 10.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (busy) AiModuleSpinner()
        else {
            IconButton(onClick = onDownload, enabled = !artifact.expired && !disabled) { Icon(Icons.Rounded.Article, null, tint = if (artifact.expired || disabled) TextTertiary else Blue) }
            IconButton(onClick = onDelete, enabled = !disabled) { Icon(Icons.Rounded.Delete, null, tint = if (disabled) TextTertiary else Red) }
        }
    }
}

@Composable
private fun ActionsCacheRow(
    cache: GHActionsCacheEntry,
    selected: Boolean,
    deleting: Boolean,
    disabled: Boolean,
    onToggleSelected: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GitHubTerminalCheckbox("", selected, onToggleSelected, enabled = !deleting && !disabled)
        Icon(Icons.Rounded.Article, null, tint = Blue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(cache.key, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatArtifactSize(cache.sizeInBytes)} • ${cache.ref}", fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniActionsBadge("Created ${cache.createdAt.take(10)}", TextSecondary)
                MiniActionsBadge("Last used ${cache.lastAccessedAt.take(10)}", TextSecondary)
                if (cache.version.isNotBlank()) MiniActionsBadge(cache.version.take(12), TextSecondary)
            }
        }
        GitHubTerminalButton(if (deleting) "deleting" else "delete", onClick = onDelete, color = Red, enabled = !deleting && !disabled)
    }
}

@Composable
private fun ActionsBulkToolbar(
    selectedCount: Int,
    totalCount: Int,
    confirm: String,
    confirmTarget: String,
    deleting: Boolean,
    onConfirmChange: (String) -> Unit,
    onSelectVisible: () -> Unit,
    onSelectExpired: (() -> Unit)?,
    onClear: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text("bulk cleanup", fontSize = 13.sp, color = TextPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold)
            Text("selected $selectedCount / $totalCount", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GitHubTerminalButton("select visible", onClick = onSelectVisible, color = Blue, enabled = totalCount > 0 && !deleting)
            if (onSelectExpired != null) GitHubTerminalButton("select expired", onClick = onSelectExpired, color = Orange, enabled = totalCount > 0 && !deleting)
            GitHubTerminalButton("clear", onClick = onClear, color = TextSecondary, enabled = selectedCount > 0 && !deleting)
        }
        if (selectedCount > 0) {
            Text("Type `$confirmTarget` to delete selected items.", fontSize = 11.sp, color = TextTertiary, fontFamily = JetBrainsMono)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GitHubTerminalTextField(
                    value = confirm,
                    onValueChange = onConfirmChange,
                    modifier = Modifier.weight(1f),
                    placeholder = confirmTarget,
                    singleLine = true,
                    minHeight = 38.dp,
                )
                GitHubTerminalButton(
                    if (deleting) "deleting..." else "delete selected",
                    onClick = onDelete,
                    color = Red,
                    enabled = !deleting && confirm.trim() == confirmTarget,
                )
            }
        }
    }
}

@Composable
internal fun ArtifactRunRow(
    artifact: GHArtifact,
    downloading: Boolean,
    deleting: Boolean,
    onCopyName: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().border(1.dp, palette.border).background(palette.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = !artifact.expired && !downloading, onClick = onDownload),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("▸", color = if (artifact.expired) palette.textMuted else palette.accent, fontFamily = JetBrainsMono, fontSize = 13.sp)
            Text(artifact.name, fontSize = 14.sp, color = if (artifact.expired) palette.textMuted else palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (!artifact.expired) {
                GitHubTerminalButton("⧉ copy", onClick = onCopyName, color = palette.textSecondary)
                GitHubTerminalButton(if (deleting) "deleting..." else "× delete", onClick = onDelete, color = palette.error, enabled = !deleting)
            }
            if (downloading) Text("⠋", fontSize = 13.sp, fontFamily = JetBrainsMono, color = palette.accent)
        }
        Text(
            listOf(formatArtifactSize(artifact.sizeInBytes), artifact.createdAt.take(10)).filter { it.isNotBlank() }.joinToString(" · "),
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun ActionInfoCard(
    title: String,
    subtitle: String,
    meta: List<String>,
    actionLabel: String?,
    actionTint: Color = Blue,
    onAction: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.Article, null, tint = Blue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (meta.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    meta.filter { it.isNotBlank() }.forEach { MiniActionsBadge(it, TextSecondary) }
                }
            }
        }
        if (actionLabel != null) {
            GitHubTerminalButton(actionLabel.lowercase(), onClick = onAction, color = actionTint)
        }
    }
}

@Composable
internal fun EmptyActionsText(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(SurfaceWhite).padding(18.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = TextTertiary)
    }
}

@Composable
internal fun LoadingActionsText(text: String) {
    Box(Modifier.fillMaxWidth().border(1.dp, AiModuleTheme.colors.border).background(AiModuleTheme.colors.surface).padding(18.dp), contentAlignment = Alignment.Center) {
        Text("⠋ ${text.lowercase()}", fontSize = 13.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textMuted)
    }
}
