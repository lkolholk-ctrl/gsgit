package gs.git.vps.data.github.model

/**
 * Модели домена Rulesets слоя GitHub API (repository rulesets и rule-suites). Вынесены из
 * god-файла GitHubManager.kt (см. docs/decomposition-log.md).
 */

data class GHRuleset(
    val id: Int,
    val name: String,
    val enforcement: String,
    val rulesCount: Int,
    val target: String = "",
    val sourceType: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val htmlUrl: String = ""
)

data class GHRulesetDetail(
    val id: Int,
    val name: String,
    val target: String,
    val sourceType: String,
    val source: String,
    val enforcement: String,
    val createdAt: String,
    val updatedAt: String,
    val rules: List<GHRulesetRule>,
    val bypassActors: List<GHRulesetBypassActor>,
    val refNameIncludes: List<String>,
    val refNameExcludes: List<String>,
    val htmlUrl: String
)

data class GHRulesetRule(
    val type: String,
    val parameters: List<Pair<String, String>>
)

data class GHRulesetBypassActor(
    val actorId: Long,
    val actorType: String,
    val bypassMode: String
)

data class GHRuleSuite(
    val id: Long,
    val actor: String,
    val beforeSha: String,
    val afterSha: String,
    val ref: String,
    val status: String,
    val result: String,
    val evaluationResult: String,
    val createdAt: String,
    val updatedAt: String
)
