package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import gs.git.vps.data.github.model.GHAsset
import gs.git.vps.data.github.model.GHRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Домен Releases слоя GitHub API — эталон конвенции декомпозиции (см. docs/decomposition-log.md).
 *
 * Всё, что обращается к сети, идёт через ядро `GitHubManager.request()` (ETag-кэш, retry на
 * rate-limit, backoff). Исключение — загрузка/выгрузка бинарных ассетов: ядро возвращает текстовый
 * body и не умеет стримить файлы, поэтому здесь используется прямой `openConnection()` для потока.
 *
 * Сигнатуры публичных вызовов (`GitHubManager.getReleases(...)` и т.п.) не изменились при выносе.
 */

internal suspend fun GitHubManager.getReleases(context: Context, owner: String, repo: String, page: Int = 1): List<GHRelease> {
    val r = request(context, "/repos/$owner/$repo/releases?per_page=20&page=$page")
    if (!r.success) return emptyList()
    val releases = try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGHRelease(arr.getJSONObject(i)) }
    } catch (e: Exception) { return emptyList() }
    val nextPage = parseNextPage(r.headers) ?: return releases
    return releases + getReleases(context, owner, repo, nextPage)
}

internal suspend fun GitHubManager.createRelease(context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean = false): Boolean {
    val json = JSONObject().apply {
        put("tag_name", tag)
        put("name", name)
        put("body", body)
        put("prerelease", prerelease)
    }.toString()
    return request(context, "/repos/$owner/$repo/releases", "POST", json).success
}

