package gs.git.vps.data.github

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import gs.git.vps.data.github.model.GHActionResult
import gs.git.vps.data.github.model.GHActionsUsage
import gs.git.vps.data.github.model.GHArtifact
import gs.git.vps.data.github.model.GHCheckAnnotation
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHJob
import gs.git.vps.data.github.model.GHStep
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowDispatchInput
import gs.git.vps.data.github.model.GHWorkflowDispatchSchema
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHWorkflowRunReview
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL

/**
 * Домен Workflows слоя GitHub API — GitHub Actions: workflows, runs, jobs, dispatch,
 * artifacts, check-runs, usage. Нарезан по эталону (см. docs/decomposition-log.md).
 * Сеть — через ядро GitHubManager.request(); парсинг — чистые parse*-функции.
 * Исключение: скачивание zip-логов/артефактов идёт прямым openConnection() (бинарные стримы,
 * которые текстовое ядро request() не обслуживает) — перенесено как было.
 */

private const val ACTIONS_TAG = "GH"

private fun parseWorkflow(j: JSONObject): GHWorkflow =
    GHWorkflow(
        id = j.optLong("id"),
        name = j.optString("name"),
        state = j.optString("state"),
        path = j.optString("path"),
        htmlUrl = j.optString("html_url", ""),
        badgeUrl = j.optString("badge_url", ""),
        createdAt = j.optString("created_at", ""),
        updatedAt = j.optString("updated_at", "")
    )

internal suspend fun GitHubManager.getWorkflows(context: Context, owner: String, repo: String): List<GHWorkflow> {
    val workflows = mutableListOf<GHWorkflow>()
    var page = 1
    while (true) {
        val r = request(context, "/repos/$owner/$repo/actions/workflows?per_page=100&page=$page")
        if (!r.success) break
        val count = try {
            val arr = JSONObject(r.body).getJSONArray("workflows")
            for (i in 0 until arr.length()) {
                val j = arr.getJSONObject(i)
                workflows += parseWorkflow(j)
            }
            arr.length()
        } catch (e: Exception) {
            0
        }
        if (count < 100) break
        page++
    }
    return workflows.distinctBy { it.id }
}

internal suspend fun GitHubManager.getWorkflow(context: Context, owner: String, repo: String, workflowId: Long): GHWorkflow? {
    val r = request(context, "/repos/$owner/$repo/actions/workflows/$workflowId")
    if (!r.success) return null
    return try { parseWorkflow(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getWorkflowRuns(
    context: Context,
    owner: String,
    repo: String,
    workflowId: Long? = null,
    perPage: Int = 20,
    page: Int = 1,
    branch: String? = null,
    event: String? = null,
    status: String? = null
): List<GHWorkflowRun> {
    val params = mutableListOf("per_page=$perPage", "page=$page")
    branch?.takeIf { it.isNotBlank() }?.let { params += "branch=${URLEncoder.encode(it, "UTF-8")}" }
    event?.takeIf { it.isNotBlank() }?.let { params += "event=${URLEncoder.encode(it, "UTF-8")}" }
    status?.takeIf { it.isNotBlank() }?.let { params += "status=${URLEncoder.encode(it, "UTF-8")}" }
    val query = params.joinToString("&")
    val endpoint = if (workflowId != null) "/repos/$owner/$repo/actions/workflows/$workflowId/runs?$query"
        else "/repos/$owner/$repo/actions/runs?$query"
    val r = request(context, endpoint)
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).getJSONArray("workflow_runs")
        (0 until arr.length()).map { i -> parseWorkflowRun(arr.getJSONObject(i)) }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getWorkflowRun(context: Context, owner: String, repo: String, runId: Long): GHWorkflowRun? {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId")
    if (!r.success) return null
    return try {
        parseWorkflowRun(JSONObject(r.body))
    } catch (e: Exception) {
        null
    }
}

internal suspend fun GitHubManager.getWorkflowRunJobs(context: Context, owner: String, repo: String, runId: Long, page: Int = 1): List<GHJob> {
    // `all` mixes jobs from previous rerun attempts into the current run UI.
    // Attempt history is loaded through getWorkflowRunAttemptJobs instead.
    // Пагинация обязательна: matrix-раны легко дают >100 джоб — без страниц
    // хвост терялся, и в UI показывалась лишь часть матрицы.
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/jobs?filter=latest&per_page=100&page=$page")
    if (!r.success) return emptyList()
    val jobs = parseJobs(r.body)
    val nextPage = parseNextPage(r.headers) ?: return jobs
    return jobs + getWorkflowRunJobs(context, owner, repo, runId, nextPage)
}

internal suspend fun GitHubManager.getWorkflowRunLogs(context: Context, owner: String, repo: String, runId: Long): String =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            val token = GitHubAuth.resolveApiToken(context)
            val url = "${getApiUrl()}/repos/$owner/$repo/actions/runs/$runId/logs"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                instanceFollowRedirects = false; connectTimeout = 15000; readTimeout = 15000
            }
            val code = conn.responseCode
            if (code == 302) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location != null) "Logs URL: $location" else "No logs available"
            } else {
                conn.disconnect()
                "Logs: HTTP $code"
            }
        } catch (e: Exception) { "Error: ${e.message}" }
    }

