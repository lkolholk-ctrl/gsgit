package gs.git.vps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gs.git.vps.logging.CrashHandler
import gs.git.vps.notifications.GitHubNotificationTarget
import gs.git.vps.ui.screens.CrashActivity
import gs.git.vps.ui.screens.GitHubScreen
import gs.git.vps.ui.theme.AiModuleSurface

class MainActivity : ComponentActivity() {
    var deepLinkTarget by mutableStateOf<GitHubNotificationTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CrashHandler.hasCrashLog(this)) {
            startActivity(Intent(this, CrashActivity::class.java))
            finish()
            return
        }

        gs.git.vps.ui.theme.ThemeState.initialize(this)

        handleDeepLink(intent)

        setContent {
            AiModuleSurface {
                GitHubScreen(
                    onBack = {},
                    initialTarget = deepLinkTarget,
                    onInitialTargetConsumed = { deepLinkTarget = null },
                )
            }
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
            else -> GitHubNotificationTarget(
                repoFullName = repoFullName,
                subjectType = "",
            )
        }
        deepLinkTarget = target
    }
}
