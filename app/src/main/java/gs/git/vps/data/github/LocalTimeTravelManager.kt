package gs.git.vps.data.github

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LocalStashItem(
    val id: String,
    val message: String,
    val timestamp: Long,
    val files: List<StashFile>
)

data class StashFile(
    val path: String,
    val content: String
)

data class LocalReflogEntry(
    val id: String,
    val ref: String,
    val beforeSha: String,
    val afterSha: String,
    val action: String,
    val timestamp: Long
)

object LocalTimeTravelManager {
    private const val PREFS_STASH = "gsgit_stash_prefs"
    private const val PREFS_REFLOG = "gsgit_reflog_prefs"

    // STASH LOGIC
    fun getStashes(context: Context, repoFullName: String): List<LocalStashItem> {
        val prefs = context.getSharedPreferences(PREFS_STASH, Context.MODE_PRIVATE)
        val raw = prefs.getString(repoFullName, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                val filesArr = j.getJSONArray("files")
                val files = (0 until filesArr.length()).map { fIdx ->
                    val f = filesArr.getJSONObject(fIdx)
                    StashFile(f.getString("path"), f.getString("content"))
                }
                LocalStashItem(
                    id = j.getString("id"),
                    message = j.getString("message"),
                    timestamp = j.getLong("timestamp"),
                    files = files
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveStashes(context: Context, repoFullName: String, stashes: List<LocalStashItem>) {
        val prefs = context.getSharedPreferences(PREFS_STASH, Context.MODE_PRIVATE)
        try {
            val arr = JSONArray()
            stashes.forEach { stash ->
                val j = JSONObject().apply {
                    put("id", stash.id)
                    put("message", stash.message)
                    put("timestamp", stash.timestamp)
                    val filesArr = JSONArray()
                    stash.files.forEach { f ->
                        filesArr.put(JSONObject().apply {
                            put("path", f.path)
                            put("content", f.content)
                        })
                    }
                    put("files", filesArr)
                }
                arr.put(j)
            }
            prefs.edit().putString(repoFullName, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addStash(context: Context, repoFullName: String, message: String, files: List<StashFile>) {
        val current = getStashes(context, repoFullName).toMutableList()
        val id = "stash@{${current.size}}"
        current.add(0, LocalStashItem(id, message, System.currentTimeMillis(), files))
        saveStashes(context, repoFullName, current)
    }

    fun deleteStash(context: Context, repoFullName: String, stashId: String) {
        val current = getStashes(context, repoFullName).filter { it.id != stashId }
        val reindexed = current.reversed().mapIndexed { index, item ->
            item.copy(id = "stash@{$index}")
        }.reversed()
        saveStashes(context, repoFullName, reindexed)
    }

    // REFLOG LOGIC
    fun getReflog(context: Context, repoFullName: String): List<LocalReflogEntry> {
        val prefs = context.getSharedPreferences(PREFS_REFLOG, Context.MODE_PRIVATE)
        val raw = prefs.getString(repoFullName, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                LocalReflogEntry(
                    id = j.getString("id"),
                    ref = j.getString("ref"),
                    beforeSha = j.getString("beforeSha"),
                    afterSha = j.getString("afterSha"),
                    action = j.getString("action"),
                    timestamp = j.getLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReflog(context: Context, repoFullName: String, entries: List<LocalReflogEntry>) {
        val prefs = context.getSharedPreferences(PREFS_REFLOG, Context.MODE_PRIVATE)
        try {
            val arr = JSONArray()
            entries.take(100).forEach { entry ->
                val j = JSONObject().apply {
                    put("id", entry.id)
                    put("ref", entry.ref)
                    put("beforeSha", entry.beforeSha)
                    put("afterSha", entry.afterSha)
                    put("action", entry.action)
                    put("timestamp", entry.timestamp)
                }
                arr.put(j)
            }
            prefs.edit().putString(repoFullName, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addReflogEntry(context: Context, repoFullName: String, ref: String, beforeSha: String, afterSha: String, action: String) {
        if (beforeSha == afterSha || beforeSha.isBlank() || afterSha.isBlank()) return
        val current = getReflog(context, repoFullName).toMutableList()
        val id = java.util.UUID.randomUUID().toString().take(8)
        current.add(0, LocalReflogEntry(id, ref, beforeSha, afterSha, action, System.currentTimeMillis()))
        saveReflog(context, repoFullName, current)
    }
}
