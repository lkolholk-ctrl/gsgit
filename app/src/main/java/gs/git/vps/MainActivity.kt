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

class MainActivity : FragmentActivity() {
    var deepLinkTarget by mutableStateOf<GitHubNotificationTarget?>(null)
    var isAppLocked by mutableStateOf(false)
    private var lastPauseTime = 0L

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
            if (!PinSecurity.isPinSet(this)) {
                isAppLocked = false
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

        if (isLockEnabled()) {
            isAppLocked = true
            triggerBiometricUnlock()
        }

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
                if (isAppLocked) {
                    AppLockedScreen(
                        onUnlock = { triggerBiometricUnlock() },
                        onPinCorrect = { isAppLocked = false }
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
}

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
