package gs.git.vps.data.github

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Дисковый персист черновика Code-таба (Стадия 6 «прочность»). Скоуп — `(репо, ветка)`: у каждой
 * ветки свой черновик. Хранит карту `path → новый контент` в SharedPreferences (как
 * LocalTimeTravelManager), переживает смерть процесса и смену ветки.
 *
 * Авто-сохраняется молча на каждое изменение черновика; авто-восстанавливается при входе в Code /
 * возврате на ветку. См. docs/code-tab-spec.md.
 */
object CodeDraftStore {
    private const val PREFS = "code_drafts"

    private fun key(repoFullName: String, branch: String): String = "$repoFullName@$branch"

    /** Загрузить черновик `(репо, ветка)` с диска. Пустая карта — если черновика нет. */
    fun load(context: Context, repoFullName: String, branch: String): Map<String, String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(repoFullName, branch), null) ?: return emptyMap()
        return try {
            val j = JSONObject(raw)
            buildMap {
                val it = j.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    put(k, j.getString(k))
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Сохранить черновик `(репо, ветка)` на диск. Пустой — удаляет слот. */
    fun save(context: Context, repoFullName: String, branch: String, draft: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val k = key(repoFullName, branch)
        if (draft.isEmpty()) {
            prefs.edit().remove(k).apply()
            return
        }
        val j = JSONObject()
        for ((path, content) in draft) j.put(path, content)
        val s = j.toString()
        // S6: лимит, чтобы огромный черновик не раздувал SharedPreferences (грузится в память целиком).
        // При превышении не персистим — in-memory черновик сессии остаётся рабочим.
        if (s.length > MAX_PERSIST_CHARS) return
        prefs.edit().putString(k, s).apply()
    }

    private const val MAX_PERSIST_CHARS = 4_000_000  // ~4 МБ JSON
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
}
