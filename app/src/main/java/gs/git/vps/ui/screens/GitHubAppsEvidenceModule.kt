package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GitHubAiIntegration
import gs.git.vps.data.github.model.GHAppInstallation
import gs.git.vps.data.github.model.GHAppMetadataResult
import gs.git.vps.data.github.model.GHObservedApp
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoAppsEvidence
import gs.git.vps.data.github.model.GHWorkflowAppEvidence
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import java.util.Locale

@Composable
internal fun GitHubAppCatalogCard(
    provider: GitHubAiIntegration,
    metadataResult: GHAppMetadataResult?,
    installation: GHAppInstallation?,
    loading: Boolean,
    onInstall: () -> Unit,
    onOpenApp: () -> Unit,
    onGuide: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    val metadata = metadataResult?.app
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, if (metadata != null) palette.accent.copy(alpha = 0.55f) else palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = metadata?.name?.ifBlank { provider.title } ?: provider.title,
                    color = palette.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "github.com/apps/${metadata?.slug ?: provider.appSlug}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppsApiPill(
                label = when {
                    loading && metadataResult == null -> "loading API"
                    metadata != null -> "metadata HTTP ${metadataResult?.code ?: 0}"
                    else -> "API ${metadataResult?.code ?: "?"}"
                },
                color = if (metadata != null) palette.accent else palette.warning,
            )
        }

        when {
            loading && metadataResult == null -> Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "GET /apps/${provider.appSlug}")
            }
            metadata != null -> {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    AppsApiPill("app #${metadata.id}", palette.textSecondary)
                    AppsApiPill("owner ${metadata.ownerLogin}", palette.accent)
                    AppsApiPill("updated ${metadata.updatedAt.appsApiShortDate()}", palette.textMuted)
                    if (installation != null) {
                        AppsApiPill(
                            "installation API ${installation.repositorySelection.ifBlank { "selection unknown" }}",
                            palette.accent,
                        )
                    }
                }
                if (metadata.description.isNotBlank()) {
                    Text(
                        text = metadata.description,
                        color = palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "permissions (${metadata.permissions.size}): " +
                        metadata.permissions.joinToString("  ") { "${it.first}:${it.second}" },
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "events (${metadata.events.size}): ${metadata.events.joinToString(", ")}",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "API fact: app identity and declared capabilities. Installation state is not part of this response.",
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                )
                if (installation != null) {
                    Text(
                        text = "Separate API fact: GET /user/installations returned installation #${installation.id} for this App and current App user token.",
                        color = palette.accent,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                    )
                }
            }
            else -> Text(
                text = metadataResult?.error?.take(220) ?: "GitHub App metadata unavailable",
                color = palette.warning,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GitHubTerminalButton(
                if (installation != null) "manage selected repos ->" else "install / choose repos ->",
                onInstall,
                color = palette.accent,
            )
            GitHubTerminalButton("app", onOpenApp, color = palette.textSecondary)
            GitHubTerminalButton("guide", onGuide, color = palette.textSecondary)
        }
    }
}

