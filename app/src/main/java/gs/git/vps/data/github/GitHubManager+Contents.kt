package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHBlameRange
import gs.git.vps.data.github.model.GHFileDeleteResult
import gs.git.vps.data.github.model.GHFileSaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Домен Contents слоя GitHub API — высокоуровневые операции с файлами/деревом репозитория:
 * чтение содержимого, blame, clone (zipball), upload (Contents API и Git Data pipeline),
 * commit рабочих изменений, удаление/скачивание файлов, заливка папки/проекта.
 * Нарезан по эталону Releases (см. docs/decomposition-log.md). Сеть — через ядро `request()`,
 * парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * Низкоуровневые git-объекты (getGitRef/createGitBlob/…) — домен GitData (тот же пакет, без импорта).
 * Прямой `openConnection()` в cloneRepo/downloadFile — законное исключение для бинарных стримов
 * (zip/raw download), которые текстовое ядро `request()` не обслуживает.
 */

private const val CONTENTS_TAG = "GH"

internal suspend fun GitHubManager.getFileContent(context: Context, owner: String, repo: String, path: String, branch: String? = null): String {
    val r = request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}${refQuery(branch)}")
    if (!r.success) return ""
    return try {
        val j = JSONObject(r.body)
        val content = j.optString("content", "")
        String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
    } catch (e: Exception) { "" }
}

internal suspend fun GitHubManager.getFileBlame(context: Context, owner: String, repo: String, path: String, branch: String? = null): List<GHBlameRange> {
    val ref = branch?.takeIf { it.isNotBlank() } ?: "HEAD"
    val query = """query(${"$"}owner: String!, ${"$"}repo: String!, ${"$"}expression: String!) {
        repository(owner: ${"$"}owner, name: ${"$"}repo) {
            object(expression: ${"$"}expression) {
                ... on Blob { blame(path: "${path}") { ranges { startingLine endingLine age commit { oid abbreviatedOid message author { name avatarUrl date } } } } }
            }
        }
    }"""
    val variables = JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
        put("expression", "$ref:$path")
    }
    val data = graphql(context, query, variables) ?: return emptyList()
    return try {
        val blob = data.optJSONObject("repository")?.optJSONObject("object") ?: return emptyList()
        val ranges = blob.optJSONObject("blame")?.optJSONArray("ranges") ?: return emptyList()
        (0 until ranges.length()).map { i ->
            val r = ranges.getJSONObject(i)
            val commit = r.optJSONObject("commit")
            GHBlameRange(
                startLine = r.optInt("startingLine"),
                endLine = r.optInt("endingLine"),
                sha = commit?.optString("oid")?.takeIf { it.isNotBlank() }
                    ?: commit?.optString("abbreviatedOid").orEmpty(),
                message = commit?.optString("message")?.lineSequence()?.firstOrNull() ?: "",
                author = commit?.optJSONObject("author")?.optString("name") ?: "?",
                date = commit?.optJSONObject("author")?.optString("date") ?: "",
                avatarUrl = commit?.optJSONObject("author")?.optString("avatarUrl") ?: ""
            )
        }
    } catch (e: Exception) { Log.e(CONTENTS_TAG, "Parse blame failed"); emptyList() }
}

