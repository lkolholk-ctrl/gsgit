package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.github.GHFollowerEntry
import gs.git.vps.data.github.GHRepo
import gs.git.vps.data.github.GHUserProfile
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleKeyValueRow
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    username: String,
    onBack: () -> Unit,
    onRepoClick: (GHRepo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<GHUserProfile?>(null) }
    var repos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) } // 0=repos, 1=starred, 2=followers, 3=following
    var starredRepos by remember { mutableStateOf<List<GHRepo>?>(null) }
    var followers by remember { mutableStateOf<List<GHFollowerEntry>?>(null) }
    var following by remember { mutableStateOf<List<GHFollowerEntry>?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    val listState = rememberSaveable(username, saver = LazyListState.Saver) { LazyListState(0, 0) }
    val cachedSelf = GitHubManager.getCachedUser(context)?.login

    LaunchedEffect(username) {
        profile = GitHubManager.getUserProfile(context, username)
        repos = GitHubManager.getUserRepos(context, username)
        isFollowing = GitHubManager.isFollowing(context, username)
        loading = false
    }

    LaunchedEffect(tab) {
        if (tab == 1 && starredRepos == null) starredRepos = GitHubManager.getStarredRepos(context)
        if (tab == 2 && followers == null) followers = GitHubManager.getFollowersNative(context)
        if (tab == 3 && following == null) following = GitHubManager.getFollowingNative(context)
    }

    val palette = AiModuleTheme.colors

    GitHubScreenFrame(
        title = "@$username",
        onBack = onBack,
        subtitle = profile?.let { "profile · ${formatGitHubNumber(it.publicRepos)} repos" } ?: "profile",
        trailing = {
            if (profile?.login == cachedSelf) {
                GitHubTopBarAction(
                    glyph = GhGlyphs.EDIT,
                    onClick = { showEdit = true },
                    tint = palette.textSecondary,
                    contentDescription = "edit profile",
                )
            }
            if (profile != null && profile!!.login != cachedSelf) {
                GitHubTopBarAction(
                    glyph = if (isFollowing) GhGlyphs.MINUS else GhGlyphs.PLUS,
                    onClick = {
                        scope.launch {
                            if (isFollowing) GitHubManager.unfollowUser(context, username)
                            else GitHubManager.followUser(context, username)
                            isFollowing = !isFollowing
                        }
                    },
                    tint = if (isFollowing) palette.textSecondary else palette.accent,
                    contentDescription = if (isFollowing) "unfollow" else "follow",
                )
            }
        },
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading profile…")
            }
            profile == null -> GitHubMonoEmpty(
                title = "user not found",
                subtitle = "@$username does not exist or is private",
            )
            else -> {
                val p = profile!!
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                ) {
                    item {
                        ProfileHeader(p, isSelf = p.login == cachedSelf)
                        AiModuleHairline()
                    }
                    item {
                        ProfileMetaBlock(p)
                        AiModuleHairline()
                    }
                    item {
                        ProfileStatsRow(p, onSelect = { tab = it })
                        AiModuleHairline()
                    }
                    item {
                        ProfileTabBar(tab, onSelect = { tab = it })
                    }
                    when (tab) {
                        0 -> {
                            if (repos.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                        Text("no public repositories", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                items(repos, key = { "${it.owner}/${it.name}" }) { repo ->
                                    RepoCard(repo, onClick = { onRepoClick(repo) })
                                    AiModuleHairline()
                                }
                            }
                        }
                        1 -> {
                            val sr = starredRepos
                            if (sr == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") } }
                            else if (sr.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("no starred repos", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp) } }
                            else items(sr, key = { "${it.owner}/${it.name}" }) { repo -> RepoCard(repo, onClick = { onRepoClick(repo) }); AiModuleHairline() }
                        }
                        2 -> {
                            val fl = followers
                            if (fl == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") } }
                            else if (fl.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("no followers", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp) } }
                            else items(fl, key = { it.login }) { entry -> FollowerRow(entry, onClick = {}); AiModuleHairline() }
                        }
                        3 -> {
                            val fg = following
                            if (fg == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") } }
                            else if (fg.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("not following anyone", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp) } }
                            else items(fg, key = { it.login }) { entry -> FollowerRow(entry, onClick = {}); AiModuleHairline() }
                        }
                    }
                }
            }
        }
    }

    if (showEdit && profile != null) {
        ProfileEditSheet(
            profile = profile!!,
            onDismiss = { showEdit = false },
            onSave = { name, bio, company, location, blog ->
                scope.launch {
                    val ok = GitHubManager.updateCurrentUserProfile(context, name, bio, company, location, blog)
                    if (ok) {
                        profile = GitHubManager.getCurrentUserProfile(context)
                        Toast.makeText(context, "profile updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "update failed", Toast.LENGTH_SHORT).show()
                    }
                    showEdit = false
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(profile: GHUserProfile, isSelf: Boolean) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            profile.avatarUrl, profile.login,
            Modifier.size(56.dp).clip(RoundedCornerShape(GitHubControlRadius)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name.ifBlank { profile.login },
                color = palette.textPrimary, fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 1.25.em,
            )
            Text(
                text = "@${profile.login}",
                color = palette.textSecondary, fontFamily = JetBrainsMono, fontSize = 12.sp,
            )
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = profile.bio,
                    color = palette.textSecondary, fontFamily = JetBrainsMono,
                    fontSize = 12.sp, lineHeight = 1.4.em,
                )
            }
        }
        if (isSelf) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accent.copy(alpha = 0.1f))
                    .border(1.dp, palette.accent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("you", fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = palette.accent)
            }
        }
    }
}

