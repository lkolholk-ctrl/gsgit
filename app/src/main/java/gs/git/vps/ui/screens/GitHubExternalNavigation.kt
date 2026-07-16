package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

internal data class GitHubSupportedLinksStatus(
    val label: String,
    val detail: String,
    val enabled: Boolean,
)

/**
 * Opens an HTTPS page in another app. GsGit handles github.com itself, so a plain
 * ACTION_VIEW can otherwise loop straight back into MainActivity.
 */
internal fun Context.openExternalHttps(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false

    val genericWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
    val externalPackage = runCatching {
        packageManager.queryIntentActivities(genericWebIntent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .firstOrNull { it != packageName }
    }.getOrNull() ?: return false

    val intent = Intent(Intent.ACTION_VIEW, uri)
        .setPackage(externalPackage)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { startActivity(intent); true }.getOrDefault(false)
}

internal fun Context.openSupportedLinksSettings(): Boolean {
    val packageUri = Uri.parse("package:$packageName")
    val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, packageUri)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (runCatching { startActivity(primary); true }.getOrDefault(false)) return true
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { startActivity(fallback); true }.getOrDefault(false)
}

internal fun Context.getGitHubSupportedLinksStatus(): GitHubSupportedLinksStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return GitHubSupportedLinksStatus(
            label = "system managed",
            detail = "Android manages GitHub links in this app's Open by default settings.",
            enabled = true,
        )
    }
    return getGitHubSupportedLinksStatus31()
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Context.getGitHubSupportedLinksStatus31(): GitHubSupportedLinksStatus {
    val state = runCatching {
        getSystemService(DomainVerificationManager::class.java)
            .getDomainVerificationUserState(packageName)
    }.getOrNull()
        ?: return GitHubSupportedLinksStatus(
            label = "status unavailable",
            detail = "Open Android settings to inspect supported links.",
            enabled = false,
        )

    val githubStates = listOf("github.com", "www.github.com")
        .mapNotNull { state.hostToStateMap[it] }
    val selected = githubStates.count {
        it == DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
            it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
    }
    val enabled = state.isLinkHandlingAllowed && selected > 0
    return GitHubSupportedLinksStatus(
        label = if (enabled) "enabled $selected/${githubStates.size.coerceAtLeast(2)}" else "not selected",
        detail = if (enabled) {
            "Repository links can open in GsGit. Unsupported GitHub pages are sent to the browser."
        } else {
            "Select github.com in Android's supported-links screen to open repository links in GsGit."
        },
        enabled = enabled,
    )
}
