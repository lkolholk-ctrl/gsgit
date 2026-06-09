package gs.git.vps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import gs.git.vps.logging.CrashHandler
import gs.git.vps.notifications.GitHubNotificationTarget
import gs.git.vps.security.BiometricHelper
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.screens.CrashActivity
import gs.git.vps.ui.screens.GitHubScreen
import gs.git.vps.ui.screens.GitHubTerminalButton
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

class MainActivity : FragmentActivity() {
    var deepLinkTarget by mutableStateOf<GitHubNotificationTarget?>(null)
    var isAppLocked by mutableStateOf(false)

    private fun isBiometricLockEnabled(): Boolean {
        val prefs = getSharedPreferences("github_prefs", MODE_PRIVATE)
        return prefs.getBoolean("biometric_lock_enabled", false)
    }

    private fun triggerBiometricUnlock() {
        if (BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.showBiometricPrompt(
                activity = this,
                onSuccess = {
                    isAppLocked = false
                },
                onError = { err ->
                    Toast.makeText(this, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            isAppLocked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CrashHandler.hasCrashLog(this)) {
            startActivity(Intent(this, CrashActivity::class.java))
            finish()
            return
        }

        gs.git.vps.ui.theme.ThemeState.initialize(this)

        if (isBiometricLockEnabled()) {
            isAppLocked = true
            triggerBiometricUnlock()
        }

        handleDeepLink(intent)

        setContent {
            AiModuleSurface {
                if (isAppLocked) {
                    AppLockedScreen(
                        onUnlock = { triggerBiometricUnlock() }
                    )
                } else {
                    GitHubScreen(
                        onBack = {},
                        initialTarget = deepLinkTarget,
                        onInitialTargetConsumed = { deepLinkTarget = null },
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBiometricLockEnabled()) {
            isAppLocked = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (isBiometricLockEnabled() && isAppLocked) {
            triggerBiometricUnlock()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.host != "github.com") return
        val segments = uri.pathSegments
        if (segments.size < 2) return

        val owner = segments[0]
        val repo = segments[1]
        val repoFullName = "$owner/$repo"

        val target = when {
            segments.size >= 4 && segments[2] == "blob" -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "File",
                number = null,
                filePath = segments.drop(4).joinToString("/"),
                branch = segments.getOrNull(3),
            )
            segments.size >= 4 && segments[2] == "tree" -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "Dir",
                number = null,
                filePath = segments.drop(4).joinToString("/"),
                branch = segments.getOrNull(3),
            )
            segments.size >= 4 && segments[2] == "issues" -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "Issue",
                number = segments[3].toIntOrNull(),
            )
            segments.size >= 4 && (segments[2] == "pull" || segments[2] == "pulls") -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "PullRequest",
                number = segments[3].toIntOrNull(),
            )
            segments.size >= 3 && segments[2] == "releases" -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "Release",
            )
            segments.size >= 3 && segments[2] == "discussions" -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "Discussion",
                number = segments.getOrNull(3)?.toIntOrNull(),
            )
            segments.size >= 3 && segments[2].contains(".") -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "File",
                number = null,
                filePath = segments.drop(2).joinToString("/"),
                branch = null,
            )
            else -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "",
            )
        }
        deepLinkTarget = target
    }
}

@Composable
private fun AppLockedScreen(onUnlock: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "locked",
                tint = palette.error,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "GsGit Secure Lock",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Application access is locked.\nPlease authenticate to proceed.",
                color = palette.textMuted,
                fontSize = 12.sp,
                fontFamily = JetBrainsMono,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            GitHubTerminalButton(
                label = "Unlock",
                onClick = onUnlock,
                color = palette.accent
            )
        }
    }
}
