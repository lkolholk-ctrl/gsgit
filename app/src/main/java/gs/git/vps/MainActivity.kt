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
import gs.git.vps.security.PinSecurity
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.screens.CrashActivity
import gs.git.vps.ui.screens.GitHubScreen
import gs.git.vps.ui.screens.GitHubTerminalButton
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.workers.NotificationSyncWorker
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

class MainActivity : FragmentActivity() {
    var deepLinkTarget by mutableStateOf<GitHubNotificationTarget?>(null)
    var openGitHubAppsRequested by mutableStateOf(false)
    var isAppLocked by mutableStateOf(false)
    private var hasUnlockedSession by mutableStateOf(false)
    private var isBiometricPromptActive = false
    private var lastPauseTime = 0L

    // Android 13+: без рантайм-разрешения POST_NOTIFICATIONS система молча
    // глотает и пуши, и уведомления фонового воркера. Спрашиваем при входе;
    // после выдачи сразу перерегистрируемся на пуш-бэкенде.
    private val notificationsPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) gs.git.vps.notifications.GsGitPush.registerAsync(this)
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationsPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun completeUnlock() {
        hasUnlockedSession = true
        isAppLocked = false
    }

    private fun isLockEnabled(): Boolean {
        val prefs = getSharedPreferences("github_prefs", MODE_PRIVATE)
        val biometric = prefs.getBoolean("biometric_lock_enabled", false)
        return biometric || PinSecurity.isPinSet(this)
    }

    private fun getAutolockTimeoutMs(): Long {
        val prefs = getSharedPreferences("github_prefs", MODE_PRIVATE)
        val minutes = prefs.getInt("security_autolock_timeout", 0)
        return minutes * 60 * 1000L
    }

    private fun triggerBiometricUnlock() {
        if (isBiometricPromptActive) return
        if (BiometricHelper.isBiometricAvailable(this)) {
            isBiometricPromptActive = true
            BiometricHelper.showBiometricPrompt(
                activity = this,
                onSuccess = {
                    isBiometricPromptActive = false
                    completeUnlock()
                },
                onError = { err ->
                    isBiometricPromptActive = false
                    Toast.makeText(this, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            if (!PinSecurity.isPinSet(this)) {
                completeUnlock()
            }
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

        PinSecurity.migrateLegacyPin(this)

        val prefs = getSharedPreferences("github_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("sync_background_enabled", false)) {
            val interval = prefs.getInt("sync_interval_mins", 30)
            NotificationSyncWorker.schedule(this, interval)
        }

        val lockEnabled = isLockEnabled()
        hasUnlockedSession = !lockEnabled
        if (lockEnabled) {
            isAppLocked = true
            triggerBiometricUnlock()
        }

        requestNotificationsPermissionIfNeeded()

        handleDeepLink(intent)

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }
            var crtEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("cosmetic_crt_effect", false)) }

            DisposableEffect(sharedPrefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "cosmetic_crt_effect") {
                        crtEnabled = sharedPrefs.getBoolean("cosmetic_crt_effect", false)
                    }
                }
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            AiModuleSurface(
                modifier = Modifier.fillMaxSize().crtEffect(crtEnabled)
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Compose the application once after the first successful unlock and
                    // keep that composition alive under subsequent lock overlays. The old
                    // conditional removed GitHubScreen from the tree on every onStop(),
                    // which discarded the open repository, tab and nested navigation state.
                    if (hasUnlockedSession) {
                        GitHubScreen(
                            onBack = {},
                            initialTarget = deepLinkTarget,
                            onInitialTargetConsumed = { deepLinkTarget = null },
                            initialOpenApps = openGitHubAppsRequested,
                            onInitialOpenAppsConsumed = { openGitHubAppsRequested = false },
                        )
                    }
                    if (isAppLocked || !hasUnlockedSession) {
                        AppLockedScreen(
                            onUnlock = { triggerBiometricUnlock() },
                            onPinCorrect = { completeUnlock() }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastPauseTime = System.currentTimeMillis()
        if (isLockEnabled() && getAutolockTimeoutMs() == 0L) {
            isAppLocked = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (isLockEnabled()) {
            val timeout = getAutolockTimeoutMs()
            if (timeout > 0L) {
                val elapsed = System.currentTimeMillis() - lastPauseTime
                if (elapsed > timeout) {
                    isAppLocked = true
                }
            } else {
                isAppLocked = true
            }
            if (isAppLocked) {
                triggerBiometricUnlock()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        // This route only navigates to the integrations screen. Query parameters
        // are deliberately ignored because custom-scheme callbacks can be forged.
        if (uri.scheme == "gsgit" && uri.host == "integrations" &&
            uri.pathSegments.firstOrNull() == "github-app"
        ) {
            openGitHubAppsRequested = true
            return
        }
        // github.com и www.github.com — оба host заявлены в intent-filter; путь дальше
        // парсится одинаково (pathSegments от host не зависит).
        if (uri.host != "github.com" && uri.host != "www.github.com") return
        val segments = uri.pathSegments

        // Фильтр ловит ВЕСЬ github.com. Всё, что приложение показать не умеет —
        // корень/профиль, системные страницы, /actions, артефакты, /commit,
        // скачивание релизов и т.п. — отдаём браузеру, а не проглатываем (иначе
        // ссылка валится на корень репо/домашний экран, а скачивание не стартует).
        val first = segments.getOrNull(0)?.lowercase()
        if (segments.size < 2 || first == null || first in RESERVED_ROOT_SEGMENTS) {
            openInBrowser(uri)
            return
        }
        val section = segments.getOrNull(2)?.lowercase()
        // Скачивание ассета релиза (releases/download/...) — это файл, не страница
        // релизов: в приложении не откроется, отдаём браузеру.
        val isReleaseDownload = section == "releases" && segments.getOrNull(3)?.lowercase() == "download"
        if (isReleaseDownload || (section != null && section in UNSUPPORTED_REPO_SECTIONS)) {
            openInBrowser(uri)
            return
        }

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
                number = null
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

    /**
     * Открывает ссылку во ВНЕШНЕМ браузере, явно исключая себя (иначе Android по
     * тому же intent-filter вернёт интент нам же и получится цикл/пустой экран).
     */
    private fun openInBrowser(uri: android.net.Uri) {
        val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val browserPkg = try {
            packageManager
                .queryIntentActivities(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.example.com")),
                    0
                )
                .map { it.activityInfo.packageName }
                .firstOrNull { it != packageName }
        } catch (e: Exception) {
            null
        }
        if (browserPkg != null) view.setPackage(browserPkg)
        runCatching { startActivity(view) }
    }
}

private val RESERVED_ROOT_SEGMENTS = setOf(
    "settings", "login", "logout", "join", "signup", "sessions", "notifications",
    "marketplace", "sponsors", "about", "pricing", "features", "explore", "topics",
    "trending", "collections", "events", "new", "organizations", "orgs", "apps",
    "codespaces", "account", "dashboard", "search", "stars", "watching", "site",
    "contact", "security", "customer-stories", "enterprise", "readme", "pulls",
    "issues", "gist", "assets", "favicons"
)

private val UNSUPPORTED_REPO_SECTIONS = setOf(
    "actions", "commit", "commits", "compare", "wiki", "security", "pulse",
    "graphs", "network", "branches", "tags", "archive", "raw", "blame", "find",
    "deployments", "packages", "projects", "settings", "hooks", "milestones",
    "labels", "watchers", "stargazers", "forks", "community", "runs", "activity",
    "contributors"
)

@Composable
private fun AppLockedScreen(onUnlock: () -> Unit, onPinCorrect: () -> Unit) {
    val palette = AiModuleTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }
    val pinSet = remember { PinSecurity.isPinSet(context) }
    val pinMaxLength = 6

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            // Consume touches over the entire lock overlay so controls in the retained
            // application composition cannot be activated through empty lock-screen space.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
                    }
                }
            }
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

            if (pinSet) {
                Text(
                    text = "Enter security PIN code:",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMono
                )

                BasicTextField(
                    value = enteredPin,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= pinMaxLength) {
                            enteredPin = input
                            pinError = false
                            if (input.length >= 4) {
                                if (PinSecurity.verifyPin(context, input)) {
                                    onPinCorrect()
                                } else if (input.length == pinMaxLength) {
                                    pinError = true
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(
                        color = if (pinError) palette.error else palette.textPrimary,
                        fontSize = 20.sp,
                        fontFamily = JetBrainsMono,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    ),
                    modifier = Modifier
                        .width(140.dp)
                        .background(palette.surfaceElevated, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )

                if (pinError) {
                    Text(
                        text = "Invalid PIN code",
                        color = palette.error,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMono
                    )
                }
            } else {
                Text(
                    text = "Application access is locked.\nPlease authenticate to proceed.",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMono,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (BiometricHelper.isBiometricAvailable(context) && prefs.getBoolean("biometric_lock_enabled", false)) {
                GitHubTerminalButton(
                    label = "Biometric Unlock",
                    onClick = onUnlock,
                    color = palette.accent
                )
            }
        }
    }
}

private fun Modifier.crtEffect(enabled: Boolean): Modifier = if (!enabled) this else this.drawWithContent {
    drawContent()
    val scanlineSpacing = 6.dp.toPx()
    val lineCount = (size.height / scanlineSpacing).toInt()
    for (i in 0 until lineCount) {
        val y = i * scanlineSpacing
        drawLine(
            color = Color.Black.copy(alpha = 0.15f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx()
        )
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.minDimension * 0.9f
        )
    )
}