internal suspend fun GitHubManager.cloneRepo(context: Context, owner: String, repo: String, destDir: java.io.File, onProgress: (String) -> Unit): Boolean =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            onProgress("Downloading...")
            val zipUrl = "${getApiUrl()}/repos/$owner/$repo/zipball"
            val token = GitHubAuth.resolveApiToken(context)
            val conn = openDownloadConnection(
                url = zipUrl,
                token = token,
                accept = "application/vnd.github.v3+json",
                connectTimeoutMs = 15_000,
                readTimeoutMs = 60_000,
            ) ?: return@withContext false
            val code = conn.responseCode
            if (code != 200) { conn.disconnect(); return@withContext false }

            val zipFile = java.io.File(destDir, "$repo.zip")
            destDir.mkdirs()
            conn.inputStream.use { input -> zipFile.outputStream().use { output -> input.copyTo(output) } }
            conn.disconnect()

            onProgress("Extracting...")
            val outDir = java.io.File(destDir, repo)
            outDir.mkdirs()
            val safeOutDir = outDir.canonicalFile
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(java.io.FileInputStream(zipFile))).use { zip ->
                var entry = zip.nextEntry
                val rootPrefix = entry?.name?.substringBefore("/", "") ?: ""
                while (entry != null) {
                    val name = entry.name.removePrefix("$rootPrefix/")
                    if (name.isNotBlank()) {
                        val target = java.io.File(safeOutDir, name).canonicalFile
                        if (!target.path.startsWith(safeOutDir.path + java.io.File.separator)) {
                            throw java.io.IOException("Unsafe archive entry: ${entry.name}")
                        }
                        if (entry.isDirectory) target.mkdirs()
                        else { target.parentFile?.mkdirs(); target.outputStream().use { zip.copyTo(it) } }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            zipFile.delete()
            onProgress("Done")
            true
        } catch (e: Exception) {
            Log.e(CONTENTS_TAG, "Clone failed")
            onProgress("Error: ${e.message}")
            false
        }
    }

internal suspend fun GitHubManager.uploadFile(
    context: Context, owner: String, repo: String, path: String,
    content: ByteArray, message: String, branch: String? = null, sha: String? = null
): Boolean {
    val b64 = android.util.Base64.encodeToString(content, android.util.Base64.NO_WRAP)
    val body = JSONObject().apply {
        put("message", message)
        put("content", b64)
        if (!sha.isNullOrBlank()) put("sha", sha)
        if (branch != null) put("branch", branch)
    }.toString()
    return request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}", "PUT", body).success
}

internal suspend fun GitHubManager.uploadFileWithResult(
    context: Context, owner: String, repo: String, path: String,
    content: ByteArray, message: String, branch: String? = null, sha: String? = null
): GHFileSaveResult {
    val b64 = android.util.Base64.encodeToString(content, android.util.Base64.NO_WRAP)
    val body = JSONObject().apply {
        put("message", message)
        put("content", b64)
        if (!sha.isNullOrBlank()) put("sha", sha)
        if (branch != null) put("branch", branch)
    }.toString()
    val r = request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}", "PUT", body)
    if (!r.success) return GHFileSaveResult(false, "", r.body)
    val newSha = try {
        JSONObject(r.body).optJSONObject("content")?.optString("sha").orEmpty()
    } catch (_: Exception) { "" }
    return GHFileSaveResult(true, newSha, "")
}

internal suspend fun GitHubManager.uploadFileFromPath(
    context: Context, owner: String, repo: String, repoPath: String,
    localPath: String, message: String, branch: String? = null
): Boolean = withContext(Dispatchers.IO) {
    try {
        val file = java.io.File(localPath)
        if (!file.exists()) return@withContext false
        val bytes = file.readBytes()
        uploadFile(context, owner, repo, repoPath, bytes, message, branch)
    } catch (e: Exception) {
        Log.e(CONTENTS_TAG, "Upload from path failed")
        false
    }
}

