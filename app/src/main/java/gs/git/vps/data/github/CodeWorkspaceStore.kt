package gs.git.vps.data.github

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent navigation state for the Code workspace, scoped to `(repository, branch)`.
 * File contents remain owned by [CodeDraftStore]; this store only keeps lightweight paths so
 * open tabs and navigation history can be restored without caching stale server text.
 */
internal data class CodeWorkspaceSnapshot(
    val openPaths: List<String> = emptyList(),
    val activePath: String? = null,
    val backHistory: List<String> = emptyList(),
    val forwardHistory: List<String> = emptyList(),
)

internal object CodeWorkspaceStore {
    private const val PREFS = "code_workspace"
    const val MAX_OPEN_TABS = 12
    const val MAX_HISTORY = 40

    private fun key(repoFullName: String, branch: String): String = "$repoFullName@$branch"

    fun load(context: Context, repoFullName: String, branch: String): CodeWorkspaceSnapshot {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(repoFullName, branch), null) ?: return CodeWorkspaceSnapshot()
        return runCatching {
            decodeWorkspaceSnapshot(JSONObject(raw))
        }.getOrDefault(CodeWorkspaceSnapshot())
    }

    fun save(
        context: Context,
        repoFullName: String,
        branch: String,
        snapshot: CodeWorkspaceSnapshot,
    ) {
        val normalized = normalizeWorkspaceSnapshot(snapshot)
        val open = normalized.openPaths
        if (open.isEmpty() && snapshot.backHistory.isEmpty() && snapshot.forwardHistory.isEmpty()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(key(repoFullName, branch)).apply()
            return
        }
        val root = encodeWorkspaceSnapshot(normalized)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key(repoFullName, branch), root.toString()).apply()
    }

    internal fun encodeWorkspaceSnapshot(snapshot: CodeWorkspaceSnapshot): JSONObject {
        val normalized = normalizeWorkspaceSnapshot(snapshot)
        return JSONObject().apply {
            put("version", 1)
            put("open", normalized.openPaths.toJsonArray())
            put("active", normalized.activePath ?: "")
            put("back", normalized.backHistory.toJsonArray())
            put("forward", normalized.forwardHistory.toJsonArray())
        }
    }

    internal fun decodeWorkspaceSnapshot(root: JSONObject): CodeWorkspaceSnapshot {
        val open = root.pathList("open").takeLast(MAX_OPEN_TABS)
        val active = root.optString("active").trim('/').takeIf { it.isNotBlank() && it in open }
        return CodeWorkspaceSnapshot(
            openPaths = open,
            activePath = active,
            backHistory = root.pathList("back").takeLast(MAX_HISTORY),
            forwardHistory = root.pathList("forward").takeLast(MAX_HISTORY),
        )
    }

    private fun normalizeWorkspaceSnapshot(snapshot: CodeWorkspaceSnapshot): CodeWorkspaceSnapshot {
        val open = snapshot.openPaths.map { it.trim('/') }.filter { it.isNotBlank() }
            .distinct().takeLast(MAX_OPEN_TABS)
        return CodeWorkspaceSnapshot(
            openPaths = open,
            activePath = snapshot.activePath?.trim('/')?.takeIf { it in open },
            backHistory = snapshot.backHistory.cleanHistory(),
            forwardHistory = snapshot.forwardHistory.cleanHistory(),
        )
    }

    private fun List<String>.cleanHistory(): List<String> = map { it.trim('/') }
        .filter { it.isNotBlank() }.takeLast(MAX_HISTORY)

    private fun List<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { value -> array.put(value) }
    }

    private fun JSONObject.pathList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim('/').takeIf { it.isNotBlank() }?.let(::add)
            }
        }.distinct()
    }
}