internal suspend fun GitHubManager.getJobLogs(context: Context, owner: String, repo: String, jobId: Long): String =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            val token = GitHubAuth.resolveApiToken(context)
            val url = "${getApiUrl()}/repos/$owner/$repo/actions/jobs/$jobId/logs"
            val conn = openDownloadConnection(
                url = url,
                token = token,
                accept = "application/vnd.github.v3+json",
                connectTimeoutMs = 15_000,
                readTimeoutMs = 30_000,
            ) ?: return@withContext "Error: unable to open logs"
            val code = conn.responseCode
            if (code == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                text
            } else {
                conn.disconnect()
                "Error: HTTP $code"
            }
        } catch (e: Exception) { "Error: ${e.message}" }
    }

internal suspend fun GitHubManager.rerunWorkflowDetailed(context: Context, owner: String, repo: String, runId: Long): GHActionResult =
    actionsCommandResult(
        request(context, "/repos/$owner/$repo/actions/runs/$runId/rerun", "POST", "{}"),
        successMessage = "Workflow rerun queued",
    )

internal suspend fun GitHubManager.rerunWorkflow(context: Context, owner: String, repo: String, runId: Long): Boolean =
    rerunWorkflowDetailed(context, owner, repo, runId).success

internal suspend fun GitHubManager.rerunFailedJobsDetailed(context: Context, owner: String, repo: String, runId: Long): GHActionResult =
    actionsCommandResult(
        request(context, "/repos/$owner/$repo/actions/runs/$runId/rerun-failed-jobs", "POST", "{}"),
        successMessage = "Failed jobs rerun queued",
    )

internal suspend fun GitHubManager.rerunFailedJobs(context: Context, owner: String, repo: String, runId: Long): Boolean =
    rerunFailedJobsDetailed(context, owner, repo, runId).success

internal suspend fun GitHubManager.cancelWorkflowRunDetailed(context: Context, owner: String, repo: String, runId: Long): GHActionResult =
    actionsCommandResult(
        request(context, "/repos/$owner/$repo/actions/runs/$runId/cancel", "POST", "{}"),
        successMessage = "Cancellation requested",
    )

internal suspend fun GitHubManager.cancelWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean =
    cancelWorkflowRunDetailed(context, owner, repo, runId).success