internal suspend fun GitHubManager.uploadMultipleFiles(
    context: Context, owner: String, repo: String, branch: String,
    files: List<Pair<String, ByteArray>>, message: String,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
): Boolean = withContext(Dispatchers.IO) {
    try {
        val refR = request(context, "/repos/$owner/$repo/git/ref/heads/$branch")
        if (!refR.success) return@withContext false
        val latestSha = JSONObject(refR.body).getJSONObject("object").getString("sha")

        val commitR = request(context, "/repos/$owner/$repo/git/commits/$latestSha")
        if (!commitR.success) return@withContext false
        val baseTree = JSONObject(commitR.body).getJSONObject("tree").getString("sha")

        val treeItems = JSONArray()
        files.forEachIndexed { index, (path, content) ->
            onProgress(index + 1, files.size)
            val b64 = android.util.Base64.encodeToString(content, android.util.Base64.NO_WRAP)
            val blobBody = JSONObject().apply { put("content", b64); put("encoding", "base64") }.toString()
            val blobR = request(context, "/repos/$owner/$repo/git/blobs", "POST", blobBody)
            if (!blobR.success) return@withContext false
            val blobSha = JSONObject(blobR.body).getString("sha")
            treeItems.put(JSONObject().apply {
                put("path", path); put("mode", "100644"); put("type", "blob"); put("sha", blobSha)
            })
        }

        val treeBody = JSONObject().apply { put("base_tree", baseTree); put("tree", treeItems) }.toString()
        val treeR = request(context, "/repos/$owner/$repo/git/trees", "POST", treeBody)
        if (!treeR.success) return@withContext false
        val newTree = JSONObject(treeR.body).getString("sha")

        val commitBody = JSONObject().apply {
            put("message", message); put("tree", newTree)
            put("parents", JSONArray().put(latestSha))
        }.toString()
        val newCommitR = request(context, "/repos/$owner/$repo/git/commits", "POST", commitBody)
        if (!newCommitR.success) return@withContext false
        val newCommitSha = JSONObject(newCommitR.body).getString("sha")

        val refBody = JSONObject().apply { put("sha", newCommitSha) }.toString()
        request(context, "/repos/$owner/$repo/git/refs/heads/$branch", "PATCH", refBody).success
    } catch (e: Exception) {
        Log.e(CONTENTS_TAG, "Multi upload failed")
        false
    }
}

internal suspend fun GitHubManager.commitWorkspaceChanges(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    changes: List<Pair<String, ByteArray?>>,
    message: String,
): String = withContext(Dispatchers.IO) {
    try {
        if (changes.isEmpty()) return@withContext ""
        val refR = request(context, "/repos/$owner/$repo/git/ref/heads/$branch")
        if (!refR.success) return@withContext ""
        val latestSha = JSONObject(refR.body).getJSONObject("object").getString("sha")

        val commitR = request(context, "/repos/$owner/$repo/git/commits/$latestSha")
        if (!commitR.success) return@withContext ""
        val baseTree = JSONObject(commitR.body).getJSONObject("tree").getString("sha")

        val treeItems = JSONArray()
        changes.forEach { (path, content) ->
            val item = JSONObject().apply {
                put("path", path)
                put("mode", "100644")
                put("type", "blob")
            }
            if (content == null) {
                item.put("sha", JSONObject.NULL)
            } else {
                val b64 = android.util.Base64.encodeToString(content, android.util.Base64.NO_WRAP)
                val blobBody = JSONObject().apply {
                    put("content", b64)
                    put("encoding", "base64")
                }.toString()
                val blobR = request(context, "/repos/$owner/$repo/git/blobs", "POST", blobBody)
                if (!blobR.success) return@withContext ""
                item.put("sha", JSONObject(blobR.body).getString("sha"))
            }
            treeItems.put(item)
        }

        val treeBody = JSONObject().apply {
            put("base_tree", baseTree)
            put("tree", treeItems)
        }.toString()
        val treeR = request(context, "/repos/$owner/$repo/git/trees", "POST", treeBody)
        if (!treeR.success) return@withContext ""
        val newTree = JSONObject(treeR.body).getString("sha")

        val commitBody = JSONObject().apply {
            put("message", message)
            put("tree", newTree)
            put("parents", JSONArray().put(latestSha))

            if (gs.git.vps.security.PgpKeyManager.isPgpEnabled(context)) {
                val privKey = gs.git.vps.security.PgpKeyManager.getPrivateKey(context)
                val passphrase = gs.git.vps.security.PgpKeyManager.getPassphrase(context)
                if (privKey != null && passphrase != null) {
                    val rawUser = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, null)
                    val (name, email) = if (rawUser != null) {
                        val j = JSONObject(rawUser)
                        val n = j.optString("name", "")
                        val e = j.optString("email", "")
                        Pair(n.ifBlank { j.optString("login", "GsGit") }, e.ifBlank { "${j.optString("login", "gsgit")}@users.noreply.github.com" })
                    } else {
                        Pair("GsGit", "gsgit@users.noreply.github.com")
                    }

                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val dateStr = sdf.format(java.util.Date())

                    val epochSec = java.util.Date().time / 1000
                    val gitTime = "$epochSec +0000"

                    val payload = "tree $newTree\nparent $latestSha\nauthor $name <$email> $gitTime\ncommitter $name <$email> $gitTime\n\n$message"
                    val signature = gs.git.vps.security.PgpKeyManager.signPayload(payload, privKey, passphrase)
                    if (signature != null) {
                        put("signature", signature)
                        put("author", JSONObject().apply {
                            put("name", name)
                            put("email", email)
                            put("date", dateStr)
                        })
                        put("committer", JSONObject().apply {
                            put("name", name)
                            put("email", email)
                            put("date", dateStr)
                        })
                    }
                }
            }
        }.toString()
        val newCommitR = request(context, "/repos/$owner/$repo/git/commits", "POST", commitBody)
        if (!newCommitR.success) return@withContext ""
        val newCommitSha = JSONObject(newCommitR.body).getString("sha")

        val refBody = JSONObject().apply { put("sha", newCommitSha) }.toString()
        val refUpdate = request(context, "/repos/$owner/$repo/git/refs/heads/$branch", "PATCH", refBody)
        if (refUpdate.success) {
            gs.git.vps.data.github.LocalTimeTravelManager.addReflogEntry(context, "$owner/$repo", "heads/$branch", latestSha, newCommitSha, "commit: $message")
            newCommitSha
        } else ""
    } catch (e: Exception) {
        Log.e(CONTENTS_TAG, "Workspace commit failed")
        ""
    }
}

