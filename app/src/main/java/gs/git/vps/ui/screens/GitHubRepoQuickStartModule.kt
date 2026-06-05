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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri == null) return@rememberLauncherForActivityResult
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

    GitHubScreenFrame(
        title = "> quick start",
        subtitle = repo.fullName,
        onBack = onBack,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!result.autoInit) {
                CodeBlock(
                    title = "create a new repository on the command line",
                    code = "echo \"# ${repo.name}\" >> README.md\ngit init\ngit add README.md\ngit commit -m \"initial commit\"\ngit remote add origin $cloneUrl\ngit push -u origin $branch",
                    onCopy = { copyToClipboard(context, "git init && git push to $cloneUrl", "echo \"# ${repo.name}\" >> README.md\ngit init\ngit add README.md\ngit commit -m \"initial commit\"\ngit remote add origin $cloneUrl\ngit push -u origin $branch") }
                )
                CodeBlock(
                    title = "push an existing repository from the command line",
                    code = "git remote add origin $cloneUrl\ngit branch -M $branch\ngit push -u origin $branch",
                    onCopy = { copyToClipboard(context, "git remote add + push", "git remote add origin $cloneUrl\ngit branch -M $branch\ngit push -u origin $branch") }
                )
            }

            AiModuleSectionLabel("> upload from device")
            if (uploadProgress != null) {
                LinearProgressIndicator(
                    progress = { uploadProgress!!.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = palette.accent,
                    trackColor = palette.surface,
                )
                AiModuleText(
                    when {
                        uploadProgress!! < 0.6f -> "uploading blobs… ${(uploadProgress!! / 0.6f * 100).toInt()}%"
                        uploadProgress!! < 0.7f -> "creating tree…"
                        uploadProgress!! < 0.8f -> "creating commit…"
                        uploadProgress!! < 0.9f -> "updating ref…"
                        uploadProgress!! >= 1f -> "done"
                        else -> "processing…"
                    },
                    fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono
                )
            }
            if (uploadError.isNotBlank()) {
                AiModuleText("error: $uploadError", fontSize = 11.sp, color = palette.error, fontFamily = JetBrainsMono)
            }
            if (!uploadComplete && uploadProgress == null) {
                AiModulePillButton("📁 upload project folder", onClick = {
                    folderLauncher.launch(null)
                }, accent = true)
            }

            if (uploadComplete || result.autoInit) {
                Spacer(Modifier.height(8.dp))
                AiModulePillButton("→ open repository", onClick = { onOpenRepo(repo) }, accent = true)
            }
        }
    }
}

@Composable
private fun CodeBlock(title: String, code: String, onCopy: () -> Unit) {
    val palette = AiModuleTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AiModuleSectionLabel(title)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                .background(palette.surface)
                .padding(10.dp)
        ) {
            AiModuleText(code, fontSize = 11.sp, fontFamily = JetBrainsMono, color = palette.textPrimary, lineHeight = 16.sp)
        }
        AiModulePillButton("copy", onClick = onCopy, accent = false)
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
