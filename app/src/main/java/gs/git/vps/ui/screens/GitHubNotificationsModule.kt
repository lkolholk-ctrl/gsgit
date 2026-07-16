package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Commit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHNotification
import gs.git.vps.data.github.model.GHThreadSubscription
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@Composable
private fun LegacyNotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<GHNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAll by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf("") }
    var selectedSubscription by remember { mutableStateOf<GHNotification?>(null) }
    var repoToMarkRead by remember { mutableStateOf<String?>(null) }

    fun handleNotificationsBack() {
        if (selectedSubscription != null) selectedSubscription = null else onBack()
    }

    LaunchedEffect(showAll) {
        loading = true
        notifications = GitHubManager.getNotifications(context, showAll)
        loading = false
    }

    val palette = AiModuleTheme.colors

    GitHubScreenFrame(
        title = "> notifications",
        onBack = ::handleNotificationsBack,
        subtitle = if (showAll) "scope: all" else "scope: unread",
        trailing = {
            GitHubTopBarAction(
                glyph = GhGlyphs.CHECK,
                onClick = {
                    scope.launch {
                        GitHubManager.markAllNotificationsRead(context)
                        notifications = GitHubManager.getNotifications(context, showAll)
                    }
                },
                tint = palette.accent,
                contentDescription = "mark all read",
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiModulePillButton(
                    label = "unread",
                    onClick = { showAll = false },
                    accent = !showAll,
                )
                AiModulePillButton(
                    label = "all",
                    onClick = { showAll = true },
                    accent = showAll,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val typeFilters = listOf(
                    "" to "all",
                    "mentions" to "mentions",
                    "my_issues_pr" to "my issues/pr",
                    "releases" to "releases",
                    "system" to "system"
                )
                typeFilters.forEach { (value, label) ->
                    val sel = typeFilter == value
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                            .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(6.dp))
                            .clickable { typeFilter = value }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
                    }
                }
            }
            AiModuleHairline()

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading inbox…")
                }
                notifications.isEmpty() -> GitHubMonoEmpty(
                    title = if (showAll) "inbox empty" else "no unread notifications",
                    subtitle = if (showAll) "you're all caught up" else "switch to all to see read items",
                )
                else -> {
                    val filtered = when (typeFilter) {
                        "mentions" -> notifications.filter { it.reason == "mention" || it.reason == "team_mention" }
                        "my_issues_pr" -> notifications.filter { (it.type == "Issue" || it.type == "PullRequest") && (it.reason == "author" || it.reason == "assign" || it.reason == "review_requested" || it.reason == "comment") }
                        "releases" -> notifications.filter { it.type == "Release" }
                        "system" -> notifications.filter { it.reason != "mention" && it.reason != "team_mention" && it.reason != "author" && it.reason != "assign" && it.reason != "review_requested" && it.reason != "comment" && it.type != "Release" }
                        else -> notifications
                    }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    ) {
                        items(filtered, key = { it.id }) { notification ->
                            SwipeableNotificationRow(
                                notification = notification,
                                onMarkRead = {
                                    scope.launch {
                                        GitHubManager.markNotificationRead(context, notification.id)
                                        notifications = GitHubManager.getNotifications(context, showAll)
                                    }
                                },
                                onMarkDone = {
                                    scope.launch {
                                        GitHubManager.markThreadDone(context, notification.id)
                                        notifications = GitHubManager.getNotifications(context, showAll)
                                    }
                                },
                                onDetail = {
                                    scope.launch {
                                        val detail = GitHubManager.getNotification(context, notification.id)
                                        if (detail != null) selectedSubscription = detail
                                    }
                                },
                                onSubscription = { selectedSubscription = notification },
                                onOpen = {
                                    val url = notification.htmlUrl.ifBlank { notification.subjectUrl }
                                    if (url.isNotBlank()) {
                                        try {
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                                        } catch (_: Exception) {}
                                    }
                                    scope.launch {
                                        if (notification.unread) {
                                            GitHubManager.markNotificationRead(context, notification.id)
                                            notifications = GitHubManager.getNotifications(context, showAll)
                                        }
                                    }
                                },
                                onLongClick = {
                                    repoToMarkRead = notification.repoName
                                }
                            )
                            AiModuleHairline()
                        }
                    }
                }
            }
        }
    }

    repoToMarkRead?.let { repoFullName ->
        AiModuleAlertDialog(
            onDismissRequest = { repoToMarkRead = null },
            title = "bulk mark read",
            content = { Text("Mark all notifications from $repoFullName as read?", fontSize = 13.sp, color = palette.textSecondary) },
            confirmButton = {
                AiModuleTextAction(
                    label = "mark read",
                    onClick = {
                        val parts = repoFullName.split("/")
                        if (parts.size == 2) {
                            scope.launch {
                                GitHubManager.markRepoNotificationsRead(context, parts[0], parts[1])
                                notifications = GitHubManager.getNotifications(context, showAll)
                                repoToMarkRead = null
                            }
                        }
                    },
                    tint = palette.accent
                )
            },
            dismissButton = {
                AiModuleTextAction(label = "cancel", onClick = { repoToMarkRead = null }, tint = palette.textSecondary)
            }
        )
    }

    selectedSubscription?.let { notification ->
        NotificationSubscriptionDialog(
            notification = notification,
            onDismiss = { selectedSubscription = null },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SwipeableNotificationRow(
    notification: GHNotification,
    onMarkRead: () -> Unit,
    onMarkDone: () -> Unit,
    onDetail: () -> Unit,
    onSubscription: () -> Unit,
    onOpen: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val palette = AiModuleTheme.colors
    var swipeOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { 70.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(notification.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        if (swipeOffset > thresholdPx) {
                            onMarkRead()
                        } else if (swipeOffset < -thresholdPx) {
                            onMarkDone()
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = {
                        swipeOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(-thresholdPx * 1.5f, thresholdPx * 1.5f)
                    }
                )
            }
    ) {
        if (swipeOffset != 0f) {
            val isRight = swipeOffset > 0
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (isRight) Color(0xFF2EA043).copy(alpha = 0.2f) else palette.error.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp),
                contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRight) {
                    Text("[mark read]", color = Color(0xFF2EA043), fontFamily = JetBrainsMono, fontSize = 11.sp)
                } else {
                    Text("[archive]", color = palette.error, fontFamily = JetBrainsMono, fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .background(palette.background)
        ) {
            NotificationRow(
                notification = notification,
                onMarkRead = onMarkRead,
                onMarkDone = onMarkDone,
                onDetail = onDetail,
                onSubscription = onSubscription,
                onOpen = onOpen,
                onLongClick = onLongClick
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(
    notification: GHNotification,
    onMarkRead: () -> Unit,
    onMarkDone: () -> Unit,
    onDetail: () -> Unit,
    onSubscription: () -> Unit,
    onOpen: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val palette = AiModuleTheme.colors
    val icon: ImageVector = when (notification.type) {
        "PullRequest" -> Icons.Rounded.MergeType
        "Issue" -> Icons.Rounded.ErrorOutline
        "Release" -> Icons.Rounded.NewReleases
        "Commit" -> Icons.Rounded.Commit
        else -> Icons.Rounded.Notifications
    }
    val typeGlyph = when (notification.type) {
        "PullRequest" -> "PR"
        "Issue" -> "IS"
        "Release" -> "RL"
        "Commit" -> "CT"
        else -> "··"
    }
    val reasonColor = when (notification.reason) {
        "mention" -> palette.warning
        "assign" -> palette.accent
        "review_requested" -> palette.accent
        else -> palette.textMuted
    }

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (notification.unread) "●" else "·",
            color = if (notification.unread) palette.accent else palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 14.sp,
            modifier = Modifier.width(14.dp),
        )
        Text(
            text = typeGlyph,
            color = if (notification.unread) palette.accent else palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (notification.unread) palette.accent else palette.textSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                notification.title,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 1.3.em,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.repoName.substringAfter("/"),
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                if (notification.reason.isNotBlank()) {
                    Text(
                        " · ${notification.reason.replace("_", " ")}",
                        color = reasonColor,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        IconButton(onClick = onDetail, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = "detail",
                modifier = Modifier.size(16.dp),
                tint = palette.textSecondary,
            )
        }
        IconButton(onClick = onMarkDone, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "mark done",
                modifier = Modifier.size(16.dp),
                tint = palette.textSecondary,
            )
        }
        IconButton(onClick = onSubscription, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription = "subscription",
                modifier = Modifier.size(16.dp),
                tint = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun NotificationSubscriptionDialog(notification: GHNotification, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember(notification.id) { mutableStateOf(true) }
    var actionInFlight by remember { mutableStateOf(false) }
    var subscription by remember(notification.id) { mutableStateOf<GHThreadSubscription?>(null) }

    fun loadSubscription() {
        loading = true
        scope.launch {
            subscription = GitHubManager.getThreadSubscription(context, notification.id)
            loading = false
        }
    }

    LaunchedEffect(notification.id) { loadSubscription() }

    val palette = AiModuleTheme.colors
    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = "subscription",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    notification.title,
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    lineHeight = 1.3.em,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "loading…")
                    }
                } else {
                    val current = subscription
                    val status = when {
                        current?.ignored == true -> "ignored"
                        current?.subscribed == true -> "subscribed"
                        else -> "default"
                    }
                    val statusColor = when {
                        current?.ignored == true -> palette.error
                        current?.subscribed == true -> palette.accent
                        else -> palette.textSecondary
                    }
                    Text(
                        text = "status · $status",
                        color = statusColor,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                    val reason = current?.reason
                    if (!reason.isNullOrBlank()) {
                        Text(
                            reason,
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        AiModulePillButton(
                            label = "subscribe",
                            enabled = !actionInFlight,
                            onClick = {
                                actionInFlight = true
                                scope.launch {
                                    GitHubManager.setThreadSubscription(context, notification.id, subscribed = true, ignored = false)
                                    actionInFlight = false
                                    loadSubscription()
                                }
                            },
                        )
                        AiModulePillButton(
                            label = "ignore",
                            enabled = !actionInFlight,
                            destructive = true,
                            onClick = {
                                actionInFlight = true
                                scope.launch {
                                    GitHubManager.setThreadSubscription(context, notification.id, subscribed = false, ignored = true)
                                    actionInFlight = false
                                    loadSubscription()
                                }
                            },
                        )
                        AiModulePillButton(
                            label = "default",
                            enabled = !actionInFlight,
                            accent = false,
                            onClick = {
                                actionInFlight = true
                                scope.launch {
                                    GitHubManager.deleteThreadSubscription(context, notification.id)
                                    actionInFlight = false
                                    loadSubscription()
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(label = "[ close ]", onClick = onDismiss)
        },
    )
}

/**
 * Centered fullscreen empty placeholder kept for callers that still
 * pass an [ImageVector]; new call sites should use [GitHubMonoEmpty].
 * Style is unified with the AI module mono empty state.
 */
@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    val palette = AiModuleTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(40.dp), tint = palette.textMuted)
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }
    }
}