internal suspend fun GitHubManager.deleteFile(
    context: Context, owner: String, repo: String, path: String,
    sha: String, message: String, branch: String? = null
): Boolean = deleteFileWithResult(context, owner, repo, path, sha, message, branch).success

internal suspend fun GitHubManager.deleteFileWithResult(
    context: Context, owner: String, repo: String, path: String,
    sha: String, message: String, branch: String? = null
): GHFileDeleteResult {
    val body = JSONObject().apply {
        put("message", message); put("sha", sha)
        if (branch != null) put("branch", branch)
    }.toString()
    val result = request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}", "DELETE", body)
    return GHFileDeleteResult(
        success = result.success,
        error = if (result.success) "" else apiErrorMessage(result),
    )
}

internal suspend fun GitHubManager.downloadFile(context: Context, owner: String, repo: String, path: String, destFile: java.io.File, branch: String? = null): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val r = request(context, "${repoPath(owner, repo, "/contents/${encPath(path)}")}${refQuery(branch)}")
            if (!r.success) return@withContext false
            val j = JSONObject(r.body)
            val downloadUrl = j.optString("download_url", "")
            if (downloadUrl.isBlank()) return@withContext false

            val token = GitHubAuth.resolveApiToken(context)
            val conn = openDownloadConnection(
                url = downloadUrl,
                token = token,
                connectTimeoutMs = 15_000,
                readTimeoutMs = 30_000,
            ) ?: return@withContext false
            destFile.parentFile?.mkdirs()
            conn.inputStream.use { input -> destFile.outputStream().use { out -> input.copyTo(out) } }
            conn.disconnect()
            true
        } catch (e: Exception) { Log.e(CONTENTS_TAG, "Download failed"); false }
    }

