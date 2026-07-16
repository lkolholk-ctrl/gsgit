package gs.git.vps.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VolumeUp
import android.content.Context
import gs.git.vps.workers.NotificationSyncWorker
import gs.git.vps.security.BackupManager
import gs.git.vps.security.BiometricHelper
import gs.git.vps.security.PinSecurity
import gs.git.vps.security.SecurityGate
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import org.json.JSONArray
import gs.git.vps.ui.screens.ToggleRow

import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHBlockedEntry
import gs.git.vps.data.github.model.GHEmailEntry
import gs.git.vps.data.github.model.GHFollowerEntry
import gs.git.vps.data.github.model.GHNotification
import gs.git.vps.data.github.model.GHOrg
import gs.git.vps.data.github.model.GHDeviceCode
import gs.git.vps.data.github.model.GHOrgMembership
import gs.git.vps.data.github.model.GHWebhook
import gs.git.vps.data.github.model.GHSocialAccountEntry
import gs.git.vps.data.github.model.GHUser
import gs.git.vps.data.github.model.GHUserKeyEntry
import gs.git.vps.data.github.model.GHUserProfile
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHInteractionLimitEntry
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHUserRepositoryInvitation
import gs.git.vps.ui.components.AiModulePageBar
import androidx.compose.ui.platform.LocalContext
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleKeyValueRow
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import kotlinx.coroutines.launch

private enum class SettingsSection(val title: String, val subtitle: String) {
    PROFILE("Profile", "Name, bio, company, location"),
    EMAILS("Emails", "Primary email and visibility"),
    NOTIFICATIONS("Notifications", "Unread filter and mark read"),
    KEYS("Keys", "SSH, SSH signing and GPG"),
    SOCIAL("Social accounts", "Linked social profiles"),
    PEOPLE("People", "Followers and following"),
    BLOCKED("Blocked users", "Block and unblock users"),
    INTERACTION("Interaction limits", "Temporary public interaction limits"),
    ORGANIZATIONS("Organizations", "Your organizations"),
    REPOSITORIES("Repositories", "Stars, watches and invitations"),
    DEVELOPER("Developer", "Token and cache"),
    THEMES("Themes", "Custom retro terminal color palettes"),
    SECURITY("Security & Backups", "Biometric lock and secure backup configuration"),
    EDITOR("Editor & Diff", "Font size, word wrap, tab size, whitespaces"),
    AI_HELPER("AI Settings", "Provider, model, API key, system prompt"),
    NETWORK("Network & Proxy", "HTTP proxy and API endpoint configuration"),
    SYNC("Background Sync", "Notification polling interval and limits")
}

private enum class KeyMode { SSH, SSH_SIGNING, GPG }

private data class SettingsConfirmation(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val danger: Boolean = true,
    val onConfirm: () -> Unit,
)