internal suspend fun GitHubManager.enableWorkflow(context: Context, owner: String, repo: String, workflowId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/workflows/$workflowId/enable", "PUT", "{}").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.disableWorkflow(context: Context, owner: String, repo: String, workflowId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/workflows/$workflowId/disable", "PUT", "{}").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.dispatchWorkflow(context: Context, owner: String, repo: String, workflowId: Long, branch: String, inputs: Map<String, String> = emptyMap()): Boolean {
    val body = JSONObject().apply {
        put("ref", branch)
        if (inputs.isNotEmpty()) {
            val inputsObj = JSONObject()
            inputs.forEach { (k, v) -> inputsObj.put(k, v) }
            put("inputs", inputsObj)
        }
    }.toString()
    return request(context, "/repos/$owner/$repo/actions/workflows/$workflowId/dispatches", "POST", body).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.dispatchWorkflowDetailed(context: Context, owner: String, repo: String, workflowId: Long, branch: String, inputs: Map<String, String> = emptyMap()): GHActionResult {
    val body = JSONObject().apply {
        put("ref", branch)
        if (inputs.isNotEmpty()) {
            val inputsObj = JSONObject()
            inputs.forEach { (k, v) -> inputsObj.put(k, v) }
            put("inputs", inputsObj)
        }
    }.toString()
    val r = request(context, "/repos/$owner/$repo/actions/workflows/$workflowId/dispatches", "POST", body)
    val success = r.code == 204 || r.success
    return GHActionResult(success, r.code, if (success) "" else apiErrorMessage(r))
}

internal suspend fun GitHubManager.dispatchWorkflow(context: Context, owner: String, repo: String, workflowId: String, ref: String, inputs: Map<String, String> = emptyMap()): Boolean {
    val body = JSONObject().apply {
        put("ref", ref)
        if (inputs.isNotEmpty()) {
            val inputsObj = JSONObject()
            inputs.forEach { (k, v) -> inputsObj.put(k, v) }
            put("inputs", inputsObj)
        }
    }.toString()
    val encodedId = URLEncoder.encode(workflowId, "UTF-8")
    return request(context, "/repos/$owner/$repo/actions/workflows/$encodedId/dispatches", "POST", body).let { it.code == 204 || it.success }
}

internal suspend fun GitHubManager.getWorkflowDispatchSchema(context: Context, owner: String, repo: String, workflowPath: String, branch: String? = null): GHWorkflowDispatchSchema? {
    val content = getFileContent(context, owner, repo, workflowPath, branch)
    if (content.isBlank()) return null
    return parseWorkflowDispatchSchema(workflowPath, content)
}

internal suspend fun GitHubManager.getWorkflowDispatchSchemas(context: Context, owner: String, repo: String, workflows: List<GHWorkflow>, branch: String? = null): List<GHWorkflowDispatchSchema> {
    return workflows.mapNotNull { workflow ->
        getWorkflowDispatchSchema(context, owner, repo, workflow.path, branch)?.copy(
            workflowName = workflow.name.ifBlank { workflow.path.substringAfterLast('/') }
        )
    }
}

private fun parseWorkflowDispatchSchema(workflowPath: String, yaml: String): GHWorkflowDispatchSchema? {
    val lines = yaml.lines()
    val workflowName = lines.firstOrNull { it.trimStart().startsWith("name:") }
        ?.substringAfter(":")
        ?.trim()
        ?.trim('"', '\'')
        .orEmpty()
        .ifBlank { workflowPath.substringAfterLast('/') }

    val inlineDispatch = lines.any { line ->
        val clean = yamlClean(line)
        clean == "on: workflow_dispatch" ||
            clean.matches(Regex("""on:\s*\[.*\bworkflow_dispatch\b.*]""")) ||
            clean == "- workflow_dispatch"
    }
    val dispatchIndex = lines.indexOfFirst { line ->
        val clean = yamlClean(line)
        clean == "workflow_dispatch" || clean.startsWith("workflow_dispatch:")
    }
    if (dispatchIndex < 0) {
        return if (inlineDispatch) GHWorkflowDispatchSchema(workflowPath, workflowName, emptyList()) else null
    }

    val dispatchIndent = lines[dispatchIndex].takeWhile { it == ' ' }.length
    val inputsIndex = lines.indexOfFirstIndexed(dispatchIndex + 1) { _, line ->
        val indent = line.takeWhile { it == ' ' }.length
        yamlClean(line).startsWith("inputs:") && indent > dispatchIndent
    }
    if (inputsIndex < 0) return GHWorkflowDispatchSchema(workflowPath, workflowName, emptyList())

    val inputsIndent = lines[inputsIndex].takeWhile { it == ' ' }.length
    val results = mutableListOf<GHWorkflowDispatchInput>()
    var i = inputsIndex + 1
    while (i < lines.size) {
        val raw = lines[i]
        if (raw.trim().isBlank()) { i++; continue }
        val indent = raw.takeWhile { it == ' ' }.length
        if (indent <= inputsIndent) break
        val trimmed = yamlClean(raw)
        if (trimmed.endsWith(":") && !trimmed.startsWith("#")) {
            val key = trimmed.removeSuffix(":").trim().trim('"', '\'')
            var description = ""
            var required = false
            var defaultValue = ""
            var type = ""
            val options = mutableListOf<String>()
            val keyIndent = indent
            i++
            while (i < lines.size) {
                val childRaw = lines[i]
                if (childRaw.trim().isBlank()) { i++; continue }
                val childIndent = childRaw.takeWhile { it == ' ' }.length
                if (childIndent <= keyIndent) break
                val childTrim = yamlClean(childRaw)
                when {
                    childTrim.startsWith("description:") -> description = yamlScalar(childTrim.substringAfter(":"))
                    childTrim.startsWith("required:") -> required = yamlScalar(childTrim.substringAfter(":")).equals("true", true)
                    childTrim.startsWith("default:") -> defaultValue = yamlScalar(childTrim.substringAfter(":"))
                    childTrim.startsWith("type:") -> type = yamlScalar(childTrim.substringAfter(":")).lowercase()
                    childTrim.startsWith("options:") -> {
                        val inlineOptions = yamlInlineList(childTrim.substringAfter(":"))
                        if (inlineOptions.isNotEmpty()) {
                            options += inlineOptions
                            i++
                            continue
                        }
                        i++
                        while (i < lines.size) {
                            val optionRaw = lines[i]
                            if (optionRaw.trim().isBlank()) { i++; continue }
                            val optionIndent = optionRaw.takeWhile { it == ' ' }.length
                            if (optionIndent <= childIndent) break
                            val optionTrim = yamlClean(optionRaw)
                            if (optionTrim.startsWith("- ")) options += yamlScalar(optionTrim.removePrefix("- "))
                            i++
                        }
                        continue
                    }
                }
                i++
            }
            results += GHWorkflowDispatchInput(
                key = key,
                description = description,
                required = required,
                defaultValue = defaultValue,
                type = type,
                options = options
            )
            continue
        }
        i++
    }
    return GHWorkflowDispatchSchema(workflowPath, workflowName, results)
}

private fun yamlClean(line: String): String = line.substringBefore("#").trim()

private fun yamlScalar(value: String): String = value.trim().trim('"', '\'')

private fun yamlInlineList(value: String): List<String> {
    val trimmed = value.trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
    return trimmed.removePrefix("[")
        .removeSuffix("]")
        .split(',')
        .map { yamlScalar(it) }
        .filter { it.isNotBlank() }
}

private inline fun List<String>.indexOfFirstIndexed(startIndex: Int, predicate: (Int, String) -> Boolean): Int {
    for (index in startIndex until size) if (predicate(index, this[index])) return index
    return -1
}

internal suspend fun GitHubManager.getRunArtifacts(context: Context, owner: String, repo: String, runId: Long): List<GHArtifact> {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/artifacts?per_page=100")
    if (!r.success) return emptyList()
    return parseArtifacts(r.body)
}

internal suspend fun GitHubManager.downloadArtifact(context: Context, owner: String, repo: String, artifactId: Long, destFile: java.io.File): Boolean =
    withContext(Dispatchers.IO) {
        try {
            updateApiUrl(context)
            val token = GitHubAuth.resolveApiToken(context)
            val url = "${getApiUrl()}/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
            val conn = openDownloadConnection(
                url = url,
                token = token,
                accept = "application/vnd.github.v3+json",
                connectTimeoutMs = 15_000,
                readTimeoutMs = 60_000,
            ) ?: return@withContext false
            val code = conn.responseCode
            if (code != 200) { conn.disconnect(); return@withContext false }
            destFile.parentFile?.mkdirs()
            conn.inputStream.use { input -> destFile.outputStream().use { out -> input.copyTo(out) } }
            conn.disconnect()
            true
        } catch (e: Exception) { Log.e(ACTIONS_TAG, "Download artifact failed"); false }
    }

internal suspend fun GitHubManager.deleteArtifact(context: Context, owner: String, repo: String, artifactId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/artifacts/$artifactId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.getRepositoryArtifacts(context: Context, owner: String, repo: String, page: Int = 1, name: String? = null): List<GHArtifact> {
    val params = mutableListOf("per_page=100", "page=$page")
    name?.takeIf { it.isNotBlank() }?.let { params += "name=${URLEncoder.encode(it, "UTF-8")}" }
    val r = request(context, "/repos/$owner/$repo/actions/artifacts?${params.joinToString("&")}")
    if (!r.success) return emptyList()
    return parseArtifacts(r.body)
}

internal suspend fun GitHubManager.getArtifact(context: Context, owner: String, repo: String, artifactId: Long): GHArtifact? {
    val r = request(context, "/repos/$owner/$repo/actions/artifacts/$artifactId")
    if (!r.success) return null
    return try { parseArtifact(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getWorkflowRunAttempt(context: Context, owner: String, repo: String, runId: Long, attempt: Int): GHWorkflowRun? {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/attempts/$attempt")
    if (!r.success) return null
    return try { parseWorkflowRun(JSONObject(r.body)) } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.getWorkflowRunAttemptJobs(context: Context, owner: String, repo: String, runId: Long, attempt: Int, page: Int = 1): List<GHJob> {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/attempts/$attempt/jobs?per_page=100&page=$page")
    if (!r.success) return emptyList()
    val jobs = parseJobs(r.body)
    val nextPage = parseNextPage(r.headers) ?: return jobs
    return jobs + getWorkflowRunAttemptJobs(context, owner, repo, runId, attempt, nextPage)
}

internal suspend fun GitHubManager.getWorkflowRunAttemptLogs(context: Context, owner: String, repo: String, runId: Long, attempt: Int): String {
    updateApiUrl(context)
    return getRedirectLocationOrText(context, "${getApiUrl()}/repos/$owner/$repo/actions/runs/$runId/attempts/$attempt/logs")
}

internal suspend fun GitHubManager.rerunJobDetailed(context: Context, owner: String, repo: String, jobId: Long): GHActionResult =
    actionsCommandResult(
        request(context, "/repos/$owner/$repo/actions/jobs/$jobId/rerun", "POST", "{}"),
        successMessage = "Job rerun queued",
    )

internal suspend fun GitHubManager.rerunJob(context: Context, owner: String, repo: String, jobId: Long): Boolean =
    rerunJobDetailed(context, owner, repo, jobId).success

internal suspend fun GitHubManager.deleteWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/runs/$runId", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.deleteWorkflowRunLogs(context: Context, owner: String, repo: String, runId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/runs/$runId/logs", "DELETE").let { it.code == 204 || it.success }

internal suspend fun GitHubManager.forceCancelWorkflowRunDetailed(context: Context, owner: String, repo: String, runId: Long): GHActionResult =
    actionsCommandResult(
        request(context, "/repos/$owner/$repo/actions/runs/$runId/force-cancel", "POST", "{}"),
        successMessage = "Force cancellation requested",
    )

internal suspend fun GitHubManager.forceCancelWorkflowRun(context: Context, owner: String, repo: String, runId: Long): Boolean =
    forceCancelWorkflowRunDetailed(context, owner, repo, runId).success

internal suspend fun GitHubManager.getWorkflowUsage(context: Context, owner: String, repo: String, workflowId: Long): GHActionsUsage? {
    val r = request(context, "/repos/$owner/$repo/actions/workflows/$workflowId/timing")
    if (!r.success) return null
    return parseActionsUsage(r.body)
}

internal suspend fun GitHubManager.getWorkflowRunUsage(context: Context, owner: String, repo: String, runId: Long): GHActionsUsage? {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/timing")
    if (!r.success) return null
    return parseActionsUsage(r.body)
}

internal suspend fun GitHubManager.getCheckRunsForRef(context: Context, owner: String, repo: String, ref: String): List<GHCheckRun> {
    if (ref.isBlank()) return emptyList()
    val encodedRef = URLEncoder.encode(ref, "UTF-8")
    val r = request(context, "/repos/$owner/$repo/commits/$encodedRef/check-runs?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONObject(r.body).optJSONArray("check_runs") ?: JSONArray()
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            val output = j.optJSONObject("output")
            GHCheckRun(
                id = j.optLong("id"),
                name = j.optString("name"),
                status = j.optString("status"),
                conclusion = j.optString("conclusion", ""),
                detailsUrl = j.optString("details_url", ""),
                htmlUrl = j.optString("html_url", ""),
                startedAt = j.optString("started_at", ""),
                completedAt = j.optString("completed_at", ""),
                title = output?.optString("title") ?: "",
                summary = output?.optString("summary") ?: "",
                annotationsCount = output?.optInt("annotations_count", 0) ?: 0
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getCheckRunAnnotations(context: Context, owner: String, repo: String, checkRunId: Long): List<GHCheckAnnotation> {
    val r = request(context, "/repos/$owner/$repo/check-runs/$checkRunId/annotations?per_page=100")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHCheckAnnotation(
                path = j.optString("path").trim().takeUnless { it.equals("null", ignoreCase = true) } ?: "",
                startLine = j.optInt("start_line", 0),
                endLine = j.optInt("end_line", 0),
                annotationLevel = j.optString("annotation_level").trim().takeUnless { it.equals("null", ignoreCase = true) } ?: "",
                message = j.optString("message").trim().takeUnless { it.equals("null", ignoreCase = true) } ?: "",
                title = j.optString("title", "").trim().takeUnless { it.equals("null", ignoreCase = true) } ?: "",
                rawDetails = j.optString("raw_details", "").trim().takeUnless { it.equals("null", ignoreCase = true) } ?: ""
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getWorkflowRunReviewHistory(context: Context, owner: String, repo: String, runId: Long): List<GHWorkflowRunReview> {
    val r = request(context, "/repos/$owner/$repo/actions/runs/$runId/approvals")
    if (!r.success) return emptyList()
    return try {
        val arr = JSONArray(r.body)
        (0 until arr.length()).map { i ->
            val j = arr.getJSONObject(i)
            GHWorkflowRunReview(
                state = j.optString("state"),
                comment = j.optString("comment", ""),
                user = j.optJSONObject("user")?.optString("login") ?: "",
                environments = j.optJSONArray("environments")?.let { envs ->
                    (0 until envs.length()).mapNotNull { idx -> envs.optJSONObject(idx)?.optString("name") }
                } ?: emptyList()
            )
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.approveWorkflowRunForFork(context: Context, owner: String, repo: String, runId: Long): Boolean =
    request(context, "/repos/$owner/$repo/actions/runs/$runId/approve", "POST", "{}").success

private fun parseArtifacts(body: String): List<GHArtifact> = try {
    val arr = JSONObject(body).optJSONArray("artifacts") ?: JSONArray()
    (0 until arr.length()).map { i -> parseArtifact(arr.getJSONObject(i)) }
} catch (e: Exception) { emptyList() }

private fun parseArtifact(j: JSONObject): GHArtifact {
    val workflowRun = j.optJSONObject("workflow_run")
    return GHArtifact(
        id = j.optLong("id"),
        name = j.optString("name"),
        sizeInBytes = j.optLong("size_in_bytes", 0),
        expired = j.optBoolean("expired", false),
        createdAt = j.optString("created_at", ""),
        expiresAt = j.optString("expires_at", ""),
        updatedAt = j.optString("updated_at", ""),
        digest = j.optString("digest", ""),
        workflowRunId = workflowRun?.optLong("id") ?: 0L,
        workflowRunBranch = workflowRun?.optString("head_branch") ?: "",
        workflowRunSha = workflowRun?.optString("head_sha") ?: ""
    )
}

private fun parseJobs(body: String): List<GHJob> = try {
    val arr = JSONObject(body).getJSONArray("jobs")
    (0 until arr.length()).map { i ->
        val j = arr.getJSONObject(i)
        val steps = mutableListOf<GHStep>()
        val stepsArr = j.optJSONArray("steps")
        if (stepsArr != null) for (s in 0 until stepsArr.length()) {
            val sj = stepsArr.getJSONObject(s)
            steps.add(GHStep(name = sj.optString("name"), status = sj.optString("status"), conclusion = sj.optString("conclusion", ""), number = sj.optInt("number"),
                startedAt = sj.optString("started_at", ""), completedAt = sj.optString("completed_at", "")))
        }
        GHJob(id = j.optLong("id"), name = j.optString("name"), status = j.optString("status"),
            conclusion = j.optString("conclusion", ""), startedAt = j.optString("started_at", ""),
            completedAt = j.optString("completed_at", ""), steps = steps)
    }
} catch (e: Exception) { emptyList() }

private fun parseActionsUsage(body: String): GHActionsUsage? = try {
    val j = JSONObject(body)
    val billable = j.optJSONObject("billable")
    val minutes = mutableMapOf<String, Int>()
    val ms = mutableMapOf<String, Long>()
    billable?.keys()?.forEach { key ->
        val platform = billable.optJSONObject(key)
        minutes[key] = platform?.optInt("total_ms", 0)?.div(60000) ?: 0
        ms[key] = platform?.optLong("total_ms", 0) ?: 0L
    }
    GHActionsUsage(runDurationMs = j.optLong("run_duration_ms", 0), billableMs = ms, billableMinutes = minutes)
} catch (e: Exception) { null }

private fun GitHubManager.actionsCommandResult(result: GitHubManager.ApiResult, successMessage: String): GHActionResult {
    val success = result.success || result.code == 201 || result.code == 202 || result.code == 204
    val requestId = result.headers["x-github-request-id"].orEmpty()
    val rateRemaining = result.headers["x-ratelimit-remaining"]?.toIntOrNull()
    val rateReset = result.headers["x-ratelimit-reset"]?.toLongOrNull()
    val message = if (success) {
        successMessage
    } else {
        val base = apiErrorMessage(result)
        val hint = when {
            result.code == 403 && rateRemaining == 0 -> "GitHub API rate limit is exhausted; reset epoch: ${rateReset ?: "unknown"}."
            result.code == 401 -> "Sign in again: the token is missing, expired, or revoked."
            result.code == 403 -> "Fine-grained PAT requires Actions: write for this repository."
            result.code == 404 -> "The workflow run or job was not found, or the token cannot access it."
            result.code == 409 -> "GitHub cannot perform this command in the current run state."
            result.code == 422 -> "GitHub rejected the command parameters or run state."
            else -> ""
        }
        val supportId = requestId.takeIf { it.isNotBlank() }?.let { "Request ID: $it" }.orEmpty()
        listOf(base, hint, supportId).filter { it.isNotBlank() }.joinToString(" · ")
    }
    return GHActionResult(
        success = success,
        code = result.code,
        message = message,
        requestId = requestId,
        rateRemaining = rateRemaining,
        rateResetEpochSeconds = rateReset,
    )
}

private suspend fun GitHubManager.getRedirectLocationOrText(context: Context, url: String): String =
    withContext(Dispatchers.IO) {
        try {
            val token = GitHubAuth.resolveApiToken(context)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
            }
            val code = conn.responseCode
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (location != null) "Logs URL: $location" else "Logs: HTTP $code"
        } catch (e: Exception) { "Error: ${e.message}" }
    }

private fun parseWorkflowRun(j: JSONObject) = GHWorkflowRun(
    id = j.optLong("id"), name = j.optString("name"), status = j.optString("status"),
    conclusion = j.optString("conclusion", ""), branch = j.optString("head_branch", ""),
    event = j.optString("event", ""), createdAt = j.optString("created_at", ""),
    updatedAt = j.optString("updated_at", ""), runNumber = j.optInt("run_number"),
    actor = j.optJSONObject("actor")?.optString("login") ?: "",
    actorAvatar = j.optJSONObject("actor")?.optString("avatar_url") ?: "",
    workflowId = j.optLong("workflow_id"),
    displayTitle = j.optString("display_title", ""),
    headSha = j.optString("head_sha", ""),
    headRepository = j.optJSONObject("head_repository")?.optString("full_name") ?: "",
    runAttempt = j.optInt("run_attempt", 1),
    htmlUrl = j.optString("html_url", ""),
    cancelUrl = j.optString("cancel_url", ""),
    rerunUrl = j.optString("rerun_url", ""),
    checkSuiteId = j.optLong("check_suite_id", 0)
)
