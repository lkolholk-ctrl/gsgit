package gs.git.vps.ui.screens

import android.content.Context
import gs.git.vps.security.NativeSecurity
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.Strings
import androidx.compose.ui.platform.LocalContext
import gs.git.vps.data.github.GHApiDiagnosticCheck
import gs.git.vps.data.github.GHApiDiagnostics
import gs.git.vps.data.github.GHApiErrorLogEntry
import gs.git.vps.data.github.GHMeta
import gs.git.vps.data.github.GHRateLimitGraphQL
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.GHStatusComponent
import gs.git.vps.data.github.GHStatusSummary
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset

@Composable
internal fun GitHubDiagnosticsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var owner by rememberSaveable { mutableStateOf("") }
    var repo by rememberSaveable { mutableStateOf("") }
    var org by rememberSaveable { mutableStateOf("") }
    var enterprise by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<GHApiDiagnostics?>(null) }
    var errorLog by remember { mutableStateOf<List<GHApiErrorLogEntry>>(emptyList()) }
    var notice by remember { mutableStateOf("") }

    fun runChecks() {
        if (loading) return
        loading = true
        notice = ""
        scope.launch {
            report = GitHubManager.runApiDiagnostics(context, owner, repo, org, enterprise)
            errorLog = GitHubManager.getApiErrorLog(context)
            notice = "checks=${report?.checks?.size ?: 0}"
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        report = GitHubManager.runApiDiagnostics(context)
        errorLog = GitHubManager.getApiErrorLog(context)
        notice = "basic checks ready"
        loading = false
    }

    GitHubScreenFrame(
        title = "> api diagnostics",
        subtitle = "token / scopes / rate limit / permissions",
        onBack = onBack,
        trailing = {
            GitHubTopBarAction(
                glyph = GhGlyphs.REFRESH,
                onClick = { runChecks() },
                tint = palette.accent,
                enabled = !loading,
                contentDescription = "run diagnostics",
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize().background(palette.background),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GitHubDiagnosticPanel {
                    Text(
                        "Read-only diagnostics. These checks do not mutate repositories or organization settings.",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GitHubDiagnosticInput("owner", owner, { owner = it }, "octocat", Modifier.weight(1f))
                        GitHubDiagnosticInput("repo", repo, { repo = it }, "hello-world", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GitHubDiagnosticInput("org", org, { org = it }, "optional", Modifier.weight(1f))
                        GitHubDiagnosticInput("enterprise", enterprise, { enterprise = it }, "optional", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GitHubTerminalButton(if (loading) "checking..." else "run checks", onClick = { runChecks() }, color = palette.accent, enabled = !loading)
                        GitHubTerminalButton("clear target", onClick = {
                            owner = ""
                            repo = ""
                            org = ""
                            enterprise = ""
                        }, color = palette.textSecondary, enabled = !loading)
                        if (loading) AiModuleSpinner(label = "polling GitHub")
                        report?.let { current ->
                            GitHubTerminalButton("export txt", onClick = {
                                val file = saveGitHubDiagnosticsExport(context, current, errorLog, "txt")
                                Toast.makeText(context, if (file != null) "${Strings.done}: ${file.name}" else Strings.error, Toast.LENGTH_SHORT).show()
                            }, color = palette.textSecondary)
                            GitHubTerminalButton("export json", onClick = {
                                val file = saveGitHubDiagnosticsExport(context, current, errorLog, "json")
                                Toast.makeText(context, if (file != null) "${Strings.done}: ${file.name}" else Strings.error, Toast.LENGTH_SHORT).show()
                            }, color = palette.textSecondary)
                        }
                        if (notice.isNotBlank()) {
                            Text(notice, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
                        }
                    }
                }
            }

            item { LocalSecurityPanel() }
            item { GitHubServiceStatusPanel() }
            item { NetworkPingPanel() }

            report?.let { current ->
                item { TokenValidationPanel() }
                item { RateLimitDashboard(current) }
                item { GitHubDiagnosticsSummary(current) }
                item { GitHubMetaPanel() }
                item { GitHubGraphQLRateLimitPanel() }
                item { CacheManagementPanel(onClearCache = {
                    GitHubManager.clearEtagCache()
                    notice = "etag cache cleared"
                }) }
                item {
                    GitHubApiErrorLogPanel(
                        errors = errorLog,
                        onClear = {
                            GitHubManager.clearApiErrorLog(context)
                            errorLog = emptyList()
                            notice = "api error log cleared"
                        },
                        onRefresh = {
                            errorLog = GitHubManager.getApiErrorLog(context)
                            notice = "api error log refreshed"
                        },
                    )
                }
                items(current.checks, key = { "${it.title}:${it.endpoint}" }) { check ->
                    GitHubDiagnosticCheckRow(check)
                }
            }

            if (report == null && !loading) {
                item {
                    Text(
                        "No diagnostics report yet.",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubApiErrorLogPanel(
    errors: List<GHApiErrorLogEntry>,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    GitHubDiagnosticPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "recent api errors",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Text(
                    if (errors.isEmpty()) "no stored errors" else "stored=${errors.size} · newest first",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
            }
            GitHubTerminalButton("refresh", onClick = onRefresh, color = palette.textSecondary)
            GitHubTerminalButton("clear", onClick = onClear, color = palette.error, enabled = errors.isNotEmpty())
        }
        if (errors.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errors.take(8).forEach { item ->
                    GitHubApiErrorRow(item)
                }
            }
        }
    }
}

@Composable
private fun GitHubApiErrorRow(item: GHApiErrorLogEntry) {
    val palette = AiModuleTheme.colors
    val color = when (item.statusCode) {
        401, 403 -> GitHubWarningAmber()
        404 -> palette.textMuted
        in 500..599 -> GitHubErrorRed
        else -> palette.error
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.background, RoundedCornerShape(GitHubControlRadius))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${item.method} HTTP ${item.statusCode}", color = color, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp)
            Text(githubDiagnosticsTime(item.timestamp), color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(item.endpoint, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.message.ifBlank { item.body.ifBlank { "no error body" } }, color = color, fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val meta = listOfNotNull(
            item.requestId.takeIf { it.isNotBlank() }?.let { "request=$it" },
            item.rateRemaining.takeIf { it.isNotBlank() }?.let { "remaining=$it" },
        ).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GitHubDiagnosticsSummary(report: GHApiDiagnostics) {
    val palette = AiModuleTheme.colors
    val updated = remember(report.generatedAt) { githubDiagnosticsTime(report.generatedAt) }
    GitHubDiagnosticPanel {
        Text(
            "summary",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubRateBox("core", "${report.rate.coreRemaining}/${report.rate.coreLimit}")
            GitHubRateBox("search", "${report.rate.searchRemaining}/${report.rate.searchLimit}")
            GitHubRateBox("graphql", "${report.rate.graphqlRemaining}/${report.rate.graphqlLimit}")
            GitHubRateBox("reset", report.rate.resetEpoch.takeIf { it > 0L }?.toString() ?: "n/a")
        }
        Spacer(Modifier.height(8.dp))
        GitHubDiagnosticKV("updated", updated)
        GitHubDiagnosticKV("token scopes", report.scopes.ifBlank { "empty or fine-grained token" })
        GitHubDiagnosticKV("accepted scopes", report.acceptedScopes.ifBlank { "not returned" })
    }
}

@Composable
private fun GitHubDiagnosticCheckRow(check: GHApiDiagnosticCheck) {
    val palette = AiModuleTheme.colors
    val color = when (check.status) {
        "ok" -> GitHubSuccessGreen
        "warn" -> GitHubWarningAmber()
        "skip" -> palette.textMuted
        else -> GitHubErrorRed
    }
    GitHubDiagnosticPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubDiagnosticStatus(check.status, color)
            Column(Modifier.weight(1f)) {
                Text(
                    check.title,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    check.endpoint,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val codeText = if (check.statusCode > 0) "HTTP ${check.statusCode}" else "skip"
            Text(codeText, color = color, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            check.message,
            color = if (check.status == "ok") palette.textSecondary else color,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        if (check.hint.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                check.hint,
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun GitHubDiagnosticInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = AiModuleTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
        GitHubTerminalTextField(value, onValueChange, placeholder = placeholder, singleLine = true, minHeight = 38.dp)
    }
}

@Composable
private fun GitHubDiagnosticPanel(content: @Composable () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface.copy(alpha = 0.72f), RoundedCornerShape(GitHubControlRadius))
            .padding(12.dp),
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun GitHubRateBox(label: String, value: String) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier
            .widthIn(min = 92.dp)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.background, RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
        Text(value, color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GitHubDiagnosticStatus(status: String, color: Color) {
    Box(
        Modifier
            .width(54.dp)
            .border(1.dp, color, RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(status, color = color, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GitHubDiagnosticKV(label: String, value: String) {
    val palette = AiModuleTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.width(118.dp))
        Text(value, color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.weight(1f), lineHeight = 16.sp)
    }
}

private fun githubDiagnosticsTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(epochMillis))

private fun githubDiagnosticsFileStamp(epochMillis: Long): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(epochMillis))

private fun saveGitHubDiagnosticsExport(
    context: Context,
    report: GHApiDiagnostics,
    errors: List<GHApiErrorLogEntry>,
    format: String,
): File? =
    try {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_Git")
        dir.mkdirs()
        val extension = if (format == "json") "json" else "txt"
        val file = File(dir, "github_diagnostics_${githubDiagnosticsFileStamp(report.generatedAt)}.$extension")
        val content = if (extension == "json") {
            githubDiagnosticsJson(report, errors).toString(2)
        } else {
            githubDiagnosticsText(report, errors)
        }
        file.writeText(content)
        file
    } catch (_: Exception) {
        null
    }

private fun githubDiagnosticsText(report: GHApiDiagnostics, errors: List<GHApiErrorLogEntry>): String =
    buildString {
        appendLine("GlassFiles GitHub API Diagnostics")
        appendLine("generated: ${githubDiagnosticsTime(report.generatedAt)}")
        appendLine()
        appendLine("[rate]")
        appendLine("core: ${report.rate.coreRemaining}/${report.rate.coreLimit}")
        appendLine("search: ${report.rate.searchRemaining}/${report.rate.searchLimit}")
        appendLine("graphql: ${report.rate.graphqlRemaining}/${report.rate.graphqlLimit}")
        appendLine("reset_epoch: ${report.rate.resetEpoch}")
        appendLine()
        appendLine("[scopes]")
        appendLine("token_scopes: ${report.scopes.ifBlank { "empty or fine-grained token" }}")
        appendLine("accepted_scopes: ${report.acceptedScopes.ifBlank { "not returned" }}")
        appendLine()
        appendLine("[checks]")
        report.checks.forEach { check ->
            appendLine("- ${check.status.uppercase(Locale.US)} ${check.title} · ${if (check.statusCode > 0) "HTTP ${check.statusCode}" else "skip"}")
            appendLine("  endpoint: ${check.endpoint}")
            appendLine("  message: ${check.message}")
            if (check.hint.isNotBlank()) appendLine("  hint: ${check.hint}")
        }
        appendLine()
        appendLine("[recent_api_errors]")
        if (errors.isEmpty()) {
            appendLine("none")
        } else {
            errors.forEach { item ->
                appendLine("- ${githubDiagnosticsTime(item.timestamp)} ${item.method} HTTP ${item.statusCode}")
                appendLine("  endpoint: ${item.endpoint}")
                appendLine("  message: ${item.message.ifBlank { item.body.ifBlank { "no error body" } }}")
                if (item.requestId.isNotBlank()) appendLine("  request_id: ${item.requestId}")
                if (item.rateRemaining.isNotBlank()) appendLine("  rate_remaining: ${item.rateRemaining}")
            }
        }
    }

private fun githubDiagnosticsJson(report: GHApiDiagnostics, errors: List<GHApiErrorLogEntry>): JSONObject =
    JSONObject().apply {
        put("generated_at", report.generatedAt)
        put("generated_at_text", githubDiagnosticsTime(report.generatedAt))
        put("scopes", report.scopes)
        put("accepted_scopes", report.acceptedScopes)
        put("rate", JSONObject().apply {
            put("core_limit", report.rate.coreLimit)
            put("core_remaining", report.rate.coreRemaining)
            put("search_limit", report.rate.searchLimit)
            put("search_remaining", report.rate.searchRemaining)
            put("graphql_limit", report.rate.graphqlLimit)
            put("graphql_remaining", report.rate.graphqlRemaining)
            put("reset_epoch", report.rate.resetEpoch)
        })
        put("checks", JSONArray().apply {
            report.checks.forEach { check ->
                put(JSONObject().apply {
                    put("title", check.title)
                    put("endpoint", check.endpoint)
                    put("status_code", check.statusCode)
                    put("status", check.status)
                    put("message", check.message)
                    put("hint", check.hint)
                })
            }
        })
        put("recent_api_errors", JSONArray().apply {
            errors.forEach { item ->
                put(JSONObject().apply {
                    put("timestamp", item.timestamp)
                    put("timestamp_text", githubDiagnosticsTime(item.timestamp))
                    put("method", item.method)
                    put("endpoint", item.endpoint)
                    put("status_code", item.statusCode)
                    put("message", item.message)
                    put("body", item.body)
                    put("request_id", item.requestId)
                    put("rate_remaining", item.rateRemaining)
                })
            }
        })
    }

@Composable
private fun GitHubMetaPanel() {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    var meta by remember { mutableStateOf<GHMeta?>(null) }
    LaunchedEffect(Unit) { meta = GitHubManager.getGitHubMeta(context) }
    GitHubDiagnosticPanel {
        Text("github /meta", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        meta?.let { m ->
            GitHubDiagnosticKV("password auth", m.verifiablePasswordAuthentication.toString())
            GitHubDiagnosticKV("ssh keys", m.sshKeys.take(3).joinToString(", "))
            GitHubDiagnosticKV("hooks", m.hooks.take(3).joinToString(", "))
            GitHubDiagnosticKV("api", m.api.take(3).joinToString(", "))
            GitHubDiagnosticKV("web", m.web.take(3).joinToString(", "))
            GitHubDiagnosticKV("git", m.git.take(3).joinToString(", "))
            GitHubDiagnosticKV("pages", m.pages.take(3).joinToString(", "))
        } ?: Text("loading...", color = palette.textMuted, fontSize = 12.sp, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun GitHubGraphQLRateLimitPanel() {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    var gqlRate by remember { mutableStateOf<GHRateLimitGraphQL?>(null) }
    LaunchedEffect(Unit) { gqlRate = GitHubManager.getRateLimitGraphQL(context) }
    GitHubDiagnosticPanel {
        Text("graphql rate limit", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        gqlRate?.let { r ->
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GitHubRateBox("remaining", "${r.remaining}/${r.limit}")
                GitHubRateBox("cost", r.cost.toString())
                GitHubRateBox("nodes", r.nodeCount.toString())
            }
            if (r.resetAt.isNotBlank()) GitHubDiagnosticKV("reset at", r.resetAt.take(19).replace('T', ' '))
        } ?: Text("loading...", color = palette.textMuted, fontSize = 12.sp, fontFamily = JetBrainsMono)
    }
}

@Composable
private fun TokenValidationPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var validation by remember { mutableStateOf<GitHubManager.TokenValidation?>(null) }
    var checking by remember { mutableStateOf(false) }

    GitHubDiagnosticPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("token validation", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("verify token is valid and check scopes", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
            GitHubTerminalButton(if (checking) "checking…" else "validate", onClick = {
                if (!checking) {
                    checking = true
                    scope.launch {
                        validation = GitHubManager.validateToken(context)
                        checking = false
                    }
                }
            }, color = palette.accent, enabled = !checking)
        }
        validation?.let { v ->
            Spacer(Modifier.height(8.dp))
            val statusColor = if (v.valid) GitHubSuccessGreen else GitHubErrorRed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(54.dp).border(1.dp, statusColor, RoundedCornerShape(GitHubControlRadius)).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text(if (v.valid) "valid" else "invalid", color = statusColor, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                if (v.valid) {
                    Text("@${v.login}", color = palette.textPrimary, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (v.scopes.isNotBlank()) {
                GitHubDiagnosticKV("scopes", v.scopes)
            } else if (v.valid) {
                GitHubDiagnosticKV("scopes", "fine-grained PAT (no OAuth scopes)")
            }
            if (!v.valid && v.error.isNotBlank()) {
                Text(v.error, color = GitHubErrorRed, fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun RateLimitDashboard(report: GHApiDiagnostics) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    var refreshing by remember { mutableStateOf(false) }
    var liveRemaining by remember { mutableStateOf(GitHubManager.getRateLimitRemaining()) }
    var liveReset by remember { mutableStateOf(GitHubManager.getRateLimitResetEpoch()) }

    fun refresh() {
        scope.launch {
            refreshing = true
            GitHubManager.getRateLimitSummaryNative(context)
            liveRemaining = GitHubManager.getRateLimitRemaining()
            liveReset = GitHubManager.getRateLimitResetEpoch()
            refreshing = false
        }
    }

    GitHubDiagnosticPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("rate limit dashboard", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            GitHubTerminalButton("refresh", onClick = { refresh() }, color = palette.textSecondary, enabled = !refreshing)
        }
        Spacer(Modifier.height(8.dp))
        val limit = report.rate.coreLimit.coerceAtLeast(1)
        val pct = liveRemaining.toFloat() / limit
        val barColor = when {
            pct > 0.5f -> GitHubSuccessGreen
            pct > 0.2f -> GitHubWarningAmber()
            else -> GitHubErrorRed
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("core", color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.background).border(1.dp, palette.border, RoundedCornerShape(4.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct.coerceIn(0f, 1f)).clip(RoundedCornerShape(4.dp)).background(barColor))
                }
                Text("$liveRemaining/$limit", color = barColor, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (liveReset > 0) {
            val resetDate = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(liveReset * 1000))
            val waitSec = ((liveReset * 1000) - System.currentTimeMillis()).coerceAtLeast(0) / 1000
            GitHubDiagnosticKV("resets at", "$resetDate (${waitSec}s remaining)")
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitHubRateBox("search", "${report.rate.searchRemaining}/${report.rate.searchLimit}")
            GitHubRateBox("graphql", "${report.rate.graphqlRemaining}/${report.rate.graphqlLimit}")
        }
        if (GitHubManager.isRateLimitLow()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(54.dp).border(1.dp, GitHubErrorRed, RoundedCornerShape(GitHubControlRadius)).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                    Text("low", color = GitHubErrorRed, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                Text("rate limit critically low — avoid API calls until reset", color = GitHubErrorRed, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CacheManagementPanel(onClearCache: () -> Unit) {
    val palette = AiModuleTheme.colors
    var showConfirm by remember { mutableStateOf(false) }

    GitHubDiagnosticPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("cache management", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("etag cache for conditional GET requests", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
            GitHubTerminalButton("clear etag cache", onClick = {
                if (!showConfirm) showConfirm = true else {
                    onClearCache()
                    showConfirm = false
                }
            }, color = if (showConfirm) GitHubErrorRed else palette.textSecondary)
        }
        if (showConfirm) {
            Spacer(Modifier.height(4.dp))
            Text("tap again to confirm cache clear", color = GitHubWarningAmber(), fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LocalSecurityPanel() {
    val palette = AiModuleTheme.colors
    var frida by remember { mutableStateOf(false) }
    var rooted by remember { mutableStateOf(false) }
    var magisk by remember { mutableStateOf(false) }
    var debugger by remember { mutableStateOf(false) }
    var emulator by remember { mutableStateOf(false) }
    var safe by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            frida = NativeSecurity.isFridaDetected()
            rooted = NativeSecurity.isRooted()
            magisk = NativeSecurity.isMagiskDetected()
            debugger = NativeSecurity.isDebuggerDetected()
            emulator = NativeSecurity.isEmulatorDetected()
            safe = NativeSecurity.isEnvironmentSafe()
            loaded = true
        } catch (e: Exception) {
            // Native library load failed or missing method
        }
    }

    GitHubDiagnosticPanel {
        Text(
            "local security environment",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Text(
            "native integrity & security environment validation",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))

        if (loaded) {
            val integrityColor = if (safe) GitHubSuccessGreen else GitHubErrorRed
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .width(92.dp)
                        .border(1.dp, integrityColor, RoundedCornerShape(GitHubControlRadius))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (safe) "SAFE" else "COMPROMISED",
                        color = integrityColor,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    if (safe) "Device environment is clean and safe" else "Environment violates integrity policy",
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            GitHubDiagnosticKV("Frida client", if (frida) "DETECTED" else "CLEAN")
            GitHubDiagnosticKV("SU binary / Root", if (rooted) "DETECTED" else "CLEAN")
            GitHubDiagnosticKV("Magisk / KSU / APatch", if (magisk) "DETECTED" else "CLEAN")
            GitHubDiagnosticKV("Debugger attached", if (debugger) "DETECTED" else "CLEAN")
            GitHubDiagnosticKV("Emulator / Virtualized", if (emulator) "DETECTED" else "CLEAN")
        } else {
            Text("Checking device integrity...", color = palette.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        }
    }
}

@Composable
private fun GitHubServiceStatusPanel() {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    var statusSummary by remember { mutableStateOf<GHStatusSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val s = GitHubManager.getGitHubStatus(context)
        if (s != null) {
            statusSummary = s
        } else {
            error = true
        }
        loading = false
    }

    GitHubDiagnosticPanel {
        Text(
            "GitHub Services Status",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Text(
            "real-time operational states from githubstatus.com",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Text("Checking service status...", color = palette.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMono)
        } else if (error || statusSummary == null) {
            Text("Failed to retrieve service status.", color = palette.error, fontSize = 11.sp, fontFamily = JetBrainsMono)
        } else {
            val summary = statusSummary!!
            val overallColor = when (summary.indicator) {
                "none" -> GitHubSuccessGreen
                "minor" -> GitHubWarningAmber()
                "major" -> Color(0xFFFF9500)
                "critical" -> GitHubErrorRed
                else -> palette.textSecondary
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, overallColor, RoundedCornerShape(GitHubControlRadius))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.indicator.uppercase(),
                        color = overallColor,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = summary.description,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summary.components.forEach { comp ->
                    val compColor = when (comp.status) {
                        "operational" -> GitHubSuccessGreen
                        "degraded_performance" -> GitHubWarningAmber()
                        "partial_outage" -> Color(0xFFFF9500)
                        "major_outage" -> GitHubErrorRed
                        else -> palette.textMuted
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comp.name,
                            color = palette.textSecondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp
                        )
                        Text(
                            text = comp.status.replace('_', ' '),
                            color = compColor,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkPingPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    
    var isPinging by remember { mutableStateOf(false) }
    val pings = remember { mutableStateListOf<Long>() }
    var lastPing by remember { mutableStateOf<Long?>(null) }
    var failedCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(isPinging) {
        if (!isPinging) return@LaunchedEffect
        while (true) {
            val start = System.currentTimeMillis()
            var success = false
            try {
                val url = URL("https://api.github.com/zen")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2500
                    readTimeout = 2500
                    setRequestProperty("User-Agent", "GlassFiles")
                }
                val code = conn.responseCode
                conn.disconnect()
                success = code == 200
            } catch (_: Exception) {}
            val duration = System.currentTimeMillis() - start
            if (success) {
                pings.add(duration)
                lastPing = duration
            } else {
                pings.add(-1L)
                lastPing = null
                failedCount++
            }
            if (pings.size > 25) {
                pings.removeAt(0)
            }
            kotlinx.coroutines.delay(1500)
        }
    }
    
    val completedPings = pings.filter { it > 0 }
    val avgPing = if (completedPings.isEmpty()) 0L else completedPings.average().toLong()
    
    GitHubDiagnosticPanel {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "ping connection check",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = "real-time latency to api.github.com",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }
            GitHubTerminalButton(
                label = if (isPinging) "stop" else "start",
                onClick = { isPinging = !isPinging },
                color = if (isPinging) palette.error else palette.accent
            )
            GitHubTerminalButton(
                label = "clear",
                onClick = {
                    pings.clear()
                    lastPing = null
                    failedCount = 0
                },
                color = palette.textSecondary,
                enabled = pings.isNotEmpty()
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GitHubRateBox("last ping", lastPing?.let { "$it ms" } ?: if (pings.isNotEmpty() && lastPing == null) "timeout" else "n/a")
            GitHubRateBox("avg latency", if (avgPing > 0) "$avgPing ms" else "n/a")
            GitHubRateBox("failed", failedCount.toString())
            GitHubRateBox("packets", pings.size.toString())
        }
        
        Spacer(Modifier.height(10.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(GitHubControlRadius))
                .background(palette.background)
                .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                .padding(8.dp)
        ) {
            if (pings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "tap START to begin connection diagnostics",
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val maxVal = pings.filter { it > 0 }.maxOrNull()?.toFloat()?.coerceAtLeast(150f) ?: 300f
                    
                    // Draw horizontal grid lines
                    val gridLines = 3
                    val paint = android.graphics.Paint().apply {
                        color = palette.textMuted.copy(alpha = 0.35f).toArgb()
                        textSize = with(density) { 8.sp.toPx() }
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    
                    for (i in 0..gridLines) {
                        val y = height * i / gridLines
                        drawLine(
                            color = palette.border.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                        // Label for grid line
                        val labelVal = (maxVal - (maxVal * i / gridLines)).toInt()
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "${labelVal}ms",
                                4.dp.toPx(),
                                y - 4.dp.toPx(),
                                paint
                            )
                        }
                    }
                    
                    val spacing = width / 24f // max 25 points
                    val points = pings.mapIndexed { idx, ping ->
                        val x = idx * spacing
                        val y = if (ping <= 0) {
                            height
                        } else {
                            height - (ping.toFloat() / maxVal * height).coerceIn(0f, height)
                        }
                        Offset(x, y)
                    }
                    
                    // Draw neon glow path
                    if (points.isNotEmpty()) {
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { pt ->
                                lineTo(pt.x, pt.y)
                            }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    palette.accent.copy(alpha = 0.18f),
                                    palette.accent.copy(alpha = 0.0f)
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )
                    }
                    
                    // Draw line segments
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val isP1Failed = pings[i] <= 0
                        val isP2Failed = pings[i + 1] <= 0
                        
                        val segmentColor = if (isP1Failed || isP2Failed) Color(0xFFFF3B30) else palette.accent
                        drawLine(
                            color = segmentColor,
                            start = p1,
                            end = p2,
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    
                    // Draw circles
                    points.forEachIndexed { idx, pt ->
                        val failed = pings[idx] <= 0
                        drawCircle(
                            color = if (failed) Color(0xFFFF3B30) else palette.accent,
                            radius = if (failed) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
