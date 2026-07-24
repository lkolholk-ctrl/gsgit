package gs.git.vps.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.notifications.GitHubNotificationTarget
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

// Compact mode — propagates through all sub-screens automatically

internal val LocalGHCompact = compositionLocalOf { false }

@Composable
fun GitHubScreen(
    onBack: () -> Unit,
    onMinimize: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    compact: Boolean = false,
    initialTarget: GitHubNotificationTarget? = null,
    onInitialTargetConsumed: () -> Unit = {},
    initialOpenApps: Boolean = false,
    onInitialOpenAppsConsumed: () -> Unit = {},
) {
    CompositionLocalProvider(LocalGHCompact provides compact) {
        AiModuleSurface {
            val context = LocalContext.current
            var isLoggedIn by remember { mutableStateOf(GitHubManager.isLoggedIn(context)) }
            var user by remember { mutableStateOf(GitHubManager.getCachedUser(context)) }
            var selectedRepo by rememberSaveable(stateSaver = NullableGitHubRepoSaver) {
                mutableStateOf<GHRepo?>(null)
            }
            var showGists by rememberSaveable { mutableStateOf(false) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showNotifications by rememberSaveable { mutableStateOf(false) }
            var showProfile by rememberSaveable { mutableStateOf<String?>(null) }
            // Мульти-аккаунт: переключатель + слот, на который вернуться при отмене добавления.
            var showAccounts by remember { mutableStateOf(false) }
            var addReturnSlot by remember { mutableStateOf<Int?>(null) }
            var pendingTarget by remember { mutableStateOf(initialTarget) }
            var pendingAppsOpen by remember { mutableStateOf(initialOpenApps) }
            val saveableStateHolder = rememberSaveableStateHolder()

            // Рантайм-конфиг с gsgit.org/app.json: техработы + принудительное/мягкое обновление
            // (модель Supercell). Проверка на каждом запуске; заблокированное состояние
            // перепроверяется раз в минуту — гейт снимается сам, без перезапуска приложения.
            var appConfig by remember { mutableStateOf<gs.git.vps.util.AppUpdate.Config?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }
            val currentVersion = gs.git.vps.BuildConfig.VERSION_NAME
            LaunchedEffect(Unit) {
                while (true) {
                    appConfig = gs.git.vps.util.AppUpdate.fetch(context) ?: appConfig
                    val blocked = appConfig?.let {
                        it.maintenance.isNotBlank() || gs.git.vps.util.AppUpdate.isOlder(currentVersion, it.minVersion)
                    } == true
                    kotlinx.coroutines.delay(if (blocked) 60_000L else 30L * 60_000L)
                }
            }
            val maintenanceNow = appConfig?.maintenance.orEmpty()
            val forceUpdate = maintenanceNow.isBlank() && appConfig?.let {
                gs.git.vps.util.AppUpdate.isOlder(currentVersion, it.minVersion)
            } == true
            val softUpdate = maintenanceNow.isBlank() && !forceUpdate && appConfig?.let {
                gs.git.vps.util.AppUpdate.isOlder(currentVersion, it.latestVersion)
            } == true

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn) {
                    user = GitHubManager.getUser(context)
                    // Моментальные пуши: регистрируем устройство на api.gsgit.org
                    // (тихий no-op, пока Firebase-конфиг не заполнен).
                    gs.git.vps.notifications.GsGitPush.registerAsync(context)
                }
            }
            LaunchedEffect(initialTarget) {
                if (initialTarget != null) pendingTarget = initialTarget
            }
            LaunchedEffect(initialOpenApps) {
                if (initialOpenApps) {
                    selectedRepo = null
                    showSettings = false
                    showGists = false
                    showProfile = null
                    showNotifications = false
                    pendingAppsOpen = true
                }
            }
            LaunchedEffect(isLoggedIn, pendingTarget) {
                val target = pendingTarget ?: return@LaunchedEffect
                if (!isLoggedIn) return@LaunchedEffect
                showSettings = false
                showGists = false
                showProfile = null
                showNotifications = false
                val repo = GitHubManager.getRepo(context, target.owner, target.repo)
                if (repo != null) {
                    selectedRepo = repo
                } else {
                    showNotifications = true
                    pendingTarget = null
                    onInitialTargetConsumed()
                }
            }
            BackHandler(enabled = isLoggedIn && selectedRepo == null) {
                when {
                    showProfile != null -> showProfile = null
                    showNotifications -> showNotifications = false
                    showSettings -> showSettings = false
                    showGists -> showGists = false
                    else -> onBack()
                }
            }
            when {
                !isLoggedIn -> LoginScreen(
                    onBack = {
                        val ret = addReturnSlot
                        if (ret != null) {
                            // Отмена добавления аккаунта — вернуться на прошлый.
                            gs.git.vps.data.github.AccountStore.switchTo(context, ret)
                            addReturnSlot = null
                            isLoggedIn = GitHubManager.isLoggedIn(context)
                            user = GitHubManager.getCachedUser(context)
                        } else onBack()
                    },
                    onMinimize = onMinimize,
                    onClose = onClose,
                ) {
                    isLoggedIn = true
                    addReturnSlot = null
                    user = GitHubManager.getCachedUser(context)
                    gs.git.vps.notifications.GsGitPush.syncActiveAsync(context)
                }
                showSettings -> saveableStateHolder.SaveableStateProvider("settings") {
                    GitHubSettingsScreen(
                        onBack = { showSettings = false },
                        onLogout = {
                            gs.git.vps.notifications.GsGitPush.unregisterAsync(context)
                            GitHubManager.logout(context)
                            isLoggedIn = false
                            user = null
                            showSettings = false
                        },
                        onOpenApps = {
                            showSettings = false
                            pendingAppsOpen = true
                        },
                        onClose = onClose,
                    )
                }
                showGists -> saveableStateHolder.SaveableStateProvider("gists") { GistsScreen({ showGists = false }, onMinimize, onClose) }
                showNotifications -> saveableStateHolder.SaveableStateProvider("notifications") { NotificationsScreen(onBack = { showNotifications = false }) }
                selectedRepo != null -> saveableStateHolder.SaveableStateProvider("repo:${selectedRepo!!.fullName}") {
                    RepoDetailScreen(
                        selectedRepo!!,
                        { selectedRepo = null },
                        onMinimize,
                        onClose,
                        initialTarget = pendingTarget?.takeIf { it.repoFullName == selectedRepo!!.fullName },
                        onInitialTargetConsumed = {
                            pendingTarget = null
                            onInitialTargetConsumed()
                        },
                    )
                }
                showProfile != null -> saveableStateHolder.SaveableStateProvider("profile:${showProfile!!}") { ProfileScreen(username = showProfile!!, onBack = { showProfile = null }, onRepoClick = { selectedRepo = it }, onProfile = { showProfile = it }) }
                else -> saveableStateHolder.SaveableStateProvider("home") {
                    ReposScreen(
                        user = user,
                        onBack = onBack,
                        onMinimize = onMinimize,
                        onClose = onClose,
                        onLogout = { GitHubManager.logout(context); isLoggedIn = false; user = null },
                        onRepoClick = { selectedRepo = it },
                        onGists = { showGists = true },
                        onSettings = { showSettings = true },
                        onNotifications = { showNotifications = true },
                        onProfile = { showProfile = it },
                        onAccounts = { showAccounts = true },
                        initialShowApps = pendingAppsOpen,
                        onInitialShowAppsConsumed = {
                            pendingAppsOpen = false
                            onInitialOpenAppsConsumed()
                        },
                    )
                }
            }

            // ── Мульти-аккаунт: Gmail-style переключатель поверх контента ──
            if (showAccounts) {
                androidx.compose.ui.window.Popup(
                    onDismissRequest = { showAccounts = false },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                ) {
                    AccountSwitcherSheet(
                        accounts = gs.git.vps.data.github.AccountStore.accounts(context),
                        canAdd = !gs.git.vps.data.github.AccountStore.isFull(context),
                        onSwitch = { slot ->
                            gs.git.vps.data.github.AccountStore.switchTo(context, slot)
                            gs.git.vps.notifications.GsGitPush.syncActiveAsync(context)
                            isLoggedIn = GitHubManager.isLoggedIn(context)
                            user = GitHubManager.getCachedUser(context)
                            selectedRepo = null; showSettings = false; showGists = false
                            showNotifications = false; showProfile = null
                            showAccounts = false
                        },
                        onAdd = {
                            val prev = gs.git.vps.data.github.AccountStore.activeSlot(context)
                            val free = gs.git.vps.data.github.AccountStore.beginAddAccount(context)
                            showAccounts = false
                            if (free != null) {
                                addReturnSlot = prev
                                isLoggedIn = false
                                user = null
                            }
                        },
                        onRemove = { slot ->
                            gs.git.vps.data.github.AccountStore.removeAccount(context, slot)
                            gs.git.vps.notifications.GsGitPush.syncActiveAsync(context)
                            isLoggedIn = GitHubManager.isLoggedIn(context)
                            user = GitHubManager.getCachedUser(context)
                            if (gs.git.vps.data.github.AccountStore.accounts(context).isEmpty()) showAccounts = false
                        },
                        onDismiss = { showAccounts = false },
                    )
                }
            }

            // ── «Скоро техработы» — верхняя плашка, не блокирует. ──
            val maintenanceSoonText = appConfig?.maintenanceSoon.orEmpty()
            if (maintenanceSoonText.isNotBlank() && maintenanceNow.isBlank() && !forceUpdate) {
                androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3A2E10))
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gs.git.vps.ui.components.AiModuleText(
                            text = "!",
                            color = Color(0xFFFF9500),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        gs.git.vps.ui.components.AiModuleText(
                            text = maintenanceSoonText,
                            color = Color(0xFFFFD9A0),
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            val cfg = appConfig
            // ── Техперерыв: полноэкранная блокировка (Supercell-style) ──
            if (maintenanceNow.isNotBlank()) {
                AppGateScreen(
                    title = "scheduled maintenance",
                    message = maintenanceNow,
                    changelog = "",
                    buttonLabel = null,
                    onButton = null,
                    showStatusLink = true,
                    footnote = "checking every minute - the app unlocks itself",
                )
            }
            // ── Принудительное обновление: фуллскрин «что нового» + одна кнопка ──
            else if (forceUpdate && cfg != null) {
                AppGateScreen(
                    title = "update required",
                    message = "v$currentVersion is no longer supported.\nUpdate to v${cfg.latestVersion} to continue.",
                    changelog = cfg.changelog,
                    buttonLabel = "update now",
                    onButton = {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(cfg.downloadUrl),
                            )
                        )
                    },
                    showStatusLink = false,
                    footnote = null,
                )
            }
            // ── Мягкое обновление: диалог с чейнджлогом, можно отложить ──
            else if (softUpdate && !updateDismissed && cfg != null) {
                gs.git.vps.ui.components.AiModuleAlertDialog(
                    onDismissRequest = { updateDismissed = true },
                    title = "update available",
                    confirmButton = {
                        gs.git.vps.ui.components.AiModuleTextAction(
                            label = "update",
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(cfg.downloadUrl),
                                    )
                                )
                            },
                            tint = AiModuleTheme.colors.accent,
                        )
                    },
                    dismissButton = {
                        gs.git.vps.ui.components.AiModuleTextAction(
                            label = "later",
                            onClick = { updateDismissed = true },
                            tint = AiModuleTheme.colors.textSecondary,
                        )
                    },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gs.git.vps.ui.components.AiModuleText(
                            text = "v$currentVersion -> v${cfg.latestVersion}",
                            color = AiModuleTheme.colors.accent,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        if (cfg.changelog.isNotBlank()) {
                            gs.git.vps.ui.components.AiModuleText(
                                text = cfg.changelog,
                                color = AiModuleTheme.colors.textSecondary,
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Полноэкранный гейт (техперерыв / принудительное обновление): перекрывает весь
 * контент, съедает тапы и кнопку «назад», показывает заголовок, сообщение,
 * чейнджлог и максимум одну кнопку действия — модель Supercell.
 */
@Composable
private fun AppGateScreen(
    title: String,
    message: String,
    changelog: String,
    buttonLabel: String?,
    onButton: (() -> Unit)?,
    showStatusLink: Boolean,
    footnote: String?,
) {
    val colors = AiModuleTheme.colors
    val context = LocalContext.current
    BackHandler(enabled = true) {}
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        gs.git.vps.ui.components.AiModuleText(
            text = ">_",
            color = colors.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
        )
        Spacer(Modifier.height(16.dp))
        gs.git.vps.ui.components.AiModuleText(
            text = title,
            color = colors.textPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        gs.git.vps.ui.components.AiModuleText(
            text = message,
            color = colors.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (changelog.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(14.dp),
            ) {
                gs.git.vps.ui.components.AiModuleText(
                    text = "// what's new",
                    color = colors.textMuted,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                gs.git.vps.ui.components.AiModuleText(
                    text = changelog,
                    color = colors.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                )
            }
        }
        if (buttonLabel != null && onButton != null) {
            Spacer(Modifier.height(24.dp))
            gs.git.vps.ui.components.AiModuleText(
                text = "[ $buttonLabel ]",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onButton)
                    .border(1.dp, colors.accent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
        if (showStatusLink) {
            Spacer(Modifier.height(18.dp))
            gs.git.vps.ui.components.AiModuleText(
                text = "status.gsgit.org",
                color = colors.accent,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.clickable {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://status.gsgit.org"),
                        )
                    )
                },
            )
        }
        if (footnote != null) {
            Spacer(Modifier.height(14.dp))
            gs.git.vps.ui.components.AiModuleText(
                text = footnote,
                color = colors.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
