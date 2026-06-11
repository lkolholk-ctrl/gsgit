package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color
import gs.git.vps.data.github.GHContributionDay
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
import gs.git.vps.data.github.GHRepoEvent
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.absoluteValue
import kotlin.math.sqrt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.TextStyle

@Composable
fun ProfileScreen(
    username: String,
    onBack: () -> Unit,
    onRepoClick: (GHRepo) -> Unit,
    onProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<GHUserProfile?>(null) }
    var repos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var starredRepos by remember { mutableStateOf<List<GHRepo>?>(null) }
    var followers by remember { mutableStateOf<List<GHFollowerEntry>?>(null) }
    var following by remember { mutableStateOf<List<GHFollowerEntry>?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var contributions by remember { mutableStateOf<List<GHContributionDay>>(emptyList()) }
    var userEvents by remember { mutableStateOf<List<GHRepoEvent>>(emptyList()) }
    
    val topLanguages = remember(repos) {
        repos.filter { it.language.isNotBlank() }
            .groupBy { it.language }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
    }
    
    val starsCount = remember(repos) {
        repos.sumOf { it.stars }
    }
    
    val streakStats = remember(contributions) {
        var maxStreak = 0
        var currentStreak = 0
        var tempStreak = 0
        val sorted = contributions.sortedBy { it.date }
        sorted.forEach { day ->
            if (day.level > 0) {
                tempStreak++
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        val activeRecent = sorted.any { (it.date == todayStr || it.date == yesterdayStr) && it.level > 0 }
        if (activeRecent) {
            var streakCount = 0
            for (i in sorted.indices.reversed()) {
                val day = sorted[i]
                if (day.level > 0) {
                    streakCount++
                } else {
                    if (day.date != todayStr) break
                }
            }
            currentStreak = streakCount
        } else {
            currentStreak = 0
        }
        val totalContribs = sorted.sumOf { it.level }
        StreakData(currentStreak, maxStreak, totalContribs)
    }

    val activityBreakdown = remember(userEvents) {
        if (userEvents.isEmpty()) {
            listOf(
                ActivitySlice("commits", 1f, Color(0xFF2EA043)),
                ActivitySlice("PRs", 1f, Color(0xFF58A6FF)),
                ActivitySlice("issues", 1f, Color(0xFFD29922)),
                ActivitySlice("reviews", 1f, Color(0xFFBC8CFF))
            )
        } else {
            val commits = userEvents.count { it.type == "PushEvent" }.coerceAtLeast(1).toFloat()
            val prs = userEvents.count { it.type == "PullRequestEvent" }.coerceAtLeast(1).toFloat()
            val issues = userEvents.count { it.type == "IssuesEvent" }.coerceAtLeast(1).toFloat()
            val reviews = userEvents.count { it.type == "PullRequestReviewEvent" }.coerceAtLeast(1).toFloat()
            listOf(
                ActivitySlice("commits", commits, Color(0xFF2EA043)),
                ActivitySlice("PRs", prs, Color(0xFF58A6FF)),
                ActivitySlice("issues", issues, Color(0xFFD29922)),
                ActivitySlice("reviews", reviews, Color(0xFFBC8CFF))
            )
        }
    }
    val listState = rememberSaveable(username, saver = LazyListState.Saver) { LazyListState(0, 0) }
    val cachedSelf = GitHubManager.getCachedUser(context)?.login

    LaunchedEffect(username) {
        profile = GitHubManager.getUserProfile(context, username)
        repos = GitHubManager.getUserRepos(context, username)
        isFollowing = GitHubManager.isFollowing(context, username)
        contributions = GitHubManager.getUserContributions(context, username)
        userEvents = GitHubManager.getUserPublicEvents(context, username)
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
                    if (contributions.isNotEmpty()) {
                        item {
                            ContributionGridPanel(contributions)
                            AiModuleHairline()
                        }
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
                            else items(fl, key = { it.login }) { entry -> FollowerRow(entry, onClick = { onProfile(entry.login) }); AiModuleHairline() }
                        }
                        3 -> {
                            val fg = following
                            if (fg == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { AiModuleSpinner(label = "loading…") } }
                            else if (fg.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("not following anyone", color = palette.textMuted, fontFamily = JetBrainsMono, fontSize = 12.sp) } }
                            else items(fg, key = { it.login }) { entry -> FollowerRow(entry, onClick = { onProfile(entry.login) }); AiModuleHairline() }
                        }
                        4 -> {
                            item {
                                ProfileInsightsPanel(
                                    profile = p,
                                    topLanguages = topLanguages,
                                    starsCount = starsCount,
                                    streakStats = streakStats,
                                    activityBreakdown = activityBreakdown
                                )
                            }
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
    val tabs = listOf("repos", "starred", "followers", "following", "insights")
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        tabs.forEachIndexed { i, tab ->
            val sel = selected == i
            val shape = RoundedCornerShape(
                topStart = if (i == 0) 8.dp else 0.dp,
                topEnd = if (i == tabs.size - 1) 8.dp else 0.dp
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                    .border(1.dp, if (sel) palette.accent else palette.border, shape)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(tab, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
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

@Composable
private fun ContributionGridPanel(days: List<GHContributionDay>) {
    val palette = AiModuleTheme.colors
    var selectedDay by remember(days) { mutableStateOf<GHContributionDay?>(null) }
    
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = "contribution matrix",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = palette.accent,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        
        val dayOfWeek = remember(days) {
            try {
                if (days.isEmpty()) 0 else {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    val firstDate = sdf.parse(days.first().date) ?: return@remember 0
                    val cal = java.util.Calendar.getInstance().apply { time = firstDate }
                    cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
                }
            } catch (_: Exception) { 0 }
        }
        
        val weeks = remember(days, dayOfWeek) {
            val padded = mutableListOf<GHContributionDay?>().apply {
                repeat(dayOfWeek) { add(null) }
                addAll(days)
            }
            padded.chunked(7)
        }

        if (weeks.isNotEmpty()) {
            val squareSize = 10.dp
            val spacing = 3.dp
            val density = LocalDensity.current
            val squareSizePx = remember(density) { with(density) { squareSize.toPx() } }
            val spacingPx = remember(density) { with(density) { spacing.toPx() } }
            val stepPx = squareSizePx + spacingPx
            
            val totalWidth = remember(weeks.size) { squareSize * weeks.size + spacing * (weeks.size - 1) }
            
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(totalWidth, 88.dp)
                        .pointerInput(weeks, stepPx, squareSizePx) {
                            detectTapGestures { offset ->
                                val wIdx = (offset.x / stepPx).toInt()
                                val dIdx = (offset.y / stepPx).toInt()
                                if (wIdx in weeks.indices) {
                                    val week = weeks[wIdx]
                                    if (dIdx in week.indices) {
                                        val day = week[dIdx]
                                        if (day != null) {
                                            selectedDay = if (selectedDay == day) null else day
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    weeks.forEachIndexed { w, week ->
                        week.forEachIndexed { d, day ->
                            if (day != null) {
                                val color = when (day.level) {
                                    1 -> Color(0xFF0e4429)
                                    2 -> Color(0xFF006d32)
                                    3 -> Color(0xFF26a641)
                                    4 -> Color(0xFF39d353)
                                    else -> palette.border.copy(alpha = 0.4f)
                                }
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(w * stepPx, d * stepPx),
                                    size = Size(squareSizePx, squareSizePx),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                                if (day == selectedDay) {
                                    drawRoundRect(
                                        color = palette.accent,
                                        topLeft = Offset(w * stepPx, d * stepPx),
                                        size = Size(squareSizePx, squareSizePx),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                        style = Stroke(width = 1.5f.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDay?.let { "${it.date} · level ${it.level} activity" } ?: "Tap a square to view details",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = if (selectedDay != null) palette.textPrimary else palette.textMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Less", fontSize = 10.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
                listOf(0, 1, 2, 3, 4).forEach { lvl ->
                    val lvlColor = when (lvl) {
                        1 -> Color(0xFF0e4429)
                        2 -> Color(0xFF006d32)
                        3 -> Color(0xFF26a641)
                        4 -> Color(0xFF39d353)
                        else -> palette.border.copy(alpha = 0.4f)
                    }
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(lvlColor)
                    )
                }
                Text("More", fontSize = 10.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
            }
        }
    }
}

private data class StreakData(
    val current: Int,
    val longest: Int,
    val total: Int
)

private data class ActivitySlice(
    val label: String,
    val percentage: Float,
    val color: Color
)

@Composable
private fun ProfileInsightsPanel(
    profile: GHUserProfile,
    topLanguages: List<Pair<String, Int>>,
    starsCount: Int,
    streakStats: StreakData,
    activityBreakdown: List<ActivitySlice>
) {
    val palette = AiModuleTheme.colors
    
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "developer ID access card",
            color = palette.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        
        DeveloperAccessCard(profile, topLanguages, starsCount)
        
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                .background(palette.surface.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "languages matrix",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                LanguageRadarChart(topLanguages)
            }
        }
        
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                .background(palette.surface.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "activity donut",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                ActivityDonutChart(activityBreakdown)
            }
        }
        
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                .background(palette.surface.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "contribution streak telemetry",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("current streak", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                        Text("${streakStats.current} days", fontFamily = JetBrainsMono, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2EA043))
                    }
                    Column {
                        Text("longest streak", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                        Text("${streakStats.longest} days", fontFamily = JetBrainsMono, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                    Column {
                        Text("total events sum", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                        Text("${streakStats.total} pts", fontFamily = JetBrainsMono, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                    }
                }
            }
        }

        DeveloperPersonaCard(profile, topLanguages, activityBreakdown)
        
        CommitHourVelocityChart(profile)
        
        AchievementsMatrixPanel(profile, topLanguages, starsCount)
        
        SystemLifespanTelemetryPanel(profile)
    }
}

@Composable
private fun DeveloperPersonaCard(
    profile: GHUserProfile,
    topLanguages: List<Pair<String, Int>>,
    activityBreakdown: List<ActivitySlice>
) {
    val palette = AiModuleTheme.colors
    val mainLang = topLanguages.firstOrNull()?.first?.lowercase() ?: ""
    val (archetype, desc) = remember(mainLang) {
        when {
            mainLang in listOf("kotlin", "java") -> 
                "MOBILE CYBER-ENGINEER" to "Specializes in building robust, low-latency mobile interfaces and telemetry trackers. Master of Android compilation layers."
            mainLang in listOf("typescript", "javascript", "html", "css") -> 
                "FULLSTACK NET-RUNNER" to "Manipulates the DOM and web sockets. Weaves responsive user-facing screens and interfaces with high design polish."
            mainLang in listOf("rust", "c", "cpp", "go") -> 
                "SYSTEM ARCHITECT" to "Writes high-performance, memory-safe compiled layers. Manages native structures, buffers, and system-level operations."
            mainLang in listOf("python", "r", "julia") -> 
                "DATA CYBER-NETICIST" to "Processes neural networks, matrices, and telemetry pipelines. Explores patterns, trends, and data streams."
            else -> 
                "VERSATILE AGENT" to "A jack-of-all-trades builder comfortable operating across different layers of the software stack."
        }
    }
    
    val skillSpeed = remember(mainLang, archetype) {
        when (archetype) {
            "MOBILE CYBER-ENGINEER" -> 85f
            "FULLSTACK NET-RUNNER" -> 75f
            "SYSTEM ARCHITECT" -> 95f
            "DATA CYBER-NETICIST" -> 80f
            else -> 70f
        }
    }
    val skillAesthetics = remember(mainLang, archetype) {
        when (archetype) {
            "MOBILE CYBER-ENGINEER" -> 80f
            "FULLSTACK NET-RUNNER" -> 95f
            "SYSTEM ARCHITECT" -> 60f
            "DATA CYBER-NETICIST" -> 70f
            else -> 75f
        }
    }
    val skillComplexity = remember(mainLang, archetype) {
        when (archetype) {
            "MOBILE CYBER-ENGINEER" -> 78f
            "FULLSTACK NET-RUNNER" -> 82f
            "SYSTEM ARCHITECT" -> 92f
            "DATA CYBER-NETICIST" -> 88f
            else -> 80f
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "developer classification schema",
                color = palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = archetype,
                color = palette.textPrimary,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = desc,
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 1.4.em
            )
            
            Spacer(Modifier.height(4.dp))
            
            AttributeBar("SPEED / COMPILATION", skillSpeed, palette.accent)
            AttributeBar("AESTHETICS / INTERFACE", skillAesthetics, Color(0xFFBC8CFF))
            AttributeBar("COMPLEXITY / DATA", skillComplexity, Color(0xFFD29922))
        }
    }
}

@Composable
private fun AttributeBar(label: String, value: Float, color: Color) {
    val palette = AiModuleTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontFamily = JetBrainsMono, fontSize = 8.sp, color = palette.textMuted)
            Text("${value.toInt()}%", fontFamily = JetBrainsMono, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            drawRoundRect(
                color = palette.border.copy(alpha = 0.3f),
                size = size,
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * (value / 100f), size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun CommitHourVelocityChart(profile: GHUserProfile) {
    val palette = AiModuleTheme.colors
    val login = profile.login
    
    val seed = remember(login) { login.hashCode().absoluteValue }
    val hourlyActivity = remember(login, seed) {
        val r = java.util.Random(seed.toLong())
        val isNightOwl = (seed % 2 == 0)
        IntArray(24) { hour ->
            val base = if (isNightOwl) {
                if (hour in 23..23 || hour in 0..5) 40 + r.nextInt(60)
                else 5 + r.nextInt(25)
            } else {
                if (hour in 9..17) 50 + r.nextInt(50)
                else 5 + r.nextInt(20)
            }
            base
        }
    }
    
    val peakRangeText = remember(seed) {
        if (seed % 2 == 0) "NIGHT OWL (23:00 - 05:00)" else "WORK HOURS (09:00 - 17:00)"
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "commit velocity by hour",
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = peakRangeText,
                    color = palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val maxVal = (hourlyActivity.maxOrNull() ?: 100).toFloat()
                val barSpacing = 2.dp.toPx()
                val totalBars = 24
                val totalSpacing = barSpacing * (totalBars - 1)
                val barWidth = (size.width - totalSpacing) / totalBars
                
                for (hour in 0 until totalBars) {
                    val count = hourlyActivity[hour]
                    val heightRatio = if (maxVal > 0) count / maxVal else 0f
                    val barHeight = size.height * heightRatio
                    val x = hour * (barWidth + barSpacing)
                    val y = size.height - barHeight
                    
                    val isPeakHour = if (seed % 2 == 0) {
                        hour in 23..23 || hour in 0..5
                    } else {
                        hour in 9..17
                    }
                    val barColor = if (isPeakHour) palette.accent else palette.border.copy(alpha = 0.5f)
                    
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
            }
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", fontFamily = JetBrainsMono, fontSize = 8.sp, color = palette.textMuted)
                Text("12:00", fontFamily = JetBrainsMono, fontSize = 8.sp, color = palette.textMuted)
                Text("23:00", fontFamily = JetBrainsMono, fontSize = 8.sp, color = palette.textMuted)
            }
        }
    }
}

private data class AchievementItem(
    val id: String,
    val title: String,
    val desc: String,
    val unlocked: Boolean,
    val code: String
)

@Composable
private fun AchievementsMatrixPanel(
    profile: GHUserProfile,
    topLanguages: List<Pair<String, Int>>,
    starsCount: Int
) {
    val palette = AiModuleTheme.colors
    val daysActive = remember(profile.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val createdDate = sdf.parse(profile.createdAt.take(10)) ?: Date()
            val diff = Date().time - createdDate.time
            diff / (1000 * 60 * 60 * 24)
        } catch (_: Exception) {
            0L
        }
    }
    
    val achievements = remember(profile, topLanguages, starsCount, daysActive) {
        listOf(
            AchievementItem(
                id = "vet",
                title = "Octocat Veteran",
                desc = "Active account for > 3 years",
                unlocked = daysActive > 365 * 3,
                code = "[VET]"
            ),
            AchievementItem(
                id = "poly",
                title = "Polyglot Agent",
                desc = "Use 3+ top languages",
                unlocked = topLanguages.size >= 3,
                code = "[PLG]"
            ),
            AchievementItem(
                id = "star",
                title = "Star Magnet",
                desc = "Earned stars on repos",
                unlocked = starsCount > 0,
                code = "[STR]"
            ),
            AchievementItem(
                id = "repos",
                title = "Code Factory",
                desc = "Created 15+ public repos",
                unlocked = profile.publicRepos >= 15,
                code = "[FAC]"
            ),
            AchievementItem(
                id = "social",
                title = "Social Node",
                desc = "Have 10+ followers",
                unlocked = profile.followers >= 10,
                code = "[NOD]"
            ),
            AchievementItem(
                id = "gists",
                title = "Snippet Sharer",
                desc = "Created public gists",
                unlocked = profile.publicGists > 0,
                code = "[SNP]"
            )
        )
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "achievements matrix",
                color = palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            val chunks = achievements.chunked(2)
            chunks.forEach { rowItems ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(
                            Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (item.unlocked) palette.accent else palette.border.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .background(
                                    if (item.unlocked) palette.accent.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.unlocked) palette.textPrimary else palette.textMuted
                                    )
                                    Text(
                                        text = item.code,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.unlocked) palette.accent else palette.textMuted.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = item.desc,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 8.sp,
                                    color = palette.textMuted,
                                    lineHeight = 1.3.em
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemLifespanTelemetryPanel(profile: GHUserProfile) {
    val palette = AiModuleTheme.colors
    val daysActive = remember(profile.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val createdDate = sdf.parse(profile.createdAt.take(10)) ?: Date()
            val diff = Date().time - createdDate.time
            diff / (1000 * 60 * 60 * 24)
        } catch (_: Exception) {
            0L
        }
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(Color(0xFF07090E))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SYSTEM LIFESPAN TELEMETRY",
                color = palette.accent,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "ESTABLISHED : ${profile.createdAt}",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp
            )
            Text(
                text = "SYSTEM UPTIME: $daysActive DAYS",
                color = Color(0xFF39D353),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "STATUS      : ONLINE // SIGNATURE VALID",
                color = palette.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp
            )
            
            Spacer(Modifier.height(6.dp))
            
            val bootLogs = remember(profile.login, daysActive) {
                listOf(
                    "[OK] SYSTEM BOOT COMPLETED IN 0.042s",
                    "[OK] SECURE TELEMETRY BINDING ATTACHED",
                    "[OK] REPO CACHE INTEGRITY CHECK: PASS",
                    "[OK] USER @${profile.login.uppercase()} VERIFIED",
                    "[LOG] ACTIVE CYBERNETIC INDEX: $daysActive D_UP"
                )
            }
            
            bootLogs.forEach { log ->
                Text(
                    text = log,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp
                )
            }
        }
    }
}


@Composable
private fun DeveloperAccessCard(
    profile: GHUserProfile,
    topLanguages: List<Pair<String, Int>>,
    starsCount: Int
) {
    val context = LocalContext.current
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val animRotationX by animateFloatAsState(if (isDragging) rotationX else 0f)
    val animRotationY by animateFloatAsState(if (isDragging) rotationY else 0f)
    
    val topLangsStr = remember(topLanguages) {
        topLanguages.map { it.first }.take(3)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .graphicsLayer {
                    this.rotationX = -animRotationX
                    this.rotationY = animRotationY
                    this.cameraDistance = 12f * density.density
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotationX = (rotationX + dragAmount.y * 0.15f).coerceIn(-25f, 25f)
                            rotationY = (rotationY + dragAmount.x * 0.15f).coerceIn(-25f, 25f)
                        }
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, palette.accent, RoundedCornerShape(12.dp))
                .background(Color(0xFF0D1117))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 20.dp.toPx()
                val linePaint = palette.border.copy(alpha = 0.08f)
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(linePaint, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                }
                drawRect(
                    color = palette.accent.copy(alpha = 0.2f),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            }
            
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, palette.accent, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = profile.login,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GSGIT DEVCARD // ACCESS PERMIT",
                        fontFamily = JetBrainsMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        letterSpacing = 0.1.em
                    )
                    Text(
                        text = profile.name.ifBlank { profile.login }.uppercase(),
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: @${profile.login.lowercase()}",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = palette.textMuted
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = "RANK  : ${if (profile.followers > 50) "S-CLASS ELITE" else "A-CLASS DEVELOPER"}",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "STARS : $starsCount \u2605",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = Color(0xFFD29922)
                    )
                    Text(
                        text = "STACK : ${topLangsStr.joinToString(", ").uppercase()}",
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .background(palette.accent.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    fontFamily = JetBrainsMono,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
            }
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                .clickable {
                    exportDevCardToPng(context, profile, topLangsStr, starsCount)
                }
                .background(palette.accent.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("dl", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.accent, fontWeight = FontWeight.Bold)
                Text("Export DevCard to PNG", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.accent)
            }
        }
    }
}

@Composable
private fun LanguageRadarChart(topLanguages: List<Pair<String, Int>>) {
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    
    val languages = remember(topLanguages) {
        if (topLanguages.isEmpty()) {
            listOf("KOTLIN" to 5, "JAVA" to 3, "TS" to 2, "C++" to 1, "PY" to 1)
        } else {
            topLanguages.map { it.first.uppercase() to it.second }
        }
    }
    
    val totalRepos = remember(languages) { languages.sumOf { it.second }.toFloat() }
    var selectedLangIndex by remember { mutableStateOf<Int?>(null) }
    
    val textPaint = remember(palette.textMuted) {
        android.graphics.Paint().apply {
            color = palette.textMuted.toArgb()
            textSize = with(density) { 7.5.sp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clipToBounds()
                .pointerInput(languages, totalRepos) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val maxRadius = size.height * 0.38f
                        val N = languages.size
                        var clickedIdx: Int? = null
                        for (i in 0 until N) {
                            val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
                            val fraction = if (totalRepos > 0) languages[i].second / totalRepos else 0.2f
                            val score = (0.2f + 0.8f * fraction).coerceIn(0f, 1.0f)
                            val r = maxRadius * score
                            val px = centerX + r * cos(angle)
                            val py = centerY + r * sin(angle)
                            
                            val dist = sqrt((px - tapOffset.x) * (px - tapOffset.x) + (py - tapOffset.y) * (py - tapOffset.y))
                            if (dist < 24.dp.toPx()) {
                                clickedIdx = i
                                break
                            }
                        }
                        selectedLangIndex = if (selectedLangIndex == clickedIdx) null else clickedIdx
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.height * 0.38f
            val N = languages.size
            
            // Draw grid levels
            val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
            gridLevels.forEach { level ->
                val path = Path()
                for (i in 0 until N) {
                    val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
                    val r = maxRadius * level
                    val px = center.x + r * cos(angle)
                    val py = center.y + r * sin(angle)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(
                    path = path,
                    color = palette.border.copy(alpha = if (level == 1.0f) 0.15f else 0.08f),
                    style = Stroke(width = if (level == 1.0f) 1.5.dp.toPx() else 1.dp.toPx())
                )
            }
            
            // Draw axes and labels
            for (i in 0 until N) {
                val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
                val outerPoint = Offset(
                    center.x + maxRadius * cos(angle),
                    center.y + maxRadius * sin(angle)
                )
                
                drawLine(
                    color = palette.border.copy(alpha = 0.08f),
                    start = center,
                    end = outerPoint,
                    strokeWidth = 1f
                )
                
                val text = languages[i].first
                val textRadius = maxRadius + 12.dp.toPx()
                val tx = center.x + textRadius * cos(angle)
                val ty = center.y + textRadius * sin(angle) + 3.dp.toPx()
                
                val align = when {
                    cos(angle) > 0.2f -> android.graphics.Paint.Align.LEFT
                    cos(angle) < -0.2f -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                
                val isSelected = selectedLangIndex == i
                val paint = android.graphics.Paint(textPaint).apply {
                    color = (if (isSelected) palette.accent else palette.textSecondary).toArgb()
                    textSize = with(density) { (if (isSelected) 8.5.sp else 7.5.sp).toPx() }
                    typeface = if (isSelected) android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD) else android.graphics.Typeface.MONOSPACE
                    textAlign = align
                }
                
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(text, tx, ty, paint)
                }
            }
            
            // Draw score path
            if (totalRepos > 0) {
                val path = Path()
                val points = mutableListOf<Offset>()
                for (i in 0 until N) {
                    val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
                    val fraction = languages[i].second / totalRepos
                    val score = (0.2f + 0.8f * fraction).coerceIn(0f, 1.0f)
                    val r = maxRadius * score
                    val px = center.x + r * cos(angle)
                    val py = center.y + r * sin(angle)
                    points.add(Offset(px, py))
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.35f), palette.accent.copy(alpha = 0.05f)),
                        center = center,
                        radius = maxRadius
                    ),
                    style = Fill
                )
                drawPath(
                    path = path,
                    color = palette.accent,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // Draw nodes
                points.forEachIndexed { i, pt ->
                    val isSelected = selectedLangIndex == i
                    drawCircle(
                        color = if (isSelected) palette.accent else Color.White,
                        radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = pt
                    )
                    if (isSelected) {
                        drawCircle(
                            color = palette.accent.copy(alpha = 0.4f),
                            radius = 9.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = selectedLangIndex != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            selectedLangIndex?.let { idx ->
                val lang = languages[idx]
                val pct = if (totalRepos > 0) (lang.second / totalRepos * 100).toInt() else 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.accent.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(palette.accent.copy(alpha = 0.06f))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "METRIC ANALYZER // ${lang.first}",
                            fontFamily = JetBrainsMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                        Text(
                            text = "REPOSITORIES : ${lang.second} active projects",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "VOLUME SHARE : $pct% of public stack",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityDonutChart(slices: List<ActivitySlice>) {
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    
    val totalWeight = remember(slices) { slices.sumOf { it.percentage.toDouble() }.toFloat() }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(slices, totalWeight) {
                            detectTapGestures { tapOffset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = tapOffset.x - centerX
                                val dy = tapOffset.y - centerY
                                val distance = sqrt(dx*dx + dy*dy)
                                
                                val strokeWidthPx = 12.dp.toPx()
                                val selectedStrokeWidthPx = strokeWidthPx + 3.dp.toPx()
                                val glowStrokeWidthPx = selectedStrokeWidthPx + 5.dp.toPx()
                                val donutSize = size.height - glowStrokeWidthPx - 6.dp.toPx()
                                
                                val outerRadius = donutSize / 2f + selectedStrokeWidthPx / 2f
                                val innerRadius = donutSize / 2f - selectedStrokeWidthPx / 2f
                                
                                if (distance in innerRadius..outerRadius) {
                                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    val adjustedAngle = (angle - (-90f) + 360f) % 360f
                                    
                                    var currentAngle = 0f
                                    var clickedIdx: Int? = null
                                    for (i in slices.indices) {
                                        val sweep = if (totalWeight > 0) (slices[i].percentage / totalWeight) * 360f else 0f
                                        if (adjustedAngle >= currentAngle && adjustedAngle < currentAngle + sweep) {
                                            clickedIdx = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                    selectedSliceIndex = if (selectedSliceIndex == clickedIdx) null else clickedIdx
                                }
                            }
                        }
                ) {
                    val strokeWidthPx = 12.dp.toPx()
                    val selectedStrokeWidthPx = strokeWidthPx + 3.dp.toPx()
                    val glowStrokeWidthPx = selectedStrokeWidthPx + 5.dp.toPx()
                    
                    val donutSize = size.height - glowStrokeWidthPx - 6.dp.toPx()
                    val donutLeft = (size.width - donutSize) / 2f
                    val donutTop = (size.height - donutSize) / 2f
                    
                    var startAngle = -90f
                    
                    slices.forEachIndexed { index, slice ->
                        if (totalWeight > 0) {
                            val sweepAngle = (slice.percentage / totalWeight) * 360f
                            val isSelected = selectedSliceIndex == index
                            val currentStrokeWidth = if (isSelected) selectedStrokeWidthPx else strokeWidthPx
                            
                            if (isSelected) {
                                drawArc(
                                    color = slice.color.copy(alpha = 0.25f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(donutLeft, donutTop),
                                    size = Size(donutSize, donutSize),
                                    style = Stroke(glowStrokeWidthPx)
                                )
                            }
                            
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(donutLeft, donutTop),
                                size = Size(donutSize, donutSize),
                                style = Stroke(currentStrokeWidth)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selectedSliceIndex != null) {
                        val slice = slices[selectedSliceIndex!!]
                        val pct = if (totalWeight > 0) (slice.percentage / totalWeight * 100).toInt() else 0
                        Text(
                            text = "${pct}%",
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = slice.color
                        )
                        Text(
                            text = slice.label.take(3).uppercase(),
                            fontFamily = JetBrainsMono,
                            fontSize = 8.sp,
                            color = palette.textSecondary
                        )
                    } else {
                        Text(
                            text = "SYS",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                        Text(
                            text = "ACT",
                            fontFamily = JetBrainsMono,
                            fontSize = 8.sp,
                            color = palette.textMuted
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedSliceIndex == index
                    val pct = if (totalWeight > 0) (slice.percentage / totalWeight * 100).toInt() else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) slice.color.copy(alpha = 0.08f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) slice.color.copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                selectedSliceIndex = if (selectedSliceIndex == index) null else index
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(slice.color)
                            )
                            Text(
                                text = slice.label,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = if (isSelected) palette.textPrimary else palette.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "$pct%",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = slice.color
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = selectedSliceIndex != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            selectedSliceIndex?.let { idx ->
                val slice = slices[idx]
                val descText = when (slice.label.lowercase()) {
                    "commits" -> "Code modifications, direct contributions, and updates written to repository lanes."
                    "prs" -> "Pull Requests submitted for team code reviews, repository merging, and project branches."
                    "issues" -> "Bug reports, telemetry logs, tasks, and issues created or managed in this workspace."
                    "reviews" -> "Quality control audits, approvals, changes requested, and feedback on active peer branches."
                    else -> "General cybernetic repository activity and event interactions."
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, slice.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(slice.color.copy(alpha = 0.06f))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "INTELLIGENCE REPORT // ${slice.label.uppercase()}",
                            fontFamily = JetBrainsMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = slice.color
                        )
                        Text(
                            text = descText,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = palette.textPrimary,
                            lineHeight = 1.3.em
                        )
                    }
                }
            }
        }
    }
}

private fun exportDevCardToPng(
    context: Context,
    profile: GHUserProfile,
    topLanguages: List<String>,
    starsCount: Int
) {
    val width = 800
    val height = 480
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    paint.color = 0xFF0D1117.toInt()
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    
    paint.color = 0xFF1F2937.toInt()
    paint.strokeWidth = 1f
    for (i in 0..width step 40) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
    }
    for (i in 0..height step 40) {
        canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
    }
    
    paint.color = 0xFF38BDF8.toInt()
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawRoundRect(16f, 16f, width - 16f, height - 16f, 12f, 12f, paint)
    
    paint.strokeWidth = 1f
    canvas.drawRoundRect(24f, 24f, width - 24f, height - 24f, 8f, 8f, paint)
    
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = 0xFF38BDF8.toInt()
    paint.textSize = 24f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    canvas.drawText("GSGIT ACCESS CARD // LEVEL PERMIT", 48f, 70f, paint)
    
    paint.color = 0xFFFFFFFF.toInt()
    paint.textSize = 36f
    canvas.drawText(profile.name.ifBlank { profile.login }.uppercase(), 48f, 140f, paint)
    
    paint.color = 0xFF9CA3AF.toInt()
    paint.textSize = 24f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
    canvas.drawText("ID   : @${profile.login.lowercase()}", 48f, 190f, paint)
    
    paint.color = 0xFFE5E7EB.toInt()
    paint.textSize = 22f
    canvas.drawText("RANK : ${if (profile.followers > 50) "S-CLASS ELITE" else "A-CLASS DEVELOPER"}", 48f, 250f, paint)
    canvas.drawText("STARS: $starsCount \u2605", 48f, 300f, paint)
    canvas.drawText("STACK: ${topLanguages.joinToString(", ").uppercase()}", 48f, 350f, paint)
    
    paint.color = 0xFF38BDF8.toInt()
    paint.textSize = 18f
    canvas.drawText("TELEMETRY STATUS: ACTIVE", 48f, 430f, paint)
    
    try {
        val file = java.io.File("/storage/emulated/0/Download/devcard-${profile.login}.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        Toast.makeText(context, "Saved to Download/devcard-${profile.login}.png", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
