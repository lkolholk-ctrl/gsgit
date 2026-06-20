package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHGitBlob
import gs.git.vps.data.github.model.GHGitCommit
import gs.git.vps.data.github.model.GHGitRef
import gs.git.vps.data.github.model.GHGitTagDetail
import gs.git.vps.data.github.model.GHGitTree
import gs.git.vps.data.github.model.GHGitTreeItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен GitData слоя GitHub API — низкоуровневый Git Data API: refs (read/match/create/update/delete),
 * trees, blobs, tag- и commit-объекты. Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Сеть — через ядро `request()`, парсинг — чистые `parseGHX`. Сигнатуры вызовов не менялись.
 *
 * Высокоуровневые операции с файлами (getFileContent, upload*, commitWorkspaceChanges, clone) —
 * домен Contents (пока в core), они вызывают эти функции (тот же пакет, без импорта).
 * PGP-подпись коммита — через gs.git.vps.security.PgpKeyManager; reflog — LocalTimeTravelManager.
 */

// ─── Refs ────────────────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getGitRef(context: Context, owner: String, repo: String, ref: String): GHGitRef? {
    val cleanRef = ref.trim().removePrefix("refs/").trim('/')
    if (cleanRef.isBlank()) return null
    val r = request(context, "/repos/$owner/$repo/git/ref/$cleanRef")
    if (!r.success) return null
    return try { parseGitRef(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getMatchingGitRefs(context: Context, owner: String, repo: String, refPrefix: String): List<GHGitRef> {
    val cleanRef = refPrefix.trim().removePrefix("refs/").trim('/')
    if (cleanRef.isBlank()) return emptyList()
    val r = request(context, "/repos/$owner/$repo/git/matching-refs/$cleanRef")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i -> parseGitRef(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.updateGitRef(context: Context, owner: String, repo: String, ref: String, sha: String, force: Boolean = false): GHGitRef? {
    val cleanRef = ref.trim().removePrefix("refs/").trim('/')
    if (cleanRef.isBlank() || sha.isBlank()) return null
    val oldRefR = request(context, "/repos/$owner/$repo/git/ref/$cleanRef")
    val oldSha = if (oldRefR.success) {
        try { JSONObject(oldRefR.body).getJSONObject("object").optString("sha", "") } catch (e: Exception) { "" }
    } else ""

    val body = JSONObject().apply {
        put("sha", sha.trim())
        put("force", force)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/git/refs/$cleanRef", "PATCH", body)
    if (!r.success) return null

    if (oldSha.isNotBlank() && oldSha != sha) {
        gs.git.vps.data.github.LocalTimeTravelManager.addReflogEntry(context, "$owner/$repo", cleanRef, oldSha, sha.trim(), "update-ref: reset heads/$cleanRef to ${sha.take(7)}")
    }

    return try { parseGitRef(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createGitRef(context: Context, owner: String, repo: String, ref: String, sha: String): GHGitRef? {
    val cleanRef = ref.trim().removePrefix("refs/").trim('/')
    if (cleanRef.isBlank() || sha.isBlank()) return null
    val body = JSONObject().apply {
        put("ref", "refs/$cleanRef")
        put("sha", sha.trim())
    }.toString()
    val r = request(context, "/repos/$owner/$repo/git/refs", "POST", body)
    if (!r.success) return null
    return try { parseGitRef(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deleteGitRef(context: Context, owner: String, repo: String, ref: String): Boolean {
    val cleanRef = ref.trim().removePrefix("refs/").trim('/')
    if (cleanRef.isBlank()) return false
    return request(context, "/repos/$owner/$repo/git/refs/$cleanRef", "DELETE").success
}

// ─── Trees / Blobs ───────────────────────────────────────────────────────────

internal suspend fun GitHubManager.getGitTree(context: Context, owner: String, repo: String, treeSha: String, recursive: Boolean = true): GHGitTree? {
    val cleanSha = treeSha.trim()
    if (cleanSha.isBlank()) return null
    val recursiveFlag = if (recursive) "?recursive=1" else ""
    val r = request(context, "/repos/$owner/$repo/git/trees/$cleanSha$recursiveFlag")
    if (!r.success) return null
    return try { parseGitTree(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getGitBlob(context: Context, owner: String, repo: String, fileSha: String): GHGitBlob? {
    val cleanSha = fileSha.trim()
    if (cleanSha.isBlank()) return null
    val r = request(context, "/repos/$owner/$repo/git/blobs/$cleanSha")
    if (!r.success) return null
    return try {
        val j = JSONObject(r.body)
        GHGitBlob(
            sha = j.optString("sha", ""),
            size = j.optLong("size", 0L),
            encoding = j.optString("encoding", ""),
            content = j.optString("content", ""),
            url = j.optString("url", "")
        )
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createGitBlob(context: Context, owner: String, repo: String, content: String, encoding: String = "utf-8", onLog: (String) -> Unit = {}): GHGitBlob? {
    if (content.isBlank()) { onLog("[BLOB-ERR] content is blank"); return null }
    val cleanEncoding = if (encoding.equals("base64", ignoreCase = true)) "base64" else "utf-8"
    val body = JSONObject().apply {
        put("content", content)
        put("encoding", cleanEncoding)
    }.toString()
    val r = request(context, "/repos/$owner/$repo/git/blobs", "POST", body)
    if (!r.success) {
        onLog("[BLOB-ERR] HTTP ${r.code} -> ${r.body.take(300)}")
        return null
    }
    return try {
        val j = JSONObject(r.body)
        val sha = j.optString("sha", "")
        if (sha.isBlank()) { onLog("[BLOB-ERR] no sha in response: ${r.body.take(200)}"); return null }
        GHGitBlob(
            sha = sha,
            size = content.length.toLong(),
            encoding = cleanEncoding,
            content = content,
            url = j.optString("url", "")
        )
    } catch (e: Exception) { onLog("[BLOB-ERR] parse: ${e.message}"); null }
}

internal suspend fun GitHubManager.createGitTree(
    context: Context,
    owner: String,
    repo: String,
    baseTree: String,
    path: String,
    mode: String,
    type: String,
    sha: String,
    content: String
): GHGitTree? {
    val cleanPath = path.trim()
    if (cleanPath.isBlank()) return null
    if (sha.isBlank() && content.isBlank()) return null
    val item = JSONObject().apply {
        put("path", cleanPath)
        put("mode", mode.ifBlank { "100644" })
        put("type", type.ifBlank { "blob" })
        if (content.isNotBlank()) put("content", content) else put("sha", sha.trim())
    }
    val body = JSONObject().apply {
        if (baseTree.isNotBlank()) put("base_tree", baseTree.trim())
        put("tree", JSONArray().put(item))
    }.toString()
    val r = request(context, "/repos/$owner/$repo/git/trees", "POST", body)
    if (!r.success) return null
    return try { parseGitTree(JSONObject(r.body)) } catch (e: Exception) { null }
}

// ─── Tag- и commit-объекты ───────────────────────────────────────────────────

internal suspend fun GitHubManager.getGitTag(context: Context, owner: String, repo: String, tagSha: String): GHGitTagDetail? {
    val cleanSha = tagSha.trim()
    if (cleanSha.isBlank()) return null
    val r = request(context, "/repos/$owner/$repo/git/tags/$cleanSha")
    if (!r.success) return null
    return try { parseGitTagDetail(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createGitTag(
    context: Context,
    owner: String,
    repo: String,
    tag: String,
    message: String,
    objectSha: String,
    objectType: String = "commit",
    taggerName: String = "",
    taggerEmail: String = ""
): GHGitTagDetail? {
    if (tag.isBlank() || message.isBlank() || objectSha.isBlank()) return null
    val body = JSONObject().apply {
        put("tag", tag.trim())
        put("message", message)
        put("object", objectSha.trim())
        put("type", objectType.ifBlank { "commit" })
        if (taggerName.isNotBlank() && taggerEmail.isNotBlank()) {
            put("tagger", JSONObject().apply {
                put("name", taggerName.trim())
                put("email", taggerEmail.trim())
            })
        }
    }.toString()
    val r = request(context, "/repos/$owner/$repo/git/tags", "POST", body)
    if (!r.success) return null
    return try { parseGitTagDetail(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getGitCommit(context: Context, owner: String, repo: String, commitSha: String): GHGitCommit? {
    val cleanSha = commitSha.trim()
    if (cleanSha.isBlank()) return null
    val r = request(context, "/repos/$owner/$repo/git/commits/$cleanSha")
    if (!r.success) return null
    return try { parseGitCommit(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createGitCommit(
    context: Context,
    owner: String,
    repo: String,
    message: String,
    treeSha: String,
    parentShas: List<String>
): GHGitCommit? {
    if (message.isBlank() || treeSha.isBlank()) return null
    val body = JSONObject().apply {
        put("message", message)
        put("tree", treeSha.trim())
        val parentsArray = JSONArray().apply { parentShas.filter { it.isNotBlank() }.forEach { put(it.trim()) } }
        put("parents", parentsArray)

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

                val payloadSb = StringBuilder()
                payloadSb.append("tree ").append(treeSha.trim()).append("\n")
                for (i in 0 until parentsArray.length()) {
                    payloadSb.append("parent ").append(parentsArray.getString(i)).append("\n")
                }
                payloadSb.append("author ").append(name).append(" <").append(email).append("> ").append(gitTime).append("\n")
                payloadSb.append("committer ").append(name).append(" <").append(email).append("> ").append(gitTime).append("\n")
                payloadSb.append("\n")
                payloadSb.append(message)

                val payload = payloadSb.toString()
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
    val r = request(context, "/repos/$owner/$repo/git/commits", "POST", body)
    if (!r.success) return null
    return try { parseGitCommit(JSONObject(r.body)) } catch (e: Exception) { null }
}

// ─── Парсеры (чистые, без IO) ────────────────────────────────────────────────

private fun parseGitRef(j: JSONObject): GHGitRef {
    val obj = j.optJSONObject("object")
    return GHGitRef(
        ref = j.optString("ref", ""),
        nodeSha = obj?.optString("sha", "") ?: "",
        nodeType = obj?.optString("type", "") ?: "",
        url = j.optString("url", "")
    )
}

private fun parseGitTree(j: JSONObject): GHGitTree {
    val tree = j.optJSONArray("tree") ?: JSONArray()
    return GHGitTree(
        sha = j.optString("sha", ""),
        truncated = j.optBoolean("truncated", false),
        items = (0 until tree.length()).map { i ->
            val item = tree.getJSONObject(i)
            GHGitTreeItem(
                path = item.optString("path", ""),
                mode = item.optString("mode", ""),
                type = item.optString("type", ""),
                sha = item.optString("sha", ""),
                size = item.optLong("size", 0L),
                url = item.optString("url", "")
            )
        }
    )
}

private fun parseGitTagDetail(j: JSONObject): GHGitTagDetail {
    val tagger = j.optJSONObject("tagger")
    val obj = j.optJSONObject("object")
    return GHGitTagDetail(
        sha = j.optString("sha", ""),
        tag = j.optString("tag", ""),
        message = j.optString("message", ""),
        taggerName = tagger?.optString("name", "") ?: "",
        taggerEmail = tagger?.optString("email", "") ?: "",
        date = tagger?.optString("date", "") ?: "",
        objectSha = obj?.optString("sha", "") ?: "",
        objectType = obj?.optString("type", "") ?: ""
    )
}

private fun parseGitCommit(j: JSONObject): GHGitCommit {
    val author = j.optJSONObject("author")
    val committer = j.optJSONObject("committer")
    val parents = j.optJSONArray("parents") ?: JSONArray()
    return GHGitCommit(
        sha = j.optString("sha", ""),
        message = j.optString("message", ""),
        treeSha = j.optJSONObject("tree")?.optString("sha", "") ?: "",
        parentShas = (0 until parents.length()).mapNotNull { i -> parents.optJSONObject(i)?.optString("sha")?.takeIf { it.isNotBlank() } },
        authorName = author?.optString("name", "") ?: "",
        authorEmail = author?.optString("email", "") ?: "",
        authorDate = author?.optString("date", "") ?: "",
        committerName = committer?.optString("name", "") ?: "",
        committerEmail = committer?.optString("email", "") ?: "",
        committerDate = committer?.optString("date", "") ?: ""
    )
}
