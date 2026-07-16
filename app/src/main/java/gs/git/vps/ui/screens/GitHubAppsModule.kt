package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import gs.git.vps.data.github.model.GHAppMetadataResult
import gs.git.vps.data.github.model.GHDeviceCode
import gs.git.vps.data.github.model.GHGitHubAppConnection
import gs.git.vps.data.github.model.GHRepoAppsEvidence
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

@Composable
internal fun GitHubAppsScreen(
    onBack: () -> Unit,
    onRepoClick: (GHRepo) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(1) }
    var totalCount by remember { mutableIntStateOf(0) }
    var installations by remember { mutableStateOf<List<GHAppInstallation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var errorCode by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<GHAppInstallation?>(null) }
    var appMetadata by remember { mutableStateOf<Map<String, GHAppMetadataResult>>(emptyMap()) }
    var catalogLoading by remember { mutableStateOf(true) }
    var repos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var selectedRepoFullName by rememberSaveable { mutableStateOf("") }
    var evidence by remember { mutableStateOf<GHRepoAppsEvidence?>(null) }
    var evidenceLoading by remember { mutableStateOf(false) }
    var evidenceRevision by remember { mutableIntStateOf(0) }
    var appConnectionRevision by remember { mutableIntStateOf(0) }
    val appConnection = remember(appConnectionRevision) { GitHubManager.getGsGitAppConnection(context) }
    var deviceCode by remember { mutableStateOf<GHDeviceCode?>(null) }
    var deviceCodeExpiresAt by remember { mutableStateOf(0L) }
    var devicePollInterval by remember { mutableIntStateOf(5) }
    var appAuthBusy by remember { mutableStateOf(false) }
    var appAuthError by remember { mutableStateOf("") }
    var resumeRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun loadInstallations(reset: Boolean = false) {
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

    fun refreshAll() {
        loading = true
        catalogLoading = true
        error = ""
        errorCode = 0
        scope.launch {
            val metadataTasks = officialGitHubAiIntegrations.map { provider ->
                async { provider.id to GitHubManager.getGitHubAppMetadata(context, provider.appSlug) }
            }
            val reposTask = async { GitHubManager.getRepos(context, perPage = 100) }
            val installationsTask = async { GitHubManager.getAppInstallations(context, page = 1, perPage = 100) }

            appMetadata = metadataTasks.awaitAll().toMap()
            catalogLoading = false

            repos = reposTask.await().distinctBy { it.fullName }
            if (selectedRepoFullName.isBlank() || repos.none { it.fullName == selectedRepoFullName }) {
                selectedRepoFullName = repos.firstOrNull()?.fullName.orEmpty()
            }

            val result = installationsTask.await()
            if (result.error.isBlank()) {
                page = 1
                totalCount = result.totalCount
                installations = result.installations
            } else {
                error = result.error
                errorCode = result.code
                installations = emptyList()
                totalCount = 0
            }
            loading = false
            evidenceRevision++
            appConnectionRevision++
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    LaunchedEffect(resumeRevision) {
        if (resumeRevision > 1) refreshAll()
    }

    LaunchedEffect(deviceCode?.deviceCode) {
        val pendingCode = deviceCode ?: return@LaunchedEffect
        var intervalSeconds = pendingCode.interval.coerceAtLeast(5)
        while (System.currentTimeMillis() < deviceCodeExpiresAt) {
            delay(intervalSeconds * 1000L)
            appAuthBusy = true
            val result = GitHubManager.pollGsGitAppDeviceToken(context, pendingCode.deviceCode)
            appAuthBusy = false
            when {
                result.token?.isNotBlank() == true -> {
                    appAuthError = ""
                    appConnectionRevision++
                    refreshAll()
                    deviceCode = null
                    Toast.makeText(context, "GsGit App connected", Toast.LENGTH_SHORT).show()
                    return@LaunchedEffect
                }
                result.error == "authorization_pending" -> Unit
                result.error == "slow_down" -> {
                    intervalSeconds += 5
                    devicePollInterval = intervalSeconds
                }
                result.error == "access_denied" -> {
                    appAuthError = "Authorization was cancelled on GitHub"
                    deviceCode = null
                    return@LaunchedEffect
                }
                result.error == "expired_token" -> {
                    appAuthError = "The device code expired. Start connection again."
                    deviceCode = null
                    return@LaunchedEffect
                }
                else -> {
                    appAuthError = result.error?.replace('_', ' ') ?: "GitHub authorization failed"
                    deviceCode = null
                    return@LaunchedEffect
                }
            }
        }
        appAuthBusy = false
        appAuthError = "The device code expired. Start connection again."
        deviceCode = null
    }

    LaunchedEffect(selectedRepoFullName, evidenceRevision) {
        val repo = repos.firstOrNull { it.fullName == selectedRepoFullName }
        if (repo == null) {
            evidence = null
            evidenceLoading = false
            return@LaunchedEffect
        }
        evidenceLoading = true
        evidence = GitHubManager.getRepoAppsEvidence(context, repo)
        evidenceLoading = false
    }

    fun openExternal(url: String) {
        if (!context.openExternalHttps(url)) {
            Toast.makeText(context, "No external browser available", Toast.LENGTH_SHORT).show()
        }
    }

    fun install(provider: GitHubAiIntegration) {
        openExternal(provider.installUrl)
    }

    fun startGsGitAppConnection() {
        appAuthBusy = true
        appAuthError = ""
        scope.launch {
            val code = GitHubManager.initiateGsGitAppDeviceFlow()
            appAuthBusy = false
            if (code == null || code.deviceCode.isBlank() || code.userCode.isBlank()) {
                appAuthError = "GitHub did not return a device code"
                return@launch
            }
            deviceCode = code
            devicePollInterval = code.interval.coerceAtLeast(5)
            deviceCodeExpiresAt = System.currentTimeMillis() + code.expiresIn.coerceAtLeast(1) * 1000L
            openExternal(code.verificationUri)
        }
    }

    fun copyDeviceCode() {
        val code = deviceCode?.userCode ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub device code", code))
        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
    }

    fun disconnectGsGitApp() {
        GitHubManager.disconnectGsGitApp(context)
        deviceCode = null
        installations = emptyList()
        totalCount = 0
        error = "Connect GsGit App to read its real installation state"
        errorCode = 401
        appConnectionRevision++
        Toast.makeText(context, "GsGit App disconnected", Toast.LENGTH_SHORT).show()
    }

    fun handleAppsBack() {
        if (selected != null) selected = null else onBack()
    }

    selected?.let { installation ->
        GitHubAppInstallationDetailScreen(
            installation = installation,
            availableRepos = repos,
            onBack = ::handleAppsBack,
            onRepoClick = onRepoClick,
        )
        return
    }

    GitHubScreenFrame(
        title = "> apps",
        onBack = ::handleAppsBack,
        subtitle = when {
            catalogLoading || loading -> "reading GitHub APIs..."
            else -> "API facts · ${repos.size} repositories"
        },
        trailing = {
            GitHubTopBarAction(
                glyph = GhGlyphs.REFRESH,
                onClick = { refreshAll() },
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
            item {
                GsGitAppConnectionCard(
                    connection = appConnection,
                    deviceCode = deviceCode,
                    pollInterval = devicePollInterval,
                    busy = appAuthBusy,
                    error = appAuthError,
                    onConnect = ::startGsGitAppConnection,
                    onCopyCode = ::copyDeviceCode,
                    onOpenDevicePage = {
                        openExternal(deviceCode?.verificationUri ?: "https://github.com/login/device")
                    },
                    onCancel = {
                        deviceCode = null
                        appAuthBusy = false
                        appAuthError = ""
                    },
                    onInstall = { openExternal(GsGitGitHubApp.INSTALL_URL) },
                    onDisconnect = ::disconnectGsGitApp,
                )
            }
            items(officialGitHubAiIntegrations, key = { it.id }) { provider ->
                val metadata = appMetadata[provider.id]
                val installation = installations.firstOrNull { it.appSlug == provider.appSlug }
                GitHubAppCatalogCard(
                    provider = provider,
                    metadataResult = metadata,
                    installation = installation,
                    loading = catalogLoading,
                    onInstall = {
                        installation?.htmlUrl?.takeIf { it.isNotBlank() }?.let(::openExternal)
                            ?: install(provider)
                    },
                    onOpenApp = { openExternal(metadata?.app?.htmlUrl?.ifBlank { provider.appUrl } ?: provider.appUrl) },
                    onGuide = { openExternal(provider.guideUrl) },
                )
            }
            item {
                GitHubRepoAppsEvidencePanel(
                    repos = repos,
                    selectedRepoFullName = selectedRepoFullName,
                    evidence = evidence,
                    loading = evidenceLoading,
                    onSelectRepo = { selectedRepoFullName = it },
                    onRefresh = { evidenceRevision++ },
                )
            }
            item { GitHubSupportedLinksCard() }
            item {
                GitHubTerminalSectionLabel(
                    label = "GsGit App installation API · live account data",
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
                        subtitle = "GET /user/installations returned zero installations for the current App user token.",
                    )
                }
                else -> {
                    item {
                        GitHubInlineTerminalNotice(
                            glyph = GhGlyphs.INFO,
                            title = "token-scoped result",
                            subtitle = "GitHub returned these installations for the GitHub App that issued the current user access token. This is not a global list of every App installed on the account.",
                        )
                    }
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
                                        loadInstallations(reset = false)
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
private fun GsGitAppConnectionCard(
    connection: GHGitHubAppConnection,
    deviceCode: GHDeviceCode?,
    pollInterval: Int,
    busy: Boolean,
    error: String,
    onConnect: () -> Unit,
    onCopyCode: () -> Unit,
    onOpenDevicePage: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(
                1.dp,
                if (connection.connected) palette.accent.copy(alpha = 0.75f) else palette.border,
                RoundedCornerShape(GitHubControlRadius),
            )
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "GsGit App authorization",
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${GsGitGitHubApp.SLUG} · app ${GsGitGitHubApp.APP_ID}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GitHubTerminalPill(
                if (deviceCode != null) "pending" else connection.status,
                if (deviceCode != null) palette.warning else if (connection.connected) palette.accent else palette.textMuted,
            )
        }

        when {
            connection.connected -> {
                Text(
                    text = "GitHub App user token is encrypted on this device and refreshed automatically. PAT remains separate for Notifications API compatibility.",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    GitHubTerminalPill(connection.accessTokenExpiresAt.appTokenExpiryLabel("access"), palette.accent)
                    GitHubTerminalPill(connection.refreshTokenExpiresAt.appTokenExpiryLabel("refresh"), palette.textSecondary)
                }
                if (connection.lastRefreshAt > 0L) {
                    Text(
                        text = connection.lastRefreshAt.appTokenRefreshLabel(connection.lastRefreshError),
                        color = if (connection.lastRefreshError.isBlank()) palette.textMuted else palette.warning,
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GitHubTerminalButton("manage install ->", onInstall, color = palette.accent)
                    GitHubTerminalButton("disconnect", onDisconnect, color = palette.error)
                }
            }
            deviceCode != null -> {
                Text(
                    text = "Enter this one-time code on GitHub",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                Text(
                    text = deviceCode.userCode,
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    text = if (busy) "checking authorization..." else "automatic check every ${pollInterval}s",
                    color = if (busy) palette.warning else palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GitHubTerminalButton("copy code", onCopyCode, color = palette.accent)
                    GitHubTerminalButton("open GitHub ->", onOpenDevicePage, color = palette.accent)
                    GitHubTerminalButton("cancel", onCancel, color = palette.textSecondary)
                }
            }
            else -> {
                Text(
                    text = "Connect once through GitHub Device Flow. No PAT, Client Secret or private key is requested.",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GitHubTerminalButton(
                        label = if (busy) "starting..." else if (connection.hasSession) "reconnect GsGit App ->" else "connect GsGit App ->",
                        onClick = onConnect,
                        color = palette.accent,
                        enabled = !busy,
                    )
                    GitHubTerminalButton("install / select repos", onInstall, color = palette.textSecondary)
                }
            }
        }
        if (error.isNotBlank()) {
            Text(
                text = error,
                color = palette.error,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

private fun Long.appTokenExpiryLabel(prefix: String): String {
    if (this <= 0L) return "$prefix non-expiring"
    val remainingMinutes = ((this - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L)
    return when {
        remainingMinutes >= 24 * 60 -> "$prefix ${remainingMinutes / (24 * 60)}d"
        remainingMinutes >= 60 -> "$prefix ${remainingMinutes / 60}h"
        else -> "$prefix ${remainingMinutes}m"
    }
}

private fun Long.appTokenRefreshLabel(error: String): String {
    val ageMinutes = ((System.currentTimeMillis() - this) / 60_000L).coerceAtLeast(0L)
    val age = when {
        ageMinutes >= 24 * 60 -> "${ageMinutes / (24 * 60)}d ago"
        ageMinutes >= 60 -> "${ageMinutes / 60}h ago"
        else -> "${ageMinutes}m ago"
    }
    return if (error.isBlank()) "last token issue/refresh: $age · ok"
    else "last refresh: $age · ${error.replace('_', ' ')}"
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
            text = "GitHub Apps · API truth",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Text(
            text = "Live App metadata, token-scoped installations and repository evidence are kept separate. GsGit never converts an empty or denied endpoint into a fake installed/not-installed status.",
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
            GitHubTerminalPill(status.label, if (status.enabled) palette.accent else palette.warning)
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
    val missingConnection = errorCode == 401
    val isTokenMismatch = errorCode == 403 || error.contains("authorized to a GitHub App", ignoreCase = true)
    GitHubInlineTerminalNotice(
        glyph = GhGlyphs.WARN,
        title = when {
            missingConnection -> if (error.contains("Reconnect", ignoreCase = true)) "reconnect GsGit App above" else "connect GsGit App above"
            isTokenMismatch -> "/user/installations rejected the App token"
            else -> "installation API unavailable"
        },
        subtitle = when {
            missingConnection -> if (error.contains("Reconnect", ignoreCase = true)) {
                "GitHub rejected the previous App user token. Device Flow will create a fresh session without replacing your PAT."
            } else {
                "Device Flow creates the compatible App user token without replacing your PAT."
            }
            isTokenMismatch -> "HTTP $errorCode. Reconnect GsGit App to issue a fresh token for ${GsGitGitHubApp.SLUG}."
            else -> error.take(180)
        },
    )
}

@Composable
private fun GitHubAppInstallationDetailScreen(
    installation: GHAppInstallation,
    availableRepos: List<GHRepo>,
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
    var actionInFlight by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<GHRepo?>(null) }

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
            if (installation.repositorySelection == "selected") {
                item {
                    GitHubAppRepoAddPicker(
                        availableRepos = availableRepos,
                        installedRepos = repos,
                        enabled = !loading && !actionInFlight,
                        onAdd = { mutateRepository(it.id, add = true) },
                    )
                }
            } else {
                item {
                    GitHubInlineTerminalNotice(
                        glyph = GhGlyphs.INFO,
                        title = "all repositories selected",
                        subtitle = "Repository membership is controlled on GitHub for this installation mode.",
                    )
                }
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
                            if (installation.repositorySelection == "selected") {
                                GitHubTerminalButton(
                                    label = "remove",
                                    onClick = { pendingRemoval = repo },
                                    color = AiModuleTheme.colors.error,
                                    enabled = !actionInFlight && repo.id > 0L,
                                )
                            }
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

    pendingRemoval?.let { repo ->
        AiModuleAlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = "remove repository",
            content = {
                Text(
                    text = "Remove ${repo.fullName} from the GsGit App installation? GsGit will immediately lose App access to this repository.",
                    color = AiModuleTheme.colors.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "remove",
                    tint = AiModuleTheme.colors.error,
                    onClick = {
                        pendingRemoval = null
                        mutateRepository(repo.id, add = false)
                    },
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = "cancel",
                    tint = AiModuleTheme.colors.textSecondary,
                    onClick = { pendingRemoval = null },
                )
            },
        )
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
    val missingPermissions = installation.missingGsGitPermissions()
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
            else GitHubTerminalPill("active", palette.accent)
            GitHubTerminalPill(
                if (missingPermissions.isEmpty()) "permissions ok" else "missing ${missingPermissions.size}",
                if (missingPermissions.isEmpty()) palette.accent else palette.warning,
            )
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
        if (missingPermissions.isNotEmpty()) {
            Text(
                text = "missing for full GsGit: " + missingPermissions.joinToString(", "),
                color = palette.warning,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
        AiModuleHairline()
    }
}

@Composable
private fun GitHubAppRepoAddPicker(
    availableRepos: List<GHRepo>,
    installedRepos: List<GHRepo>,
    enabled: Boolean,
    onAdd: (GHRepo) -> Unit,
) {
    val palette = AiModuleTheme.colors
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val installedIds = installedRepos.map { it.id }.toSet()
    val addableRepos = availableRepos
        .asSequence()
        .filter { it.id > 0L && it.id !in installedIds }
        .filter { query.isBlank() || it.fullName.contains(query, ignoreCase = true) }
        .sortedBy { it.fullName.lowercase(Locale.US) }
        .toList()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("add repository", color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp)
                Text(
                    "${addableRepos.size} available from current account token",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
            }
            GitHubTerminalButton(
                label = if (expanded) "close" else "select ->",
                onClick = { expanded = !expanded },
                color = palette.accent,
                enabled = enabled,
            )
        }
        if (expanded) {
            Box(Modifier.fillMaxWidth()) {
                GitHubTerminalTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "search owner/repository",
                    minHeight = 36.dp,
                    singleLine = true,
                )
            }
            if (addableRepos.isEmpty()) {
                Text(
                    if (query.isBlank()) "No additional repositories available" else "No repository matches this search",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
            }
            addableRepos.take(8).forEach { repo ->
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            repo.fullName,
                            color = palette.textPrimary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(repo.language, if (repo.isPrivate) "private" else "public").filter { it.isNotBlank() }.joinToString(" · "),
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 9.sp,
                        )
                    }
                    GitHubTerminalButton(
                        label = "add",
                        onClick = {
                            expanded = false
                            query = ""
                            onAdd(repo)
                        },
                        color = palette.accent,
                        enabled = enabled,
                    )
                }
            }
            if (addableRepos.size > 8) {
                Text(
                    "Refine search to show ${addableRepos.size - 8} more repositories",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp,
                )
            }
        }
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

private val GSGIT_REQUIRED_REPOSITORY_PERMISSIONS = linkedMapOf(
    "actions" to "write",
    "actions_variables" to "write",
    "checks" to "write",
    "contents" to "write",
    "deployments" to "write",
    "environments" to "write",
    "issues" to "write",
    "metadata" to "read",
    "pull_requests" to "write",
    "secrets" to "write",
    "statuses" to "write",
    "workflows" to "write",
)

private fun GHAppInstallation.missingGsGitPermissions(): List<String> {
    val actual = permissions.associate { it.first to it.second.lowercase(Locale.US) }
    return GSGIT_REQUIRED_REPOSITORY_PERMISSIONS.mapNotNull { (name, required) ->
        val granted = actual[name].orEmpty()
        val sufficient = when (required) {
            "read" -> granted == "read" || granted == "write" || granted == "admin"
            "write" -> granted == "write" || granted == "admin"
            else -> granted == required
        }
        name.takeUnless { sufficient }
    }
}
