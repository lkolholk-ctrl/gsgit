package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.github.model.GHAppInstallation
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

@Composable
internal fun GitHubAppsScreen(
    onBack: () -> Unit,
    onRepoClick: (GHRepo) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableIntStateOf(0) }
    var installations by remember { mutableStateOf<List<GHAppInstallation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var errorCode by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<GHAppInstallation?>(null) }
    val prefs = remember { context.getSharedPreferences("github_prefs", android.content.Context.MODE_PRIVATE) }
    var pendingProviderId by rememberSaveable {
        mutableStateOf(prefs.getString("pending_github_app_provider", "").orEmpty())
    }

    fun load(reset: Boolean = false) {
        loading = true
        error = ""
        errorCode = 0
        scope.launch {
            val nextPage = if (reset) 1 else page
            val result = GitHubManager.getAppInstallations(context, nextPage)
            if (result.error.isBlank()) {
                page = nextPage
                totalCount = result.totalCount
                installations = if (reset || nextPage == 1) result.installations else installations + result.installations
            } else {
                error = result.error
                errorCode = result.code
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load(reset = true) }
    LaunchedEffect(installations, pendingProviderId) {
        val pending = officialGitHubAiIntegrations.firstOrNull { it.id == pendingProviderId }
        if (pending != null && installations.any { it.appSlug == pending.appSlug }) {
            pendingProviderId = ""
            prefs.edit().remove("pending_github_app_provider").apply()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, pendingProviderId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingProviderId.isNotBlank()) {
                load(reset = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openExternal(url: String) {
        if (!context.openExternalHttps(url)) {
            Toast.makeText(context, "No external browser available", Toast.LENGTH_SHORT).show()
        }
    }

    fun install(provider: GitHubAiIntegration) {
        pendingProviderId = provider.id
        prefs.edit().putString("pending_github_app_provider", provider.id).apply()
        openExternal(provider.installUrl)
    }

    fun handleAppsBack() {
        if (selected != null) selected = null else onBack()
    }

    selected?.let { installation ->
        GitHubAppInstallationDetailScreen(
            installation = installation,
            onBack = ::handleAppsBack,
            onRepoClick = onRepoClick,
        )
        return
    }

    GitHubScreenFrame(
        title = "> apps",
        onBack = ::handleAppsBack,
        subtitle = when {
            loading -> "syncing GitHub Apps..."
            error.isNotBlank() -> "connectors ready · API sync unavailable"
            else -> "connectors · $totalCount installations"
        },
        trailing = {
            GitHubTopBarAction(
                glyph = GhGlyphs.REFRESH,
                onClick = { load(reset = true) },
                tint = AiModuleTheme.colors.accent,
                contentDescription = "refresh apps",
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GitHubAiIntegrationsIntro(
                    onManageInstallations = { openExternal("https://github.com/settings/installations") },
                )
            }
            items(officialGitHubAiIntegrations, key = { it.id }) { provider ->
                val installation = installations.firstOrNull { it.appSlug == provider.appSlug }
                GitHubAiIntegrationCard(
                    provider = provider,
                    installation = installation,
                    pending = pendingProviderId == provider.id,
                    onInstall = { install(provider) },
                    onOpenApp = { openExternal(provider.appUrl) },
                    onGuide = { openExternal(provider.guideUrl) },
                    onOpenInstallation = installation?.htmlUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        { openExternal(url) }
                    },
                )
            }
            item { GitHubSupportedLinksCard() }
            item {
                GitHubTerminalSectionLabel(
                    label = "installation API",
                    color = AiModuleTheme.colors.textSecondary,
                )
            }
            when {
                loading && installations.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "checking installations...")
                    }
                }
                error.isNotBlank() -> item {
                    GitHubAppsApiNotice(errorCode = errorCode, error = error)
                }
                installations.isEmpty() -> item {
                    GitHubInlineTerminalNotice(
                        glyph = GhGlyphs.INFO,
                        title = "no readable installations",
                        subtitle = "Install a connector above, then return and refresh.",
                    )
                }
                else -> {
                    item { GitHubAppsSummary(installations, totalCount) }
                items(installations, key = { it.id }) { installation ->
                    GitHubAppInstallationRow(installation) { selected = installation }
                    AiModuleHairline()
                }
                if (installations.size < totalCount) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            GitHubTerminalButton(
                                label = "load more ->",
                                onClick = {
                                    page += 1
                                    load(reset = false)
                                },
                                color = AiModuleTheme.colors.accent,
                                enabled = !loading,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubAiIntegrationsIntro(onManageInstallations: () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
            .background(palette.accent.copy(alpha = 0.07f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "AI access to GitHub",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Text(
            text = "GitHub performs the installation and shows the real choice: all repositories or only selected repositories. GsGit never asks for an App private key.",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GitHubTerminalButton(
                label = "manage on GitHub ->",
                onClick = onManageInstallations,
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun GitHubAiIntegrationCard(
    provider: GitHubAiIntegration,
    installation: GHAppInstallation?,
    pending: Boolean,
    onInstall: () -> Unit,
    onOpenApp: () -> Unit,
    onGuide: () -> Unit,
    onOpenInstallation: (() -> Unit)?,
) {
    val palette = AiModuleTheme.colors
    val installed = installation != null
    val statusColor = when {
        installed -> palette.success
        pending -> palette.warning
        else -> palette.textMuted
    }
    val status = when {
        installed && installation.repositorySelection == "all" -> "connected · all repositories"
        installed && installation.repositorySelection == "selected" -> "connected · selected repositories"
        installed -> "connected · selection unknown"
        pending -> "setup opened · check GitHub"
        else -> "not verified"
    }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, if (installed) palette.success.copy(alpha = 0.55f) else palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = provider.title,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "github.com/apps/${provider.appSlug}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GitHubTerminalPill(status, statusColor)
        }
        Text(
            text = provider.description,
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
        Text(
            text = provider.finishHint,
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GitHubTerminalButton(
                label = if (installed) "change repositories ->" else "install / select repos ->",
                onClick = onOpenInstallation ?: onInstall,
                color = palette.accent,
            )
            GitHubTerminalButton(label = "app", onClick = onOpenApp, color = palette.textSecondary)
            GitHubTerminalButton(label = "guide", onClick = onGuide, color = palette.textSecondary)
        }
    }
}

@Composable
private fun GitHubSupportedLinksCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeCount by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeCount++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val status = remember(resumeCount) { context.getGitHubSupportedLinksStatus() }
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "supported links",
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            GitHubTerminalPill(status.label, if (status.enabled) palette.success else palette.warning)
        }
        Text(status.detail, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        Text(
            text = "GitHub owns github.com, so Android cannot auto-verify that domain for GsGit. You can explicitly select it in system settings.",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            GitHubTerminalButton(
                label = "open Android link settings ->",
                onClick = {
                    if (!context.openSupportedLinksSettings()) {
                        Toast.makeText(context, "Link settings unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun GitHubAppsApiNotice(errorCode: Int, error: String) {
    val isTokenMismatch = errorCode == 403 || error.contains("authorized to a GitHub App", ignoreCase = true)
    GitHubInlineTerminalNotice(
        glyph = GhGlyphs.WARN,
        title = if (isTokenMismatch) "installation status not readable by PAT" else "installation API unavailable",
        subtitle = if (isTokenMismatch) {
            "Your token can still manage its repository. GitHub only returns /user/installations to a compatible GitHub App user token; install and configure connectors with the official buttons above."
        } else {
            error.take(180)
        },
    )
}

@Composable
private fun GitHubAppInstallationDetailScreen(
    installation: GHAppInstallation,
    onBack: () -> Unit,
    onRepoClick: (GHRepo) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember(installation.id) { mutableIntStateOf(1) }
    var totalCount by remember(installation.id) { mutableIntStateOf(0) }
    var repos by remember(installation.id) { mutableStateOf<List<GHRepo>>(emptyList()) }
    var loading by remember(installation.id) { mutableStateOf(true) }
    var error by remember(installation.id) { mutableStateOf("") }
    var repositoryId by remember(installation.id) { mutableStateOf("") }
    var actionInFlight by remember { mutableStateOf(false) }

    fun loadRepos(reset: Boolean = false) {
        loading = true
        error = ""
        scope.launch {
            val nextPage = if (reset) 1 else page
            val result = GitHubManager.getAppInstallationRepositories(context, installation.id, nextPage)
            if (result.error.isBlank()) {
                page = nextPage
                totalCount = result.totalCount
                repos = if (reset || nextPage == 1) result.repositories else repos + result.repositories
            } else {
                error = result.error
            }
            loading = false
        }
    }

    fun mutateRepository(repoId: Long, add: Boolean) {
        actionInFlight = true
        scope.launch {
            val result = if (add) {
                GitHubManager.addRepositoryToAppInstallation(context, installation.id, repoId)
            } else {
                GitHubManager.removeRepositoryFromAppInstallation(context, installation.id, repoId)
            }
            Toast.makeText(context, result.message.take(160), Toast.LENGTH_SHORT).show()
            actionInFlight = false
            if (result.success) {
                repositoryId = ""
                loadRepos(reset = true)
            }
        }
    }

    LaunchedEffect(installation.id) { loadRepos(reset = true) }

    GitHubScreenFrame(
        title = "> ${installation.appSlug.ifBlank { "app" }}",
        onBack = onBack,
        subtitle = "#${installation.id} ${installation.targetLogin.ifBlank { installation.targetType }}",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GitHubTopBarAction(
                    glyph = GhGlyphs.REFRESH,
                    onClick = { loadRepos(reset = true) },
                    tint = AiModuleTheme.colors.accent,
                    contentDescription = "refresh installation",
                )
                if (installation.htmlUrl.isNotBlank()) {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.OPEN_NEW,
                        onClick = {
                            if (!context.openExternalHttps(installation.htmlUrl)) {
                                Toast.makeText(context, "No external browser available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tint = AiModuleTheme.colors.accent,
                        contentDescription = "open installation",
                    )
                }
            }
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
        ) {
            item { GitHubAppInstallationHeader(installation) }
            item {
                GitHubAppRepoMutationRow(
                    repositoryId = repositoryId,
                    onRepositoryIdChange = { repositoryId = it.filter { ch -> ch.isDigit() } },
                    enabled = !actionInFlight,
                    onAdd = {
                        val id = repositoryId.toLongOrNull()
                        if (id == null) {
                            Toast.makeText(context, "Repository id required", Toast.LENGTH_SHORT).show()
                        } else {
                            mutateRepository(id, add = true)
                        }
                    },
                )
            }
            item {
                GitHubTerminalSectionLabel(
                    label = if (loading && repos.isEmpty()) "repositories: loading..." else "repositories: $totalCount",
                    color = AiModuleTheme.colors.textSecondary,
                )
            }
            when {
                loading && repos.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "loading repos...")
                    }
                }
                error.isNotBlank() -> item {
                    GitHubInlineTerminalNotice(
                        glyph = GhGlyphs.WARN,
                        title = "repositories unavailable",
                        subtitle = error.take(180),
                    )
                }
                repos.isEmpty() -> item {
                    GitHubInlineTerminalNotice(
                        glyph = GhGlyphs.INFO,
                        title = "no repositories",
                        subtitle = "this installation returned an empty repository list",
                    )
                }
                else -> {
                    items(repos, key = { "${it.id}:${it.fullName}" }) { repo ->
                        Row(
                            Modifier.fillMaxWidth().padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) {
                                RepoCard(repo, onClick = { onRepoClick(repo) }, showStats = true)
                            }
                            GitHubTerminalButton(
                                label = "remove",
                                onClick = { mutateRepository(repo.id, add = false) },
                                color = AiModuleTheme.colors.error,
                                enabled = !actionInFlight && repo.id > 0L,
                            )
                        }
                        AiModuleHairline()
                    }
                    if (repos.size < totalCount) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                GitHubTerminalButton(
                                    label = "load more ->",
                                    onClick = {
                                        page += 1
                                        loadRepos(reset = false)
                                    },
                                    color = AiModuleTheme.colors.accent,
                                    enabled = !loading,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubAppsSummary(installations: List<GHAppInstallation>, totalCount: Int) {
    val palette = AiModuleTheme.colors
    val selected = installations.count { it.repositorySelection == "selected" }
    val suspended = installations.count { it.suspendedAt.isNotBlank() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GitHubTerminalPill("loaded ${installations.size}/$totalCount", palette.accent)
        GitHubTerminalPill("selected $selected", palette.textSecondary)
        GitHubTerminalPill("suspended $suspended", if (suspended > 0) palette.error else palette.textMuted)
    }
}

@Composable
private fun GitHubAppInstallationRow(installation: GHAppInstallation, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = installation.targetAvatarUrl,
            contentDescription = installation.targetLogin,
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(GitHubControlRadius)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = installation.appSlug.ifBlank { "app-${installation.appId}" },
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                GitHubTerminalPill(installation.repositorySelection.ifBlank { "unknown" }, palette.accent)
            }
            Text(
                text = listOf(installation.targetLogin, installation.targetType.lowercase(Locale.US), "#${installation.id}")
                    .filter { it.isNotBlank() }
                    .joinToString("  "),
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "perm ${installation.permissions.size}  events ${installation.events.size}  upd ${installation.updatedAt.ghAppShortDate()}",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = GhGlyphs.ARROW_RIGHT,
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun GitHubAppInstallationHeader(installation: GHAppInstallation) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = installation.targetAvatarUrl,
                contentDescription = installation.targetLogin,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(GitHubControlRadius)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = installation.appSlug.ifBlank { "app-${installation.appId}" },
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${installation.targetLogin.ifBlank { "unknown" }} / ${installation.targetType.ifBlank { "target" }}",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GitHubTerminalPill("#${installation.id}", palette.textMuted)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            GitHubTerminalPill(installation.repositorySelection.ifBlank { "selection ?" }, palette.accent)
            GitHubTerminalPill("app ${installation.appId}", palette.textSecondary)
            GitHubTerminalPill("updated ${installation.updatedAt.ghAppShortDate()}", palette.textMuted)
            if (installation.suspendedAt.isNotBlank()) GitHubTerminalPill("suspended", palette.error)
        }
        if (installation.permissions.isNotEmpty()) {
            Text(
                text = installation.permissions.take(8).joinToString("  ") { "${it.first}:${it.second}" },
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (installation.events.isNotEmpty()) {
            Text(
                text = "events " + installation.events.take(10).joinToString(", "),
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AiModuleHairline()
    }
}

@Composable
private fun GitHubAppRepoMutationRow(
    repositoryId: String,
    onRepositoryIdChange: (String) -> Unit,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "add repository by id",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                GitHubTerminalTextField(
                    value = repositoryId,
                    onValueChange = onRepositoryIdChange,
                    placeholder = "repository id",
                    minHeight = 36.dp,
                    singleLine = true,
                )
            }
            GitHubTerminalButton(
                label = "add",
                onClick = onAdd,
                color = palette.accent,
                enabled = enabled,
            )
        }
        Text(
            text = "endpoint requires compatible GitHub token and admin access",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun GitHubTerminalSectionLabel(label: String, color: Color) {
    Text(
        text = label.lowercase(Locale.US),
        color = color,
        fontFamily = JetBrainsMono,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun GitHubInlineTerminalNotice(glyph: String, title: String, subtitle: String) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(glyph, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 18.sp)
        Text(title, color = palette.textSecondary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(subtitle, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
    }
}

@Composable
private fun GitHubTerminalPill(label: String, color: Color) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label.lowercase(Locale.US),
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun String.ghAppShortDate(): String =
    takeIf { it.length >= 10 }?.take(10) ?: ifBlank { "unknown" }
