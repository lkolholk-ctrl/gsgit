package gs.git.vps.data.github

import android.content.Context
import android.util.Base64
import org.json.JSONObject

internal data class CodeRemoteBytes(
    val bytes: ByteArray,
    val size: Long,
    val sha: String,
    val downloadUrl: String,
)

/** Reads a bounded file through Contents/Git Blobs API without ever decoding an oversized payload. */
internal suspend fun GitHubManager.getCodeRemoteBytes(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    path: String,
    maxBytes: Long,
    knownSha: String? = null,
    knownSize: Long = 0L,
): CodeRemoteBytes? {
    if (knownSize > maxBytes) throw codeTooLarge(path, knownSize, maxBytes)
    val cleanPath = path.trim('/')
    var sha = knownSha.orEmpty()
    var size = knownSize
    var downloadUrl = ""
    var encoded = ""

    if (sha.isBlank() || size <= 0L) {
        val response = request(
            context,
            "${repoPath(owner, repo, "/contents/${encPath(cleanPath)}")}${refQuery(branch)}",
            trackErrors = false,
        )
        if (response.code == 404) return null
        if (!response.success) throw IllegalStateException("failed to read $cleanPath (${response.code})")
        val root = JSONObject(response.body)
        size = root.optLong("size", size)
        if (size > maxBytes) throw codeTooLarge(cleanPath, size, maxBytes)
        sha = root.optString("sha")
        downloadUrl = root.optString("download_url")
        if (root.optString("encoding").equals("base64", ignoreCase = true)) {
            encoded = root.optString("content")
        }
    }

    if (encoded.isBlank() && sha.isNotBlank()) {
        val blobResponse = request(
            context,
            repoPath(owner, repo, "/git/blobs/${encPath(sha)}"),
            trackErrors = false,
        )
        if (blobResponse.code == 404) return null
        if (!blobResponse.success) throw IllegalStateException("failed to read blob for $cleanPath (${blobResponse.code})")
        val blob = JSONObject(blobResponse.body)
        size = blob.optLong("size", size)
        if (size > maxBytes) throw codeTooLarge(cleanPath, size, maxBytes)
        require(blob.optString("encoding").equals("base64", ignoreCase = true)) { "unsupported blob encoding" }
        encoded = blob.optString("content")
    }

    val compact = encoded.replace("\n", "").replace("\r", "")
    val maxEncodedChars = ((maxBytes + 2L) / 3L * 4L + 16L).coerceAtMost(Int.MAX_VALUE.toLong())
    if (compact.length.toLong() > maxEncodedChars) throw codeTooLarge(cleanPath, size.coerceAtLeast(maxBytes + 1L), maxBytes)
    val bytes = Base64.decode(compact, Base64.DEFAULT)
    if (bytes.size.toLong() > maxBytes) throw codeTooLarge(cleanPath, bytes.size.toLong(), maxBytes)
    return CodeRemoteBytes(bytes, size.coerceAtLeast(bytes.size.toLong()), sha, downloadUrl)
}

private fun codeTooLarge(path: String, actual: Long, limit: Long) = CodeFileRejectedException(
    "$path is ${formatCodeFileBytes(actual)}; safe load limit is ${formatCodeFileBytes(limit)}. The file was not loaded into memory.",
    actual,
)
