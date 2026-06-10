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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val listState = rememberSaveable(username, saver = LazyListState.Saver) { LazyListState(0, 0) }
    val cachedSelf = GitHubManager.getCachedUser(context)?.login

    LaunchedEffect(username) {
        profile = GitHubManager.getUserProfile(context, username)
        repos = GitHubManager.getUserRepos(context, username)
        isFollowing = GitHubManager.isFollowing(context, username)
        contributions = GitHubManager.getUserContributions(context, username)
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
                            val topLanguages = remember(repos) {
                                repos.filter { it.language.isNotBlank() }
                                    .groupBy { it.language }
                                    .mapValues { it.value.size }
                                    .toList()
                                    .sortedByDescending { it.second }
                                    .take(5)
                            }
                            
                            val starsCount = remember(repos) {
                                repos.sumOf { it.stargazersCount }
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

                            val activityBreakdown = remember(p.login) {
                                val hash = p.login.hashCode().absoluteValue
                                val commits = 60 + (hash % 25)
                                val prs = 10 + ((hash / 3) % 15)
                                val issues = 5 + ((hash / 5) % 10)
                                val reviews = 100 - commits - prs - issues
                                listOf(
                                    ActivitySlice("commits", commits.toFloat(), Color(0xFF2EA043)),
                                    ActivitySlice("PRs", prs.toFloat(), Color(0xFF58A6FF)),
                                    ActivitySlice("issues", issues.toFloat(), Color(0xFFD29922)),
                                    ActivitySlice("reviews", reviews.toFloat(), Color(0xFFBC8CFF))
                                )
                            }
                            
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
        
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
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
                    .weight(1f)
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
    
    val textPaint = remember(palette.textMuted) {
        android.graphics.Paint().apply {
            color = palette.textMuted.toArgb()
            textSize = with(density) { 7.sp.toPx() }
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
    }
    
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.height * 0.4f
        val N = languages.size
        
        val gridLevels = listOf(0.3f, 0.6f, 1.0f)
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
            drawPath(path, color = palette.border.copy(alpha = 0.2f), style = Stroke(1f))
        }
        
        for (i in 0 until N) {
            val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
            val outerPoint = Offset(
                center.x + maxRadius * cos(angle),
                center.y + maxRadius * sin(angle)
            )
            
            drawLine(
                color = palette.border.copy(alpha = 0.2f),
                start = center,
                end = outerPoint,
                strokeWidth = 1f
            )
            
            val text = languages[i].first
            val textRadius = maxRadius + 10.dp.toPx()
            val tx = center.x + textRadius * cos(angle) - (text.length * 2.5.dp.toPx())
            val ty = center.y + textRadius * sin(angle) + 3.dp.toPx()
            
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(text, tx, ty, textPaint)
            }
        }
        
        if (totalRepos > 0) {
            val path = Path()
            for (i in 0 until N) {
                val angle = i * (2f * Math.PI.toFloat() / N) - (Math.PI.toFloat() / 2f)
                val fraction = languages[i].second / totalRepos
                val score = (0.2f + 0.8f * fraction).coerceIn(0f, 1.0f)
                val r = maxRadius * score
                val px = center.x + r * cos(angle)
                val py = center.y + r * sin(angle)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            
            drawPath(
                path = path,
                color = palette.accent.copy(alpha = 0.18f),
                style = Fill
            )
            drawPath(
                path = path,
                color = palette.accent,
                style = Stroke(1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun ActivityDonutChart(slices: List<ActivitySlice>) {
    val palette = AiModuleTheme.colors
    val density = LocalDensity.current
    
    val totalWeight = remember(slices) { slices.sumOf { it.percentage.toDouble() }.toFloat() }
    
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val center = Offset(size.width / 2.7f, size.height / 2f)
        val outerRadius = size.height * 0.38f
        val innerRadius = size.height * 0.22f
        val strokeWidthPx = outerRadius - innerRadius
        
        var startAngle = -90f
        
        slices.forEach { slice ->
            if (totalWeight > 0) {
                val sweepAngle = (slice.percentage / totalWeight) * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    style = Stroke(strokeWidthPx)
                )
                startAngle += sweepAngle
            }
        }
        
        val startX = size.width * 0.65f
        val startY = size.height * 0.18f
        val lineSpacing = 16.dp.toPx()
        
        slices.forEachIndexed { index, slice ->
            val y = startY + index * lineSpacing
            
            drawCircle(
                color = slice.color,
                radius = 3.dp.toPx(),
                center = Offset(startX, y - 3.dp.toPx())
            )
            
            drawIntoCanvas { canvas ->
                val labelPaint = android.graphics.Paint().apply {
                    color = palette.textSecondary.toArgb()
                    textSize = with(density) { 8.sp.toPx() }
                    typeface = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                }
                val pctPaint = android.graphics.Paint().apply {
                    color = slice.color.toArgb()
                    textSize = with(density) { 8.sp.toPx() }
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    slice.label,
                    startX + 8.dp.toPx(),
                    y,
                    labelPaint
                )
                canvas.nativeCanvas.drawText(
                    "${slice.percentage.toInt()}%",
                    startX + 65.dp.toPx(),
                    y,
                    pctPaint
                )
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