@Composable
private fun ProfileMetaBlock(profile: GHUserProfile) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (profile.company.isNotBlank()) {
            AiModuleKeyValueRow(key = "company", value = profile.company)
        }
        if (profile.location.isNotBlank()) {
            AiModuleKeyValueRow(key = "location", value = profile.location)
        }
        if (profile.blog.isNotBlank()) {
            AiModuleKeyValueRow(key = "blog", value = profile.blog, valueColor = AiModuleTheme.colors.accent)
        }
        if (profile.email.isNotBlank()) {
            AiModuleKeyValueRow(key = "email", value = profile.email, valueColor = AiModuleTheme.colors.accent)
        }
        if (profile.twitterUsername.isNotBlank()) {
            AiModuleKeyValueRow(key = "twitter", value = "@${profile.twitterUsername}")
        }
        if (profile.planName.isNotBlank()) {
            AiModuleKeyValueRow(key = "plan", value = profile.planName)
        }
        if (profile.createdAt.isNotBlank()) {
            AiModuleKeyValueRow(key = "joined", value = profile.createdAt.take(10))
        }
    }
}

@Composable
private fun ProfileStatsRow(profile: GHUserProfile, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatCell("repos", formatGitHubNumber(profile.publicRepos), Modifier.weight(1f).clickable { onSelect(0) })
        StatCell("followers", formatGitHubNumber(profile.followers), Modifier.weight(1f).clickable { onSelect(2) })
        StatCell("following", formatGitHubNumber(profile.following), Modifier.weight(1f).clickable { onSelect(3) })
        StatCell("gists", formatGitHubNumber(profile.publicGists), Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = AiModuleTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 0.4.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileTabBar(selected: Int, onSelect: (Int) -> Unit) {
    val palette = AiModuleTheme.colors
    val tabs = listOf("repos", "starred", "followers", "following")
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        tabs.forEachIndexed { i, tab ->
            val sel = selected == i
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = if (i == 0) 8.dp else 0.dp, topEnd = if (i == 3) 8.dp else 0.dp))
                    .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                    .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(topStart = if (i == 0) 8.dp else 0.dp, topEnd = if (i == 3) 8.dp else 0.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(tab, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
            }
        }
    }
}

@Composable
private fun FollowerRow(entry: GHFollowerEntry, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(entry.avatarUrl, entry.login, Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)))
        Spacer(Modifier.width(10.dp))
        Text(entry.login, color = palette.textPrimary, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileEditSheet(
    profile: GHUserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, company: String, location: String, blog: String) -> Unit
) {
    val palette = AiModuleTheme.colors
    var name by remember { mutableStateOf(profile.name) }
    var bio by remember { mutableStateOf(profile.bio) }
    var company by remember { mutableStateOf(profile.company) }
    var location by remember { mutableStateOf(profile.location) }
    var blog by remember { mutableStateOf(profile.blog) }
    var saving by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(palette.background.copy(alpha = 0.85f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("edit profile", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.accent)
            EditField("name", name) { name = it }
            EditField("bio", bio, singleLine = false) { bio = it }
            EditField("company", company) { company = it }
            EditField("location", location) { location = it }
            EditField("blog", blog) { blog = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(palette.surface).border(1.dp, palette.border, RoundedCornerShape(6.dp)).clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("cancel", fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.textSecondary) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(palette.accent.copy(alpha = 0.15f)).border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                        .clickable(enabled = !saving) {
                            saving = true
                            onSave(name, bio, company, location, blog)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (saving) "saving…" else "save", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = palette.accent)
                }
            }
        }
    }
}

@Composable
private fun EditField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    val palette = AiModuleTheme.colors
    Column {
        Text(label, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.fillMaxWidth().height(if (singleLine) 32.dp else 64.dp).clip(RoundedCornerShape(4.dp)).background(palette.background).border(1.dp, palette.border, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = palette.textPrimary),
                singleLine = singleLine,
                modifier = Modifier.fillMaxSize()
            )
            if (value.isEmpty()) {
                Text(label, fontFamily = JetBrainsMono, fontSize = 12.sp, color = palette.textMuted.copy(alpha = 0.4f))
            }
        }
    }
}

private fun copyClipboard(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}