@Composable
internal fun GitHubRepoAppsEvidencePanel(
    repos: List<GHRepo>,
    selectedRepoFullName: String,
    evidence: GHRepoAppsEvidence?,
    loading: Boolean,
    onSelectRepo: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("repository API evidence", color = palette.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("checks + workflows + secret names", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            }
            GitHubTerminalButton("refresh", onRefresh, color = palette.accent, enabled = !loading && selectedRepoFullName.isNotBlank())
        }

        if (repos.isEmpty()) {
            Text("No repositories returned by GET /user/repos.", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repos.forEach { repo ->
                val selected = repo.fullName == selectedRepoFullName
                Box(
                    Modifier
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .border(1.dp, if (selected) palette.accent else palette.border, RoundedCornerShape(GitHubControlRadius))
                        .background(if (selected) palette.accent.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onSelectRepo(repo.fullName) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        repo.fullName,
                        color = if (selected) palette.accent else palette.textSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "reading repository APIs...")
            }
            return@Column
        }
        if (evidence == null) {
            Text("Select a repository to inspect real API evidence.", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp)
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            AppsApiPill("checks HTTP ${evidence.checksCode}", apiColor(evidence.checksCode))
            AppsApiPill("workflows HTTP ${evidence.workflowsCode}", apiColor(evidence.workflowsCode))
            AppsApiPill("secrets HTTP ${evidence.secretsCode}", apiColor(evidence.secretsCode))
            AppsApiPill("rate ${gs.git.vps.data.github.GitHubManager.getRateLimitRemaining()}", palette.textMuted)
        }
        Text(
            text = "${evidence.repoFullName} · ${evidence.branch} · ${evidence.commitsScanned} recent commits scanned",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        AppsApiSubheading("observed GitHub Apps in check-runs", "GET /commits/{sha}/check-runs")
        if (evidence.observedApps.isEmpty()) {
            AppsApiEmptyOrError(
                error = evidence.checksError,
                empty = "No App check-runs were observed on the scanned commits. This does not mean that no Apps are installed.",
            )
        } else {
            evidence.observedApps.forEach { ObservedAppRow(it) }
            if (evidence.checksError.isNotBlank()) AppsApiWarning(evidence.checksError)
        }

        AppsApiSubheading("workflow configuration", "GET /actions/workflows + Contents API")
        Text(
            "${evidence.workflowsTotal} workflows returned; ${evidence.workflowEvidence.size} definitions contain Action references",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
        if (evidence.workflowEvidence.isEmpty()) {
            AppsApiEmptyOrError(evidence.workflowsError, "No readable workflow Action references found.")
        } else {
            evidence.workflowEvidence.forEach { WorkflowEvidenceRow(it) }
            if (evidence.workflowsError.isNotBlank()) AppsApiWarning(evidence.workflowsError)
        }

        AppsApiSubheading("provider secret names", "GET /actions/secrets")
        if (evidence.providerSecretNames.isEmpty()) {
            AppsApiEmptyOrError(
                evidence.secretsError,
                "No Claude/OpenAI-related secret names returned. GitHub never exposes secret values.",
            )
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                evidence.providerSecretNames.forEach { AppsApiPill(it, palette.warning) }
            }
            if (evidence.secretsError.isNotBlank()) AppsApiWarning(evidence.secretsError)
        }

        Text(
            text = "Evidence is factual but repository-scoped: observed activity and configuration are not promoted to an installation claim.",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ObservedAppRow(item: GHObservedApp) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                item.app.name.ifBlank { item.app.slug },
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppsApiPill("app #${item.app.id}", palette.accent)
            AppsApiPill("runs ${item.checkRunCount}", palette.textSecondary)
        }
        Text(
            "${item.app.slug} · owner ${item.app.ownerLogin} · ${item.lastStatus}/${item.lastConclusion.ifBlank { "-" }}",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "checks: ${item.checkNames.joinToString(", ")} · commit ${item.lastCommitSha.take(7)} · ${item.lastSeenAt.appsApiShortDate()}",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkflowEvidenceRow(item: GHWorkflowAppEvidence) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth().border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                item.name,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppsApiPill(item.state.ifBlank { "unknown" }, palette.textSecondary)
        }
        Text(item.path, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.providerIds.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.providerIds.forEach { AppsApiPill("provider $it", palette.accent) }
            }
        }
        Text(
            "uses: ${item.actionReferences.joinToString(", ")}",
            color = palette.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppsApiSubheading(title: String, endpoint: String) {
    val palette = AiModuleTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        Text(endpoint, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 9.sp)
    }
}

@Composable
private fun AppsApiEmptyOrError(error: String, empty: String) {
    val palette = AiModuleTheme.colors
    Text(
        text = if (error.isNotBlank()) error.take(240) else empty,
        color = if (error.isNotBlank()) palette.warning else palette.textMuted,
        fontFamily = JetBrainsMono,
        fontSize = 10.sp,
    )
}

@Composable
private fun AppsApiWarning(message: String) {
    Text(message.take(240), color = AiModuleTheme.colors.warning, fontFamily = JetBrainsMono, fontSize = 10.sp)
}

@Composable
private fun AppsApiPill(label: String, color: Color) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, color.copy(alpha = 0.65f), RoundedCornerShape(GitHubControlRadius))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            label.lowercase(Locale.US),
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun apiColor(code: Int): Color = when (code) {
    in 200..399 -> AiModuleTheme.colors.accent
    401, 403 -> AiModuleTheme.colors.warning
    else -> AiModuleTheme.colors.error
}

private fun String.appsApiShortDate(): String =
    takeIf { it.length >= 10 }?.take(10) ?: ifBlank { "unknown" }
