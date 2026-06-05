package gs.git.vps.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.NoteAdd
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val IGNORED_DIRS = setOf(
    "build", "dist", "out", "target", "node_modules", ".gradle", "__pycache__",
    ".idea", ".vscode", ".cache", ".tox", ".mypy_cache", ".pytest_cache",
    "venv", ".venv", "env", ".env", "Pods", ".dart_tool", ".fvm",
    ".next", ".nuxt", ".output", ".svelte-kit", "vendor", "bundle"
)
private val IGNORED_EXTENSIONS = setOf(
    ".pyc", ".pyo", ".o", ".so", ".dylib", ".dll", ".exe", ".class",
    ".jar", ".war", ".apk", ".aab", ".ipa", ".obj", ".pdb", ".ilk",
    ".min.js", ".min.css"
)
private val MAX_FILE_SIZE = 25 * 1024 * 1024

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
    val sshUrl = result.sshUrl
    val branch = repo.defaultBranch.ifBlank { "main" }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadError by remember { mutableStateOf("") }
    var uploadComplete by remember { mutableStateOf(false) }
    var commitMsg by remember { mutableStateOf("") }
    val logLines = remember { mutableStateListOf<String>() }
    var uploadJob by remember { mutableStateOf<Job?>(null) }

    // preview state
    var pendingFiles by remember { mutableStateOf<List<Pair<String, ByteArray>>?>(null) }
    var scanning by remember { mutableStateOf(false) }

    fun cancelUpload() {
        uploadJob?.cancel()
        uploadJob = null
        uploadProgress = null
        uploadError = "Cancelled"
        logLines.add("[CANCELLED] upload aborted by user")
    }

    fun startUpload(files: List<Pair<String, ByteArray>>) {
        uploadJob = scope.launch {
            uploadProgress = 0f
            uploadError = ""
            uploadComplete = false
            logLines.clear()
            try {
                val msg = commitMsg.ifBlank { "Initial commit via GsGit" }
                val ok = GitHubManager.uploadProjectFolder(
                    context, repo.owner, repo.name, branch, files,
                    onProgress = { progress -> uploadProgress = progress },
                    commitMessage = msg,
                    onLog = { line -> logLines.add(line) }
                )
                if (ok) {
                    uploadComplete = true
                    uploadProgress = 1f
                } else {
                    uploadError = "Upload failed"
                    uploadProgress = null
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                uploadError = "Cancelled"
                uploadProgress = null
            } catch (e: Exception) {
                uploadError = e.message ?: "Upload error"
                uploadProgress = null
            }
        }
    }

    fun scanAndPreview(treeUri: Uri) {
        scope.launch {
            scanning = true
            try {
                val files = withContext(Dispatchers.IO) { collectFiles(context, treeUri) }
                if (files.isEmpty()) {
                    uploadError = "No files found in selected folder"
                    scanning = false
                    return@launch
                }
                pendingFiles = files
            } catch (e: Exception) {
                uploadError = e.message ?: "Scan error"
            }
            scanning = false
        }
    }

    fun scanAndPreviewUris(uris: List<Uri>) {
        scope.launch {
            scanning = true
            try {
                val files = withContext(Dispatchers.IO) { collectFilesFromUris(context, uris) }
                if (files.isEmpty()) {
                    uploadError = "No files could be read"
                    scanning = false
                    return@launch
                }
                pendingFiles = files
            } catch (e: Exception) {
                uploadError = e.message ?: "Scan error"
            }
            scanning = false
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? -> if (treeUri != null) scanAndPreview(treeUri) }

    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> -> if (uris.isNotEmpty()) scanAndPreviewUris(uris) }

    GitHubScreenFrame(
        title = "> quick start",
        subtitle = repo.fullName,
        onBack = onBack,
        trailing = {
            if (uploadJob?.isActive == true) {
                BracketButton("[ cancel ]") { cancelUpload() }
            }
        }
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
                        repo.owner, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = palette.accent
                    )
                    androidx.compose.material3.Text(
                        " / ", fontFamily = JetBrainsMono, fontSize = 16.sp, color = palette.textMuted
                    )
                    androidx.compose.material3.Text(
                        repo.name, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = palette.textPrimary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        if (repo.isPrivate) "[ PRIVATE ]" else "[ PUBLIC ]",
                        fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp, color = if (repo.isPrivate) palette.warning else palette.accent
                    )
                }
                Spacer(Modifier.height(8.dp))

                // HTTPS clone url
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        "HTTPS", fontFamily = JetBrainsMono, fontSize = 9.sp,
                        color = palette.textMuted, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Text(
                        cloneUrl, fontFamily = JetBrainsMono, fontSize = 11.sp,
                        color = palette.textSecondary, modifier = Modifier.weight(1f)
                    )
                    BracketButton("[ copy ]") { copyToClipboard(context, "clone url", cloneUrl) }
                }
                // SSH clone url
                if (sshUrl != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(
                            "SSH  ", fontFamily = JetBrainsMono, fontSize = 9.sp,
                            color = palette.textMuted, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        androidx.compose.material3.Text(
                            sshUrl, fontFamily = JetBrainsMono, fontSize = 11.sp,
                            color = palette.textSecondary, modifier = Modifier.weight(1f)
                        )
                        BracketButton("[ copy ]") { copyToClipboard(context, "ssh url", sshUrl) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    "─".repeat(54), fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.border
                )
                Spacer(Modifier.height(12.dp))

                // ═══ Б. ИНТЕРАКТИВНАЯ ЗОНА ДЕЙСТВИЙ ═══
                AiModuleSectionLabel("> actions")
                Spacer(Modifier.height(6.dp))

                // commit message field
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        "commit msg:", fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.textSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .background(palette.surface, RoundedCornerShape(4.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        BasicTextField(
                            value = commitMsg,
                            onValueChange = { commitMsg = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.textPrimary
                            ),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (commitMsg.isEmpty()) {
                                    androidx.compose.material3.Text(
                                        "Initial commit via GsGit",
                                        fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.textMuted
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // upload buttons / preview / progress / complete
                if (uploadComplete) {
                    AiModuleText(
                        "upload complete", fontSize = 12.sp,
                        color = palette.accent, fontFamily = JetBrainsMono
                    )
                    Spacer(Modifier.height(6.dp))
                    BracketButton("[ → open repository ]", accent = true) { onOpenRepo(repo) }
                } else if (uploadJob?.isActive == true) {
                    // upload in progress — show cancel hint in trailing
                } else if (pendingFiles != null) {
                    // file preview — show before uploading
                    val files = pendingFiles!!
                    val totalBytes = files.sumOf { it.second.size }
                    androidx.compose.material3.Text(
                        "${files.size} file(s) selected — ${formatBytes(totalBytes)}",
                        fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    if (files.size <= 8) {
                        files.forEach { (path, bytes) ->
                            androidx.compose.material3.Text(
                                "  $path (${formatBytes(bytes.size)})",
                                fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted
                            )
                        }
                    } else {
                        files.take(5).forEach { (path, bytes) ->
                            androidx.compose.material3.Text(
                                "  $path (${formatBytes(bytes.size)})",
                                fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted
                            )
                        }
                        androidx.compose.material3.Text(
                            "  … and ${files.size - 5} more",
                            fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BracketButton("[ upload now ]", accent = true) {
                            val f = pendingFiles!!
                            pendingFiles = null
                            startUpload(f)
                        }
                        BracketButton("[ cancel ]") { pendingFiles = null }
                    }
                } else if (!scanning) {
                    BracketButton("[ upload project folder ]", accent = true, icon = Icons.Rounded.CreateNewFolder) {
                        folderLauncher.launch(null)
                    }
                    Spacer(Modifier.height(4.dp))
                    BracketButton("[ upload single files ]", icon = Icons.Rounded.NoteAdd) {
                        filesLauncher.launch(arrayOf("*/*"))
                    }
                } else {
                    AiModuleSpinner(label = "scanning files…")
                }

                // error + retry
                if (uploadError.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    AiModuleText(
                        "error: $uploadError", fontSize = 11.sp,
                        color = palette.error, fontFamily = JetBrainsMono
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BracketButton("[ pick folder ]") { folderLauncher.launch(null) }
                        BracketButton("[ pick files ]") { filesLauncher.launch(arrayOf("*/*")) }
                    }
                }

                // auto-init open button
                if (result.autoInit && !uploadComplete) {
                    Spacer(Modifier.height(6.dp))
                    BracketButton("[ → open repository ]", accent = true) { onOpenRepo(repo) }
                }

                // ═══ ТЕРМИНАЛЬНЫЙ ЛОГГЕР ═══
                if (logLines.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    AsciiLogConsole(logLines)
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
                    "ProTip! Use background processing if you are uploading heavy directories.",
                    fontSize = 10.sp, color = palette.textMuted, fontFamily = JetBrainsMono
                )
            }
        }
    }
}

@Composable
private fun BracketButton(label: String, accent: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val color = if (accent) palette.accent else palette.textSecondary
    Row(
        Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(icon, null, Modifier.size(14.dp), tint = color)
        }
        androidx.compose.material3.Text(
            label, fontFamily = JetBrainsMono, fontSize = 12.sp,
            fontWeight = FontWeight.Medium, color = color
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
        androidx.compose.material3.Text(label, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.border)
        lines.forEach { line ->
            val padded = line.padEnd(w - 2)
            androidx.compose.material3.Text("| $padded |", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary)
        }
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.border)
        Spacer(Modifier.height(4.dp))
        BracketButton("[ copy ]", accent = false, onClick = onCopy)
    }
}

@Composable
private fun AsciiLogConsole(lines: List<String>) {
    val palette = AiModuleTheme.colors
    val maxLen = lines.maxOfOrNull { it.length } ?: 20
    val w = (maxLen + 2).coerceIn(30, 70)

    Column {
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.border)
        lines.forEach { line ->
            val tagColor = when {
                line.startsWith("[OK]") || line.startsWith("[DONE]") -> palette.accent
                line.startsWith("[FAIL]") || line.startsWith("[CANCELLED]") -> palette.error
                line.startsWith("[BLOB]") || line.startsWith("[PREPARING]") -> palette.textSecondary
                else -> palette.textMuted
            }
            val padded = line.padEnd(w - 2)
            androidx.compose.material3.Text("| $padded |", fontFamily = JetBrainsMono, fontSize = 9.sp, color = tagColor)
        }
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.border)
    }
}

private fun formatBytes(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clip = ClipData.newPlainText(label, text)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
}

private fun shouldIgnore(name: String): Boolean {
    if (name.startsWith(".")) return true
    val lower = name.lowercase()
    if (lower in IGNORED_DIRS) return true
    return IGNORED_EXTENSIONS.any { lower.endsWith(it) }
}

private fun collectFiles(context: Context, treeUri: Uri): List<Pair<String, ByteArray>> {
    val pickedDir = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
    val files = mutableListOf<Pair<String, ByteArray>>()
    fun walk(dir: DocumentFile, pathPrefix: String) {
        dir.listFiles().forEach { doc ->
            val name = doc.name ?: return@forEach
            if (doc.isDirectory) {
                if (!shouldIgnore(name)) walk(doc, "$pathPrefix$name/")
            } else if (doc.isFile) {
                if (shouldIgnore(name)) return@forEach
                val relPath = "$pathPrefix$name"
                try {
                    val bytes = context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() } ?: return@forEach
                    if (bytes.size > MAX_FILE_SIZE) return@forEach
                    files.add(relPath to bytes)
                } catch (_: Exception) {}
            }
        }
    }
    walk(pickedDir, "")
    return files
}

private fun collectFilesFromUris(context: Context, uris: List<Uri>): List<Pair<String, ByteArray>> {
    val files = mutableListOf<Pair<String, ByteArray>>()
    uris.forEach { uri ->
        try {
            var name: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
            if (name.isNullOrBlank()) {
                name = uri.lastPathSegment?.substringAfterLast(":")?.substringAfterLast("/") ?: "file"
            }
            if (shouldIgnore(name!!)) return@forEach
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEach
            if (bytes.size > MAX_FILE_SIZE) return@forEach
            files.add(name to bytes)
        } catch (_: Exception) {}
    }
    return files
}
