package gs.git.vps.data.github

import java.net.HttpURLConnection
import java.net.URL

private const val MAX_DOWNLOAD_REDIRECTS = 5

/**
 * Opens a binary download without allowing HttpURLConnection to replay a bearer
 * token on a redirect. GitHub commonly redirects API downloads to a signed CDN
 * URL; that URL must never receive the account token.
 */
internal fun GitHubManager.openDownloadConnection(
    url: String,
    token: String,
    accept: String? = null,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): HttpURLConnection? {
    val trustedApiHost = runCatching { URL(getApiUrl()).host }.getOrNull() ?: return null
    var current = runCatching { URL(url) }.getOrNull() ?: return null
    var redirectCount = 0

    while (redirectCount <= MAX_DOWNLOAD_REDIRECTS) {
        if (!current.protocol.equals("https", ignoreCase = true)) return null
        val connection = (current.openConnection() as? HttpURLConnection) ?: return null
        connection.instanceFollowRedirects = false
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        accept?.let { connection.setRequestProperty("Accept", it) }

        // Authenticate the original API request only. Redirect targets are
        // frequently signed object-storage URLs and must be anonymous.
        if (redirectCount == 0 && current.host.equals(trustedApiHost, ignoreCase = true) && token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }

        val code = connection.responseCode
        if (code !in 300..399) return connection

        val location = connection.getHeaderField("Location")
        connection.disconnect()
        if (location.isNullOrBlank()) return null
        current = runCatching { URL(current, location) }.getOrNull() ?: return null
        redirectCount++
    }
    return null
}