internal suspend fun GitHubManager.createReleaseDetailed(
    context: Context,
    owner: String,
    repo: String,
    tag: String,
    name: String,
    body: String,
    prerelease: Boolean = false,
    draft: Boolean = false,
    targetCommitish: String = ""
): GHRelease? {
    val json = JSONObject().apply {
        put("tag_name", tag)
        if (targetCommitish.isNotBlank()) put("target_commitish", targetCommitish)
        put("name", name)
        put("body", body)
        put("prerelease", prerelease)
        put("draft", draft)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/releases", "POST", json)
    if (!r.success) return null
    return try { parseGHRelease(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getReleaseByTag(context: Context, owner: String, repo: String, tag: String): GHRelease? {
    val encodedTag = URLEncoder.encode(tag, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/releases/tags/$encodedTag")
    if (!r.success) return null
    return try { parseGHRelease(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateRelease(context: Context, owner: String, repo: String, tag: String, name: String, body: String, prerelease: Boolean): Boolean =
    updateReleaseDetailed(context, owner, repo, tag, name, body, prerelease, draft = null) != null

internal suspend fun GitHubManager.updateReleaseDetailed(
    context: Context,
    owner: String,
    repo: String,
    tag: String,
    name: String,
    body: String,
    prerelease: Boolean,
    draft: Boolean? = null,
    releaseId: Long = 0L
): GHRelease? {
    val resolvedReleaseId = if (releaseId > 0L) releaseId else {
        val encodedTag = URLEncoder.encode(tag, "UTF-8")
        val r = request(context, "/repos/$owner/$repo/releases/tags/$encodedTag")
        if (!r.success) return null
        JSONObject(r.body).optLong("id")
    }
    if (resolvedReleaseId == 0L) return null
    val json = JSONObject().apply {
        put("tag_name", tag)
        put("name", name)
        put("body", body)
        put("prerelease", prerelease)
        if (draft != null) put("draft", draft)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/releases/$resolvedReleaseId", "PATCH", json)
    if (!r.success) return null
    return try { parseGHRelease(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.publishRelease(context: Context, owner: String, repo: String, release: GHRelease): GHRelease? {
    if (!release.draft) return release
    val tag = release.tag.ifBlank { return null }
    val name = release.name.ifBlank { tag }
    return updateReleaseDetailed(
        context = context,
        owner = owner,
        repo = repo,
        tag = tag,
        name = name,
        body = release.body,
        prerelease = release.prerelease,
        draft = false,
        releaseId = release.id
    )
}

internal suspend fun GitHubManager.deleteRelease(context: Context, owner: String, repo: String, tag: String): Boolean {
    val encodedTag = URLEncoder.encode(tag, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/releases/tags/$encodedTag")
    if (!r.success) return false
    val releaseId = JSONObject(r.body).optLong("id")
    if (releaseId == 0L) return false
    return request(context, "/repos/$owner/$repo/releases/$releaseId", "DELETE").let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.deleteReleaseAsset(context: Context, owner: String, repo: String, assetId: Long): Boolean =
    request(context, "/repos/$owner/$repo/releases/assets/$assetId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.downloadReleaseAsset(context: Context, asset: GHAsset, destFile: java.io.File): Boolean =
    withContext(Dispatchers.IO) {
        try {
            if (asset.downloadUrl.isBlank()) return@withContext false
            val token = GitHubAuth.resolveApiToken(context)
            val conn = openDownloadConnection(
                url = asset.downloadUrl,
                token = token,
                accept = "application/octet-stream",
                connectTimeoutMs = 15_000,
                readTimeoutMs = 60_000,
            ) ?: return@withContext false
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext false
            }
            destFile.parentFile?.mkdirs()
            conn.inputStream.use { input -> destFile.outputStream().use { out -> input.copyTo(out) } }
            conn.disconnect()
            true
        } catch (e: Exception) {
            Log.e(RELEASES_TAG, "Download release asset failed")
            false
        }
    }

internal suspend fun GitHubManager.downloadReleaseAssetWithProgress(
    context: Context,
    asset: GHAsset,
    destFile: java.io.File,
    onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
): Boolean =
    withContext(Dispatchers.IO) {
        try {
            if (asset.downloadUrl.isBlank()) return@withContext false
            val token = GitHubAuth.resolveApiToken(context)
            val conn = openDownloadConnection(
                url = asset.downloadUrl,
                token = token,
                accept = "application/octet-stream",
                connectTimeoutMs = 15_000,
                readTimeoutMs = 60_000,
            ) ?: return@withContext false
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext false
            }
            destFile.parentFile?.mkdirs()
            val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else asset.size
            conn.inputStream.use { input ->
                destFile.outputStream().use { out ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var bytesDownloaded = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        onProgress(bytesDownloaded, totalBytes)
                    }
                }
            }
            conn.disconnect()
            true
        } catch (e: Exception) {
            Log.e(RELEASES_TAG, "Download release asset with progress failed")
            false
        }
    }

internal suspend fun GitHubManager.uploadReleaseAsset(context: Context, owner: String, repo: String, releaseId: Long, file: java.io.File, label: String = ""): Boolean =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            val token = GitHubAuth.resolveApiToken(context)
            val uploadUrl = "${getApiUrl()}/repos/$owner/$repo/releases/$releaseId/assets?name=${URLEncoder.encode(file.name, "UTF-8")}"
            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("Content-Type", releaseContentType(file.name))
                doOutput = true
                connectTimeout = 30000
                readTimeout = 120000
            }
            file.inputStream().use { input -> conn.outputStream.use { output -> input.copyTo(output) } }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            Log.e(RELEASES_TAG, "Upload asset failed")
            false
        }
    }

internal suspend fun GitHubManager.uploadReleaseAssetDetailed(context: Context, owner: String, repo: String, releaseId: Long, file: java.io.File, label: String = ""): GHAsset? =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            val token = GitHubAuth.resolveApiToken(context)
            val labelQuery = label.takeIf { it.isNotBlank() }?.let { "&label=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val uploadUrl = "${getApiUrl()}/repos/$owner/$repo/releases/$releaseId/assets?name=${URLEncoder.encode(file.name, "UTF-8")}$labelQuery"
            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("Content-Type", releaseContentType(file.name))
                doOutput = true
                connectTimeout = 30000
                readTimeout = 120000
            }
            file.inputStream().use { input -> conn.outputStream.use { output -> input.copyTo(output) } }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code in 200..299) parseGHAsset(JSONObject(text)) else null
        } catch (e: Exception) {
            Log.e(RELEASES_TAG, "Upload asset failed")
            null
        }
    }

// --- Парсинг: чистые функции JSON→модель, без IO (конвенция parseGHX) ---

internal fun parseGHRelease(j: JSONObject): GHRelease {
    val assetsArr = j.optJSONArray("assets") ?: JSONArray()
    val assets = (0 until assetsArr.length()).map { i -> parseGHAsset(assetsArr.getJSONObject(i)) }
    return GHRelease(
        id = j.optLong("id"),
        tag = j.optString("tag_name"),
        name = j.optString("name", ""),
        body = j.optString("body", ""),
        prerelease = j.optBoolean("prerelease", false),
        draft = j.optBoolean("draft", false),
        createdAt = j.optString("published_at", j.optString("created_at", "")),
        htmlUrl = j.optString("html_url", ""),
        uploadUrl = j.optString("upload_url", "").substringBefore("{"),
        assets = assets
    )
}

internal fun parseGHAsset(j: JSONObject): GHAsset = GHAsset(
    id = j.optLong("id"),
    name = j.optString("name"),
    size = j.optLong("size", 0),
    downloadUrl = j.optString("browser_download_url", ""),
    downloadCount = j.optInt("download_count", 0),
    contentType = j.optString("content_type", ""),
    state = j.optString("state", "")
)

private val RELEASES_TAG = "GH"

private fun releaseContentType(filename: String): String =
    when (filename.substringAfterLast(".", "").lowercase()) {
        "zip" -> "application/zip"
        "tar" -> "application/x-tar"
        "gz" -> "application/gzip"
        "jar" -> "application/java-archive"
        "apk" -> "application/vnd.android.package-archive"
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }
