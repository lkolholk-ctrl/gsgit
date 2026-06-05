package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import gs.git.vps.data.github.*
import gs.git.vps.ui.components.*
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun RepoQuickStartScreen(
    result: GHRepoCreateResult,
    onBack: () -> Unit,
    onOpenRepo: (GHRepo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = AiModuleTheme.colors
    val repo = result.repo ?: return
    val cloneUrl = result.cloneUrl
    val branch = repo.defaultBranch.ifBlank { "main" }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadError by remember { mutableStateOf("") }
    var uploadComplete by remember { mutableStateOf(false) }
    var lastTreeUri by remember { mutableStateOf<Uri?>(null) }

    fun startUpload(treeUri: Uri) {
        scope.launch {
            uploadProgress = 0f
            uploadError = ""
            uploadComplete = false
            try {
                val files = withContext(Dispatchers.IO) {
                    collectFiles(context, treeUri)
                }
                if (files.isEmpty()) {
                    uploadError = "No files found in selected folder"
                    uploadProgress = null
                    return@launch
                }
                val ok = GitHubManager.uploadProjectFolder(
                    context, repo.owner, repo.name, branch, files
                ) { progress -> uploadProgress = progress }
                if (ok) {
                    uploadComplete = true
                    uploadProgress = 1f
                } else {
                    uploadError = "Upload failed"
                    uploadProgress = null
                }
            } catch (e: Exception) {
                uploadError = e.message ?: "Upload error"
                uploadProgress = null
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) return@rememberLauncherForActivityResult
        lastTreeUri = treeUri
        startUpload(treeUri)
    }

    GitHubScreenFrame(
        title = "> quick start",
        subtitle = repo.fullName,
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ═══ А. ШАПКА РЕПОЗИТОРИЯ ═══
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        repo.owner,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = palette.accent
                    )
                    androidx.compose.material3.Text(
                        " / ",
                        fontFamily = JetBrainsMono,
                        fontSize = 16.sp,
                        color = palette.textMuted
                    )
                    androidx.compose.material3.Text(
                        repo.name,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = palette.textPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        if (repo.isPrivate) "[ PRIVATE ]" else "[ PUBLIC ]",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (repo.isPrivate) palette.warning else palette.accent
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Text(
                        cloneUrl,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    BracketButton("[ copy url ]") {
                        copyToClipboard(context, "clone url", cloneUrl)
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    "─".repeat(54),
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = palette.border
                )
                Spacer(Modifier.height(12.dp))

                // ═══ Б. ИНТЕРАКТИВНАЯ ЗОНА ДЕЙСТВИЙ ═══
                AiModuleSectionLabel("> actions")
                Spacer(Modifier.height(6.dp))

                if (uploadComplete) {
                    AiModuleText(
                        "upload complete",
                        fontSize = 12.sp,
                        color = palette.accent,
                        fontFamily = JetBrainsMono
                    )
                    Spacer(Modifier.height(6.dp))
                    BracketButton("[ → open repository ]", accent = true) { onOpenRepo(repo) }
                } else if (uploadProgress != null) {
                    LinearProgressIndicator(
                        progress = { uploadProgress!!.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                        color = palette.accent,
                        trackColor = palette.surface,
                    )
                    Spacer(Modifier.height(4.dp))
                    AiModuleText(
                        when {
                            uploadProgress!! < 0f -> "error"
                            uploadProgress!! < 0.6f -> "uploading blobs… ${(uploadProgress!! / 0.6f * 100).toInt()}%"
                            uploadProgress!! < 0.7f -> "building tree…"
                            uploadProgress!! < 0.8f -> "creating commit…"
                            uploadProgress!! < 0.9f -> "updating ref…"
                            uploadProgress!! >= 1f -> "finalizing…"
                            else -> "processing…"
                        },
                        fontSize = 11.sp,
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono
                    )
                } else {
                    BracketButton("[ 📂 upload project folder ]", accent = true) {
                        folderLauncher.launch(null)
                    }
                }

                if (uploadError.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    AiModuleText(
                        "error: $uploadError",
                        fontSize = 11.sp,
                        color = palette.error,
                        fontFamily = JetBrainsMono
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (lastTreeUri != null) {
                            BracketButton("[ retry upload ]") { startUpload(lastTreeUri!!) }
                        }
                        BracketButton("[ pick folder ]") { folderLauncher.launch(null) }
                    }
                }

                if (result.autoInit && !uploadComplete) {
                    Spacer(Modifier.height(6.dp))
                    BracketButton("[ → open repository ]", accent = true) { onOpenRepo(repo) }
                }

                Spacer(Modifier.height(12.dp))

                // ═══ В. ТЕРМИНАЛЬНЫЙ БЛОК ШПАРАГАЛОК ═══
                if (!result.autoInit || !uploadComplete) {
                    AiModuleSectionLabel("> command line")
                    Spacer(Modifier.height(6.dp))

                    val newRepoCmd = buildString {
                        appendLine("echo \"# ${repo.name}\" >> README.md")
                        appendLine("git init")
                        appendLine("git add README.md")
                        appendLine("git commit -m \"first commit\"")
                        appendLine("git branch -M $branch")
                        appendLine("git remote add origin $cloneUrl")
                        append("git push -u origin $branch")
                    }
                    AsciiCodeBox(
                        label = "...or create a new repository on the command line",
                        code = newRepoCmd
                    ) { copyToClipboard(context, "new repo commands", newRepoCmd) }

                    Spacer(Modifier.height(10.dp))

                    val existingCmd = buildString {
                        appendLine("git remote add origin $cloneUrl")
                        appendLine("git branch -M $branch")
                        append("git push -u origin $branch")
                    }
                    AsciiCodeBox(
                        label = "...or push an existing repository from the command line",
                        code = existingCmd
                    ) { copyToClipboard(context, "existing repo commands", existingCmd) }
                }
            }

            // ═══ Г. СИСТЕМНЫЙ ФУТЕР ═══
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AiModuleText(
                    "💡 ProTip! Use background processing if you are uploading heavy directories.",
                    fontSize = 10.sp,
                    color = palette.textMuted,
                    fontFamily = JetBrainsMono
                )
            }
        }
    }
}

@Composable
private fun BracketButton(label: String, accent: Boolean = false, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val color = if (accent) palette.accent else palette.textSecondary
    Box(
        Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        androidx.compose.material3.Text(
            label,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun AsciiCodeBox(label: String, code: String, onCopy: () -> Unit) {
    val palette = AiModuleTheme.colors
    val lines = code.lines()
    val maxLen = lines.maxOfOrNull { it.length }?.coerceAtLeast(label.length) ?: 40
    val w = (maxLen + 4).coerceIn(20, 70)

    Column {
        androidx.compose.material3.Text(
            label,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = palette.textMuted
        )
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.Text(
            "+" + "─".repeat(w) + "+",
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = palette.border
        )
        lines.forEach { line ->
            val padded = line.padEnd(w - 2)
            androidx.compose.material3.Text(
                "| $padded |",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textPrimary
            )
        }
        androidx.compose.material3.Text(
            "+" + "─".repeat(w) + "+",
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = palette.border
        )
        Spacer(Modifier.height(4.dp))
        BracketButton("[ copy ]", accent = false, onClick = onCopy)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
}

private fun collectFiles(context: Context, treeUri: Uri): List<Pair<String, ByteArray>> {
    val pickedDir = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
    val files = mutableListOf<Pair<String, ByteArray>>()
    fun walk(dir: DocumentFile, pathPrefix: String) {
        dir.listFiles().forEach { doc ->
            if (doc.isDirectory) {
                walk(doc, "$pathPrefix${doc.name}/")
            } else if (doc.isFile) {
                val name = doc.name ?: return@forEach
                if (name.startsWith(".")) return@forEach
                val relPath = "$pathPrefix$name"
                try {
                    val bytes = context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() } ?: return@forEach
                    if (bytes.size > 25 * 1024 * 1024) return@forEach
                    files.add(relPath to bytes)
                } catch (_: Exception) {}
            }
        }
    }
    walk(pickedDir, "")
    return files
}
