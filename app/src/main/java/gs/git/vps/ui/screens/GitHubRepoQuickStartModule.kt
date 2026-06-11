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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
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

private data class StagedItem(val name: String, val size: Int, val isFolder: Boolean, val files: List<Pair<String, ByteArray>>)

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

    // REMOTE tab
    var remoteTab by remember { mutableStateOf(0) } // 0=HTTPS, 1=SSH, 2=SHA, 3=LOCAL

    // UPLOAD staging
    val stagedItems = remember { mutableStateListOf<StagedItem>() }
    var commitMsg by remember { mutableStateOf("") }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadComplete by remember { mutableStateOf(false) }
    val logLines = remember { mutableStateListOf<String>() }
    var uploadError by remember { mutableStateOf("") }

    // COMMANDS expand
    var showCommands by remember { mutableStateOf(false) }

    // starter presets
    var showPresetsDialog by remember { mutableStateOf(false) }

    // scanning
    var scanning by remember { mutableStateOf(false) }

    fun startUpload() {
        uploadJob = scope.launch {
            uploading = true
            uploadError = ""
            uploadComplete = false
            logLines.clear()
            try {
                val files = stagedItems.flatMap { it.files }
                if (files.isEmpty()) {
                    uploadError = "No files to upload"
                    uploading = false
                    return@launch
                }
                val msg = commitMsg.ifBlank { "Initial commit via GsGit" }
                val ok = GitHubManager.uploadProjectFolder(
                    context, repo.owner, repo.name, branch, files,
                    onProgress = {},
                    commitMessage = msg,
                    onLog = { line -> logLines.add(line) }
                )
                if (ok) {
                    uploadComplete = true
                    logLines.add("[DONE] upload complete")
                } else {
                    uploadError = "Upload failed"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                uploadError = "Cancelled"
            } catch (e: Exception) {
                uploadError = e.message ?: "Upload error"
            }
            uploading = false
        }
    }

    fun addFolder(uri: Uri) {
        scope.launch {
            scanning = true
            try {
                val dir = DocumentFile.fromTreeUri(context, uri) ?: return@launch
                val name = dir.name ?: "folder"
                var totalSize = 0
                val collected = withContext(Dispatchers.IO) {
                    val all = collectFilesFromDir(context, dir, "")
                    totalSize = all.sumOf { it.second.size }
                    all
                }
                stagedItems.add(StagedItem("$name/ (${totalSize.formatBytes()})", totalSize, true, collected))
            } catch (_: Exception) {}
            scanning = false
        }
    }

    fun addFiles(uris: List<Uri>) {
        scope.launch {
            scanning = true
            try {
                withContext(Dispatchers.IO) {
                    uris.forEach { uri ->
                        try {
                            var name: String? = null
                            try {
                                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (idx != -1 && cursor.moveToFirst()) name = cursor.getString(idx)
                                }
                            } catch (_: Exception) {}
                            if (name.isNullOrBlank()) name = uri.lastPathSegment?.substringAfterLast(":")?.substringAfterLast("/") ?: "file"
                            val safeName: String = name!!
                            if (shouldIgnore(safeName)) return@forEach
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEach
                            if (bytes.size > MAX_FILE_SIZE) return@forEach
                            stagedItems.add(StagedItem("$safeName (${bytes.size.formatBytes()})", bytes.size, false, listOf(safeName to bytes)))
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
            scanning = false
        }
    }

    fun generateStarter(templateName: String, projectName: String) {
        val filesList = mutableListOf<Pair<String, ByteArray>>()
        val cleanProjectName = projectName.lowercase().replace("[^a-z0-9_-]".toRegex(), "")
        when (templateName) {
            "Node.js" -> {
                val packageJson = """
                    {
                      "name": "$cleanProjectName",
                      "version": "1.0.0",
                      "main": "index.js",
                      "scripts": {
                        "start": "node index.js"
                      },
                      "dependencies": {}
                    }
                """.trimIndent()
                val indexJs = """
                    console.log("Hello from GsGit Node.js template!");
                """.trimIndent()
                val gitignore = """
                    node_modules/
                    npm-debug.log
                    .env
                """.trimIndent()
                filesList.add("package.json" to packageJson.toByteArray(Charsets.UTF_8))
                filesList.add("index.js" to indexJs.toByteArray(Charsets.UTF_8))
                filesList.add(".gitignore" to gitignore.toByteArray(Charsets.UTF_8))
            }
            "Python" -> {
                val mainPy = """
                    if __name__ == "__main__":
                        print("Hello from GsGit Python template!")
                """.trimIndent()
                val requirements = """
                    # Add dependencies here
                """.trimIndent()
                val gitignore = """
                    __pycache__/
                    *.py[cod]
                    .env
                    venv/
                """.trimIndent()
                filesList.add("main.py" to mainPy.toByteArray(Charsets.UTF_8))
                filesList.add("requirements.txt" to requirements.toByteArray(Charsets.UTF_8))
                filesList.add(".gitignore" to gitignore.toByteArray(Charsets.UTF_8))
            }
            "HTML5 Static" -> {
                val indexHtml = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>$projectName</title>
                        <link rel="stylesheet" href="style.css">
                    </head>
                    <body>
                        <h1>Welcome to $projectName</h1>
                        <p>Created with GsGit</p>
                        <script src="app.js"></script>
                    </body>
                    </html>
                """.trimIndent()
                val styleCss = """
                    body {
                        background: #0d1117;
                        color: #c9d1d9;
                        font-family: sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 100vh;
                        margin: 0;
                    }
                    h1 {
                        color: #58a6ff;
                    }
                """.trimIndent()
                val appJs = """
                    console.log("Hello from GsGit Static HTML template!");
                """.trimIndent()
                filesList.add("index.html" to indexHtml.toByteArray(Charsets.UTF_8))
                filesList.add("style.css" to styleCss.toByteArray(Charsets.UTF_8))
                filesList.add("app.js" to appJs.toByteArray(Charsets.UTF_8))
            }
            "Rust" -> {
                val cargoToml = """
                    [package]
                    name = "$cleanProjectName"
                    version = "0.1.0"
                    edition = "2021"

                    [dependencies]
                """.trimIndent()
                val mainRs = """
                    fn main() {
                        println!("Hello from GsGit Rust template!");
                    }
                """.trimIndent()
                val gitignore = """
                    /target
                    **/*.rs.bk
                """.trimIndent()
                filesList.add("Cargo.toml" to cargoToml.toByteArray(Charsets.UTF_8))
                filesList.add("src/main.rs" to mainRs.toByteArray(Charsets.UTF_8))
                filesList.add(".gitignore" to gitignore.toByteArray(Charsets.UTF_8))
            }
        }
        val totalSize = filesList.sumOf { it.second.size }
        stagedItems.add(StagedItem("Starter Preset: $templateName (${totalSize.formatBytes()})", totalSize, true, filesList))
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) addFolder(uri)
    }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) addFiles(uris)
    }

    GitHubScreenFrame(
        title = "> quick start",
        subtitle = repo.fullName,
        onBack = onBack,
        trailing = {
            if (uploading) {
                androidx.compose.material3.Text("uploading…", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.warning)
            }
            if (uploadComplete) {
                BracketBtn("[ open repo ]", accent = true) { onOpenRepo(repo) }
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ═══ ШАПКА ═══
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(repo.owner, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
                    androidx.compose.material3.Text(" / ", fontFamily = JetBrainsMono, fontSize = 16.sp, color = palette.textMuted)
                    androidx.compose.material3.Text(repo.name, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.textPrimary)
                }
                androidx.compose.material3.Text(
                    if (repo.isPrivate) "[ PRIVATE ]" else "[ PUBLIC ]",
                    fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = if (repo.isPrivate) palette.warning else palette.accent
                )
                // clone urls
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("HTTPS", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Text(cloneUrl, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary, modifier = Modifier.weight(1f))
                    BracketBtn("[ copy ]") { copyClipboard(context, "clone url", cloneUrl) }
                }
                if (sshUrl.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text("SSH  ", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        androidx.compose.material3.Text(sshUrl, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary, modifier = Modifier.weight(1f))
                        BracketBtn("[ copy ]") { copyClipboard(context, "ssh url", sshUrl) }
                    }
                }

                // ═══ REMOTE ═══
                SectionHeader("> REMOTE")
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    listOf("HTTPS", "SSH", "SHA", "LOCAL").forEachIndexed { i, tab ->
                        val sel = remoteTab == i
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(topStart = if (i == 0) 8.dp else 0.dp, topEnd = if (i == 3) 8.dp else 0.dp))
                                .background(if (sel) palette.accent.copy(alpha = 0.15f) else palette.surface)
                                .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(topStart = if (i == 0) 8.dp else 0.dp, topEnd = if (i == 3) 8.dp else 0.dp))
                                .clickable { remoteTab = i }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            androidx.compose.material3.Text(tab, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (sel) palette.accent else palette.textSecondary)
                        }
                    }
                }
                CardBox {
                    val remoteValue = when (remoteTab) {
                        0 -> cloneUrl
                        1 -> sshUrl
                        2 -> repo.htmlUrl.replace("https://github.com/", "")
                        3 -> "/storage/emulated/0/Projects/${repo.name}"
                        else -> cloneUrl
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(remoteValue, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        BracketBtn("[ copy ]") { copyClipboard(context, "remote", remoteValue) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = palette.textMuted)
                        androidx.compose.material3.Text("ProTip! Use this URL when adding GitHub as a remote.", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                    }
                }

                // ═══ UPLOAD ═══
                SectionHeader("> UPLOAD")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(Icons.Default.Folder, "Upload Folder") { folderLauncher.launch(null) }
                    ActionChip(Icons.Default.Description, "Upload Files") { filesLauncher.launch(arrayOf("*/*")) }
                    ActionChip(Icons.Default.Info, "Add Starter Preset") { showPresetsDialog = !showPresetsDialog }
                }
                if (showPresetsDialog) {
                    CardBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text("Select a Starter Preset:", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = palette.accent)
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.clickable { showPresetsDialog = false }.padding(4.dp)) {
                                androidx.compose.material3.Icon(Icons.Default.Close, null, Modifier.size(12.dp), tint = palette.error)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val templatesList = listOf("Node.js", "Python", "HTML5 Static", "Rust")
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            templatesList.forEach { name ->
                                Box(
                                    Modifier
                                        .clickable {
                                            generateStarter(name, repo.name)
                                            showPresetsDialog = false
                                        }
                                        .background(palette.surface, RoundedCornerShape(4.dp))
                                        .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        name,
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // commit msg
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("commit msg:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.weight(1f).height(28.dp)
                            .background(palette.surface, RoundedCornerShape(4.dp))
                            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = commitMsg, onValueChange = { commitMsg = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary),
                            singleLine = true,
                            decorationBox = { inner ->
                                if (commitMsg.isEmpty()) androidx.compose.material3.Text("Initial commit via GsGit", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
                                inner()
                            }
                        )
                    }
                }

                // staging list
                if (stagedItems.isNotEmpty()) {
                    CardBox {
                        stagedItems.forEachIndexed { idx, item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Text(
                                    item.name, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    Modifier.clickable { stagedItems.removeAt(idx) }.padding(4.dp)
                                ) {
                                    androidx.compose.material3.Icon(Icons.Default.Close, null, Modifier.size(12.dp), tint = palette.error)
                                }
                            }
                        }
                    }
                    if (!uploading && !uploadComplete) {
                        Spacer(Modifier.height(4.dp))
                        CommitPushButton { startUpload() }
                    }
                }
                if (scanning) AiModuleSpinner(label = "scanning…")
                if (uploading) AiModuleSpinner(label = "uploading…")
                if (uploadError.isNotBlank()) {
                    androidx.compose.material3.Text("error: $uploadError", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.error)
                }
                if (uploadComplete) {
                    androidx.compose.material3.Text("upload complete", fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.accent, fontWeight = FontWeight.SemiBold)
                }
                // log console
                if (logLines.isNotEmpty()) AsciiLogConsole(logLines)

                // ═══ STATUS ═══
                SectionHeader("> STATUS")
                CardBox {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val statusLabel = if (stagedItems.isNotEmpty()) "modified" else "clean"
                        val statusColor = if (stagedItems.isNotEmpty()) palette.warning else palette.accent
                        androidx.compose.material3.Text("[ $statusLabel ]", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Text("${stagedItems.size} staged", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textMuted)
                    }
                }

                // ═══ LAST COMMIT ═══
                if (result.autoInit) {
                    SectionHeader("> LAST COMMIT")
                    CardBox {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text("SHA:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text("initial", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                            BracketBtn("[ copy ]") { copyClipboard(context, "sha", "initial") }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text("Author:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text(repo.owner, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary)
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text("Branch:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text(branch, fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary)
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text("Message:", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textSecondary)
                            Spacer(Modifier.width(4.dp))
                            androidx.compose.material3.Text("Initial commit", fontFamily = JetBrainsMono, fontSize = 10.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ═══ COMMANDS ═══
                SectionHeaderWithToggle("> COMMANDS", if (showCommands) "hide" else "show") { showCommands = !showCommands }
                if (showCommands) {
                    CardBox {
                        val newCmd = "echo \"# ${repo.name}\" >> README.md\ngit init\ngit add README.md\ngit commit -m \"first commit\"\ngit branch -M $branch\ngit remote add origin $cloneUrl\ngit push -u origin $branch"
                        AsciiCodeBlock(newCmd)
                        Spacer(Modifier.height(6.dp))
                        BracketBtn("[ copy ]") { copyClipboard(context, "commands", newCmd) }
                    }
                }

                // ═══ Финальный ProTip ═══
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Info, null, Modifier.size(10.dp), tint = palette.textMuted)
                    androidx.compose.material3.Text("ProTip! Use background processing for heavy directories.", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textMuted)
                }
            }
        }
    }
}

// ─── shared UI primitives ───────────────────────────

@Composable
private fun SectionHeader(text: String) {
    val palette = AiModuleTheme.colors
    androidx.compose.material3.Text(text, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)
}

@Composable
private fun SectionHeaderWithToggle(text: String, toggleLabel: String, onToggle: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(text, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)
        Spacer(Modifier.weight(1f))
        Box(Modifier.clickable(onClick = onToggle).padding(4.dp)) {
            androidx.compose.material3.Text("[ $toggleLabel ]", fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = palette.textSecondary)
        }
    }
}

@Composable
private fun CardBox(content: @Composable () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) { content() }
}

@Composable
private fun BracketBtn(label: String, accent: Boolean = false, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    val color = if (accent) palette.accent else palette.textSecondary
    androidx.compose.material3.Text(
        label, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(palette.accent.copy(alpha = 0.08f)).border(1.dp, palette.accent.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.material3.Icon(icon, null, Modifier.size(14.dp), tint = palette.accent)
        androidx.compose.material3.Text(label, fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = palette.accent)
    }
}

@Composable
private fun CommitPushButton(onClick: () -> Unit) {
    val palette = AiModuleTheme.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(palette.accent.copy(alpha = 0.12f)).border(1.dp, palette.accent, RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text("Commit & Push staged items", fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)
    }
}

@Composable
private fun AsciiCodeBlock(code: String) {
    val palette = AiModuleTheme.colors
    val lines = code.lines()
    val maxLen = lines.maxOfOrNull { it.length } ?: 40
    val w = (maxLen + 2).coerceIn(30, 68)
    androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.border)
    lines.forEach { line ->
        val padded = line.padEnd(w - 2)
        androidx.compose.material3.Text("| $padded |", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.textPrimary)
    }
    androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.border)
}

@Composable
private fun AsciiLogConsole(lines: List<String>) {
    val palette = AiModuleTheme.colors
    val maxLen = lines.maxOfOrNull { it.length } ?: 20
    val w = (maxLen + 2).coerceIn(30, 68)
    CardBox {
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.border)
        lines.forEach { line ->
            val tagColor = when {
                line.startsWith("[OK]") || line.startsWith("[DONE]") -> palette.accent
                line.startsWith("[FAIL]") || line.startsWith("[CANCELLED]") -> palette.error
                else -> palette.textMuted
            }
            val padded = line.padEnd(w - 2)
            androidx.compose.material3.Text("| $padded |", fontFamily = JetBrainsMono, fontSize = 8.sp, color = tagColor)
        }
        androidx.compose.material3.Text("+" + "─".repeat(w) + "+", fontFamily = JetBrainsMono, fontSize = 9.sp, color = palette.border)
    }
}

// ─── utility functions ───────────────────────────────

private fun Int.formatBytes(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${this / 1024} KB"
    else -> "${"%.1f".format(this / (1024.0 * 1024.0))} MB"
}

private fun copyClipboard(context: Context, label: String, text: String) {
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

private fun collectFilesFromDir(context: Context, dir: DocumentFile, prefix: String): List<Pair<String, ByteArray>> {
    val files = mutableListOf<Pair<String, ByteArray>>()
    dir.listFiles().forEach { doc ->
        val name = doc.name ?: return@forEach
        if (doc.isDirectory) {
            if (!shouldIgnore(name)) files.addAll(collectFilesFromDir(context, doc, "$prefix$name/"))
        } else if (doc.isFile) {
            if (shouldIgnore(name)) return@forEach
            try {
                val bytes = context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() } ?: return@forEach
                if (bytes.size <= MAX_FILE_SIZE) files.add("$prefix$name" to bytes)
            } catch (_: Exception) {}
        }
    }
    return files
}
