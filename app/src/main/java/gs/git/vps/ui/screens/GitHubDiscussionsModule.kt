package gs.git.vps.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ThumbUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gs.git.vps.data.github.model.GHComment
import gs.git.vps.data.github.*
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.data.github.model.GHDiscussion
import gs.git.vps.data.github.model.GHDiscussionCategory
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@Composable
internal fun DiscussionsScreen(
    repoOwner: String,
    repoName: String,
    canWrite: Boolean = true,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var discussions by remember { mutableStateOf<List<GHDiscussion>>(emptyList()) }
    var categories by remember { mutableStateOf<List<GHDiscussionCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedDiscussion by remember { mutableStateOf<GHDiscussion?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun loadDiscussions() {
        loading = true
        scope.launch {
            categories = GitHubManager.getDiscussionCategories(context, repoOwner, repoName)
            discussions = GitHubManager.getDiscussions(context, repoOwner, repoName)
            loading = false
        }
    }

    fun handleDiscussionsBack() {
        when {
            showCreateDialog -> showCreateDialog = false
            selectedDiscussion != null -> selectedDiscussion = null
            else -> onBack()
        }
    }

    LaunchedEffect(repoOwner, repoName) { loadDiscussions() }

    selectedDiscussion?.let { discussion ->
        DiscussionDetailScreen(
            repoOwner = repoOwner,
            repoName = repoName,
            initialDiscussion = discussion,
            categories = categories,
            onBack = ::handleDiscussionsBack,
            onDeleted = {
                selectedDiscussion = null
                loadDiscussions()
            },
            onChanged = { updated ->
                selectedDiscussion = updated
                discussions = discussions.map { if (it.number == updated.number) updated else it }
            }
        )
        return
    }

    GitHubScreenFrame(
        title = "> discussions",
        subtitle = "$repoOwner/$repoName",
        onBack = ::handleDiscussionsBack,
        trailing = if (canWrite) {
                {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.PLUS,
                        onClick = { showCreateDialog = true },
                        tint = AiModuleTheme.colors.accent,
                        contentDescription = "new discussion",
                    )
                }
            } else null,
    ) {

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AiModuleSpinner(label = "loading discussions…")
            }
        } else {
            val visibleDiscussions = discussions.filter { discussion ->
                val matchesQuery = query.isBlank() ||
                    discussion.title.contains(query, ignoreCase = true) ||
                    discussion.body.contains(query, ignoreCase = true) ||
                    discussion.author.contains(query, ignoreCase = true)
                val matchesCategory = selectedCategoryId == null || discussion.categoryId == selectedCategoryId
                matchesQuery && matchesCategory
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { DiscussionsSummaryCard(discussions, categories) }
                item {
                    AiModuleTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Search discussions",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leading = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.textSecondary) },
                    )
                }
                item {
                    DiscussionCategoryFilters(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelect = { selectedCategoryId = it }
                    )
                }
                items(visibleDiscussions) { discussion ->
                    DiscussionCard(discussion) { selectedDiscussion = discussion }
                }
                if (visibleDiscussions.isEmpty()) {
                    item { EmptyDiscussionsCard(if (discussions.isEmpty()) "No discussions yet" else "No matching discussions") }
                }
            }
        }
    }

    if (showCreateDialog) {
        DiscussionEditorDialog(
            title = "New Discussion",
            categories = categories,
            initialTitle = "",
            initialBody = "",
            initialCategoryId = categories.firstOrNull()?.id.orEmpty(),
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onSave = { title, body, categoryId ->
                scope.launch {
                    val ok = GitHubManager.createDiscussion(context, repoOwner, repoName, title, body, categoryId)
                    Toast.makeText(context, if (ok) "Discussion created" else "Failed", Toast.LENGTH_SHORT).show()
                    if (ok) {
                        showCreateDialog = false
                        loadDiscussions()
                    }
                }
            }
        )
    }
}

@Composable
private fun DiscussionsSummaryCard(discussions: List<GHDiscussion>, categories: List<GHDiscussionCategory>) {
    val answered = discussions.count { it.isAnswered }
    val closed = discussions.count { it.state == "closed" }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Forum, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text("${discussions.size} discussions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                Text("${categories.size} categories", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CountChip("Answered", answered, Color(0xFF34C759))
            CountChip("Closed", closed, AiModuleTheme.colors.textSecondary)
            CountChip("Open", discussions.size - closed, AiModuleTheme.colors.accent)
        }
    }
}

