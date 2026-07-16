package gs.git.vps.data.github

import android.content.Context
import android.util.AtomicFile
import gs.git.vps.data.github.model.GHContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

internal enum class CodeCacheSource { NETWORK, DISK }

internal data class CodeCacheResult<T>(
    val value: T,
    val source: CodeCacheSource,
    val cachedAt: Long,
)

/** Best-effort, bounded offline cache. Draft persistence never depends on this directory. */
internal object CodeWorkspaceCache {
    private const val DIR = "code_workspace_cache"
    private const val MAX_ENTRY_BYTES = 2_500_000
    private const val MAX_TOTAL_BYTES = 24_000_000L

    suspend fun loadText(
        context: Context,
        owner: String,
        repo: String,
        branch: String,
        path: String,
    ): CodeCacheResult<String>? = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(context, "text", owner, repo, branch, path)
        try {
            val remote = GitHubManager.getCodeRemoteText(context, owner, repo, branch, path)
            if (remote == null) {
                cacheFile.delete()
                null
            } else {
                val savedAt = System.currentTimeMillis()
                runCatching {
                    write(cacheFile, JSONObject().apply {
                        put("kind", "text")
                        put("savedAt", savedAt)
                        put("content", remote)
                    })
                }
                CodeCacheResult(remote, CodeCacheSource.NETWORK, savedAt)
            }
        } catch (rejected: CodeFileRejectedException) {
            throw rejected
        } catch (_: Exception) {
            val cached = read(cacheFile)?.takeIf { it.optString("kind") == "text" }
                ?: return@withContext null
            val bytes = cached.optString("content", "").toByteArray(Charsets.UTF_8)
            if (bytes.size.toLong() > CODE_TEXT_MAX_BYTES) {
                cacheFile.delete()
                throw CodeFileRejectedException(
                    "Cached file is ${formatCodeFileBytes(bytes.size.toLong())}; safe editor limit is ${formatCodeFileBytes(CODE_TEXT_MAX_BYTES)}.",
                    bytes.size.toLong(),
                )
            }
            CodeCacheResult(
                value = decodeCodeText(bytes),
                source = CodeCacheSource.DISK,
                cachedAt = cached.optLong("savedAt", cacheFile.baseFile.lastModified()),
            )
        }
    }

    suspend fun loadDirectory(
        context: Context,
        owner: String,
        repo: String,
        branch: String,
        path: String,
    ): CodeCacheResult<List<GHContent>>? = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(context, "dir", owner, repo, branch, path)
        try {
            val response = GitHubManager.request(
                context,
                "${GitHubManager.repoPath(owner, repo, "/contents/${GitHubManager.encPath(path)}")}${GitHubManager.refQuery(branch)}",
                trackErrors = false,
            )
            if (response.code == 404) {
                cacheFile.delete()
                return@withContext null
            }
            if (!response.success) throw IllegalStateException("directory request failed (${response.code})")
            val items = parseContents(JSONArray(response.body))
            val savedAt = System.currentTimeMillis()
            runCatching {
                write(cacheFile, JSONObject().apply {
                    put("kind", "dir")
                    put("savedAt", savedAt)
                    put("items", encodeContents(items))
                })
            }
            CodeCacheResult(items, CodeCacheSource.NETWORK, savedAt)
        } catch (_: Exception) {
            read(cacheFile)?.takeIf { it.optString("kind") == "dir" }?.let { cached ->
                CodeCacheResult(
                    value = parseContents(cached.optJSONArray("items") ?: JSONArray()),
                    source = CodeCacheSource.DISK,
                    cachedAt = cached.optLong("savedAt", cacheFile.baseFile.lastModified()),
                )
            }
        }
    }

    private fun parseContents(array: JSONArray): List<GHContent> = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            add(
                GHContent(
                    name = item.optString("name"),
                    path = item.optString("path"),
                    type = item.optString("type"),
                    size = item.optLong("size", 0L),
                    downloadUrl = item.optString("download_url", item.optString("downloadUrl", "")),
                    sha = item.optString("sha", ""),
                )
            )
        }
    }.sortedWith(compareBy<GHContent> { it.type != "dir" }.thenBy { it.name.lowercase() })

    private fun encodeContents(items: List<GHContent>): JSONArray = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("name", item.name)
                put("path", item.path)
                put("type", item.type)
                put("size", item.size)
                put("downloadUrl", item.downloadUrl)
                put("sha", item.sha)
            })
        }
    }

    private fun cacheFile(
        context: Context,
        kind: String,
        owner: String,
        repo: String,
        branch: String,
        path: String,
    ): AtomicFile {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        return AtomicFile(File(dir, "${kind}_${sha256("$owner/$repo@$branch:$path")}.json"))
    }

    private fun read(file: AtomicFile): JSONObject? = runCatching {
        file.openRead().use { input ->
            val bytes = input.readBytes()
            require(bytes.size <= MAX_ENTRY_BYTES)
            JSONObject(bytes.toString(Charsets.UTF_8))
        }
    }.getOrNull()

    private fun write(file: AtomicFile, root: JSONObject) {
        val bytes = root.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_ENTRY_BYTES) return
        val output = file.startWrite()
        try {
            output.write(bytes)
            output.flush()
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
        file.baseFile.parentFile?.let { directory -> runCatching { evict(directory) } }
    }

    private fun evict(directory: File) {
        val files = directory.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= MAX_TOTAL_BYTES) break
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