internal suspend fun GitHubManager.uploadDirectory(
    context: Context, owner: String, repo: String, branch: String,
    localDir: java.io.File, repoBasePath: String = "", message: String,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
): Boolean {
    val allFiles = mutableListOf<Pair<String, ByteArray>>()
    collectFiles(localDir, localDir, repoBasePath, allFiles)
    if (allFiles.isEmpty()) return false
    return uploadMultipleFiles(context, owner, repo, branch, allFiles, message, onProgress)
}

private fun collectFiles(root: java.io.File, current: java.io.File, basePath: String, result: MutableList<Pair<String, ByteArray>>) {
    current.listFiles()?.forEach { f ->
        val rel = if (basePath.isNotBlank()) "$basePath/${f.name}" else f.name
        if (f.isDirectory) collectFiles(root, f, rel, result)
        else if (f.length() < 50 * 1024 * 1024) {
            try { result.add(rel to f.readBytes()) } catch (_: Exception) {}
        }
    }
}

internal suspend fun GitHubManager.uploadProjectFolder(
    context: Context, owner: String, repo: String, branch: String,
    files: List<Pair<String, ByteArray>>,
    onProgress: (Float) -> Unit,
    commitMessage: String = "Initial commit via GsGit",
    onLog: (String) -> Unit = {}
): Boolean {
    if (files.isEmpty()) return false
    return try {
        withContext(Dispatchers.IO) {
            onLog("[PREPARING] ${files.size} file(s) to upload")

            // Check if repo is empty by trying to resolve the branch ref
            val ref = getGitRef(context, owner, repo, "heads/$branch")
            if (ref == null) {
                // Empty repo — Git Data API won't work, use Contents API for first file
                onLog("[INIT] repo is empty, seeding via Contents API")
                val (firstPath, firstBytes) = files.first()
                onProgress(0.05f)
                onLog("[PUT] $firstPath (${firstBytes.size} bytes)")
                val seed = uploadFileWithResult(context, owner, repo, firstPath, firstBytes, commitMessage, branch)
                if (!seed.success) {
                    onLog("[FAIL] Contents API seed: ${seed.error.take(300)}")
                    throw Exception("Contents API seed failed for $firstPath")
                }
                onLog("[OK] seeded $firstPath — repo now has initial commit")
                onProgress(0.1f)

                // Remaining files via normal Git Data pipeline
                if (files.size == 1) {
                    onLog("[DONE] single file uploaded via Contents API")
                    onProgress(1f)
                    return@withContext
                }
                val remaining = files.drop(1)
                onLog("[BLOBS] ${remaining.size} remaining file(s) via Git Data API")

                val seedRef = getGitRef(context, owner, repo, "heads/$branch")
                    ?: throw Exception("ref not found after seed")
                val parentSha = seedRef.nodeSha
                onLog("[REF] parent commit ${parentSha.take(7)}")
                val parentCommit = getGitCommit(context, owner, repo, parentSha)
                    ?: throw Exception("commit not found after seed")
                val baseTreeSha = parentCommit.treeSha

                val blobShas = mutableListOf<Pair<String, String>>()
                remaining.forEachIndexed { i, (path, bytes) ->
                    try {
                        onProgress(0.1f + i.toFloat() / remaining.size * 0.6f)
                        onLog("[BLOB] $path (${bytes.size} bytes)")
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val blob = createGitBlob(context, owner, repo, b64, "base64", onLog)
                            ?: throw Exception("blob API returned null for $path")
                        blobShas.add(path to blob.sha)
                        onLog("[OK] blob ${blob.sha.take(7)}")
                    } catch (e: Exception) {
                        onLog("[FAIL] $path: ${e.javaClass.simpleName} -> ${e.message}")
                        throw e
                    }
                }
                onProgress(0.75f)
                onLog("[TREE] building tree with ${blobShas.size} entries")
                val treeItems = JSONArray()
                blobShas.forEach { (p, sha) ->
                    treeItems.put(JSONObject().apply {
                        put("path", p); put("mode", "100644"); put("type", "blob"); put("sha", sha)
                    })
                }
                val treeBody = JSONObject().apply {
                    put("base_tree", baseTreeSha); put("tree", treeItems)
                }.toString()
                val treeR = request(context, "/repos/$owner/$repo/git/trees", "POST", treeBody)
                if (!treeR.success) throw Exception("tree HTTP ${treeR.code}: ${treeR.body.take(200)}")
                val newTreeSha = JSONObject(treeR.body).optString("sha", "")
                if (newTreeSha.isBlank()) throw Exception("tree sha empty")
                onLog("[OK] tree ${newTreeSha.take(7)}")
                onProgress(0.85f)
                onLog("[COMMIT] $commitMessage")
                val commit = createGitCommit(context, owner, repo, commitMessage, newTreeSha, listOf(parentSha))
                    ?: throw Exception("commit create failed")
                onLog("[OK] commit ${commit.sha.take(7)}")
                onProgress(0.95f)
                onLog("[REF] updating heads/$branch → ${commit.sha.take(7)}")
                updateGitRef(context, owner, repo, "heads/$branch", commit.sha)
                    ?: throw Exception("ref update failed")
                onLog("[DONE] upload complete")
                onProgress(1f)
                return@withContext
            }

            // Normal path — repo already has commits
            val parentSha = ref.nodeSha
            onLog("[REF] parent commit ${parentSha.take(7)}")
            val parentCommit = getGitCommit(context, owner, repo, parentSha)
                ?: throw Exception("commit not found")
            val baseTreeSha = parentCommit.treeSha
            onProgress(0.05f)

            val blobShas = mutableListOf<Pair<String, String>>()
            files.forEachIndexed { i, (path, bytes) ->
                try {
                    onProgress(0.05f + i.toFloat() / files.size * 0.6f)
                    onLog("[BLOB] $path (${bytes.size} bytes)")
                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val blob = createGitBlob(context, owner, repo, b64, "base64", onLog)
                        ?: throw Exception("blob API returned null for $path")
                    blobShas.add(path to blob.sha)
                    onLog("[OK] blob ${blob.sha.take(7)}")
                } catch (e: Exception) {
                    onLog("[FAIL] $path: ${e.javaClass.simpleName} -> ${e.message}")
                    throw e
                }
            }
            onProgress(0.7f)
            onLog("[TREE] building tree with ${blobShas.size} entries")
            val treeItems = JSONArray()
            blobShas.forEach { (p, sha) ->
                treeItems.put(JSONObject().apply {
                    put("path", p); put("mode", "100644"); put("type", "blob"); put("sha", sha)
                })
            }
            val treeBody = JSONObject().apply {
                put("base_tree", baseTreeSha); put("tree", treeItems)
            }.toString()
            val treeR = request(context, "/repos/$owner/$repo/git/trees", "POST", treeBody)
            if (!treeR.success) throw Exception("tree HTTP ${treeR.code}: ${treeR.body.take(200)}")
            val newTreeSha = JSONObject(treeR.body).optString("sha", "")
            if (newTreeSha.isBlank()) throw Exception("tree sha empty")
            onLog("[OK] tree ${newTreeSha.take(7)}")
            onProgress(0.8f)
            onLog("[COMMIT] $commitMessage")
            val commit = createGitCommit(context, owner, repo, commitMessage, newTreeSha, listOf(parentSha))
                ?: throw Exception("commit create failed")
            onLog("[OK] commit ${commit.sha.take(7)}")
            onProgress(0.9f)
            onLog("[REF] updating heads/$branch → ${commit.sha.take(7)}")
            updateGitRef(context, owner, repo, "heads/$branch", commit.sha)
                ?: throw Exception("ref update failed")
            onLog("[DONE] upload complete")
            onProgress(1f)
        }
        true
    } catch (e: Exception) { onLog("[FAIL] ${e.message}"); onProgress(-1f); false }
}