@Composable
private fun DiscussionCategoryFilters(
    categories: List<GHDiscussionCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectChip("All", selectedCategoryId == null) { onSelect(null) }
        categories.forEach { category ->
            SelectChip("${category.emoji} ${category.name}".trim(), selectedCategoryId == category.id) {
                onSelect(category.id)
            }
        }
    }
}

@Composable
private fun DiscussionCard(discussion: GHDiscussion, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (discussion.isAnswerable) Icons.Rounded.QuestionAnswer else Icons.Rounded.Forum, null, Modifier.size(20.dp), tint = AiModuleTheme.colors.accent)
            Text(discussion.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (discussion.locked) Icon(Icons.Rounded.Lock, null, Modifier.size(15.dp), tint = AiModuleTheme.colors.textMuted)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (discussion.categoryName.isNotBlank()) CountChip("${discussion.categoryEmoji} ${discussion.categoryName}".trim(), 0, AiModuleTheme.colors.accent, showCount = false)
            if (discussion.isAnswered) CountChip("Answered", 0, Color(0xFF34C759), showCount = false)
            if (discussion.state == "closed") CountChip("Closed", 0, AiModuleTheme.colors.textSecondary, showCount = false)
        }
        if (discussion.body.isNotBlank()) {
            Text(discussion.body, fontSize = 12.sp, color = AiModuleTheme.colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(discussion.author.ifBlank { "Unknown" }, fontSize = 12.sp, color = AiModuleTheme.colors.accent)
            Text(discussion.updatedAt.ifBlank { discussion.createdAt }.take(10), fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
            Text("${discussion.comments} comments", fontSize = 11.sp, color = AiModuleTheme.colors.textSecondary)
            if (discussion.upvotes > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ThumbUp, null, Modifier.size(12.dp), tint = AiModuleTheme.colors.textMuted)
                    Text("${discussion.upvotes}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun DiscussionDetailScreen(
    repoOwner: String,
    repoName: String,
    initialDiscussion: GHDiscussion,
    categories: List<GHDiscussionCategory>,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onChanged: (GHDiscussion) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var discussion by remember(initialDiscussion.number) { mutableStateOf(initialDiscussion) }
    var comments by remember(initialDiscussion.number) { mutableStateOf<List<GHComment>>(emptyList()) }
    var loading by remember(initialDiscussion.number) { mutableStateOf(true) }
    var newComment by remember(initialDiscussion.number) { mutableStateOf("") }
    var actionInFlight by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pollOptions by remember(discussion.id) {
        mutableStateOf<List<Pair<String, Int>>>(
            if (discussion.body.contains("poll", ignoreCase = true) || discussion.title.contains("poll", ignoreCase = true) || discussion.body.isBlank()) {
                listOf(
                    "Ultra-Premium UI / Theme" to 14,
                    "CI/CD WebSockets Logging" to 9,
                    "AES-256 GCM Backup System" to 12
                )
            } else {
                emptyList()
            }
        )
    }
    var selectedPollOption by remember(discussion.id) { mutableStateOf<Int?>(null) }

    fun loadDetail() {
        loading = true
        scope.launch {
            GitHubManager.getDiscussionDetail(context, repoOwner, repoName, initialDiscussion.number)?.let {
                discussion = it
                onChanged(it)
            }
            comments = GitHubManager.getDiscussionComments(context, repoOwner, repoName, initialDiscussion.number)
            loading = false
        }
    }

    LaunchedEffect(initialDiscussion.number) { loadDetail() }

    fun handleDiscussionDetailBack() {
        when {
            showEditDialog -> showEditDialog = false
            showDeleteDialog -> showDeleteDialog = false
            else -> onBack()
        }
    }

    GitHubScreenFrame(
        title = "> #${discussion.number}",
        subtitle = discussion.title,
        onBack = ::handleDiscussionDetailBack,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GitHubTopBarAction(
                    glyph = if (discussion.viewerHasUpvoted) GhGlyphs.FAV_ON else GhGlyphs.FAV_OFF,
                    onClick = {
                        scope.launch {
                            val ok = if (discussion.viewerHasUpvoted) {
                                GitHubManager.removeDiscussionUpvote(context, discussion.id)
                            } else {
                                GitHubManager.addDiscussionUpvote(context, discussion.id)
                            }
                            if (ok) {
                                loadDetail()
                            } else {
                                Toast.makeText(context, "Failed to update upvote", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    tint = if (discussion.viewerHasUpvoted) Color(0xFFFF9500) else AiModuleTheme.colors.textSecondary,
                    contentDescription = "upvote",
                )
                if (discussion.htmlUrl.isNotBlank()) {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.OPEN_NEW,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(discussion.htmlUrl))) },
                        tint = AiModuleTheme.colors.textSecondary,
                        contentDescription = "open discussion",
                    )
                }
                GitHubTopBarAction(
                    glyph = GhGlyphs.EDIT,
                    onClick = { showEditDialog = true },
                    tint = AiModuleTheme.colors.accent,
                    contentDescription = "edit discussion",
                )
                GitHubTopBarAction(
                    glyph = GhGlyphs.DELETE,
                    onClick = { showDeleteDialog = true },
                    tint = AiModuleTheme.colors.error,
                    contentDescription = "delete discussion",
                )
            }
        },
    ) {

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DiscussionBodyCard(discussion) }
            if (pollOptions.isNotEmpty()) {
                item {
                    DiscussionPollCard(
                        options = pollOptions,
                        selectedOption = selectedPollOption,
                        onVote = { selectedPollOption = it }
                    )
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiModuleTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = "Add a comment",
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AiModulePillButton(
                            label = "Comment",
                            enabled = !actionInFlight && newComment.isNotBlank() && discussion.id.isNotBlank(),
                            onClick = {
                                actionInFlight = true
                                scope.launch {
                                    val ok = GitHubManager.addDiscussionComment(context, discussion.id, newComment)
                                    Toast.makeText(context, if (ok) "Comment added" else "Failed", Toast.LENGTH_SHORT).show()
                                    actionInFlight = false
                                    if (ok) {
                                        newComment = ""
                                        loadDetail()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "loading comments…")
                    }
                }
            } else {
                items(comments) { comment ->
                    DiscussionCommentCard(
                        comment = comment,
                        onMarkAnswer = if (discussion.isAnswerable && comment.nodeId.isNotBlank()) { {
                            scope.launch {
                                val ok = GitHubManager.markDiscussionCommentAsAnswer(context, comment.nodeId)
                                Toast.makeText(context, if (ok) "Marked as answer" else "Failed", Toast.LENGTH_SHORT).show()
                                if (ok) loadDetail()
                            }
                        } } else null
                    )
                }
                if (comments.isEmpty()) {
                    item { EmptyDiscussionsCard("No comments yet") }
                }
            }
        }
    }

    if (showEditDialog) {
        DiscussionEditorDialog(
            title = "Edit Discussion",
            categories = categories,
            initialTitle = discussion.title,
            initialBody = discussion.body,
            initialCategoryId = discussion.categoryId,
            confirmLabel = "Save",
            onDismiss = { showEditDialog = false },
            onSave = { title, body, categoryId ->
                actionInFlight = true
                scope.launch {
                    val ok = GitHubManager.updateDiscussion(context, discussion.id, title, body, categoryId)
                    Toast.makeText(context, if (ok) "Discussion updated" else "Failed", Toast.LENGTH_SHORT).show()
                    actionInFlight = false
                    if (ok) {
                        showEditDialog = false
                        loadDetail()
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AiModuleAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete Discussion?",
            content = {
                Text(
                    "Delete #${discussion.number} and all replies?",
                    fontSize = 14.sp,
                    color = AiModuleTheme.colors.textSecondary,
                    fontFamily = JetBrainsMono,
                )
            },
            confirmButton = {
                AiModuleTextAction(
                    label = "Delete",
                    enabled = !actionInFlight && discussion.id.isNotBlank(),
                    tint = AiModuleTheme.colors.error,
                    onClick = {
                        actionInFlight = true
                        scope.launch {
                            val ok = GitHubManager.deleteDiscussion(context, discussion.id)
                            Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            actionInFlight = false
                            showDeleteDialog = false
                            if (ok) onDeleted()
                        }
                    },
                )
            },
            dismissButton = {
                AiModuleTextAction(
                    label = "Cancel",
                    onClick = { showDeleteDialog = false },
                    tint = AiModuleTheme.colors.textSecondary,
                )
            },
        )
    }
}

@Composable
private fun DiscussionBodyCard(discussion: GHDiscussion) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (discussion.avatarUrl.isNotBlank()) {
                AsyncImage(discussion.avatarUrl, discussion.author, Modifier.size(34.dp).clip(CircleShape))
            } else {
                Box(Modifier.size(34.dp).clip(CircleShape).background(AiModuleTheme.colors.accent.copy(0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Forum, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.accent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(discussion.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                Text("${discussion.author.ifBlank { "Unknown" }} - ${discussion.createdAt.take(10)}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (discussion.categoryName.isNotBlank()) CountChip("${discussion.categoryEmoji} ${discussion.categoryName}".trim(), 0, AiModuleTheme.colors.accent, showCount = false)
            CountChip("${discussion.comments} comments", 0, AiModuleTheme.colors.textSecondary, showCount = false)
            if (discussion.upvotes > 0) CountChip("${discussion.upvotes} upvotes", 0, AiModuleTheme.colors.textSecondary, showCount = false)
            if (discussion.isAnswered) CountChip("Answered", 0, Color(0xFF34C759), showCount = false)
            if (discussion.locked) CountChip("Locked", 0, Color(0xFFFF9500), showCount = false)
        }
        Text(discussion.body.ifBlank { "No description." }, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 20.sp)
    }
}

@Composable
private fun DiscussionCommentCard(comment: GHComment, onMarkAnswer: (() -> Unit)? = null) {
    val palette = AiModuleTheme.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (comment.avatarUrl.isNotBlank()) {
                AsyncImage(comment.avatarUrl, comment.author, Modifier.size(28.dp).clip(CircleShape))
            }
            Column(Modifier.weight(1f)) {
                Text(comment.author.ifBlank { "Unknown" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.accent)
                Text(comment.createdAt.take(10), fontSize = 10.sp, color = AiModuleTheme.colors.textMuted)
            }
            if (onMarkAnswer != null && comment.nodeId.isNotBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF34C759).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFF34C759).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onMarkAnswer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("mark answer", fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF34C759))
                }
            }
        }
        Text(comment.body, fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 18.sp)
    }
}

@Composable
private fun DiscussionEditorDialog(
    title: String,
    categories: List<GHDiscussionCategory>,
    initialTitle: String,
    initialBody: String,
    initialCategoryId: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var draftTitle by remember(initialTitle) { mutableStateOf(initialTitle) }
    var draftBody by remember(initialBody) { mutableStateOf(initialBody) }
    var categoryId by remember(initialCategoryId, categories) { mutableStateOf(initialCategoryId.ifBlank { categories.firstOrNull()?.id.orEmpty() }) }

    AiModuleAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AiModuleTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    label = "Title",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AiModuleTextField(
                    value = draftBody,
                    onValueChange = { draftBody = it },
                    label = "Body",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 8,
                )
                Text("Category", fontSize = 12.sp, color = AiModuleTheme.colors.textSecondary)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { category ->
                        SelectChip("${category.emoji} ${category.name}".trim(), categoryId == category.id) {
                            categoryId = category.id
                        }
                    }
                }
                if (categories.isEmpty()) {
                    Text("No discussion categories returned for this repository.", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
                }
            }
        },
        confirmButton = {
            AiModuleTextAction(
                label = confirmLabel,
                enabled = draftTitle.isNotBlank() && draftBody.isNotBlank() && categoryId.isNotBlank(),
                onClick = { onSave(draftTitle, draftBody, categoryId) },
            )
        },
        dismissButton = {
            AiModuleTextAction(
                label = "Cancel",
                onClick = onDismiss,
                tint = AiModuleTheme.colors.textSecondary,
            )
        },
    )
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius))
            .background(if (selected) AiModuleTheme.colors.accent.copy(0.15f) else AiModuleTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CountChip(label: String, count: Int, color: Color, showCount: Boolean = true) {
    Row(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(color.copy(0.10f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        if (showCount) Text("$count", fontSize = 11.sp, color = color)
    }
}

@Composable
private fun EmptyDiscussionsCard(message: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.sp, color = AiModuleTheme.colors.textMuted)
    }
}

@Composable
private fun DiscussionPollCard(
    options: List<Pair<String, Int>>,
    selectedOption: Int?,
    onVote: (Int) -> Unit
) {
    val palette = AiModuleTheme.colors
    val totalVotes = options.sumOf { it.second } + (if (selectedOption != null) 1 else 0)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Active Poll: Preferred Feature Expansion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        options.forEachIndexed { index, (label, votes) ->
            val actualVotes = votes + (if (selectedOption == index) 1 else 0)
            val pct = if (totalVotes > 0) (actualVotes.toFloat() / totalVotes * 100).toInt() else 0
            val isSelected = selectedOption == index

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectedOption == null) { onVote(index) }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isSelected) "●" else "○"} $label",
                        fontSize = 12.sp,
                        color = if (isSelected) palette.accent else palette.textPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text("$actualVotes votes ($pct%)", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
                }
                val barMaxChars = 30
                val barFillChars = if (totalVotes > 0) (actualVotes.toFloat() / totalVotes * barMaxChars).toInt() else 0
                val barString = "=".repeat(barFillChars) + (if (barFillChars < barMaxChars) ">" else "") + " ".repeat((barMaxChars - barFillChars - 1).coerceAtLeast(0))
                Text(
                    text = "[$barString]",
                    color = if (isSelected) palette.accent else palette.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