@Composable
internal fun GitHubSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenApps: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notifications are disabled; background sync will run silently", Toast.LENGTH_LONG).show()
        }
    }

    var user by remember { mutableStateOf<GHUser?>(GitHubManager.getCachedUser(context)) }
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }
    var loading by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_lock_enabled", false)) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }

    var currentCacheSize by remember { mutableStateOf(getCacheSize(context)) }
    var cacheLimitMb by remember { mutableStateOf(prefs.getInt("cache_limit_mb", 100)) }
    var autoCleanLogs by remember { mutableStateOf(prefs.getBoolean("auto_clean_logs", true)) }

    // Category 1: Editor & Diff
    var editorTabSize by remember { mutableStateOf(prefs.getInt("editor_tab_size", 4)) }
    var editorWordWrap by remember { mutableStateOf(prefs.getBoolean("editor_word_wrap", false)) }
    var editorFontSize by remember { mutableStateOf(prefs.getInt("editor_font_size", 13)) }
    var editorIgnoreWhitespace by remember { mutableStateOf(prefs.getBoolean("editor_ignore_whitespace", false)) }
    var editorUseTabs by remember { mutableStateOf(prefs.getBoolean("editor_use_tabs", false)) }

    // Category 2: Advanced Security
    var securityAutolockTimeout by remember { mutableStateOf(prefs.getInt("security_autolock_timeout", 0)) }
    var securityPinInput by remember { mutableStateOf("") }
    var securityPgpKeyAlgorithm by remember { mutableStateOf(prefs.getString("security_pgp_key_algorithm", "RSA-4096").orEmpty()) }
    var securityEnvironmentPolicy by remember { mutableStateOf(SecurityGate.policy(context)) }

    // Category 3: GitHub Copilot
    var copilotModel by remember { mutableStateOf(prefs.getString("copilot_model", "gpt-5.5").orEmpty()) }
    var copilotRouting by remember { mutableStateOf(prefs.getString("copilot_routing", "Auto").orEmpty()) }
    var aiSystemPrompt by remember { mutableStateOf(prefs.getString("ai_system_prompt", "You are a professional developer helping to review code, troubleshoot errors, and suggest fixes.").orEmpty()) }

    // Category 4: Network & Proxy
    var networkProxyEnabled by remember { mutableStateOf(prefs.getBoolean("network_proxy_enabled", false)) }
    var networkProxyHost by remember { mutableStateOf(prefs.getString("network_proxy_host", "").orEmpty()) }
    var networkProxyPort by remember { mutableStateOf(prefs.getInt("network_proxy_port", 8080)) }

    // Category 5: Background Sync
    var syncBackgroundEnabled by remember { mutableStateOf(prefs.getBoolean("sync_background_enabled", false)) }
    var syncIntervalMins by remember { mutableStateOf(prefs.getInt("sync_interval_mins", 30)) }
    var syncWifiOnly by remember { mutableStateOf(prefs.getBoolean("sync_wifi_only", false)) }

    // Category 6: Cyberpunk Cosmetics
    var cosmeticCrtEffect by remember { mutableStateOf(prefs.getBoolean("cosmetic_crt_effect", false)) }
    var cosmeticKeyboardSound by remember { mutableStateOf(prefs.getBoolean("cosmetic_keyboard_sound", false)) }


    var profile by remember { mutableStateOf<GHUserProfile?>(null) }
    var profileName by remember { mutableStateOf("") }
    var profileBio by remember { mutableStateOf("") }
    var profileCompany by remember { mutableStateOf("") }
    var profileLocation by remember { mutableStateOf("") }
    var profileBlog by remember { mutableStateOf("") }
    var profileEmail by remember { mutableStateOf("") }
    var profileTwitter by remember { mutableStateOf("") }
    var profileHireable by remember { mutableStateOf(false) }

    var emails by remember { mutableStateOf<List<GHEmailEntry>>(emptyList()) }
    var newEmail by remember { mutableStateOf("") }
    var emailVisibility by remember { mutableStateOf("private") }

    var notifications by remember { mutableStateOf<List<GHNotification>>(emptyList()) }
    var notificationsUnreadOnly by remember { mutableStateOf(true) }

    var keyMode by remember { mutableStateOf(KeyMode.SSH) }
    var sshKeys by remember { mutableStateOf<List<GHUserKeyEntry>>(emptyList()) }
    var sshSigningKeys by remember { mutableStateOf<List<GHUserKeyEntry>>(emptyList()) }
    var gpgKeys by remember { mutableStateOf<List<GHUserKeyEntry>>(emptyList()) }
    var keyTitle by remember { mutableStateOf("") }
    var keyValue by remember { mutableStateOf("") }

    var localPgpUser by remember { mutableStateOf("") }
    var localPgpPass by remember { mutableStateOf("") }
    var localPgpPublicKey by remember { mutableStateOf(gs.git.vps.security.PgpKeyManager.getPublicKey(context)) }
    var localPgpUserId by remember { mutableStateOf(gs.git.vps.security.PgpKeyManager.getUserId(context)) }
    var pgpSigningEnabled by remember { mutableStateOf(gs.git.vps.security.PgpKeyManager.isPgpEnabled(context)) }

    LaunchedEffect(user, emails) {
        if (localPgpUser.isBlank() && user != null) {
            val email = emails.firstOrNull { it.primary }?.email.orEmpty().ifBlank { emails.firstOrNull()?.email.orEmpty() }
            val name = user?.name?.ifBlank { user?.login }.orEmpty().ifBlank { user?.login.orEmpty() }
            localPgpUser = if (email.isNotBlank()) "$name <$email>" else name
        }
    }

    var socialAccounts by remember { mutableStateOf<List<GHSocialAccountEntry>>(emptyList()) }
    var newSocialUrl by remember { mutableStateOf("") }

    var followers by remember { mutableStateOf<List<GHFollowerEntry>>(emptyList()) }
    var following by remember { mutableStateOf<List<GHFollowerEntry>>(emptyList()) }

    var blockedUsers by remember { mutableStateOf<List<GHBlockedEntry>>(emptyList()) }
    var blockUsername by remember { mutableStateOf("") }

    var interactionLimit by remember { mutableStateOf<GHInteractionLimitEntry?>(null) }
    var organizations by remember { mutableStateOf<List<GHOrg>>(emptyList()) }
    var starredRepos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var watchedRepos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var repoInvitations by remember { mutableStateOf<List<GHUserRepositoryInvitation>>(emptyList()) }
    var rateLimitSummary by remember { mutableStateOf("Unavailable") }
    var gsGitAppConnection by remember { mutableStateOf(GitHubManager.getGsGitAppConnection(context)) }
    var showChangeToken by remember { mutableStateOf(false) }
    var showChangeApiUrl by remember { mutableStateOf(false) }
    var showDeviceLogin by remember { mutableStateOf(false) }
    var newToken by remember { mutableStateOf("") }
    var newApiUrl by remember { mutableStateOf(GitHubManager.getApiUrl()) }
    var emailQuery by remember { mutableStateOf("") }
    var notificationQuery by remember { mutableStateOf("") }
    var keyQuery by remember { mutableStateOf("") }
    var socialQuery by remember { mutableStateOf("") }
    var peopleQuery by remember { mutableStateOf("") }
    var blockedQuery by remember { mutableStateOf("") }
    var organizationQuery by remember { mutableStateOf("") }
    var repositoryQuery by remember { mutableStateOf("") }
    var pendingConfirmation by remember { mutableStateOf<SettingsConfirmation?>(null) }
    val actionLog = remember { mutableStateListOf<String>() }

    fun addLog(line: String) {
        actionLog.add(0, line)
        while (actionLog.size > 25) actionLog.removeLast()
    }

    fun handleBack() {
        when {
            pendingConfirmation != null -> pendingConfirmation = null
            showChangeToken -> showChangeToken = false
            showChangeApiUrl -> showChangeApiUrl = false
            currentSection == null -> onBack()
            else -> currentSection = null
        }
    }

    fun confirmAction(
        title: String,
        body: String,
        confirmLabel: String,
        danger: Boolean = true,
        onConfirm: () -> Unit,
    ) {
        pendingConfirmation = SettingsConfirmation(title, body, confirmLabel, danger, onConfirm)
    }

    suspend fun refreshSection(section: SettingsSection?) {
        loading = true
        when (section) {
            null -> user = GitHubManager.getUser(context) ?: GitHubManager.getCachedUser(context)
            SettingsSection.PROFILE -> {
                user = GitHubManager.getUser(context) ?: GitHubManager.getCachedUser(context)
                profile = GitHubManager.getCurrentUserProfile(context)
                emails = GitHubManager.getEmailEntries(context)
                profileName = profile?.name ?: user?.name.orEmpty()
                profileBio = profile?.bio.orEmpty()
                profileCompany = profile?.company.orEmpty()
                profileLocation = profile?.location.orEmpty()
                profileBlog = profile?.blog.orEmpty()
                profileEmail = profile?.email.orEmpty()
                profileTwitter = profile?.twitterUsername.orEmpty()
                profileHireable = profile?.hireable ?: false
            }
            SettingsSection.EMAILS -> {
                emails = GitHubManager.getEmailEntries(context)
                emailVisibility = emails.firstOrNull { it.primary }?.visibility?.ifBlank { "private" } ?: "private"
            }
            SettingsSection.NOTIFICATIONS -> notifications = GitHubManager.getNotifications(context, all = !notificationsUnreadOnly)
            SettingsSection.KEYS -> {
                sshKeys = GitHubManager.getSshKeysNative(context)
                sshSigningKeys = GitHubManager.getSshSigningKeysNative(context)
                gpgKeys = GitHubManager.getGpgKeysNative(context)
            }
            SettingsSection.SOCIAL -> socialAccounts = GitHubManager.getSocialAccountsNative(context)
            SettingsSection.PEOPLE -> {
                followers = GitHubManager.getFollowersNative(context)
                following = GitHubManager.getFollowingNative(context)
            }
            SettingsSection.BLOCKED -> blockedUsers = GitHubManager.getBlockedUsersNative(context)
            SettingsSection.INTERACTION -> interactionLimit = GitHubManager.getInteractionLimitNative(context)
            SettingsSection.ORGANIZATIONS -> organizations = GitHubManager.getOrganizations(context)
            SettingsSection.REPOSITORIES -> {
                starredRepos = GitHubManager.getStarredRepos(context)
                watchedRepos = GitHubManager.getWatchedRepos(context)
                repoInvitations = GitHubManager.getUserRepositoryInvitations(context)
            }
            SettingsSection.DEVELOPER -> {
                rateLimitSummary = GitHubManager.getRateLimitSummaryNative(context)
                gsGitAppConnection = GitHubManager.getGsGitAppConnection(context)
            }
            SettingsSection.THEMES -> {}
            SettingsSection.SECURITY -> {}
            SettingsSection.EDITOR -> {}
            SettingsSection.AI_HELPER -> {}
            SettingsSection.NETWORK -> {}
            SettingsSection.SYNC -> {}
        }
        loading = false
    }

    LaunchedEffect(currentSection) { refreshSection(currentSection) }
    BackHandler(onBack = ::handleBack)

    val visibleEmails = emails.filter { email ->
        matchesSettingsQuery(emailQuery, email.email, email.visibility, if (email.primary) "primary" else "", if (email.verified) "verified" else "")
    }
    val visibleNotifications = notifications.filter { item ->
        matchesSettingsQuery(notificationQuery, item.title, item.repoName, item.reason, item.id)
    }
    val currentKeys = when (keyMode) {
        KeyMode.SSH -> sshKeys
        KeyMode.SSH_SIGNING -> sshSigningKeys
        KeyMode.GPG -> gpgKeys
    }
    val visibleKeys = currentKeys.filter { key ->
        matchesSettingsQuery(keyQuery, key.title, key.key, key.kind, key.createdAt, key.id.toString())
    }
    val visibleSocialAccounts = socialAccounts.filter { acc ->
        matchesSettingsQuery(socialQuery, acc.provider, acc.url)
    }
    val visibleFollowers = followers.filter { person -> matchesSettingsQuery(peopleQuery, person.login) }
    val visibleFollowing = following.filter { person -> matchesSettingsQuery(peopleQuery, person.login) }
    val visibleBlockedUsers = blockedUsers.filter { blocked -> matchesSettingsQuery(blockedQuery, blocked.login) }
    val visibleOrganizations = organizations.filter { org ->
        matchesSettingsQuery(organizationQuery, org.login, org.description)
    }
    val visibleInvitations = repoInvitations.filter { invitation ->
        matchesSettingsQuery(repositoryQuery, invitation.repoFullName, invitation.inviter, invitation.permissions, invitation.id.toString())
    }
    val visibleWatchedRepos = watchedRepos.filter { repo ->
        matchesSettingsQuery(repositoryQuery, repo.fullName, repo.name, repo.owner, repo.language, repo.description)
    }
    val visibleStarredRepos = starredRepos.filter { repo ->
        matchesSettingsQuery(repositoryQuery, repo.fullName, repo.name, repo.owner, repo.language, repo.description)
    }

    GitHubScreenFrame(
        title = "> ${(currentSection?.title ?: "settings").lowercase()}",
        subtitle = currentSection?.let { user?.name?.takeIf { n -> n.isNotBlank() } ?: user?.login },
        onBack = ::handleBack,
        trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loading) {
                        AiModuleSpinner()
                    } else {
                        GitHubTopBarAction(
                            glyph = GhGlyphs.REFRESH,
                            onClick = { scope.launch { refreshSection(currentSection) } },
                            tint = AiModuleTheme.colors.accent,
                            contentDescription = "refresh settings",
                        )
                    }
                    if (onClose != null) {
                        GitHubTopBarAction(
                            glyph = GhGlyphs.CLOSE,
                            onClick = onClose,
                            tint = AiModuleTheme.colors.error,
                            contentDescription = "close",
                        )
                    }
                }
        },
    ) {

        if (currentSection == null) {
            HomeSettingsMenu(user = user, onOpen = { currentSection = it })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { HomeUserHeader(user) }
                item {
                    when (currentSection) {
                        SettingsSection.PROFILE -> SectionCard("Profile") {
                            CompactField("Name", profileName) { profileName = it }
                            CompactField("Bio", profileBio, singleLine = false, minLines = 3) { profileBio = it }
                            CompactField("Company", profileCompany) { profileCompany = it }
                            CompactField("Location", profileLocation) { profileLocation = it }
                            CompactField("Blog", profileBlog) { profileBlog = it }
                            CompactField("Public email", profileEmail) { profileEmail = it }
                            PublicEmailChooser(profileEmail, emails) { profileEmail = it }
                            CompactField("Twitter/X username", profileTwitter) { profileTwitter = it }
                            HireableChooser(profileHireable) { profileHireable = it }
                            ProfileAccountSummary(profile)
                            ActionRow(Icons.Rounded.Check, "Save profile") {
                                scope.launch {
                                    val profileOk = GitHubManager.updateCurrentUserProfile(
                                        context = context,
                                        name = profileName,
                                        bio = profileBio,
                                        company = profileCompany,
                                        location = profileLocation,
                                        blog = profileBlog,
                                        email = profileEmail,
                                        twitterUsername = profileTwitter,
                                        hireable = profileHireable,
                                    )
                                    val visibilityOk = if (profileEmail.isBlank()) GitHubManager.setEmailVisibility(context, "private") else true
                                    val ok = profileOk && visibilityOk
                                    addLog("Profile updated: profile=$profileOk visibility=$visibilityOk")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.PROFILE)
                                }
                            }
                        }
                        SettingsSection.EMAILS -> SectionCard("Emails") {
                            CompactField("Filter emails", emailQuery) { emailQuery = it }
                            SettingsCountLine("showing ${visibleEmails.size} / ${emails.size}")
                            VisibilityChooser(emailVisibility) { emailVisibility = it }
                            ActionRow(Icons.Rounded.Check, "Apply visibility") {
                                scope.launch {
                                    val ok = GitHubManager.setEmailVisibility(context, emailVisibility)
                                    addLog("Email visibility: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.EMAILS)
                                }
                            }
                            CompactField("Add email", newEmail) { newEmail = it }
                            ActionRow(Icons.Rounded.Add, "Add email") {
                                val email = newEmail.trim()
                                if (email.isNotBlank()) {
                                    scope.launch {
                                        val ok = GitHubManager.addEmailAddress(context, email)
                                        addLog("Add email: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        if (ok) newEmail = ""
                                        refreshSection(SettingsSection.EMAILS)
                                    }
                                }
                            }
                            if (visibleEmails.isEmpty()) SettingsEmptyLine(if (emails.isEmpty()) "No emails returned" else "No emails match the filter")
                        }
                        SettingsSection.NOTIFICATIONS -> SectionCard("Notifications") {
                            CompactField("Filter notifications", notificationQuery) { notificationQuery = it }
                            SettingsCountLine("showing ${visibleNotifications.size} / ${notifications.size}")
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        notificationsUnreadOnly = !notificationsUnreadOnly
                                        scope.launch { refreshSection(SettingsSection.NOTIFICATIONS) }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (notificationsUnreadOnly) "[x]" else "[ ]",
                                    color = AiModuleTheme.colors.accent,
                                    fontSize = 13.sp,
                                    fontFamily = JetBrainsMono,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "unread only",
                                    color = AiModuleTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = JetBrainsMono,
                                )
                            }
                            ActionRow(Icons.Rounded.Check, "Mark all read") {
                                scope.launch {
                                    val ok = GitHubManager.markAllNotificationsRead(context)
                                    addLog("Mark all read: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.NOTIFICATIONS)
                                }
                            }
                            if (visibleNotifications.isEmpty()) SettingsEmptyLine(if (notifications.isEmpty()) "No notifications" else "No notifications match the filter")
                        }
                        SettingsSection.KEYS -> SectionCard("Keys") {
                            KeyModeRow(keyMode) { keyMode = it }
                            if (keyMode == KeyMode.GPG) {
                                val palette = AiModuleTheme.colors
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .border(0.5.dp, palette.border, RoundedCornerShape(8.dp))
                                        .background(palette.surface.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "[ LOCAL PGP KEYRING ]",
                                            color = palette.accent,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                "Sign commits via PGP",
                                                color = palette.textPrimary,
                                                fontFamily = JetBrainsMono,
                                                fontSize = 12.sp
                                            )
                                            androidx.compose.material3.Switch(
                                                checked = pgpSigningEnabled,
                                                onCheckedChange = {
                                                    pgpSigningEnabled = it
                                                    gs.git.vps.security.PgpKeyManager.setPgpEnabled(context, it)
                                                    addLog("PGP Signing ${if (it) "enabled" else "disabled"}")
                                                },
                                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                                    checkedThumbColor = palette.accent,
                                                    checkedTrackColor = palette.accent.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = palette.textSecondary,
                                                    uncheckedTrackColor = palette.border
                                                )
                                            )
                                        }

                                        if (localPgpPublicKey.isNullOrBlank()) {
                                            Text(
                                                "No local PGP keypair found. Generate one to sign commits on push.",
                                                color = palette.textSecondary,
                                                fontFamily = JetBrainsMono,
                                                fontSize = 11.sp
                                            )
                                            CompactField("User ID (Name <email>)", localPgpUser) { localPgpUser = it }
                                            CompactField("Passphrase", localPgpPass) { localPgpPass = it }

                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "PGP key algorithm size:",
                                                color = palette.textSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = JetBrainsMono
                                            )
                                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("RSA-2048", "RSA-4096").forEach { algo ->
                                                    val isSelected = securityPgpKeyAlgorithm == algo
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                                            .background(if (isSelected) palette.accent.copy(alpha = 0.14f) else palette.border)
                                                            .border(1.dp, if (isSelected) palette.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                            .clickable {
                                                                securityPgpKeyAlgorithm = algo
                                                                prefs.edit().putString("security_pgp_key_algorithm", algo).apply()
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(algo, fontSize = 11.sp, color = if (isSelected) palette.accent else palette.textPrimary, fontFamily = JetBrainsMono)
                                                    }
                                                }
                                            }

                                            ActionRow(Icons.Rounded.Key, "Generate PGP Keypair") {
                                                if (localPgpUser.isBlank()) {
                                                    Toast.makeText(context, "User ID is required", Toast.LENGTH_SHORT).show()
                                                    return@ActionRow
                                                }
                                                scope.launch {
                                                    loading = true
                                                    val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                                        gs.git.vps.security.PgpKeyManager.generateKeyPair(
                                                            context,
                                                            localPgpUser,
                                                            localPgpPass
                                                        )
                                                    }
                                                    loading = false
                                                    if (success) {
                                                        localPgpPublicKey = gs.git.vps.security.PgpKeyManager.getPublicKey(context)
                                                        localPgpUserId = gs.git.vps.security.PgpKeyManager.getUserId(context)
                                                        pgpSigningEnabled = true
                                                        gs.git.vps.security.PgpKeyManager.setPgpEnabled(context, true)
                                                        Toast.makeText(context, "Keypair generated successfully!", Toast.LENGTH_SHORT).show()
                                                        addLog("Generated PGP Keypair: $localPgpUserId")
                                                    } else {
                                                        Toast.makeText(context, "Failed to generate keypair", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    "User ID: $localPgpUserId",
                                                    color = palette.textPrimary,
                                                    fontFamily = JetBrainsMono,
                                                    fontSize = 11.sp
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(100.dp)
                                                        .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                                        .background(palette.surface)
                                                        .padding(6.dp)
                                                ) {
                                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                        item {
                                                            Text(
                                                                localPgpPublicKey.orEmpty(),
                                                                color = palette.textSecondary,
                                                                fontFamily = JetBrainsMono,
                                                                fontSize = 9.sp,
                                                                lineHeight = 12.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    // Copy button
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(palette.surface)
                                                            .border(0.5.dp, palette.border, RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("pgp_pubkey", localPgpPublicKey))
                                                                Toast.makeText(context, "Copied public key", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .padding(vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("copy", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp)
                                                    }

                                                    // Upload to GitHub
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1.5f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(palette.accent.copy(alpha = 0.1f))
                                                            .border(0.5.dp, palette.accent, RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                scope.launch {
                                                                    loading = true
                                                                    val ok = GitHubManager.addGpgKeyNative(context, localPgpPublicKey.orEmpty())
                                                                    loading = false
                                                                    Toast.makeText(context, if (ok) "Uploaded to GitHub!" else "Upload failed", Toast.LENGTH_SHORT).show()
                                                                    if (ok) {
                                                                        refreshSection(SettingsSection.KEYS)
                                                                    }
                                                                }
                                                            }
                                                            .padding(vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("upload to github", color = palette.accent, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    // Delete button
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(palette.surface)
                                                            .border(0.5.dp, palette.error, RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                confirmAction(
                                                                    title = "delete local PGP",
                                                                    body = "Delete local PGP keys? You will no longer be able to sign commits with this key.",
                                                                    confirmLabel = "delete",
                                                                    danger = true
                                                                ) {
                                                                    gs.git.vps.security.PgpKeyManager.deleteKeys(context)
                                                                    gs.git.vps.security.PgpKeyManager.setPgpEnabled(context, false)
                                                                    localPgpPublicKey = null
                                                                    localPgpUserId = null
                                                                    pgpSigningEnabled = false
                                                                    localPgpPass = ""
                                                                    addLog("Deleted local PGP keypair")
                                                                    Toast.makeText(context, "Deleted local PGP keys", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            .padding(vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("delete", color = palette.error, fontFamily = JetBrainsMono, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            CompactField("Filter keys", keyQuery) { keyQuery = it }
                            SettingsCountLine("showing ${visibleKeys.size} / ${currentKeys.size}")
                            CompactField(if (keyMode == KeyMode.GPG) "Name" else "Title", keyTitle) { keyTitle = it }
                            CompactField(if (keyMode == KeyMode.GPG) "ASCII-armored key" else "Public key", keyValue, singleLine = false, minLines = 4) { keyValue = it }
                            ActionRow(Icons.Rounded.Add, "Add key") {
                                val title = keyTitle.trim()
                                val publicKey = keyValue.trim()
                                if (publicKey.isNotBlank() && (keyMode == KeyMode.GPG || title.isNotBlank())) {
                                    scope.launch {
                                        val ok = when (keyMode) {
                                            KeyMode.SSH -> GitHubManager.addSshKeyNative(context, title, publicKey)
                                            KeyMode.SSH_SIGNING -> GitHubManager.addSshSigningKeyNative(context, title, publicKey)
                                            KeyMode.GPG -> GitHubManager.addGpgKeyNative(context, publicKey)
                                        }
                                        addLog("Add key: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        if (ok) {
                                            keyTitle = ""
                                            keyValue = ""
                                        }
                                        refreshSection(SettingsSection.KEYS)
                                    }
                                }
                            }
                            if (visibleKeys.isEmpty()) SettingsEmptyLine(if (currentKeys.isEmpty()) "No ${keyMode.name.lowercase()} keys" else "No keys match the filter")
                        }
                        SettingsSection.SOCIAL -> SectionCard("Social accounts") {
                            CompactField("Filter social accounts", socialQuery) { socialQuery = it }
                            SettingsCountLine("showing ${visibleSocialAccounts.size} / ${socialAccounts.size}")
                            CompactField("Add social URL", newSocialUrl) { newSocialUrl = it }
                            ActionRow(Icons.Rounded.Add, "Add social account") {
                                val url = newSocialUrl.trim()
                                if (url.isNotBlank()) {
                                    scope.launch {
                                        val ok = GitHubManager.addSocialAccountNative(context, url)
                                        addLog("Add social account: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        if (ok) newSocialUrl = ""
                                        refreshSection(SettingsSection.SOCIAL)
                                    }
                                }
                            }
                            if (visibleSocialAccounts.isEmpty()) SettingsEmptyLine(if (socialAccounts.isEmpty()) "No social accounts" else "No social accounts match the filter")
                        }
                        SettingsSection.PEOPLE -> SectionCard("People") {
                            CompactField("Filter people", peopleQuery) { peopleQuery = it }
                            SettingsCountLine("followers ${visibleFollowers.size} / ${followers.size} • following ${visibleFollowing.size} / ${following.size}")
                            Text("Followers", color = AiModuleTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (visibleFollowers.isEmpty()) SettingsEmptyLine(if (followers.isEmpty()) "No followers" else "No followers match the filter")
                            visibleFollowers.forEach { person ->
                                CompactPersonRow(person.login, person.avatarUrl, "Follow") {
                                    scope.launch {
                                        val ok = GitHubManager.followUser(context, person.login)
                                        addLog("Follow ${person.login}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.PEOPLE)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Following", color = AiModuleTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (visibleFollowing.isEmpty()) SettingsEmptyLine(if (following.isEmpty()) "Not following anyone" else "No following users match the filter")
                            visibleFollowing.forEach { person ->
                                CompactPersonRow(person.login, person.avatarUrl, "Unfollow") {
                                    confirmAction(
                                        title = "unfollow user",
                                        body = "Stop following @${person.login}?",
                                        confirmLabel = "unfollow",
                                    ) {
                                        scope.launch {
                                            val ok = GitHubManager.unfollowUser(context, person.login)
                                            addLog("Unfollow ${person.login}: $ok")
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            refreshSection(SettingsSection.PEOPLE)
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.BLOCKED -> SectionCard("Blocked users") {
                            CompactField("Filter blocked users", blockedQuery) { blockedQuery = it }
                            SettingsCountLine("showing ${visibleBlockedUsers.size} / ${blockedUsers.size}")
                            CompactField("Block username", blockUsername) { blockUsername = it }
                            ActionRow(Icons.Rounded.Block, "Block user") {
                                val username = blockUsername.trim()
                                if (username.isNotBlank()) {
                                    confirmAction(
                                        title = "block user",
                                        body = "Block @$username from interacting with you?",
                                        confirmLabel = "block",
                                    ) {
                                        scope.launch {
                                            val ok = GitHubManager.blockUserNative(context, username)
                                            addLog("Block $username: $ok")
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            if (ok) blockUsername = ""
                                            refreshSection(SettingsSection.BLOCKED)
                                        }
                                    }
                                }
                            }
                            if (visibleBlockedUsers.isEmpty()) SettingsEmptyLine(if (blockedUsers.isEmpty()) "No blocked users" else "No blocked users match the filter")
                        }
                        SettingsSection.INTERACTION -> SectionCard("Interaction limits") {
                            Text(interactionLimit?.let { "Current: ${it.limit}${it.expiry?.let { exp -> " • $exp" } ?: ""}" } ?: "No active interaction limit", color = AiModuleTheme.colors.textSecondary, fontSize = 12.sp)
                            ActionRow(Icons.Rounded.Warning, "Existing users for 24h") {
                                scope.launch {
                                    val ok = GitHubManager.setInteractionLimitNative(context, "existing_users", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Warning, "Contributors only for 24h") {
                                scope.launch {
                                    val ok = GitHubManager.setInteractionLimitNative(context, "contributors_only", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Warning, "Collaborators only for 24h") {
                                scope.launch {
                                    val ok = GitHubManager.setInteractionLimitNative(context, "collaborators_only", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Delete, "Remove interaction limit", tint = AiModuleTheme.colors.error) {
                                confirmAction(
                                    title = "remove interaction limit",
                                    body = "Remove the active public interaction limit from your account?",
                                    confirmLabel = "remove",
                                ) {
                                    scope.launch {
                                        val ok = GitHubManager.removeInteractionLimitNative(context)
                                        addLog("Remove interaction limit: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.INTERACTION)
                                    }
                                }
                            }
                        }
                        SettingsSection.ORGANIZATIONS -> SectionCard("Organizations") {
                            CompactField("Filter organizations", organizationQuery) { organizationQuery = it }
                            SettingsCountLine("showing ${visibleOrganizations.size} / ${organizations.size}")
                            if (visibleOrganizations.isEmpty()) SettingsEmptyLine(if (organizations.isEmpty()) "No organizations" else "No organizations match the filter")
                            visibleOrganizations.forEach { org -> CompactOrgRow(org) }
                        }
                        SettingsSection.REPOSITORIES -> SectionCard("Repositories") {
                            CompactField("Filter repositories", repositoryQuery) { repositoryQuery = it }
                            Text(
                                "stars ${visibleStarredRepos.size} / ${starredRepos.size} • watched ${visibleWatchedRepos.size} / ${watchedRepos.size} • invites ${visibleInvitations.size} / ${repoInvitations.size}",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                            )
                            if (visibleInvitations.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                SectionHeader("Repository invitations")
                                visibleInvitations.forEach { invitation ->
                                    UserRepositoryInvitationRow(
                                        invitation = invitation,
                                        onAccept = {
                                            scope.launch {
                                                val ok = GitHubManager.acceptUserRepositoryInvitation(context, invitation.id)
                                                addLog("Accept invitation ${invitation.id}: $ok")
                                                Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                                refreshSection(SettingsSection.REPOSITORIES)
                                            }
                                        },
                                        onDecline = {
                                            confirmAction(
                                                title = "decline invitation",
                                                body = "Decline invitation to ${invitation.repoFullName.ifBlank { "repository ${invitation.id}" }}?",
                                                confirmLabel = "decline",
                                            ) {
                                                scope.launch {
                                                    val ok = GitHubManager.declineUserRepositoryInvitation(context, invitation.id)
                                                    addLog("Decline invitation ${invitation.id}: $ok")
                                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                                    refreshSection(SettingsSection.REPOSITORIES)
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            SectionHeader("Watched repositories")
                            if (visibleWatchedRepos.isEmpty()) SettingsEmptyLine(if (watchedRepos.isEmpty()) "No watched repositories" else "No watched repositories match the filter")
                            visibleWatchedRepos.forEach { repo ->
                                CompactRepoRow(repo, action = "unwatch") {
                                    confirmAction(
                                        title = "unwatch repository",
                                        body = "Stop watching ${repo.fullName}?",
                                        confirmLabel = "unwatch",
                                    ) {
                                        scope.launch {
                                            val ok = GitHubManager.unwatchRepo(context, repo.owner, repo.name)
                                            addLog("Unwatch ${repo.fullName}: $ok")
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            refreshSection(SettingsSection.REPOSITORIES)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            SectionHeader("Starred repositories")
                            if (visibleStarredRepos.isEmpty()) SettingsEmptyLine(if (starredRepos.isEmpty()) "No starred repositories" else "No starred repositories match the filter")
                            visibleStarredRepos.forEach { repo ->
                                CompactRepoRow(repo, action = "unstar") {
                                    confirmAction(
                                        title = "unstar repository",
                                        body = "Remove star from ${repo.fullName}?",
                                        confirmLabel = "unstar",
                                    ) {
                                        scope.launch {
                                            val ok = GitHubManager.unstarRepo(context, repo.owner, repo.name)
                                            addLog("Unstar ${repo.fullName}: $ok")
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            refreshSection(SettingsSection.REPOSITORIES)
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.DEVELOPER -> SectionCard("Developer") {
                            InfoLine("Token", maskToken(GitHubManager.getToken(context)))
                            InfoLine("GsGit App", gsGitAppConnection.status)
                            InfoLine("Base URL", GitHubManager.getApiUrl())
                            InfoLine("Rate limit", rateLimitSummary)
                            ActionRow(Icons.Rounded.Business, "Manage GsGit App") { onOpenApps() }
                            ActionRow(Icons.Rounded.Key, "Change token") { showChangeToken = true }
                            ActionRow(Icons.Rounded.Code, "Custom API Base URL") {
                                newApiUrl = GitHubManager.getApiUrl()
                                showChangeApiUrl = true
                            }
                            ActionRow(Icons.Rounded.Add, "Device login (recommended for Copilot)") { showDeviceLogin = true }
                            ActionRow(Icons.Rounded.Delete, "Clear GitHub cache") {
                                confirmAction(
                                    title = "clear github cache",
                                    body = "Clear cached GitHub user and settings data from this device?",
                                    confirmLabel = "clear",
                                ) {
                                    GitHubManager.clearGitHubUserCache(context)
                                    addLog("Cleared GitHub cache")
                                    Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                                }
                            }
                            ActionRow(Icons.Rounded.Logout, "Sign out", tint = AiModuleTheme.colors.error) {
                                confirmAction(
                                    title = "sign out",
                                    body = "Remove the GitHub token from this device and leave the GitHub module?",
                                    confirmLabel = "sign out",
                                ) {
                                    GitHubManager.logout(context)
                                    onLogout()
                                }
                            }
                            if (actionLog.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "// recent actions",
                                    color = AiModuleTheme.colors.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.height(4.dp))
                                actionLog.forEach { line ->
                                    Text(
                                        line,
                                        color = AiModuleTheme.colors.textMuted,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMono,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                        SettingsSection.THEMES -> SectionCard("Themes") {
                            Text(
                                text = "// premium color schemes",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val themes = listOf(
                                Triple(gs.git.vps.data.AppThemeMode.DARK, "Dark Classic", "Default green terminal style"),
                                Triple(gs.git.vps.data.AppThemeMode.AMOLED, "Amoled Black", "Pure black high contrast green"),
                                Triple(gs.git.vps.data.AppThemeMode.LIGHT, "Github Light", "Clean white background coding space"),
                                Triple(gs.git.vps.data.AppThemeMode.DRACULA, "Dracula Premium", "Rich dark purple and pink neon accent"),
                                Triple(gs.git.vps.data.AppThemeMode.GRUVBOX, "Gruvbox Sand", "Retro warm desert sand terminal aesthetic"),
                                Triple(gs.git.vps.data.AppThemeMode.ONEDARK, "One Dark Core", "Modern elegant deep gray multi-color syntax"),
                                Triple(gs.git.vps.data.AppThemeMode.NORD, "Nord Ice", "Frosty arctic blue-gray minimalistic vibe"),
                                Triple(gs.git.vps.data.AppThemeMode.CYBERPUNK, "Cyberpunk Neon", "High contrast magenta, cyan, and black theme")
                            )
                            
                            themes.forEach { theme ->
                                val mode = theme.first
                                val name = theme.second
                                val desc = theme.third
                                val selected = gs.git.vps.ui.theme.ThemeState.mode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(GitHubControlRadius))
                                        .background(if (selected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.surfaceElevated)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.border,
                                            shape = RoundedCornerShape(GitHubControlRadius)
                                        )
                                        .clickable {
                                            gs.git.vps.ui.theme.ThemeState.saveTheme(context, mode)
                                            addLog("Switched theme to $name")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (mode) {
                                                    gs.git.vps.data.AppThemeMode.DARK -> Color(0xFF00FF00)
                                                    gs.git.vps.data.AppThemeMode.AMOLED -> Color(0xFF00DD00)
                                                    gs.git.vps.data.AppThemeMode.LIGHT -> Color(0xFF0969DA)
                                                    gs.git.vps.data.AppThemeMode.DRACULA -> Color(0xFFBD93F9)
                                                    gs.git.vps.data.AppThemeMode.GRUVBOX -> Color(0xFFFABD2F)
                                                    gs.git.vps.data.AppThemeMode.ONEDARK -> Color(0xFF61AFEF)
                                                    gs.git.vps.data.AppThemeMode.NORD -> Color(0xFF88C0D0)
                                                    gs.git.vps.data.AppThemeMode.CYBERPUNK -> Color(0xFF00F0FF)
                                                }
                                            )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            color = AiModuleTheme.colors.textMuted,
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMono
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "selected",
                                            tint = AiModuleTheme.colors.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// typography settings",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(AiModuleTheme.colors.surfaceElevated)
                                    .border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Editor Font Size",
                                        color = AiModuleTheme.colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Current size: ${gs.git.vps.ui.theme.ThemeState.fileFontSize}sp",
                                        color = AiModuleTheme.colors.textMuted,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMono
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(AiModuleTheme.colors.border)
                                            .clickable {
                                                if (gs.git.vps.ui.theme.ThemeState.fileFontSize > 10) {
                                                    gs.git.vps.ui.theme.ThemeState.saveFontSize(context, gs.git.vps.ui.theme.ThemeState.fileFontSize - 1)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("-", color = AiModuleTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(AiModuleTheme.colors.border)
                                            .clickable {
                                                if (gs.git.vps.ui.theme.ThemeState.fileFontSize < 28) {
                                                    gs.git.vps.ui.theme.ThemeState.saveFontSize(context, gs.git.vps.ui.theme.ThemeState.fileFontSize + 1)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("+", color = AiModuleTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// cyberpunk cosmetics",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ToggleRow(
                                label = "CRT Screen Scanlines",
                                checked = cosmeticCrtEffect,
                                icon = Icons.Rounded.Palette
                            ) {
                                cosmeticCrtEffect = it
                                prefs.edit().putBoolean("cosmetic_crt_effect", it).apply()
                                addLog("CRT scanlines ${if (it) "enabled" else "disabled"}")
                            }
                            ToggleRow(
                                label = "Mechanical Keyboard Click Sound",
                                checked = cosmeticKeyboardSound,
                                icon = Icons.Rounded.VolumeUp
                            ) {
                                cosmeticKeyboardSound = it
                                prefs.edit().putBoolean("cosmetic_keyboard_sound", it).apply()
                                addLog("Keyboard key click sound ${if (it) "enabled" else "disabled"}")
                            }
                        }
                        
                        SettingsSection.SECURITY -> SectionCard("Security & Backups") {
                            Text(
                                text = "// local authentication",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ToggleRow(
                                label = "Enforce Biometric Lock",
                                checked = biometricEnabled,
                                icon = Icons.Rounded.Fingerprint
                            ) {
                                if (it && !BiometricHelper.isBiometricAvailable(context)) {
                                    Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_SHORT).show()
                                } else {
                                    biometricEnabled = it
                                    prefs.edit().putBoolean("biometric_lock_enabled", it).apply()
                                    addLog("Biometric lock ${if (it) "enabled" else "disabled"}")
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// compromised environment",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Controls access to the GitHub token if root, instrumentation, a debugger, or an emulator is detected.",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                listOf(
                                    SecurityGate.Policy.BLOCK_SENSITIVE to "Block",
                                    SecurityGate.Policy.WARN_ONLY to "Warn",
                                    SecurityGate.Policy.WIPE to "Wipe",
                                ).forEach { (policy, label) ->
                                    val selected = securityEnvironmentPolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(if (selected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                            .border(1.dp, if (selected) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                            .clickable {
                                                securityEnvironmentPolicy = policy
                                                SecurityGate.setPolicy(context, policy)
                                                addLog("Compromised environment policy: $label")
                                            }
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(label, fontSize = 11.sp, color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// auto-lock timeout",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Inactivity Lock:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0 to "Immediate", 1 to "1m", 5 to "5m", 15 to "15m").forEach { (mins, name) ->
                                        val isSelected = securityAutolockTimeout == mins
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(GitHubControlRadius))
                                                .background(if (isSelected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                                .border(1.dp, if (isSelected) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                .clickable {
                                                    securityAutolockTimeout = mins
                                                    prefs.edit().putInt("security_autolock_timeout", mins).apply()
                                                    addLog("Auto-lock set to $name")
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(name, fontSize = 11.sp, color = if (isSelected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            CompactField(
                                label = "Security PIN code (fallback, 4-6 digits)",
                                value = securityPinInput,
                                password = true
                            ) {
                                val digitsOnly = it.filter { ch -> ch.isDigit() }.take(6)
                                securityPinInput = digitsOnly
                                if (digitsOnly.isBlank()) {
                                    PinSecurity.clearPin(context)
                                } else if (digitsOnly.length in 4..6) {
                                    PinSecurity.setPin(context, digitsOnly)
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// secure account backup (aes-256)",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ActionRow(Icons.Rounded.Backup, "Export backup to downloads") {
                                showExportPasswordDialog = true
                            }
                            ActionRow(Icons.Rounded.Restore, "Import backup from downloads") {
                                if (BackupManager.getBackupFile(context).exists()) {
                                    showImportPasswordDialog = true
                                } else {
                                    Toast.makeText(context, "Backup file not found in Downloads folder.", Toast.LENGTH_LONG).show()
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// storage & cache limits",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Current Cache Size:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Text(ghFmtSize(currentCacheSize), fontSize = 13.sp, color = AiModuleTheme.colors.accent, fontFamily = JetBrainsMono)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Limit:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(50, 100, 250, 500).forEach { limit ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(GitHubControlRadius))
                                                .background(if (cacheLimitMb == limit) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                                .border(1.dp, if (cacheLimitMb == limit) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                .clickable {
                                                    cacheLimitMb = limit
                                                    prefs.edit().putInt("cache_limit_mb", limit).apply()
                                                    BackupManager.enforceCacheLimit(context, limit)
                                                    currentCacheSize = getCacheSize(context)
                                                    addLog("Cache limit set to ${limit}MB")
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("${limit}M", fontSize = 11.sp, color = if (cacheLimitMb == limit) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                            
                            ToggleRow(
                                label = "Auto Clean Old Logs (>7d)",
                                checked = autoCleanLogs,
                                icon = Icons.Rounded.DeleteSweep
                            ) {
                                autoCleanLogs = it
                                prefs.edit().putBoolean("auto_clean_logs", it).apply()
                                if (it) {
                                    BackupManager.autoCleanOldLogs(context)
                                    currentCacheSize = getCacheSize(context)
                                }
                                addLog("Auto clean logs ${if (it) "enabled" else "disabled"}")
                            }
                            
                            ActionRow(Icons.Rounded.Delete, "Clear all cache files", tint = AiModuleTheme.colors.error) {
                                confirmAction(
                                    title = "clear cache files",
                                    body = "Are you sure you want to clear all cache files? This will clear logs, syntax highlighter metadata and temporary files.",
                                    confirmLabel = "clear cache",
                                ) {
                                    clearCache(context)
                                    currentCacheSize = getCacheSize(context)
                                    addLog("Cleared all cache files")
                                    Toast.makeText(context, "Cache files cleared", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        SettingsSection.EDITOR -> SectionCard("Editor & Diff") {
                            Text(
                                text = "// code editor layout",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ToggleRow(
                                label = "Word Wrap",
                                checked = editorWordWrap,
                                icon = Icons.Rounded.Edit
                            ) {
                                editorWordWrap = it
                                prefs.edit().putBoolean("editor_word_wrap", it).apply()
                                addLog("Word wrap ${if (it) "enabled" else "disabled"}")
                            }
                            ToggleRow(
                                label = "Ignore Whitespace in Diffs",
                                checked = editorIgnoreWhitespace,
                                icon = Icons.Rounded.Check
                            ) {
                                editorIgnoreWhitespace = it
                                prefs.edit().putBoolean("editor_ignore_whitespace", it).apply()
                                addLog("Ignore whitespace in diffs ${if (it) "enabled" else "disabled"}")
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// font size scaling",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Font Size:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(10, 12, 13, 14, 16, 18).forEach { size ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(GitHubControlRadius))
                                                .background(if (editorFontSize == size) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                                .border(1.dp, if (editorFontSize == size) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                .clickable {
                                                    editorFontSize = size
                                                    prefs.edit().putInt("editor_font_size", size).apply()
                                                    addLog("Editor font size set to ${size}sp")
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("${size}px", fontSize = 11.sp, color = if (editorFontSize == size) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// indentation style",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Tab Indent:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("2 spaces", "4 spaces", "8 spaces", "tabs").forEach { style ->
                                        val isSelected = when (style) {
                                            "2 spaces" -> editorTabSize == 2 && !editorUseTabs
                                            "4 spaces" -> editorTabSize == 4 && !editorUseTabs
                                            "8 spaces" -> editorTabSize == 8 && !editorUseTabs
                                            "tabs" -> editorUseTabs
                                            else -> false
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(GitHubControlRadius))
                                                .background(if (isSelected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                                .border(1.dp, if (isSelected) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                .clickable {
                                                    when (style) {
                                                        "2 spaces" -> { editorTabSize = 2; editorUseTabs = false }
                                                        "4 spaces" -> { editorTabSize = 4; editorUseTabs = false }
                                                        "8 spaces" -> { editorTabSize = 8; editorUseTabs = false }
                                                        "tabs" -> { editorUseTabs = true }
                                                    }
                                                    prefs.edit().putInt("editor_tab_size", editorTabSize).putBoolean("editor_use_tabs", editorUseTabs).apply()
                                                    addLog("Tab style set to $style")
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(style, fontSize = 11.sp, color = if (isSelected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                        }
                        SettingsSection.AI_HELPER -> SectionCard("GitHub Copilot Settings") {
                            Text(
                                text = "// active model",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("gpt-5.5", "gpt-5.4", "gpt-5-mini", "claude-sonnet-4.6", "claude-opus-4.7", "claude-fable-5", "claude-haiku-4.5", "claude-3-5-haiku", "gpt-4o-mini", "mai-code-1", "mai-code-1-flash").forEach { modelName ->
                                    val isSelected = copilotModel == modelName
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(if (isSelected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                            .border(1.dp, if (isSelected) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                            .clickable {
                                                copilotModel = modelName
                                                prefs.edit().putString("copilot_model", modelName).apply()
                                                addLog("Copilot model set to $modelName")
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(modelName, fontSize = 11.sp, color = if (isSelected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "// plan routing endpoint",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Auto", "Individual", "Business", "Enterprise").forEach { routeName ->
                                    val isSelected = copilotRouting == routeName
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(GitHubControlRadius))
                                            .background(if (isSelected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                            .border(1.dp, if (isSelected) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                            .clickable {
                                                copilotRouting = routeName
                                                prefs.edit().putString("copilot_routing", routeName).apply()
                                                addLog("Copilot routing set to $routeName")
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(routeName, fontSize = 11.sp, color = if (isSelected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            var testingState by remember { mutableStateOf<String?>(null) }
                            val testScope = rememberCoroutineScope()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AiModuleTextAction(
                                    label = "Test Copilot Connection",
                                    onClick = {
                                        testingState = "Fetching Copilot Token..."
                                        testScope.launch {
                                            try {
                                                val token = GitHubManager.getCopilotToken(context)
                                                if (token.isNotBlank()) {
                                                    testingState = "Token Ok. Testing Chat endpoint..."
                                                    // Make a simple ping request
                                                    val routeHost = when (copilotRouting) {
                                                        "Individual" -> "api.individual.githubcopilot.com"
                                                        "Business" -> "api.business.githubcopilot.com"
                                                        "Enterprise" -> "api.enterprise.githubcopilot.com"
                                                        else -> "api.githubcopilot.com"
                                                    }
                                                    val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        val url = URL("https://$routeHost/chat/completions")
                                                        val conn = url.openConnection() as HttpURLConnection
                                                        try {
                                                            conn.requestMethod = "POST"
                                                            conn.connectTimeout = 15_000
                                                            conn.readTimeout = 15_000
                                                            conn.setRequestProperty("Authorization", "Bearer $token")
                                                            conn.setRequestProperty("Content-Type", "application/json")
                                                            conn.setRequestProperty("User-Agent", "GitHubCopilotChat/0.11.0")
                                                            conn.doOutput = true
                                                            val requestBody = JSONObject().apply {
                                                                put("model", copilotModel)
                                                                put("messages", JSONArray().apply {
                                                                    put(JSONObject().apply {
                                                                        put("role", "user")
                                                                        put("content", "ping")
                                                                    })
                                                                })
                                                                put("max_tokens", 5)
                                                            }.toString()
                                                            conn.outputStream.use { it.write(requestBody.toByteArray()) }
                                                            val responseCode = conn.responseCode
                                                            (if (responseCode in 200..299) conn.inputStream else conn.errorStream)
                                                                ?.close()
                                                            responseCode in 200..299
                                                        } finally {
                                                            conn.disconnect()
                                                        }
                                                    }
                                                    testingState = if (success) "Connection successful! Copilot is active." else "Chat API request failed."
                                                } else {
                                                    testingState = "Token request returned empty."
                                                }
                                            } catch (e: Exception) {
                                                testingState = "Error: ${e.message ?: e.javaClass.simpleName}"
                                            }
                                        }
                                    }
                                )
                                testingState?.let {
                                    Spacer(Modifier.width(12.dp))
                                    Text(it, fontSize = 11.sp, color = if (it.contains("successful")) AiModuleTheme.colors.accent else AiModuleTheme.colors.error, fontFamily = JetBrainsMono)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            CompactField("System Instructions", aiSystemPrompt, singleLine = false, minLines = 3) {
                                aiSystemPrompt = it
                                prefs.edit().putString("ai_system_prompt", it).apply()
                            }
                        }
                        SettingsSection.NETWORK -> SectionCard("Network & Proxy") {
                            Text(
                                text = "// proxy options",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ToggleRow(
                                label = "Enable HTTP Proxy",
                                checked = networkProxyEnabled,
                                icon = Icons.Rounded.Dns
                            ) {
                                networkProxyEnabled = it
                                prefs.edit().putBoolean("network_proxy_enabled", it).apply()
                                addLog("HTTP Proxy ${if (it) "enabled" else "disabled"}")
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            CompactField("Proxy Host / IP", networkProxyHost) {
                                networkProxyHost = it
                                prefs.edit().putString("network_proxy_host", it).apply()
                            }
                            
                            val portStr = if (networkProxyPort == 0) "" else networkProxyPort.toString()
                            CompactField("Proxy Port", portStr) {
                                val port = it.toIntOrNull() ?: 8080
                                networkProxyPort = port
                                prefs.edit().putInt("network_proxy_port", port).apply()
                            }
                            
                        }
                        SettingsSection.SYNC -> SectionCard("Background Sync") {
                            Text(
                                text = "// background service",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            ToggleRow(
                                label = "Background Notification Sync",
                                checked = syncBackgroundEnabled,
                                icon = Icons.Rounded.Sync
                            ) {
                                syncBackgroundEnabled = it
                                prefs.edit().putBoolean("sync_background_enabled", it).apply()
                                if (it) {
                                    NotificationSyncWorker.schedule(context, syncIntervalMins)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    NotificationSyncWorker.cancel(context)
                                }
                                addLog("Background sync ${if (it) "enabled" else "disabled"}")
                            }
                            
                            ToggleRow(
                                label = "Only Sync on Wi-Fi",
                                checked = syncWifiOnly,
                                icon = Icons.Rounded.Sync
                            ) {
                                syncWifiOnly = it
                                prefs.edit().putBoolean("sync_wifi_only", it).apply()
                                if (syncBackgroundEnabled) {
                                    NotificationSyncWorker.schedule(context, syncIntervalMins)
                                }
                                addLog("Sync Wi-Fi only limit ${if (it) "enabled" else "disabled"}")
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "// polling check interval",
                                color = AiModuleTheme.colors.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Interval:", fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(15, 30, 60, 120).forEach { mins ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(GitHubControlRadius))
                                                .background(if (syncIntervalMins == mins) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.border)
                                                .border(1.dp, if (syncIntervalMins == mins) AiModuleTheme.colors.accent else Color.Transparent, RoundedCornerShape(GitHubControlRadius))
                                                .clickable {
                                                    syncIntervalMins = mins
                                                    prefs.edit().putInt("sync_interval_mins", mins).apply()
                                                    if (syncBackgroundEnabled) {
                                                        NotificationSyncWorker.schedule(context, mins)
                                                    }
                                                    addLog("Sync interval set to ${mins}m")
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("${mins}m", fontSize = 11.sp, color = if (syncIntervalMins == mins) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                                        }
                                    }
                                }
                            }
                        }
                    
                        null -> Unit
                    }
                }

                when (currentSection) {
                    SettingsSection.EMAILS -> items(visibleEmails) { email ->
                        CompactCard {
                            EmailRow(email) {
                                confirmAction(
                                    title = "delete email",
                                    body = "Delete ${email.email} from your GitHub account?",
                                    confirmLabel = "delete",
                                ) {
                                    scope.launch {
                                        val ok = GitHubManager.deleteEmailAddress(context, email.email)
                                        addLog("Delete email ${email.email}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.EMAILS)
                                    }
                                }
                            }
                        }
                    }
                    SettingsSection.NOTIFICATIONS -> items(visibleNotifications) { item ->
                        CompactCard {
                            NotificationRow(item) {
                                scope.launch {
                                    val ok = GitHubManager.markNotificationRead(context, item.id)
                                    addLog("Mark thread ${item.id}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.NOTIFICATIONS)
                                }
                            }
                        }
                    }
                    SettingsSection.KEYS -> {
                        items(visibleKeys) { key ->
                            CompactCard {
                                KeyRow(key) {
                                    confirmAction(
                                        title = "delete key",
                                        body = "Delete ${key.kind} key ${key.title.ifBlank { key.id.toString() }}?",
                                        confirmLabel = "delete",
                                    ) {
                                        scope.launch {
                                            val ok = when (keyMode) {
                                                KeyMode.SSH -> GitHubManager.deleteSshKeyNative(context, key.id)
                                                KeyMode.SSH_SIGNING -> GitHubManager.deleteSshSigningKeyNative(context, key.id)
                                                KeyMode.GPG -> GitHubManager.deleteGpgKeyNative(context, key.id)
                                            }
                                            addLog("Delete key ${key.id}: $ok")
                                            Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                            refreshSection(SettingsSection.KEYS)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SettingsSection.SOCIAL -> items(visibleSocialAccounts) { acc ->
                        CompactCard {
                            SocialRow(acc) {
                                confirmAction(
                                    title = "delete social account",
                                    body = "Delete ${acc.url} from your GitHub social accounts?",
                                    confirmLabel = "delete",
                                ) {
                                    scope.launch {
                                        val ok = GitHubManager.deleteSocialAccountNative(context, acc.url)
                                        addLog("Delete social ${acc.url}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.SOCIAL)
                                    }
                                }
                            }
                        }
                    }
                    SettingsSection.BLOCKED -> items(visibleBlockedUsers) { blocked ->
                        CompactCard {
                            BlockedRow(blocked) {
                                confirmAction(
                                    title = "unblock user",
                                    body = "Unblock @${blocked.login}?",
                                    confirmLabel = "unblock",
                                    danger = false,
                                ) {
                                    scope.launch {
                                        val ok = GitHubManager.unblockUserNative(context, blocked.login)
                                        addLog("Unblock ${blocked.login}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.BLOCKED)
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    if (showExportPasswordDialog) {
        AiModuleAlertDialog(
            onDismissRequest = { showExportPasswordDialog = false; exportPassword = "" },
            title = "export backup",
            content = {
                CompactField("Backup encryption password", exportPassword, password = true) { exportPassword = it }
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "export",
                    enabled = exportPassword.isNotBlank(),
                    onClick = {
                        try {
                            val file = BackupManager.createBackup(context, exportPassword.toCharArray())
                            addLog("Exported backup to ${file.name}")
                            Toast.makeText(context, "Backup exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        showExportPasswordDialog = false
                        exportPassword = ""
                        currentCacheSize = getCacheSize(context)
                    }
                )
            },
            dismissButton = {
                AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showExportPasswordDialog = false; exportPassword = "" }, tint = AiModuleTheme.colors.textSecondary)
            }
        )
    }

    if (showImportPasswordDialog) {
        AiModuleAlertDialog(
            onDismissRequest = { showImportPasswordDialog = false; importPassword = "" },
            title = "import backup",
            content = {
                CompactField("Backup decryption password", importPassword, password = true) { importPassword = it }
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "import",
                    enabled = importPassword.isNotBlank(),
                    onClick = {
                        try {
                            val ok = BackupManager.restoreBackup(context, importPassword.toCharArray())
                            if (ok) {
                                addLog("Imported backup successfully")
                                Toast.makeText(context, "Backup restored. Please restart app to apply all changes.", Toast.LENGTH_LONG).show()
                                user = GitHubManager.getCachedUser(context)
                                biometricEnabled = prefs.getBoolean("biometric_lock_enabled", false)
                                cacheLimitMb = prefs.getInt("cache_limit_mb", 100)
                                autoCleanLogs = prefs.getBoolean("auto_clean_logs", true)
                            } else {
                                Toast.makeText(context, "Restore failed. Backup file missing or invalid.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Decrypt failed. Invalid password?", Toast.LENGTH_SHORT).show()
                        }
                        showImportPasswordDialog = false
                        importPassword = ""
                        currentCacheSize = getCacheSize(context)
                    }
                )
            },
            dismissButton = {
                AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showImportPasswordDialog = false; importPassword = "" }, tint = AiModuleTheme.colors.textSecondary)
            }
        )
    }

    if (showChangeToken) {
        AiModuleAlertDialog(
            onDismissRequest = { showChangeToken = false },
            title = "change token",
            content = { CompactField("Personal access token", newToken, password = true) { newToken = it } },
            confirmButton = {
                AiModuleTextAction(label = Strings.done.lowercase(), onClick = {
                    val candidate = newToken.trim()
                    if (candidate.isBlank()) return@AiModuleTextAction
                    scope.launch {
                        val previousToken = GitHubManager.getToken(context)
                        GitHubManager.saveToken(context, candidate)
                        val updatedUser = GitHubManager.getUser(context)
                        if (updatedUser != null) {
                            user = updatedUser
                            addLog("Token updated")
                            newToken = ""
                            showChangeToken = false
                            refreshSection(SettingsSection.DEVELOPER)
                        } else {
                            GitHubManager.saveToken(context, previousToken)
                            Toast.makeText(context, "Token validation failed; the previous token was kept", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            },
            dismissButton = {
                AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showChangeToken = false }, tint = AiModuleTheme.colors.textSecondary)
            },
        )
    }

    if (showChangeApiUrl) {
        AiModuleAlertDialog(
            onDismissRequest = { showChangeApiUrl = false },
            title = "custom api base url",
            content = {
                CompactField("API Base URL", newApiUrl, onValueChange = { newApiUrl = it })
            },
            confirmButton = {
                AiModuleTextAction(label = Strings.done.lowercase(), onClick = {
                    if (GitHubManager.setApiUrl(context, newApiUrl.trim())) {
                        addLog("API base URL updated")
                        showChangeApiUrl = false
                        scope.launch { refreshSection(SettingsSection.DEVELOPER) }
                    } else {
                        Toast.makeText(context, "Enter an HTTPS API URL without query parameters", Toast.LENGTH_LONG).show()
                    }
                })
            },
            dismissButton = {
                AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showChangeApiUrl = false }, tint = AiModuleTheme.colors.textSecondary)
            },
        )
    }

    if (showDeviceLogin) {
        var deviceClientId by remember { mutableStateOf("") }
        var deviceCode by remember { mutableStateOf<GHDeviceCode?>(null) }
        var devicePolling by remember { mutableStateOf(false) }
        var deviceError by remember { mutableStateOf<String?>(null) }

        AiModuleAlertDialog(
            onDismissRequest = { showDeviceLogin = false; deviceCode = null },
            title = "device login",
            confirmButton = {
                if (deviceCode == null) {
                    AiModuleTextAction(label = "start", enabled = deviceClientId.isNotBlank(), onClick = {
                        scope.launch {
                            deviceCode = GitHubManager.initiateDeviceFlow(deviceClientId.trim())
                            if (deviceCode == null) deviceError = "Failed to start device flow"
                        }
                    }, tint = AiModuleTheme.colors.accent)
                } else {
                    AiModuleTextAction(label = "poll", enabled = !devicePolling, onClick = {
                        devicePolling = true
                        scope.launch {
                            val result = GitHubManager.pollDeviceToken(deviceClientId.trim(), deviceCode!!.deviceCode)
                            devicePolling = false
                            if (result.token != null) {
                                val previousToken = GitHubManager.getToken(context)
                                GitHubManager.saveToken(context, result.token)
                                val updatedUser = GitHubManager.getUser(context)
                                if (updatedUser != null) {
                                    user = updatedUser
                                    showDeviceLogin = false
                                    deviceCode = null
                                    addLog("Logged in via device flow")
                                } else {
                                    GitHubManager.saveToken(context, previousToken)
                                    deviceError = "Device token validation failed; the previous token was kept"
                                }
                            } else {
                                deviceError = result.error ?: "pending"
                            }
                        }
                    }, tint = AiModuleTheme.colors.accent)
                }
            },
            dismissButton = {
                AiModuleTextAction(label = Strings.cancel.lowercase(), onClick = { showDeviceLogin = false; deviceCode = null }, tint = AiModuleTheme.colors.textSecondary)
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (deviceCode == null) {
                    Text(
                        text = "Use a Client ID from your own GitHub OAuth App with Device Flow enabled. The app never impersonates another GitHub client.",
                        fontSize = 11.sp,
                        color = AiModuleTheme.colors.textMuted,
                        lineHeight = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    CompactField("Client ID", deviceClientId) { deviceClientId = it }
                } else {
                    Text("Enter this code on GitHub:", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
                    Text(deviceCode!!.userCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AiModuleTheme.colors.accent, fontFamily = JetBrainsMono)
                    Text("Verification URL:", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
                    Text(deviceCode!!.verificationUri, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                }
                deviceError?.let {
                    Text(it, fontSize = 12.sp, color = AiModuleTheme.colors.error, fontFamily = JetBrainsMono)
                }
            }
        }
    }

    pendingConfirmation?.let { request ->
        AiModuleAlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = request.title,
            content = {
                Text(
                    request.body,
                    color = AiModuleTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMono,
                )
            },
            confirmButton = {
                AiModuleTextAction(
                    label = request.confirmLabel.lowercase(),
                    onClick = {
                        val action = request.onConfirm
                        pendingConfirmation = null
                        action()
                    },
                    tint = if (request.danger) AiModuleTheme.colors.error else AiModuleTheme.colors.accent,
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = Strings.cancel.lowercase(),
                    onClick = { pendingConfirmation = null },
                    tint = AiModuleTheme.colors.textSecondary,
                )
            },
        )
    }
}

@Composable
private fun HomeSettingsMenu(user: GHUser?, onOpen: (SettingsSection) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { HomeUserHeader(user) }
        item { TerminalSectionHeader("account") }
        item { MenuRow(Icons.Rounded.Person, SettingsSection.PROFILE, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Email, SettingsSection.EMAILS, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Notifications, SettingsSection.NOTIFICATIONS, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Key, SettingsSection.KEYS, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Public, SettingsSection.SOCIAL, onOpen); AiModuleHairline() }
        item { TerminalSectionHeader("people") }
        item { MenuRow(Icons.Rounded.Group, SettingsSection.PEOPLE, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Block, SettingsSection.BLOCKED, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Warning, SettingsSection.INTERACTION, onOpen); AiModuleHairline() }
        item { TerminalSectionHeader("workspace") }
        item { MenuRow(Icons.Rounded.Business, SettingsSection.ORGANIZATIONS, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Description, SettingsSection.REPOSITORIES, onOpen); AiModuleHairline() }
        item { TerminalSectionHeader("developer") }
        item { MenuRow(Icons.Rounded.Code, SettingsSection.DEVELOPER, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.SmartToy, SettingsSection.AI_HELPER, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Sync, SettingsSection.SYNC, onOpen); AiModuleHairline() }
        item { TerminalSectionHeader("customization") }
        item { MenuRow(Icons.Rounded.Edit, SettingsSection.EDITOR, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Palette, SettingsSection.THEMES, onOpen); AiModuleHairline() }
        item { TerminalSectionHeader("security") }
        item { MenuRow(Icons.Rounded.Security, SettingsSection.SECURITY, onOpen); AiModuleHairline() }
        item { MenuRow(Icons.Rounded.Dns, SettingsSection.NETWORK, onOpen); AiModuleHairline() }
    }
}

@Composable
private fun HomeUserHeader(user: GHUser?) {
    val colors = AiModuleTheme.colors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = user?.avatarUrl,
                contentDescription = user?.login,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceElevated)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    user?.name?.takeIf { it.isNotBlank() } ?: user?.login ?: "github",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "@${user?.login ?: "unknown"}",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMono
                )
            }
        }
        AiModuleHairline()
    }
}

@Composable
private fun TerminalSectionHeader(title: String) {
    Text(
        "// $title",
        color = AiModuleTheme.colors.textMuted,
        fontSize = 11.sp,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 4.dp).padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun CompactCard(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        content()
        Spacer(Modifier.height(8.dp))
        AiModuleHairline()
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "// ${title.lowercase()}",
            color = AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = AiModuleTheme.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingsCountLine(text: String) {
    Text(
        text,
        color = AiModuleTheme.colors.textMuted,
        fontSize = 11.sp,
        fontFamily = JetBrainsMono,
        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsEmptyLine(text: String) {
    Text(
        text,
        color = AiModuleTheme.colors.textMuted,
        fontSize = 12.sp,
        fontFamily = JetBrainsMono,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
    )
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, section: SettingsSection, onOpen: (SettingsSection) -> Unit) {
    val colors = AiModuleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(section) }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(colors.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                section.title.lowercase(),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
            )
            Text(
                section.subtitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = JetBrainsMono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = colors.textSecondary)
    }
}

@Composable
private fun MenuDivider() {
    AiModuleHairline()
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    password: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "> ${label.lowercase()}",
            color = AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
        )
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = AiModuleTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AiModuleTheme.colors.accent),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        AiModuleHairline()
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: Color = AiModuleTheme.colors.accent, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            "> ${title.lowercase()}",
            color = tint,
            fontSize = 13.sp,
            fontFamily = JetBrainsMono,
        )
    }
}

@Composable
private fun VisibilityChooser(current: String, onSet: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VisibilityChip("private", current == "private") { onSet("private") }
        VisibilityChip("public", current == "public") { onSet("public") }
    }
}

@Composable
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(
            if (selected) "[${label.lowercase()}]" else " ${label.lowercase()} ",
            color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted,
            fontSize = 12.sp,
            fontFamily = JetBrainsMono,
        )
    }
}

@Composable
private fun PublicEmailChooser(current: String, emails: List<GHEmailEntry>, onSet: (String) -> Unit) {
    if (emails.isEmpty()) {
        Text(
            "No email list returned. The token may not have user:email scope.",
            color = AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
        )
        return
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileChoiceChip("private/no public email", current.isBlank()) { onSet("") }
        emails.filter { it.verified }.distinctBy { it.email }.forEach { email ->
            ProfileChoiceChip(
                label = buildString {
                    append(email.email)
                    if (email.primary) append(" primary")
                },
                selected = current.equals(email.email, ignoreCase = true),
            ) { onSet(email.email) }
        }
    }
}

@Composable
private fun HireableChooser(current: Boolean, onSet: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("> hireable", color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChoiceChip("available", current) { onSet(true) }
            ProfileChoiceChip("not hireable", !current) { onSet(false) }
        }
    }
}

@Composable
private fun ProfileChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.surfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(
            if (selected) "[$label]" else label,
            color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = JetBrainsMono,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileAccountSummary(profile: GHUserProfile?) {
    if (profile == null) return
    Spacer(Modifier.height(8.dp))
    SectionHeader("Account metadata")
    InfoLine("Login", "@${profile.login}")
    InfoLine(
        "Public stats",
        "repos ${formatGitHubNumber(profile.publicRepos)} • gists ${formatGitHubNumber(profile.publicGists)} • followers ${formatGitHubNumber(profile.followers)} • following ${formatGitHubNumber(profile.following)}",
    )
    if (profile.privateRepos > 0 || profile.privateGists > 0 || profile.collaborators > 0 || profile.diskUsageKb > 0L) {
        InfoLine(
            "Private account stats",
            "repos ${formatGitHubNumber(profile.privateRepos)} • gists ${formatGitHubNumber(profile.privateGists)} • collaborators ${formatGitHubNumber(profile.collaborators)} • disk ${formatGitHubKilobytes(profile.diskUsageKb)}",
        )
    }
    InfoLine("2FA", profile.twoFactorAuthentication?.let { if (it) "enabled" else "disabled" } ?: "not returned")
    if (profile.planName.isNotBlank()) {
        InfoLine("Plan", "${profile.planName}${profile.planSpace.takeIf { it > 0L }?.let { " • ${formatGitHubKilobytes(it)}" }.orEmpty()}")
    }
    if (profile.createdAt.isNotBlank()) InfoLine("Joined", profile.createdAt.take(10))
    if (profile.updatedAt.isNotBlank()) InfoLine("Updated", profile.updatedAt.take(10))
}

@Composable
private fun KeyModeRow(mode: KeyMode, onSet: (KeyMode) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VisibilityChip("SSH", mode == KeyMode.SSH) { onSet(KeyMode.SSH) }
        VisibilityChip("SSH signing", mode == KeyMode.SSH_SIGNING) { onSet(KeyMode.SSH_SIGNING) }
        VisibilityChip("GPG", mode == KeyMode.GPG) { onSet(KeyMode.GPG) }
    }
}

@Composable
private fun EmailRow(email: GHEmailEntry, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Email, null, tint = AiModuleTheme.colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(email.email, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val tags = buildList {
                if (email.primary) add("primary")
                if (email.verified) add("verified")
                add(email.visibility.ifBlank { "private" })
            }.joinToString(" • ")
            Text(tags, color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        }
        AiModuleTextAction(label = "delete", onClick = onDelete, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun NotificationRow(item: GHNotification, onMarkRead: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Notifications, null, tint = if (item.unread) AiModuleTheme.colors.accent else AiModuleTheme.colors.textMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.repoName} • ${item.reason}", color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (item.unread) AiModuleTextAction(label = "read", onClick = onMarkRead)
    }
}

@Composable
private fun KeyRow(key: GHUserKeyEntry, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Key, null, tint = AiModuleTheme.colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(key.title.ifBlank { "Key ${key.id}" }, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${key.kind} • ${key.createdAt.take(10)}", color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        }
        AiModuleTextAction(label = "delete", onClick = onDelete, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun SocialRow(acc: GHSocialAccountEntry, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Public, null, tint = AiModuleTheme.colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(acc.provider.ifBlank { "Social account" }, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono)
            Text(acc.url, color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AiModuleTextAction(label = "delete", onClick = onDelete, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun CompactPersonRow(login: String, avatarUrl: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = avatarUrl, contentDescription = login, modifier = Modifier.size(24.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(login, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, modifier = Modifier.weight(1f))
        AiModuleTextAction(label = action.lowercase(), onClick = onAction)
    }
}

@Composable
private fun BlockedRow(entry: GHBlockedEntry, onUnblock: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = entry.avatarUrl, contentDescription = entry.login, modifier = Modifier.size(24.dp).clip(CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(entry.login, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, modifier = Modifier.weight(1f))
        AiModuleTextAction(label = "unblock", onClick = onUnblock, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun CompactOrgRow(org: GHOrg) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var membership by remember { mutableStateOf<GHOrgMembership?>(null) }
    var hooks by remember { mutableStateOf<List<GHWebhook>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().ghGlassCard(10.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = org.avatarUrl, contentDescription = org.login, modifier = Modifier.size(24.dp).clip(CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(org.login, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono)
                if (org.description.isNotBlank()) Text(org.description, color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (expanded) "▼" else "▶", color = AiModuleTheme.colors.textMuted, fontSize = 12.sp, fontFamily = JetBrainsMono)
        }
        if (expanded) {
            LaunchedEffect(org.login) {
                membership = GitHubManager.getOrgMembership(context, org.login)
                hooks = GitHubManager.getOrgHooks(context, org.login)
            }
            if (membership != null) {
                AiModuleKeyValueRow("role", membership!!.role)
                AiModuleKeyValueRow("state", membership!!.state)
            }
            if (hooks.isNotEmpty()) {
                Text("Webhooks (${hooks.size})", fontSize = 12.sp, color = AiModuleTheme.colors.textSecondary, fontFamily = JetBrainsMono)
                hooks.take(5).forEach { hook: GHWebhook ->
                    Text("· ${hook.url.take(50)}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, fontFamily = JetBrainsMono)
                }
            }
        }
    }
}

@Composable
private fun UserRepositoryInvitationRow(
    invitation: GHUserRepositoryInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = invitation.inviterAvatarUrl,
            contentDescription = invitation.inviter,
            modifier = Modifier.size(24.dp).clip(CircleShape).background(AiModuleTheme.colors.surfaceElevated),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                invitation.repoFullName.ifBlank { "repository invitation ${invitation.id}" },
                color = AiModuleTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = JetBrainsMono,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                if (invitation.inviter.isNotBlank()) add("from ${invitation.inviter}")
                if (invitation.permissions.isNotBlank()) add(invitation.permissions)
                if (invitation.expired) add("expired")
                if (invitation.createdAt.isNotBlank()) add(invitation.createdAt.take(10))
            }.joinToString(" • ")
            if (meta.isNotBlank()) {
                Text(meta, color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AiModuleTextAction(label = "accept", onClick = onAccept)
        Spacer(Modifier.width(8.dp))
        AiModuleTextAction(label = "decline", onClick = onDecline, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun CompactRepoRow(repo: GHRepo, action: String = "unstar", onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Description, null, tint = AiModuleTheme.colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(repo.fullName, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(repo.language.takeIf { it.isNotBlank() }, repo.updatedAt.takeIf { it.isNotBlank() }?.take(10)).joinToString(" • ")
            if (sub.isNotBlank()) Text(sub, color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        }
        AiModuleTextAction(label = action, onClick = onAction, tint = AiModuleTheme.colors.error)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("> ${label.lowercase()}", color = AiModuleTheme.colors.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        Text(value, color = AiModuleTheme.colors.textPrimary, fontSize = 13.sp, fontFamily = JetBrainsMono)
    }
}

private fun maskToken(token: String): String {
    if (token.isBlank()) return "Not set"
    return if (token.length <= 8) "••••••••" else token.take(4) + "••••••••" + token.takeLast(4)
}

private fun formatGitHubKilobytes(kb: Long): String = when {
    kb <= 0L -> "0 KB"
    kb < 1024L -> "$kb KB"
    kb < 1024L * 1024L -> "${kb / 1024L} MB"
    else -> "${kb / (1024L * 1024L)} GB"
}

private fun matchesSettingsQuery(query: String, vararg values: String): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true
    return values.any { it.contains(q, ignoreCase = true) }
}

private fun getCacheSize(context: Context): Long {
    var size = 0L
    try {
        context.cacheDir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
    } catch (_: Exception) {}
    return size
}

private fun clearCache(context: Context) {
    try {
        context.cacheDir.deleteRecursively()
    } catch (_: Exception) {}
}

@Composable
private fun TerminalToggleIndicator(
    checked: Boolean,
    tint: Color = AiModuleTheme.colors.accent,
) {
    val color = if (checked) tint else AiModuleTheme.colors.textMuted
    Text(
        text = if (checked) "[on]" else "[off]",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
