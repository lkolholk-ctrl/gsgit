package gs.git.vps.data.github

import android.content.Context
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
        prefs.edit().putString(k, j.toString()).apply()
    }
}
