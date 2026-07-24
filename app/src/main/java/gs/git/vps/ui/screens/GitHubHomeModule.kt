package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHDeviceCode
import gs.git.vps.data.github.model.QuickGlanceStats
import gs.git.vps.data.github.model.GHUser
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoCreateResult
import gs.git.vps.data.github.model.GHRepoEvent
import gs.git.vps.security.SecurityGate
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Compact mode — propagates through all sub-screens automatically

// Saver для pending device-кода: состояние device-flow должно пережить уход в браузер и
// убийство Activity/процесса (MIUI/HyperOS, «Don't keep activities»), иначе на возврате код
// теряется и поллинг не возобновляется — вход «сбрасывается, не дождавшись авторизации».
private val DeviceCodeSaver = listSaver<GHDeviceCode?, Any>(
    save = { c -> if (c == null) emptyList() else listOf(c.deviceCode, c.userCode, c.verificationUri, c.expiresIn, c.interval) },
    restore = { l -> if (l.isEmpty()) null else GHDeviceCode(l[0] as String, l[1] as String, l[2] as String, l[3] as Int, l[4] as Int) },
)

@Composable
internal fun LoginScreen(onBack: () -> Unit, onMinimize: () -> Unit, onClose: (() -> Unit)? = null, onLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var securityWarning by remember { mutableStateOf("") }
    // rememberSaveable — переживает пересоздание Activity/процесса при уходе в браузер,
    // LaunchedEffect(deviceCode?.deviceCode) на возврате возобновит поллинг по тому же коду.
    var deviceCode by rememberSaveable(stateSaver = DeviceCodeSaver) { mutableStateOf<GHDeviceCode?>(null) }
    var deviceCodeExpiresAt by rememberSaveable { mutableStateOf(0L) }
    var appAuthBusy by remember { mutableStateOf(false) }
    var appAuthError by remember { mutableStateOf("") }
    // Вход по токену (PAT) — опционально, только через явное согласие «на свой страх и риск».
    var patExpanded by remember { mutableStateOf(false) }
    var patAccepted by remember { mutableStateOf(false) }
    var patInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        securityWarning = withContext(Dispatchers.Default) {
            SecurityGate.environmentMessage(context)
        }
    }

    LaunchedEffect(deviceCode?.deviceCode) {
        val pendingCode = deviceCode ?: return@LaunchedEffect
        var intervalSeconds = pendingCode.interval.coerceAtLeast(5)
        while (System.currentTimeMillis() < deviceCodeExpiresAt) {
            delay(intervalSeconds * 1000L)
            val result = GitHubManager.pollGsGitAppDeviceToken(context, pendingCode.deviceCode)
            when {
                result.token?.isNotBlank() == true -> {
                    appAuthError = ""
                    deviceCode = null
                    onLogin()
                    return@LaunchedEffect
                }
                result.error == "authorization_pending" -> Unit
                result.error == "slow_down" -> intervalSeconds += 5
                result.error == "access_denied" -> {
                    appAuthError = "Authorization was cancelled on GitHub"
                    deviceCode = null
                    return@LaunchedEffect
                }
                result.error == "expired_token" -> {
                    appAuthError = "The device code expired. Try again."
                    deviceCode = null
                    return@LaunchedEffect
                }
                result.error == "secure_storage_unavailable" -> {
                    appAuthError = SecurityGate.blockedMessage(context)
                    deviceCode = null
                    return@LaunchedEffect
                }
                // Конфигурационные ошибки GitHub — вход невозможен, есть смысл упасть сразу.
                result.error in setOf("unsupported_grant_type", "incorrect_client_credentials", "incorrect_device_code") -> {
                    appAuthError = result.error?.replace('_', ' ') ?: "GitHub authorization failed"
                    deviceCode = null
                    return@LaunchedEffect
                }
                // Всё остальное (сетевой сбой, parse_error, пустой ответ, неизвестный код) —
                // ТРАНЗИЕНТНО: не роняем вход, продолжаем ждать авторизацию до истечения кода.
                else -> Unit
            }
        }
        appAuthError = "The device code expired. Try again."
        deviceCode = null
    }

    fun startAppSignIn() {
        appAuthBusy = true
        appAuthError = ""
        scope.launch {
            val code = GitHubManager.initiateGsGitAppDeviceFlow()
            appAuthBusy = false
            if (code == null || code.deviceCode.isBlank() || code.userCode.isBlank()) {
                appAuthError = "GitHub did not return a device code"
                return@launch
            }
            deviceCode = code
            // GitHub всегда шлёт expires_in (~900с); фолбэк на 900, если пришло 0/мусор,
            // иначе цикл поллинга умрёт после первого delay, не дождавшись авторизации.
            val ttlSeconds = if (code.expiresIn > 0) code.expiresIn else 900
            deviceCodeExpiresAt = System.currentTimeMillis() + ttlSeconds * 1000L
            if (!context.openExternalHttps(code.verificationUri)) {
                Toast.makeText(context, "Open ${code.verificationUri} and enter the code", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun copyDeviceCode() {
        val code = deviceCode?.userCode ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub device code", code))
        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
    }

    // Пригодится в 1.0.91, когда гостя вернём с полным гейтингом.
    @Suppress("unused")
    fun continueAsGuest() {
        GitHubAuth.enterGuestMode(context)
        onLogin()
    }

    fun savePersonalAccessToken() {
        val token = patInput.trim()
        if (token.isBlank()) { appAuthError = "Token is empty"; return }
        if (GitHubAuth.saveToken(context, token)) {
            patInput = ""; patExpanded = false; patAccepted = false; appAuthError = ""
            onLogin()
        } else {
            appAuthError = SecurityGate.blockedMessage(context)
        }
    }

    AiModuleSurface {
        Column(Modifier.fillMaxSize()) {
            GitHubPageBar(
                title = "> github",
                subtitle = "sign in",
                onBack = onBack,
                trailing = {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.PIP,
                        onClick = onMinimize,
                        tint = palette.textSecondary,
                        contentDescription = "minimize",
                    )
                    if (onClose != null) {
                        GitHubTopBarAction(
                            glyph = GhGlyphs.CLOSE,
                            onClick = onClose,
                            tint = palette.error,
                            contentDescription = "close",
                        )
                    }
                },
            )
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "[ github ]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Strings.ghLoginDesc,
                    fontSize = 12.sp,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                val pendingDeviceCode = deviceCode
                if (pendingDeviceCode == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .border(1.dp, palette.accent, RoundedCornerShape(GitHubControlRadius))
                            .background(if (!appAuthBusy) palette.accent else palette.surface)
                            .clickable(enabled = !appAuthBusy) { startAppSignIn() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (appAuthBusy) {
                            AiModuleSpinner(label = "starting…")
                        } else {
                            Text(
                                "[ sign in with github ]",
                                color = palette.background,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    // Инфо под рекомендуемым входом + контакт разработчика.
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Recommended. For private repositories you may need to install GsGit App. Questions about permissions — developer: t.me/fengbei1998",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.openExternalHttps("https://t.me/fengbei1998") },
                    )

                    // Дверь 2: гость — временно СКРЫТ до 1.0.91. Гостевой режим
                    // готов в data-слое (enterGuestMode), но выкатим его только с
                    // полным гейтингом: кнопка входа на главном, скрытие настроек/
                    // repo-settings/записи у гостя и модалка при исчерпании 60 анонимных
                    // запросов. `continueAsGuest()` оставлен для повторного включения.
                    @Suppress("unused") val guestDoorHiddenUntil = "1.0.91"

                    // Дверь 3: токен (PAT) — на свой страх и риск, только через согласие.
                    Spacer(Modifier.height(14.dp))
                    if (!patExpanded) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .border(1.dp, palette.error, RoundedCornerShape(GitHubControlRadius))
                                .clickable { patExpanded = true; patAccepted = false; appAuthError = "" }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "[ sign in with token ]",
                                color = palette.error,
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .border(1.dp, palette.error, RoundedCornerShape(GitHubControlRadius))
                                .padding(12.dp),
                        ) {
                            Text(
                                "WARNING: a personal access token is a long-lived secret with broad access, stored on this device. It is less safe than the GitHub App sign-in. Continue only if you understand the risk — you proceed at your own responsibility.",
                                color = palette.error,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                            )
                            if (!patAccepted) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(palette.error)
                                        .clickable { patAccepted = true }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("[ I understand the risk ]", color = palette.background, fontFamily = JetBrainsMono, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "[ cancel ]",
                                    color = palette.textSecondary,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().clickable { patExpanded = false },
                                )
                            } else {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(palette.background)
                                        .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                ) {
                                    if (patInput.isEmpty()) {
                                        Text("ghp_… / github_pat_…", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp)
                                    }
                                    BasicTextField(
                                        value = patInput,
                                        onValueChange = { patInput = it },
                                        textStyle = TextStyle(color = palette.textPrimary, fontSize = 12.sp, fontFamily = JetBrainsMono),
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                                            .clickable { patExpanded = false; patInput = "" }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("[ cancel ]", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp) }
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(palette.accent)
                                            .clickable { savePersonalAccessToken() }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("[ save token ]", color = palette.background, fontFamily = JetBrainsMono, fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GitHubControlRadius))
                            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                            .background(palette.surface)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "enter this code on github.com",
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            pendingDeviceCode.userCode,
                            color = palette.accent,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            modifier = Modifier.clickable { copyDeviceCode() },
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "tap the code to copy",
                            color = palette.textMuted,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        AiModuleSpinner(label = "waiting for authorization…")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "[ cancel ]",
                            color = palette.error,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { deviceCode = null },
                        )
                    }
                }
                if (appAuthError.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        appAuthError,
                        color = palette.error,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                if (securityWarning.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        securityWarning,
                        color = palette.error,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                // Футер: версия и ссылки на сайт/статус — то же, что в Settings → About.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "v${gs.git.vps.BuildConfig.VERSION_NAME}",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                    )
                    Text("  ·  ", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
                    Text(
                        "gsgit.org",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://gsgit.org"))
                            )
                        },
                    )
                    Text("  ·  ", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
                    Text(
                        "status",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://status.gsgit.org"))
                            )
                        },
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun QuickGlanceWidget(stats: QuickGlanceStats, onRefresh: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ":: quick glance (last 24h)",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                if (stats.loading) {
                    AiModuleSpinner(label = "loading…")
                } else {
                    Text(
                        "[refresh]",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable(onClick = onRefresh)
                    )
                }
            }
            if (!stats.loading) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickGlanceItem(
                        value = "${stats.assignedPrsCount}",
                        label = "prs",
                        color = palette.accent
                    )
                    QuickGlanceItem(
                        value = "${stats.openIssuesCount}",
                        label = "issues",
                        color = palette.textSecondary
                    )
                    QuickGlanceItem(
                        value = "${stats.failedBuildsCount}",
                        label = "failed builds",
                        color = if (stats.failedBuildsCount > 0) palette.error else palette.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickGlanceItem(value: String, label: String, color: Color) {
    val palette = AiModuleTheme.colors
    Column(
        modifier = Modifier
            .weight(1f)
            .border(1.dp, palette.border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(palette.background.copy(alpha = 0.5f))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FeedFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) palette.accent else palette.surface)
            .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) palette.background else palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun FeedEventCard(event: GHRepoEvent, onProfile: (String) -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, palette.border.copy(alpha = 0.8f), RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "@${event.actor}",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onProfile(event.actor) }
                )
                Text(
                    text = event.createdAt.take(16).replace("T", " "),
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
            }
            
            val eventDesc = remember(event) {
                val actionWord = when (event.type) {
                    "PushEvent" -> "pushed to"
                    "WatchEvent" -> "starred"
                    "CreateEvent" -> "created ${event.refType} ${event.ref}"
                    "PullRequestEvent" -> "${event.action} pull request"
                    "IssuesEvent" -> "${event.action} issue"
                    "IssueCommentEvent" -> "commented on issue"
                    "ForkEvent" -> "forked"
                    "DeleteEvent" -> "deleted ${event.refType} ${event.ref}"
                    else -> repoEventLabel(event.type)
                }
                val repoPart = if (event.repoName.isNotBlank()) " in ${event.repoName}" else ""
                "$actionWord$repoPart"
            }
            
            Text(
                text = eventDesc,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp
            )
            
            val details = remember(event) {
                val list = mutableListOf<String>()
                if (event.type == "PushEvent" && event.size > 0) {
                    list.add("${event.size} commit(s)")
                }
                if (event.ref.isNotBlank() && event.type != "CreateEvent" && event.type != "DeleteEvent") {
                    list.add("ref: ${event.ref}")
                }
                list.joinToString(" · ")
            }
            
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun repoEventLabel(type: String): String =
    type.removeSuffix("Event").replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()

private fun loadRepoTags(context: Context): Map<String, List<String>> {
    val prefs = context.getSharedPreferences("gsgit_repo_tags", Context.MODE_PRIVATE)
    val all = prefs.all
    val result = mutableMapOf<String, List<String>>()
    for ((k, v) in all) {
        if (v is String) {
            result[k] = v.split(",").filter { it.isNotBlank() }
        }
    }
    return result
}

@Composable
internal fun ReposScreen(
    user: GHUser?,
    onBack: () -> Unit,
    onMinimize: () -> Unit,
    onClose: (() -> Unit)? = null,
    onLogout: () -> Unit,
    onRepoClick: (GHRepo) -> Unit,
    onGists: () -> Unit,
    onSettings: () -> Unit,
    onNotifications: () -> Unit = {},
    onProfile: (String) -> Unit = {},
    onAccounts: () -> Unit = {},
    initialShowApps: Boolean = false,
    onInitialShowAppsConsumed: () -> Unit = {},
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var repos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }; var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }; var showCreate by rememberSaveable { mutableStateOf(false) }
    var searchPublic by rememberSaveable { mutableStateOf(false) }; var publicResults by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var showStarred by rememberSaveable { mutableStateOf(false) }
    var showOrgs by rememberSaveable { mutableStateOf(false) }
    var showPackages by rememberSaveable { mutableStateOf(false) }
    var showApps by rememberSaveable { mutableStateOf(false) }
    var showEnterpriseAdmin by rememberSaveable { mutableStateOf(false) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    var showAdvancedSearch by rememberSaveable { mutableStateOf(false) }
    var showEmojis by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showTopics by rememberSaveable { mutableStateOf(false) }
    var showGitignore by rememberSaveable { mutableStateOf(false) }
    var showMeta by rememberSaveable { mutableStateOf(false) }
    var quickStartResult by remember { mutableStateOf<GHRepoCreateResult?>(null) }
    var reposPage by rememberSaveable { mutableIntStateOf(1) }; var reposHasMore by rememberSaveable { mutableStateOf(true) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }

    var repoTags by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("updated") }
    var editingRepoTags by remember { mutableStateOf<GHRepo?>(null) }

    var quickGlanceStats by remember { mutableStateOf(QuickGlanceStats()) }
    var showQuickGlance by rememberSaveable { mutableStateOf(true) }

    val pagerState = rememberPagerState(pageCount = { 2 })

    var feedEvents by remember { mutableStateOf<List<GHRepoEvent>>(emptyList()) }
    var feedPage by rememberSaveable { mutableIntStateOf(1) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedHasMore by rememberSaveable { mutableStateOf(true) }
    var feedFilter by remember { mutableStateOf<String?>(null) }

    fun loadFeed(reset: Boolean = false) {
        if (user == null || feedLoading) return
        feedLoading = true
        scope.launch {
            val pageToLoad = if (reset) 1 else feedPage
            val r = GitHubManager.getUserReceivedEvents(context, user.login, pageToLoad)
            if (reset) {
                feedEvents = r
                feedPage = 2
                feedHasMore = r.size >= 30
            } else {
                feedEvents = feedEvents + r
                feedPage++
                feedHasMore = r.size >= 30
            }
            feedLoading = false
        }
    }

    LaunchedEffect(Unit) {
        repoTags = loadRepoTags(context)
    }

    LaunchedEffect(initialShowApps) {
        if (initialShowApps) {
            showApps = true
            onInitialShowAppsConsumed()
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            quickGlanceStats = GitHubManager.getQuickGlanceStats(context)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1 && feedEvents.isEmpty()) {
            loadFeed(reset = true)
        }
    }

    fun handleReposBack() {
        when {
            quickStartResult != null -> quickStartResult = null
            showCreate -> showCreate = false
            showStarred -> showStarred = false
            showOrgs -> showOrgs = false
            showPackages -> showPackages = false
            showApps -> showApps = false
            showEnterpriseAdmin -> showEnterpriseAdmin = false
            showDiagnostics -> showDiagnostics = false
            showAdvancedSearch -> showAdvancedSearch = false
            showEmojis -> showEmojis = false
            showLicenses -> showLicenses = false
            showTopics -> showTopics = false
            showGitignore -> showGitignore = false
            showMeta -> showMeta = false
            else -> onBack()
        }
    }
    BackHandler(enabled = quickStartResult != null || showStarred || showOrgs || showPackages || showApps || showEnterpriseAdmin || showDiagnostics || showAdvancedSearch || showEmojis || showLicenses || showTopics || showGitignore || showMeta || showCreate) {
        handleReposBack()
    }
    LaunchedEffect(Unit) { val r = GitHubManager.getRepos(context, 1); repos = r; reposHasMore = r.size >= 30; loading = false }
    LaunchedEffect(query, searchPublic) { if (searchPublic && query.length >= 2) publicResults = GitHubManager.searchRepos(context, query) }
    val filtered = remember(repos, query, searchPublic, repoTags, selectedTagFilter, sortBy) {
        val baseList = if (searchPublic) publicResults else if (query.isNotBlank()) repos.filter { it.name.contains(query, true) || it.description.contains(query, true) } else repos
        val tagFiltered = if (selectedTagFilter != null) {
            baseList.filter { repo ->
                val tags = repoTags[repo.fullName] ?: emptyList()
                tags.contains(selectedTagFilter)
            }
        } else {
            baseList
        }
        when (sortBy) {
            "stars" -> tagFiltered.sortedByDescending { it.stars }
            "forks" -> tagFiltered.sortedByDescending { it.forks }
            "name" -> tagFiltered.sortedBy { it.name.lowercase() }
            else -> tagFiltered
        }
    }
    if (showStarred) { StarredScreen(onBack = { showStarred = false }, onRepoClick = { showStarred = false; onRepoClick(it) }); return }
    if (showOrgs) { OrgsScreen(onBack = { showOrgs = false }, onRepoClick = { showOrgs = false; onRepoClick(it) }); return }
    if (showPackages && user != null) { PackagesScreen(userLogin = user.login, onBack = { showPackages = false }); return }
    if (showApps) { GitHubAppsScreen(onBack = { showApps = false }, onRepoClick = { showApps = false; onRepoClick(it) }); return }
    if (showEnterpriseAdmin) { GitHubEnterpriseAdminScreen(onBack = { showEnterpriseAdmin = false }); return }
    if (showDiagnostics) { GitHubDiagnosticsScreen(onBack = { showDiagnostics = false }); return }
    if (showAdvancedSearch) { AdvancedSearchScreen(onBack = { showAdvancedSearch = false }, onRepoClick = onRepoClick, onProfile = onProfile); return }
    if (showEmojis) { EmojisScreen(onBack = { showEmojis = false }); return }
    if (showLicenses) { LicensesScreen(onBack = { showLicenses = false }); return }
    if (showTopics) { TopicsScreen(onBack = { showTopics = false }); return }
    if (showGitignore) { GitignoreScreen(onBack = { showGitignore = false }); return }
    if (showMeta) { GitHubMetaScreen(onBack = { showMeta = false }); return }
    if (showCreate && user != null) {
        RepoCreateScreen(
            userLogin = user.login,
            onBack = { showCreate = false },
            onCreate = { params ->
                scope.launch {
                    val r = if (params.templateRepo.isNotBlank() && params.templateOwner.isNotBlank()) {
                        GitHubManager.createRepoFromTemplateWithResult(
                            context,
                            params.templateOwner,
                            params.templateRepo,
                            params.name,
                            params.description,
                            params.isPrivate,
                            params.includeAllBranches
                        )
                    } else {
                        GitHubManager.createRepoWithResult(
                            context,
                            params.name,
                            params.description,
                            params.isPrivate,
                            params.autoInit,
                            params.gitignoreTemplate,
                            params.licenseTemplate,
                            params.hasIssues,
                            params.hasProjects,
                            params.hasWiki
                        )
                    }
                    if (r.success) {
                        showCreate = false
                        reposPage = 1
                        repos = GitHubManager.getRepos(context, 1)
                        reposHasMore = repos.size >= 30
                        quickStartResult = r
                    } else {
                        Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        return
    }
    if (quickStartResult != null) { RepoQuickStartScreen(result = quickStartResult!!, onBack = { quickStartResult = null }, onOpenRepo = { quickStartResult = null; onRepoClick(it) }); return }
    
    AiModuleSurface {
    val palette = AiModuleTheme.colors
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(palette.background)) {
            GitHubPageBar(
                title = "> github",
                onBack = ::handleReposBack,
                trailing = {
                    GitHubTopBarAction(GhGlyphs.NOTIFY, onNotifications, palette.accent, contentDescription = "notifications")
                    GitHubTopBarAction(GhGlyphs.FILE, onGists, palette.accent, contentDescription = "gists")
                    GitHubTopBarAction(GhGlyphs.PLUS, { showCreate = true }, palette.accent, contentDescription = "create repository")
                    GitHubTopBarAction(GhGlyphs.SETTINGS, onSettings, palette.textSecondary, contentDescription = "settings")
                    GitHubTopBarAction(GhGlyphs.PIP, onMinimize, palette.textSecondary, contentDescription = "minimize")
                    if (onClose != null) {
                        GitHubTopBarAction(GhGlyphs.CLOSE, onClose, palette.error, contentDescription = "close")
                    }
                },
            )
            
            if (user != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        user.avatarUrl, user.login,
                        Modifier.size(36.dp).clip(CircleShape).clickable { onAccounts() },
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "@${user.login}",
                                color = palette.accent,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = "repos: ${formatGitHubNumber(user.publicRepos + user.privateRepos)}  followers: ${formatGitHubNumber(user.followers)}",
                                color = palette.textSecondary,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                            )
                        }
                        if (user.bio.isNotBlank()) {
                            Text(
                                user.bio,
                                color = palette.textMuted,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "dashboard",
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
                Text(
                    text = if (showQuickGlance) "[hide stats]" else "[show stats]",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { showQuickGlance = !showQuickGlance }
                )
            }
            
            if (showQuickGlance) {
                Box(Modifier.padding(horizontal = 12.dp)) {
                    QuickGlanceWidget(quickGlanceStats) {
                        scope.launch {
                            quickGlanceStats = quickGlanceStats.copy(loading = true)
                            quickGlanceStats = GitHubManager.getQuickGlanceStats(context)
                        }
                    }
                }
            }

            // Tab Selector Row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(palette.surface, RoundedCornerShape(GitHubControlRadius))
                    .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (pagerState.currentPage == 0) palette.accent else Color.Transparent)
                        .clickable { scope.launch { pagerState.animateScrollToPage(0) } }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "repositories",
                        color = if (pagerState.currentPage == 0) palette.background else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (pagerState.currentPage == 1) palette.accent else Color.Transparent)
                        .clickable { scope.launch { pagerState.animateScrollToPage(1) } }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "activity feed",
                        color = if (pagerState.currentPage == 1) palette.background else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                if (page == 0) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    ) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                TerminalQuickChip(Strings.ghStarredRepos) { showStarred = true }
                                TerminalQuickChip(Strings.ghOrganizations) { showOrgs = true }
                                TerminalQuickChip("emojis") { showEmojis = true }
                                TerminalQuickChip("licenses") { showLicenses = true }
                                TerminalQuickChip("topics") { showTopics = true }
                                TerminalQuickChip("gitignore") { showGitignore = true }
                                TerminalQuickChip("meta") { showMeta = true }
                                TerminalQuickChip("Search") { showAdvancedSearch = true }
                                TerminalQuickChip("Packages") { showPackages = true }
                                TerminalQuickChip("Apps") { showApps = true }
                                TerminalQuickChip("Admin API") { showEnterpriseAdmin = true }
                                TerminalQuickChip("Diagnostics") { showDiagnostics = true }
                                TerminalQuickChip(Strings.ghProfile) { if (user != null) onProfile(user.login) }
                            }
                        }

                        val allTags = repoTags.values.flatten().distinct().sorted()
                        if (allTags.isNotEmpty()) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("filter tags:", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (selectedTagFilter == null) palette.accent else palette.surface)
                                            .border(1.dp, if (selectedTagFilter == null) palette.accent else palette.border, RoundedCornerShape(3.dp))
                                            .clickable { selectedTagFilter = null }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("all", color = if (selectedTagFilter == null) palette.background else palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 9.sp)
                                    }
                                    allTags.forEach { tag ->
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (selectedTagFilter == tag) palette.accent else palette.surface)
                                                .border(1.dp, if (selectedTagFilter == tag) palette.accent else palette.border, RoundedCornerShape(3.dp))
                                                .clickable { selectedTagFilter = tag }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(tag, color = if (selectedTagFilter == tag) palette.background else palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("sort:", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                                listOf("updated" to "updated", "stars" to "★", "forks" to "forks", "name" to "a-z").forEach { (key, label) ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (sortBy == key) palette.accent else palette.surface)
                                            .border(1.dp, if (sortBy == key) palette.accent else palette.border, RoundedCornerShape(3.dp))
                                            .clickable { sortBy = key }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(label, color = if (sortBy == key) palette.background else palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    if (searchPublic) "search public:" else "search:",
                                    color = palette.textMuted,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(GitHubControlRadius))
                                        .background(palette.surface)
                                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    if (query.isEmpty()) {
                                        Text(
                                            if (searchPublic) "name / desc / public…" else "name / desc",
                                            color = palette.textMuted,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 12.sp,
                                        )
                                    }
                                    BasicTextField(
                                        query,
                                        { query = it },
                                        textStyle = TextStyle(
                                            color = palette.textPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = JetBrainsMono,
                                        ),
                                        singleLine = true,
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(GitHubControlRadius))
                                        .background(if (searchPublic) palette.accent.copy(alpha = 0.10f) else palette.surface)
                                        .border(1.dp, if (searchPublic) palette.accent.copy(alpha = 0.55f) else palette.border, RoundedCornerShape(GitHubControlRadius))
                                        .clickable { searchPublic = !searchPublic; query = "" }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        "public",
                                        color = if (searchPublic) palette.accent else palette.textSecondary,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                        if (loading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                    AiModuleSpinner(label = "loading repos\u2026")
                                }
                            }
                        } else if (filtered.isEmpty()) {
                            item {
                                Text(
                                    "no repositories",
                                    color = palette.textMuted,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            items(filtered) { repo ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    RepoCard(repo, onClick = { onRepoClick(repo) })
                                    val tags = repoTags[repo.fullName] ?: emptyList()
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (tags.isNotEmpty()) {
                                            Text(
                                                "tags:",
                                                color = palette.textMuted,
                                                fontFamily = JetBrainsMono,
                                                fontSize = 10.sp
                                            )
                                            tags.forEach { tag ->
                                                Box(
                                                    Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(palette.accent.copy(alpha = 0.15f))
                                                        .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(tag, color = palette.accent, fontFamily = JetBrainsMono, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = "[edit tags]",
                                            color = palette.textSecondary,
                                            fontFamily = JetBrainsMono,
                                            fontSize = 9.sp,
                                            modifier = Modifier.clickable {
                                                editingRepoTags = repo
                                            }
                                        )
                                    }
                                }
                            }
                            if (!searchPublic && query.isBlank() && reposHasMore) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AiModuleSecondaryButton(
                                            label = "load more \u2192",
                                            onClick = {
                                                scope.launch {
                                                    reposPage++
                                                    val r = GitHubManager.getRepos(context, reposPage)
                                                    if (r.size < 30) reposHasMore = false
                                                    repos = repos + r
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("filter feed:", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
                            
                            FeedFilterChip(label = "all", selected = feedFilter == null) { feedFilter = null }
                            FeedFilterChip(label = "pushes", selected = feedFilter == "PushEvent") { feedFilter = "PushEvent" }
                            FeedFilterChip(label = "stars", selected = feedFilter == "WatchEvent") { feedFilter = "WatchEvent" }
                            FeedFilterChip(label = "prs", selected = feedFilter == "PullRequestEvent") { feedFilter = "PullRequestEvent" }
                            FeedFilterChip(label = "issues", selected = feedFilter == "IssuesEvent") { feedFilter = "IssuesEvent" }
                        }
                        
                        val filteredFeed = remember(feedEvents, feedFilter) {
                            if (feedFilter == null) feedEvents else feedEvents.filter { it.type == feedFilter }
                        }
                        
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            if (feedLoading && filteredFeed.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                        AiModuleSpinner(label = "loading feed\u2026")
                                    }
                                }
                            } else if (filteredFeed.isEmpty()) {
                                item {
                                    Text(
                                        "no feed events",
                                        color = palette.textMuted,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 12.sp,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(filteredFeed) { event ->
                                    FeedEventCard(event, onProfile)
                                }
                                if (feedHasMore) {
                                    item {
                                        Box(
                                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AiModuleSecondaryButton(
                                                label = "load more \u2192",
                                                onClick = { loadFeed() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (editingRepoTags != null) {
            val repo = editingRepoTags!!
            var tagsInput by remember(repo) { mutableStateOf(repoTags[repo.fullName]?.joinToString(", ") ?: "") }
            
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { editingRepoTags = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable(enabled = false) {}
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = ":: manage tags",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = repo.fullName,
                        color = palette.textPrimary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.background)
                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        if (tagsInput.isEmpty()) {
                            Text(
                                "work, personal, pet...",
                                color = palette.textMuted,
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            textStyle = TextStyle(
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono
                            ),
                            singleLine = true,
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                                .clickable { editingRepoTags = null }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("[cancel]", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp)
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.accent)
                                .clickable {
                                    val tagList = tagsInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    val newTagsMap = repoTags.toMutableMap()
                                    if (tagList.isEmpty()) {
                                        newTagsMap.remove(repo.fullName)
                                    } else {
                                        newTagsMap[repo.fullName] = tagList
                                    }
                                    repoTags = newTagsMap
                                    val prefs = context.getSharedPreferences("gsgit_repo_tags", Context.MODE_PRIVATE)
                                    prefs.edit().putString(repo.fullName, tagList.joinToString(",")).apply()
                                    editingRepoTags = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("[save]", color = palette.background, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TerminalQuickChip(label: String, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label.lowercase(java.util.Locale.US),
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
        )
    }
}

// ═══════════════════════════════════
// Code Search Tab
// ═══════════════════════════════════
