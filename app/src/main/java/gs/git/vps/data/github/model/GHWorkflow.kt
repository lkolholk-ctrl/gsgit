package gs.git.vps.data.github.model

/** Модели домена Workflows (GitHub Actions): workflows/runs/jobs/artifacts/checks/usage. */

data class GHActionResult(val success: Boolean, val code: Int, val message: String)

data class GHWorkflow(
    val id: Long,
    val name: String,
    val state: String,
    val path: String,
    val htmlUrl: String = "",
    val badgeUrl: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class GHWorkflowDispatchInput(
    val key: String,
    val description: String,
    val required: Boolean,
    val defaultValue: String,
    val type: String,
    val options: List<String>
)

data class GHWorkflowDispatchSchema(
    val workflowPath: String,
    val workflowName: String,
    val inputs: List<GHWorkflowDispatchInput>
)

data class GHWorkflowRun(val id: Long, val name: String, val status: String, val conclusion: String,
    val branch: String, val event: String, val createdAt: String, val updatedAt: String,
    val runNumber: Int, val actor: String, val actorAvatar: String, val workflowId: Long,
    val displayTitle: String = "", val headSha: String = "", val headRepository: String = "",
    val runAttempt: Int = 1, val htmlUrl: String = "", val cancelUrl: String = "",
    val rerunUrl: String = "", val checkSuiteId: Long = 0)

data class GHJob(val id: Long, val name: String, val status: String, val conclusion: String,
    val startedAt: String, val completedAt: String, val steps: List<GHStep>)

data class GHStep(val name: String, val status: String, val conclusion: String, val number: Int)

data class GHArtifact(val id: Long, val name: String, val sizeInBytes: Long,
    val expired: Boolean, val createdAt: String, val expiresAt: String,
    val updatedAt: String = "", val digest: String = "", val workflowRunId: Long = 0,
    val workflowRunBranch: String = "", val workflowRunSha: String = "")

data class GHCheckRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String,
    val detailsUrl: String,
    val startedAt: String,
    val completedAt: String,
    val outputTitle: String = "",
    val outputSummary: String = "",
    val htmlUrl: String = "",
    val title: String = outputTitle,
    val summary: String = outputSummary,
    val annotationsCount: Int = 0
)

data class GHCheckAnnotation(val path: String, val startLine: Int, val endLine: Int,
    val annotationLevel: String, val message: String, val title: String, val rawDetails: String)

data class GHActionsUsage(val runDurationMs: Long, val billableMs: Map<String, Long>,
    val billableMinutes: Map<String, Int>)

data class GHWorkflowRunReview(val state: String, val comment: String, val user: String,
    val environments: List<String>)
