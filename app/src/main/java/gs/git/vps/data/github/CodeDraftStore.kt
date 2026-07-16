package gs.git.vps.data.github

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Дисковый персист черновика Code-таба (Стадия 6 «прочность»). Скоуп — `(репо, ветка)`: у каждой
 * ветки свой черновик. Хранит типизированные операции Add/Modify/Delete/Rename в
 * SharedPreferences и переживает смерть процесса и смену ветки. Старый формат `path → content`
 * читается как Modify, поэтому обновление приложения не теряет существующие черновики.
 *
 * Авто-сохраняется молча на каждое изменение черновика; авто-восстанавливается при входе в Code /
 * возврате на ветку. См. docs/code-tab-spec.md.
 */
internal object CodeDraftStore {
    private const val PREFS = "code_drafts"

    private fun key(repoFullName: String, branch: String): String = "$repoFullName@$branch"

    /** Загрузить черновик `(репо, ветка)` с диска. Пустая карта — если черновика нет. */
    fun load(context: Context, repoFullName: String, branch: String): Map<String, CodeChange> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(repoFullName, branch), null) ?: return emptyMap()
        return try {
            decodeCodeDraft(JSONObject(raw))
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Сохранить черновик `(репо, ветка)` на диск. Пустой — удаляет слот. */
    fun save(context: Context, repoFullName: String, branch: String, draft: Map<String, CodeChange>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = key(repoFullName, branch)
        if (draft.isEmpty()) {
            prefs.edit().remove(k).apply()
            return
        }
        val j = encodeCodeDraft(draft)
        val s = j.toString()
        // S6: лимит, чтобы огромный черновик не раздувал SharedPreferences (грузится в память целиком).
        // При превышении не персистим — in-memory черновик сессии остаётся рабочим.
        if (s.length > MAX_PERSIST_CHARS) return
        prefs.edit().putString(k, s).apply()
    }

    private const val MAX_PERSIST_CHARS = 4_000_000  // ~4 МБ JSON
    private const val DRAFT_FORMAT_VERSION = 2
    private const val RECENTS_PREFS = "code_recents"
    private const val RECENTS_MAX = 8

    /** Загрузить недавно открытые файлы (пути, most-recent-first) для репо. */
    fun loadRecents(context: Context, repoFullName: String): List<String> {
        val raw = context.getSharedPreferences(RECENTS_PREFS, Context.MODE_PRIVATE)
            .getString(repoFullName, null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Сохранить недавние (пути) для репо — переживает рестарт. */
    fun saveRecents(context: Context, repoFullName: String, paths: List<String>) {
        val a = JSONArray()
        paths.take(RECENTS_MAX).forEach { a.put(it) }
        context.getSharedPreferences(RECENTS_PREFS, Context.MODE_PRIVATE)
            .edit().putString(repoFullName, a.toString()).apply()
    }

    internal fun decodeCodeDraft(root: JSONObject): Map<String, CodeChange> {
        val encoded = root.optJSONArray("changes")
        if (root.optInt("version", 0) >= DRAFT_FORMAT_VERSION && encoded != null) {
            return buildMap {
                for (i in 0 until encoded.length()) {
                    decodeCodeChange(encoded.optJSONObject(i) ?: continue)?.let { put(it.path, it) }
                }
            }
        }

        // v1 migration: every top-level property was path → edited content.
        return buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val path = keys.next()
                put(path, CodeModify(path, root.optString(path, "")))
            }
        }
    }

    internal fun encodeCodeDraft(draft: Map<String, CodeChange>): JSONObject = JSONObject().apply {
        put("version", DRAFT_FORMAT_VERSION)
        put("changes", JSONArray().apply {
            draft.values.sortedBy { it.path }.forEach { change -> put(encodeCodeChange(change)) }
        })
    }

    private fun encodeCodeChange(change: CodeChange): JSONObject = JSONObject().apply {
        put("kind", change.kind.name)
        put("path", change.path)
        when (change) {
            is CodeAdd -> {
                putNullable("content", change.content)
                putNullable("sourcePath", change.sourcePath)
                putNullable("sourceSha", change.sourceSha)
            }
            is CodeModify -> put("content", change.content)
            is CodeDelete -> change.restore?.let { put("restore", encodeCodeChange(it)) }
            is CodeRename -> {
                put("oldPath", change.oldPath)
                putNullable("content", change.content)
                putNullable("sourceSha", change.sourceSha)
            }
        }
    }

    private fun decodeCodeChange(j: JSONObject): CodeChange? {
        val path = j.optString("path").trim('/')
        if (path.isBlank()) return null
        return when (runCatching { CodeChangeKind.valueOf(j.optString("kind")) }.getOrNull()) {
            CodeChangeKind.ADD -> CodeAdd(
                path = path,
                content = j.nullableString("content"),
                sourcePath = j.nullableString("sourcePath"),
                sourceSha = j.nullableString("sourceSha"),
            )
            CodeChangeKind.MODIFY -> CodeModify(path, j.optString("content", ""))
            CodeChangeKind.DELETE -> CodeDelete(path, j.optJSONObject("restore")?.let(::decodeCodeChange))
            CodeChangeKind.RENAME -> {
                val oldPath = j.optString("oldPath").trim('/')
                if (oldPath.isBlank()) null else CodeRename(
                    oldPath = oldPath,
                    path = path,
                    content = j.nullableString("content"),
                    sourceSha = j.nullableString("sourceSha"),
                )
            }
            null -> null
        }
    }

    private fun JSONObject.putNullable(name: String, value: String?) {
        put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name)
}
