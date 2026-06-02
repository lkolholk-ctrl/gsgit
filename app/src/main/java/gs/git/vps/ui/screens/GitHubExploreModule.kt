package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.Strings
import gs.git.vps.data.github.GHCodeResult
import gs.git.vps.data.github.GHLicense
import gs.git.vps.data.github.GHLicenseDetail
import gs.git.vps.data.github.GHOrg
import gs.git.vps.data.github.GHRepo
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleKeyValueRow
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

// ═══════════════════════════════════
// Code Search Tab (used inside RepoDetailScreen — keeps its own
// header so it can be embedded as a tab pane, not a full screen)
// ═══════════════════════════════════

@Composable
internal fun CodeSearchTab(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GHCodeResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val palette = AiModuleTheme.colors

    Column(Modifier.fillMaxSize().background(palette.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                    .background(palette.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = Strings.ghSearchCodeHint,
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (query.length >= 2) palette.accent else palette.surface)
                    .border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                    .clickable(enabled = query.length >= 2 && !searching) {
                        searching = true
                        searched = true
                        scope.launch {
                            results = GitHubManager.searchCode(context, query, repo.owner, repo.name)
                            searching = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (searching) {
                    AiModuleSpinner()
                } else {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = "search",
                        modifier = Modifier.size(16.dp),
                        tint = if (query.length >= 2) palette.background else palette.textSecondary,
                    )
                }
            }
        }
        AiModuleHairline()

        when {
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "searching code…")
            }
            searched && results.isEmpty() -> GitHubMonoEmpty(
                title = Strings.ghNoResults.lowercase(),
                subtitle = "try another query or scope",
            )
            results.isNotEmpty() -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                items(results) { r -> CodeResultRow(r); AiModuleHairline() }
            }
        }
    }
}

@Composable
private fun CodeResultRow(r: GHCodeResult) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ">",
            color = palette.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.width(14.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                r.name,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 1.3.em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                r.path,
                color = palette.textMuted,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 1.3.em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ═══════════════════════════════════
// Starred Repos Screen
// ═══════════════════════════════════

@Composable
internal fun StarredScreen(onBack: () -> Unit, onRepoClick: (GHRepo) -> Unit) {
    val context = LocalContext.current
    var repos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        repos = GitHubManager.getStarredRepos(context)
        loading = false
    }

    GitHubScreenFrame(
        title = "> ${Strings.ghStarredRepos.lowercase()}",
        onBack = onBack,
        subtitle = if (loading) "loading…" else "${repos.size} starred",
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading stars…")
            }
            repos.isEmpty() -> GitHubMonoEmpty(
                title = "no starred repositories",
                subtitle = "stars will appear here once you star a repo",
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                items(repos, key = { "${it.owner}/${it.name}" }) { repo ->
                    RepoCard(repo, onClick = { onRepoClick(repo) }, showStats = true)
                    AiModuleHairline()
                }
            }
        }
    }
}

// ═══════════════════════════════════
// Organizations Screen
// ═══════════════════════════════════

@Composable
internal fun OrgsScreen(onBack: () -> Unit, onRepoClick: (GHRepo) -> Unit) {
    val context = LocalContext.current
    var orgs by remember { mutableStateOf<List<GHOrg>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedOrg by remember { mutableStateOf<GHOrg?>(null) }
    var orgRepos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var loadingRepos by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        orgs = GitHubManager.getOrganizations(context)
        loading = false
    }
    LaunchedEffect(selectedOrg) {
        if (selectedOrg != null) {
            loadingRepos = true
            orgRepos = GitHubManager.getOrgRepos(context, selectedOrg!!.login)
            loadingRepos = false
        }
    }

    if (selectedOrg != null) {
        GitHubScreenFrame(
            title = "@${selectedOrg!!.login}",
            onBack = { selectedOrg = null; orgRepos = emptyList() },
            subtitle = if (loadingRepos) "loading repos…" else "${orgRepos.size} repositories",
        ) {
            when {
                loadingRepos -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AiModuleSpinner(label = "loading repos…")
                }
                orgRepos.isEmpty() -> GitHubMonoEmpty(
                    title = "no repositories",
                    subtitle = "this organization has no public repos",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                ) {
                    items(orgRepos, key = { "${it.owner}/${it.name}" }) { repo ->
                        RepoCard(repo, onClick = { onRepoClick(repo) })
                        AiModuleHairline()
                    }
                }
            }
        }
        return
    }

    GitHubScreenFrame(
        title = "> ${Strings.ghOrganizations.lowercase()}",
        onBack = onBack,
        subtitle = if (loading) "loading…" else "${orgs.size} orgs",
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading orgs…")
            }
            orgs.isEmpty() -> GitHubMonoEmpty(
                title = Strings.ghNoResults.lowercase(),
                subtitle = "you are not a member of any organisation",
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                items(orgs, key = { it.login }) { org ->
                    OrgRow(org, onClick = { selectedOrg = org })
                    AiModuleHairline()
                }
            }
        }
    }
}

@Composable
private fun OrgRow(org: GHOrg, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            org.avatarUrl,
            org.login,
            Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "@${org.login}",
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (org.description.isNotBlank()) {
                Text(
                    org.description,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    lineHeight = 1.3.em,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = palette.textSecondary,
        )
    }
}

@Composable
internal fun EmojisScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var emojis by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { emojis = GitHubManager.getEmojis(context); loading = false }
    GitHubScreenFrame(title = "> emojis", onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                AiModuleSearchField(value = query, onValueChange = { query = it }, placeholder = "search emojis")
            }
            AiModuleHairline()
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading emojis") }
                emojis.isEmpty() -> GitHubMonoEmpty(title = "no emojis loaded")
                else -> {
                    val filtered = emojis.filter { (name, _) -> query.isBlank() || name.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered.entries.toList()) { (name, url) ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AsyncImage(model = url, contentDescription = name, modifier = Modifier.size(20.dp))
                                Text(name, fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var licenses by remember { mutableStateOf<List<GHLicense>>(emptyList()) }
    var selectedLicense by remember { mutableStateOf<GHLicenseDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { licenses = GitHubManager.getLicenses(context); loading = false }
    fun handleBack() { if (selectedLicense != null) selectedLicense = null else onBack() }
    BackHandler(onBack = ::handleBack)
    GitHubScreenFrame(title = "> licenses", onBack = ::handleBack) {
        when {
            selectedLicense != null -> {
                val l = selectedLicense!!
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(l.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AiModuleTheme.colors.textPrimary, fontFamily = JetBrainsMono)
                    AiModuleKeyValueRow("key", l.key)
                    AiModuleKeyValueRow("spdx", l.spdxId)
                    if (l.description.isNotBlank()) Text(l.description, fontSize = 12.sp, color = AiModuleTheme.colors.textSecondary)
                    if (l.body.isNotBlank()) {
                        AiModuleSectionLabel(text = "full text")
                        Text(l.body, fontSize = 11.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 16.sp, fontFamily = JetBrainsMono)
                    }
                }
            }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading licenses") }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        AiModuleSearchField(value = query, onValueChange = { query = it }, placeholder = "search licenses")
                    }
                    AiModuleHairline()
                    val filtered = licenses.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filtered) { license ->
                            Row(Modifier.fillMaxWidth().clickable {
                                scope.launch { selectedLicense = GitHubManager.getLicense(context, license.key) }
                            }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(license.name, fontSize = 12.sp, fontFamily = JetBrainsMono, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                if (license.featured) Text("★", fontSize = 12.sp, color = AiModuleTheme.colors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
