package gs.git.vps.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.deleteRepositorySubscription
import gs.git.vps.data.github.deleteThreadSubscriptionResult
import gs.git.vps.data.github.getNotificationResult
import gs.git.vps.data.github.getNotificationsPage
import gs.git.vps.data.github.getRepositorySubscription
import gs.git.vps.data.github.getThreadSubscriptionResult
import gs.git.vps.data.github.markAllNotificationsReadResult
import gs.git.vps.data.github.markNotificationReadResult
import gs.git.vps.data.github.markRepoNotificationsReadResult
import gs.git.vps.data.github.markThreadDoneResult
import gs.git.vps.data.github.setRepositorySubscription
import gs.git.vps.data.github.setThreadSubscriptionResult
import gs.git.vps.data.github.model.GHNotification
import gs.git.vps.data.github.model.GHNotificationActionResult
import gs.git.vps.data.github.model.GHRepositorySubscriptionResult
import gs.git.vps.data.github.model.GHThreadSubscriptionResult
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<GHNotification>>(emptyList()) }
    var knownRepositories by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var httpCode by remember { mutableIntStateOf(0) }
    var pollInterval by remember { mutableIntStateOf(60) }
    var lastModified by remember { mutableStateOf("") }
    var nextPage by remember { mutableStateOf<Int?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var participating by rememberSaveable { mutableStateOf(false) }
    var timeWindow by rememberSaveable { mutableStateOf("any") }
    var repositoryScope by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("") }
    var reasonFilter by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<GHNotification?>(null) }
    var revision by remember { mutableIntStateOf(0) }

    fun showResult(result: GHNotificationActionResult) {
        Toast.makeText(
            context,
            "HTTP ${result.code}: ${result.message.take(150)}",
            if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
        ).show()
    }

    fun load(reset: Boolean) {
        if (reset) loading = true else loadingMore = true
        error = ""
        scope.launch {
            val pageToLoad = if (reset) 1 else nextPage ?: 1
            val result = GitHubManager.getNotificationsPage(
                context = context,
                all = showAll,
                participating = participating,
                since = notificationSince(timeWindow),
                page = pageToLoad,
                perPage = 50,
                repositoryFullName = repositoryScope.ifBlank { null },
            )
            httpCode = result.code
            pollInterval = result.pollIntervalSeconds
            lastModified = result.lastModified
            if (result.error.isBlank()) {
                val merged = if (reset) result.notifications else notifications + result.notifications
                notifications = merged.distinctBy { it.id }
                nextPage = result.nextPage
                if (repositoryScope.isBlank()) {
                    knownRepositories = (knownRepositories + result.notifications.map { it.repoName })
                        .filter { '/' in it }
                        .distinct()
                        .sorted()
                }
            } else {
                error = result.error
                if (reset) {
                    notifications = emptyList()
                    nextPage = null
                }
            }
            loading = false
            loadingMore = false
        }
    }

    fun markAllRead() {
        scope.launch {
            val result = if (repositoryScope.isBlank()) {
                GitHubManager.markAllNotificationsReadResult(context)
            } else {
                val parts = repositoryScope.split('/')
                GitHubManager.markRepoNotificationsReadResult(context, parts[0], parts[1])
            }
            showResult(result)
            if (result.success) load(reset = true)
        }
    }

    LaunchedEffect(showAll, participating, timeWindow, repositoryScope, revision) {
        load(reset = true)
    }

    val visible = notifications.filter { item ->
        (typeFilter.isBlank() || item.type == typeFilter) &&
            (reasonFilter.isBlank() || item.reason == reasonFilter) &&
            (searchQuery.isBlank() || listOf(item.title, item.repoName, item.reason, item.type, item.id)
                .any { it.contains(searchQuery, ignoreCase = true) })
    }
    val availableTypes = notifications.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()
    val availableReasons = notifications.map { it.reason }.filter { it.isNotBlank() }.distinct().sorted()
    val palette = AiModuleTheme.colors

    GitHubScreenFrame(
        title = "> notifications",
        onBack = { if (selected != null) selected = null else onBack() },
        subtitle = buildString {
            append(if (showAll) "all" else "unread")
            if (participating) append(" · participating")
            if (repositoryScope.isNotBlank()) append(" · $repositoryScope")
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GitHubTopBarAction(
                    glyph = GhGlyphs.REFRESH,
                    onClick = { revision++ },
                    tint = palette.accent,
                    enabled = !loading && !loadingMore,
                    contentDescription = "refresh notifications",
                )
                GitHubTopBarAction(
                    glyph = GhGlyphs.CHECK,
                    onClick = ::markAllRead,
                    tint = palette.accent,
                    enabled = !loading && notifications.any { it.unread },
                    contentDescription = "mark notifications read",
                )
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            NotificationsControls(
                showAll = showAll,
                onShowAll = { showAll = it },
                participating = participating,
                onParticipating = { participating = it },
                timeWindow = timeWindow,
                onTimeWindow = { timeWindow = it },
                repositoryScope = repositoryScope,
                repositories = knownRepositories,
                onRepositoryScope = { repositoryScope = it },
                typeFilter = typeFilter,
                types = availableTypes,
                onTypeFilter = { typeFilter = it },
                reasonFilter = reasonFilter,
                reasons = availableReasons,
                onReasonFilter = { reasonFilter = it },
                searchQuery = searchQuery,
                onSearchQuery = { searchQuery = it },
            )
            NotificationsApiSummary(
                code = httpCode,
                loaded = notifications.size,
                visible = visible.size,
                unread = notifications.count { it.unread },
                pollInterval = pollInterval,
                lastModified = lastModified,
            )
            AiModuleHairline()

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "GET ${if (repositoryScope.isBlank()) "/notifications" else "/repos/$repositoryScope/notifications"}")
                }
                error.isNotBlank() -> NotificationsApiError(httpCode, error) { revision++ }
                notifications.isEmpty() -> GitHubMonoEmpty(
                    title = if (showAll) "inbox empty" else "no unread notifications",
                    subtitle = "GitHub returned HTTP $httpCode with an empty thread list",
                )
                visible.isEmpty() -> GitHubMonoEmpty(
                    title = "no matching threads",
                    subtitle = "clear search, type or reason filters",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                ) {
                    items(visible, key = { it.id }) { notification ->
                        FullNotificationRow(
                            notification = notification,
                            onOpenDetail = { selected = notification },
                            onOpenSubject = {
                                if (!context.openExternalHttps(notification.htmlUrl)) {
                                    Toast.makeText(context, "Subject URL unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onMarkRead = {
                                scope.launch {
                                    val result = GitHubManager.markNotificationReadResult(context, notification.id)
                                    showResult(result)
                                    if (result.success) {
                                        notifications = if (showAll) {
                                            notifications.map { if (it.id == notification.id) it.copy(unread = false) else it }
                                        } else notifications.filterNot { it.id == notification.id }
                                    }
                                }
                            },
                            onDone = {
                                scope.launch {
                                    val result = GitHubManager.markThreadDoneResult(context, notification.id)
                                    showResult(result)
                                    if (result.success) notifications = notifications.filterNot { it.id == notification.id }
                                }
                            },
                        )
                        AiModuleHairline()
                    }
                    if (nextPage != null) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                GitHubTerminalButton(
                                    label = if (loadingMore) "loading page $nextPage..." else "load page $nextPage ->",
                                    onClick = { load(reset = false) },
                                    color = palette.accent,
                                    enabled = !loadingMore,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { notification ->
        FullNotificationDialog(
            initial = notification,
            onDismiss = { selected = null },
            onChanged = {
                selected = null
                revision++
            },
        )
    }
}

@Composable
private fun NotificationsControls(
    showAll: Boolean,
    onShowAll: (Boolean) -> Unit,
    participating: Boolean,
    onParticipating: (Boolean) -> Unit,
    timeWindow: String,
    onTimeWindow: (String) -> Unit,
    repositoryScope: String,
    repositories: List<String>,
    onRepositoryScope: (String) -> Unit,
    typeFilter: String,
    types: List<String>,
    onTypeFilter: (String) -> Unit,
    reasonFilter: String,
    reasons: List<String>,
    onReasonFilter: (String) -> Unit,
    searchQuery: String,
    onSearchQuery: (String) -> Unit,
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NotificationChipRow {
            NotificationChip("unread", !showAll) { onShowAll(false) }
            NotificationChip("all", showAll) { onShowAll(true) }
            NotificationChip("participating", participating) { onParticipating(!participating) }
        }
        NotificationChipRow {
            listOf("any" to "any time", "24h" to "24h", "7d" to "7d", "30d" to "30d").forEach { (value, label) ->
                NotificationChip(label, timeWindow == value) { onTimeWindow(value) }
            }
        }
        if (repositories.isNotEmpty() || repositoryScope.isNotBlank()) {
            NotificationChipRow {
                NotificationChip("all repositories", repositoryScope.isBlank()) { onRepositoryScope("") }
                repositories.forEach { repo ->
                    NotificationChip(repo, repositoryScope == repo) { onRepositoryScope(repo) }
                }
            }
        }
        if (types.isNotEmpty()) {
            NotificationChipRow {
                NotificationChip("all types", typeFilter.isBlank()) { onTypeFilter("") }
                types.forEach { type -> NotificationChip(type, typeFilter == type) { onTypeFilter(type) } }
            }
        }
        if (reasons.isNotEmpty()) {
            NotificationChipRow {
                NotificationChip("all reasons", reasonFilter.isBlank()) { onReasonFilter("") }
                reasons.forEach { reason -> NotificationChip(reason.replace('_', ' '), reasonFilter == reason) { onReasonFilter(reason) } }
            }
        }
        GitHubTerminalTextField(
            value = searchQuery,
            onValueChange = onSearchQuery,
            placeholder = "filter loaded threads...",
            minHeight = 36.dp,
            singleLine = true,
        )
        Text(
            "API filters: all=$showAll · participating=$participating · since=${notificationSince(timeWindow) ?: "none"}",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotificationChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun NotificationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) palette.accent.copy(alpha = 0.1f) else palette.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label.lowercase(Locale.US),
            color = if (selected) palette.accent else palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun NotificationsApiSummary(
    code: Int,
    loaded: Int,
    visible: Int,
    unread: Int,
    pollInterval: Int,
    lastModified: String,
) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NotificationStatusPill("HTTP $code", if (code in 200..399) palette.accent else palette.error)
        NotificationStatusPill("loaded $loaded", palette.textSecondary)
        NotificationStatusPill("visible $visible", palette.textSecondary)
        NotificationStatusPill("unread $unread", if (unread > 0) palette.warning else palette.textMuted)
        NotificationStatusPill("poll ${pollInterval}s", palette.textMuted)
        if (lastModified.isNotBlank()) NotificationStatusPill("modified $lastModified", palette.textMuted)
        NotificationStatusPill("rate ${GitHubManager.getRateLimitRemaining()}", palette.textMuted)
    }
}

@Composable
private fun NotificationsApiError(code: Int, error: String, onRetry: () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("HTTP $code", color = palette.error, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(error.take(400), color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp)
        if (code == 401 || code == 403) {
            Spacer(Modifier.height(8.dp))
            Text(
                "GitHub Notifications REST endpoints require a classic PAT with notifications or repo scope. Fine-grained and GitHub App tokens are not supported by GitHub.",
                color = palette.warning,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        GitHubTerminalButton("retry", onRetry, color = palette.accent)
    }
}

@Composable
private fun FullNotificationRow(
    notification: GHNotification,
    onOpenDetail: () -> Unit,
    onOpenSubject: () -> Unit,
    onMarkRead: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpenDetail).padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(if (notification.unread) "●" else "·", color = if (notification.unread) palette.accent else palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp)
            NotificationStatusPill(notificationTypeGlyph(notification.type), if (notification.unread) palette.accent else palette.textSecondary)
            Text(
                notification.title,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = if (notification.unread) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(notification.repoName, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp)
            NotificationStatusPill(notification.reason.replace('_', ' '), notificationReasonColor(notification.reason))
            if (notification.subjectNumber != null) NotificationStatusPill("#${notification.subjectNumber}", palette.textMuted)
            NotificationStatusPill(notification.updatedAt.take(16).replace('T', ' '), palette.textMuted)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NotificationMiniAction("details", palette.accent, onOpenDetail)
            NotificationMiniAction("open", palette.textSecondary, onOpenSubject)
            if (notification.unread) NotificationMiniAction("read", palette.accent, onMarkRead)
            NotificationMiniAction("done", palette.error, onDone)
        }
    }
}

@Composable
private fun FullNotificationDialog(
    initial: GHNotification,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember(initial.id) { mutableStateOf(true) }
    var actionInFlight by remember(initial.id) { mutableStateOf(false) }
    var thread by remember(initial.id) { mutableStateOf(initial) }
    var threadCode by remember(initial.id) { mutableIntStateOf(0) }
    var threadError by remember(initial.id) { mutableStateOf("") }
    var threadSubscription by remember(initial.id) { mutableStateOf(GHThreadSubscriptionResult()) }
    var repoSubscription by remember(initial.id) { mutableStateOf(GHRepositorySubscriptionResult()) }
    val parts = initial.repoName.split('/')
    val owner = parts.getOrNull(0).orEmpty()
    val repo = parts.getOrNull(1).orEmpty()

    fun load() {
        loading = true
        scope.launch {
            val threadTask = async { GitHubManager.getNotificationResult(context, initial.id) }
            val threadSubTask = async { GitHubManager.getThreadSubscriptionResult(context, initial.id) }
            val repoSubTask = async {
                if (owner.isNotBlank() && repo.isNotBlank()) GitHubManager.getRepositorySubscription(context, owner, repo)
                else GHRepositorySubscriptionResult(code = -1, error = "Repository unavailable")
            }
            val threadResult = threadTask.await()
            threadResult.notification?.let { thread = it }
            threadCode = threadResult.code
            threadError = threadResult.error
            threadSubscription = threadSubTask.await()
            repoSubscription = repoSubTask.await()
            loading = false
        }
    }

    fun runAction(action: suspend () -> GHNotificationActionResult, close: Boolean = false) {
        actionInFlight = true
        scope.launch {
            val result = action()
            Toast.makeText(context, "HTTP ${result.code}: ${result.message.take(150)}", if (result.success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            actionInFlight = false
            if (result.success && close) onChanged() else load()
        }
    }

    LaunchedEffect(initial.id) { load() }
    val palette = AiModuleTheme.colors
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "notification thread #${initial.id}",
        content = {
            Column(
                Modifier.fillMaxWidth().height(520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(thread.title, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationStatusPill("thread HTTP $threadCode", notificationApiColor(threadCode))
                    NotificationStatusPill("subscription HTTP ${threadSubscription.code}", notificationApiColor(threadSubscription.code))
                    NotificationStatusPill("repo HTTP ${repoSubscription.code}", notificationApiColor(repoSubscription.code))
                }
                NotificationDetailLine("repository", thread.repoName)
                NotificationDetailLine("subject", "${thread.type}${thread.subjectNumber?.let { " #$it" }.orEmpty()}")
                NotificationDetailLine("reason", thread.reason)
                NotificationDetailLine("unread", thread.unread.toString())
                NotificationDetailLine("updated", thread.updatedAt)
                NotificationDetailLine("last read", thread.lastReadAt ?: "never")
                NotificationDetailLine("subject api", thread.subjectUrl)
                NotificationDetailLine("latest comment", thread.latestCommentUrl.ifBlank { "not supplied" })
                if (threadError.isNotBlank()) Text(threadError.take(250), color = palette.error, fontFamily = JetBrainsMono, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationMiniAction("open subject", palette.accent) { context.openExternalHttps(thread.htmlUrl) }
                    if (thread.latestCommentUrl.isNotBlank()) {
                        NotificationMiniAction("latest comment", palette.textSecondary) {
                            context.openExternalHttps(notificationApiUrlToWeb(thread.latestCommentUrl))
                        }
                    }
                    if (thread.unread) NotificationMiniAction("mark read", palette.accent) {
                        runAction({ GitHubManager.markNotificationReadResult(context, thread.id) })
                    }
                    NotificationMiniAction("done", palette.error) {
                        runAction({ GitHubManager.markThreadDoneResult(context, thread.id) }, close = true)
                    }
                }

                AiModuleHairline()
                Text("thread subscription", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                val threadSub = threadSubscription.subscription
                NotificationDetailLine(
                    "state",
                    when {
                        threadSub?.ignored == true -> "ignored"
                        threadSub?.subscribed == true -> "subscribed"
                        threadSubscription.code == 404 -> "no explicit subscription"
                        else -> "unknown"
                    },
                )
                if (threadSubscription.error.isNotBlank()) Text(threadSubscription.error.take(250), color = palette.warning, fontFamily = JetBrainsMono, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationMiniAction("receive", palette.accent, enabled = !actionInFlight) {
                        runAction({ GitHubManager.setThreadSubscriptionResult(context, thread.id, ignored = false) })
                    }
                    NotificationMiniAction("ignore", palette.warning, enabled = !actionInFlight) {
                        runAction({ GitHubManager.setThreadSubscriptionResult(context, thread.id, ignored = true) })
                    }
                    NotificationMiniAction("remove", palette.error, enabled = !actionInFlight) {
                        runAction({ GitHubManager.deleteThreadSubscriptionResult(context, thread.id) })
                    }
                }

                AiModuleHairline()
                Text("repository subscription", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                val repoSub = repoSubscription.subscription
                NotificationDetailLine(
                    "state",
                    when {
                        repoSub?.ignored == true -> "ignored"
                        repoSub?.subscribed == true -> "watching"
                        repoSubscription.code == 404 -> "not watching"
                        else -> "unknown"
                    },
                )
                if (repoSubscription.error.isNotBlank() && repoSubscription.code != 404) Text(repoSubscription.error.take(250), color = palette.warning, fontFamily = JetBrainsMono, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NotificationMiniAction("watch", palette.accent, enabled = !actionInFlight && owner.isNotBlank()) {
                        runAction({ GitHubManager.setRepositorySubscription(context, owner, repo, subscribed = true, ignored = false) })
                    }
                    NotificationMiniAction("ignore repo", palette.warning, enabled = !actionInFlight && owner.isNotBlank()) {
                        runAction({ GitHubManager.setRepositorySubscription(context, owner, repo, subscribed = false, ignored = true) })
                    }
                    NotificationMiniAction("unwatch", palette.error, enabled = !actionInFlight && owner.isNotBlank()) {
                        runAction({ GitHubManager.deleteRepositorySubscription(context, owner, repo) })
                    }
                }
                if (loading) AiModuleSpinner(label = "refreshing API state...")
            }
        },
        confirmButton = { AiModuleTextAction("[ close ]", onDismiss) },
    )
}

@Composable
private fun NotificationDetailLine(label: String, value: String) {
    val palette = AiModuleTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("$label:", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.width(92.dp))
        Text(value.ifBlank { "-" }, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NotificationMiniAction(label: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, color.copy(alpha = if (enabled) 0.75f else 0.3f), RoundedCornerShape(GitHubControlRadius))
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(label, color = color.copy(alpha = if (enabled) 1f else 0.4f), fontFamily = JetBrainsMono, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun NotificationStatusPill(label: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label.lowercase(Locale.US), color = color, fontFamily = JetBrainsMono, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun notificationReasonColor(reason: String): Color = when (reason) {
    "mention", "team_mention", "approval_requested" -> AiModuleTheme.colors.warning
    "assign", "review_requested" -> AiModuleTheme.colors.accent
    "security_alert", "security_advisory_credit" -> AiModuleTheme.colors.error
    else -> AiModuleTheme.colors.textMuted
}

@Composable
private fun notificationApiColor(code: Int): Color = when (code) {
    in 200..399 -> AiModuleTheme.colors.accent
    401, 403, 404 -> AiModuleTheme.colors.warning
    else -> AiModuleTheme.colors.error
}

private fun notificationTypeGlyph(type: String): String = when (type) {
    "PullRequest" -> "PR"
    "Issue" -> "IS"
    "Release" -> "RL"
    "Commit" -> "CT"
    "Discussion" -> "DS"
    "CheckSuite" -> "CI"
    "RepositoryVulnerabilityAlert" -> "SEC"
    "Deployment" -> "DEP"
    else -> type.take(3).uppercase(Locale.US).ifBlank { "---" }
}

private fun notificationSince(window: String): String? {
    val hours = when (window) {
        "24h" -> 24L
        "7d" -> 24L * 7L
        "30d" -> 24L * 30L
        else -> return null
    }
    return Instant.now().minus(hours, ChronoUnit.HOURS).toString()
}

private fun notificationApiUrlToWeb(url: String): String =
    url.replace("https://api.github.com/repos/", "https://github.com/")
        .replace("/pulls/", "/pull/")
