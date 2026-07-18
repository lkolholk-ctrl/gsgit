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
            var pendingTarget by remember { mutableStateOf(initialTarget) }
            var pendingAppsOpen by remember { mutableStateOf(initialOpenApps) }
            val saveableStateHolder = rememberSaveableStateHolder()

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
                !isLoggedIn -> LoginScreen(onBack, onMinimize, onClose) { isLoggedIn = true }
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
                        initialShowApps = pendingAppsOpen,
                        onInitialShowAppsConsumed = {
                            pendingAppsOpen = false
                            onInitialOpenAppsConsumed()
                        },
                    )
                }
            }
        }
    }
}
