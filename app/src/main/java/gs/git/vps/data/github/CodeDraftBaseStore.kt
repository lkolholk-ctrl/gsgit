package gs.git.vps.data.github

import android.content.Context
import org.json.JSONObject

/**
 * Persists the remote ancestor text used by the Code workspace for a real three-way rebase.
 * It is intentionally separate from [CodeDraftStore] so the existing A/M/D/R wire format stays
 * backwards compatible. A missing ancestor is recovered from the current remote file on load.
 */
internal object CodeDraftBaseStore {
    private const val PREFS = "code_draft_bases"
    private const val MAX_PERSIST_CHARS = 4_000_000

    private fun key(repoFullName: String, branch: String): String = "$repoFullName@$branch"

    fun load(context: Context, repoFullName: String, branch: String): Map<String, String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(repoFullName, branch), null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val path = keys.next()
                    put(path, root.optString(path, ""))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun save(context: Context, repoFullName: String, branch: String, bases: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storageKey = key(repoFullName, branch)
        if (bases.isEmpty()) {
            prefs.edit().remove(storageKey).apply()
            return
        }
        val encoded = JSONObject().apply {
            bases.toSortedMap().forEach { (path, content) -> put(path, content) }
        }.toString()
        if (encoded.length <= MAX_PERSIST_CHARS) prefs.edit().putString(storageKey, encoded).apply()
    }
}
