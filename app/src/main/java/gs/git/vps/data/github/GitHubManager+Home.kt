package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.QuickGlanceStats
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Home слоя GitHub API: дашборд-агрегат «быстрый взгляд» (назначенные PR, открытые issues,
 * упавшие сборки за сутки). Оркестрирует несколько эндпоинтов; вынесен из god-файла в отдельный
 * файл, чтобы ядро осталось чистым сетевым слоем. Сеть — через ядро `request()`.
 */

internal suspend fun GitHubManager.getQuickGlanceStats(context: Context): QuickGlanceStats {
    var prsCount = 0
    var issuesCount = 0
    val issuesRes = request(context, "/issues?filter=assigned&state=open")
    if (issuesRes.success) {
        try {
            val arr = JSONArray(issuesRes.body)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.has("pull_request")) {
                    prsCount++
                } else {
                    issuesCount++
                }
            }
        } catch (e: Exception) {}
    }

    var failedRuns = 0
    val reposRes = request(context, "/user/repos?sort=updated&per_page=5")
    if (reposRes.success) {
        try {
            val reposArr = JSONArray(reposRes.body)
            for (i in 0 until reposArr.length()) {
                val repo = reposArr.getJSONObject(i)
                val ownerObj = repo.optJSONObject("owner")
                val owner = ownerObj?.optString("login") ?: ""
                val name = repo.optString("name", "")
                if (owner.isNotBlank() && name.isNotBlank()) {
                    val runsRes = request(context, "/repos/$owner/$name/actions/runs?per_page=5")
                    if (runsRes.success) {
                        val runsObj = JSONObject(runsRes.body)
                        val runsArr = runsObj.optJSONArray("workflow_runs")
                        if (runsArr != null) {
                            for (j in 0 until runsArr.length()) {
                                val run = runsArr.getJSONObject(j)
                                val conclusion = run.optString("conclusion", "")
                                val createdAtStr = run.optString("created_at", "")
                                if (conclusion == "failure" && createdAtStr.isNotBlank()) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        val date = sdf.parse(createdAtStr)
                                        if (date != null && System.currentTimeMillis() - date.time < 24 * 60 * 60 * 1000) {
                                            failedRuns++
                                        }
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
    }

    return QuickGlanceStats(
        assignedPrsCount = prsCount,
        openIssuesCount = issuesCount,
        failedBuildsCount = failedRuns,
        loading = false
    )
}
