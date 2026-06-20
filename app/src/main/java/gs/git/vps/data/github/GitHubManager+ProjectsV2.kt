package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHProjectV2
import gs.git.vps.data.github.model.GHProjectV2Detail
import gs.git.vps.data.github.model.GHProjectV2Field
import gs.git.vps.data.github.model.GHProjectV2FieldOption
import gs.git.vps.data.github.model.GHProjectV2Item
import gs.git.vps.data.github.model.GHProjectV2ItemFieldValue
import gs.git.vps.data.github.model.GHProjectV2View
import gs.git.vps.data.github.model.GHProjectV2Workflow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Projects V2 (GraphQL) слоя GitHub API — проекты нового поколения: поля,
 * представления, workflow'ы, draft-issue и items с field-values. Нарезан по эталону
 * (см. docs/decomposition-log.md). Classic Projects (REST) — в GitHubManager+Projects.kt.
 * Сеть — через ядерный helper graphql() поверх request(); парсинг — чистые parse*-функции без IO.
 */

// ─── Функции ───

internal suspend fun GitHubManager.getRepoProjectsV2(context: Context, owner: String, repo: String): List<GHProjectV2> {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            projectsV2(first: 30, orderBy: {field: UPDATED_AT, direction: DESC}) {
              nodes {
                id
                number
                title
                shortDescription
                url
                closed
                public
                updatedAt
                items { totalCount }
              }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
    }) ?: return emptyList()
    return try {
        val nodes = data.optJSONObject("repository")?.optJSONObject("projectsV2")?.optJSONArray("nodes") ?: return emptyList()
        (0 until nodes.length()).mapNotNull { i ->
            nodes.optJSONObject(i)?.let { j ->
                GHProjectV2(
                    id = j.optString("id"),
                    number = j.optInt("number"),
                    title = j.optString("title"),
                    shortDescription = j.optString("shortDescription", ""),
                    url = j.optString("url", ""),
                    closed = j.optBoolean("closed", false),
                    isPublic = j.optBoolean("public", false),
                    updatedAt = j.optString("updatedAt", ""),
                    itemsCount = j.optJSONObject("items")?.optInt("totalCount", 0) ?: 0
                )
            }
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getProjectV2Detail(context: Context, projectId: String): GHProjectV2Detail? {
    val data = graphql(context, """
        query(${'$'}projectId: ID!) {
          node(id: ${'$'}projectId) {
            ... on ProjectV2 {
              id
              number
              title
              shortDescription
              readme
              url
              closed
              public
              updatedAt
              fields(first: 50) {
                nodes {
                  __typename
                  ... on ProjectV2FieldCommon {
                    id
                    name
                    dataType
                  }
                  ... on ProjectV2SingleSelectField {
                    id
                    name
                    dataType
                    options { id name color description }
                  }
                  ... on ProjectV2IterationField {
                    id
                    name
                    dataType
                  }
                }
              }
              views(first: 30) {
                totalCount
                nodes {
                  id
                  number
                  name
                  layout
                  filter
                  updatedAt
                  fields(first: 20) {
                    nodes {
                      ... on ProjectV2FieldCommon {
                        id
                        name
                        dataType
                      }
                    }
                  }
                }
              }
              workflows(first: 30) {
                totalCount
                nodes {
                  id
                  number
                  name
                  enabled
                  updatedAt
                }
              }
              items(first: 100) {
                totalCount
                nodes {
                  id
                  type
                  archived
                  updatedAt
                  creator { login }
                  content {
                    __typename
                    ... on DraftIssue { id title body createdAt updatedAt }
                    ... on Issue { id title body number state url createdAt updatedAt }
                    ... on PullRequest { id title body number state url createdAt updatedAt }
                  }
                  fieldValues(first: 50) {
                    nodes {
                      __typename
                      ... on ProjectV2ItemFieldTextValue {
                        text
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldNumberValue {
                        number
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldDateValue {
                        date
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldSingleSelectValue {
                        name
                        optionId
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldIterationValue {
                        title
                        iterationId
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldPullRequestValue {
                        pullRequests(first: 3) { totalCount }
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                      ... on ProjectV2ItemFieldRepositoryValue {
                        repository { nameWithOwner }
                        field { ... on ProjectV2FieldCommon { id name dataType } }
                      }
                    }
                  }
                }
              }
            }
          }
        }
    """.trimIndent(), JSONObject().apply { put("projectId", projectId) }) ?: return null
    return try {
        data.optJSONObject("node")?.let(::parseProjectV2Detail)
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateProjectV2(context: Context, projectId: String, title: String, shortDescription: String, readme: String, closed: Boolean, isPublic: Boolean): Boolean {
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}title: String!, ${'$'}shortDescription: String, ${'$'}readme: String, ${'$'}closed: Boolean!, ${'$'}public: Boolean!) {
          updateProjectV2(input: {projectId: ${'$'}projectId, title: ${'$'}title, shortDescription: ${'$'}shortDescription, readme: ${'$'}readme, closed: ${'$'}closed, public: ${'$'}public}) {
            projectV2 { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("title", title)
        put("shortDescription", shortDescription)
        put("readme", readme)
        put("closed", closed)
        put("public", isPublic)
    })
    return data?.optJSONObject("updateProjectV2")?.optJSONObject("projectV2")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.createProjectV2Field(context: Context, projectId: String, name: String, dataType: String, options: List<String> = emptyList()): GHProjectV2Field? {
    val variables = JSONObject().apply {
        put("projectId", projectId)
        put("name", name)
        put("dataType", dataType)
        if (dataType == "SINGLE_SELECT") put("singleSelectOptions", projectV2SingleSelectOptionsJson(options))
    }
    val optionVariable = if (dataType == "SINGLE_SELECT") ", ${'$'}singleSelectOptions: [ProjectV2SingleSelectFieldOptionInput!]" else ""
    val optionInput = if (dataType == "SINGLE_SELECT") ", singleSelectOptions: ${'$'}singleSelectOptions" else ""
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}name: String!, ${'$'}dataType: ProjectV2CustomFieldType!$optionVariable) {
          createProjectV2Field(input: {projectId: ${'$'}projectId, name: ${'$'}name, dataType: ${'$'}dataType$optionInput}) {
            projectV2Field {
              ... on ProjectV2FieldCommon { id name dataType }
              ... on ProjectV2SingleSelectField { options { id name color description } }
            }
          }
        }
    """.trimIndent(), variables) ?: return null
    return try {
        data.optJSONObject("createProjectV2Field")?.optJSONObject("projectV2Field")?.let(::parseProjectV2Field)
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateProjectV2Field(context: Context, field: GHProjectV2Field, name: String, options: List<String> = emptyList()): GHProjectV2Field? {
    val variables = JSONObject().apply {
        put("fieldId", field.id)
        put("name", name)
        if (field.dataType == "SINGLE_SELECT") put("singleSelectOptions", projectV2SingleSelectOptionsJson(options))
    }
    val optionVariable = if (field.dataType == "SINGLE_SELECT") ", ${'$'}singleSelectOptions: [ProjectV2SingleSelectFieldOptionInput!]" else ""
    val optionInput = if (field.dataType == "SINGLE_SELECT") ", singleSelectOptions: ${'$'}singleSelectOptions" else ""
    val data = graphql(context, """
        mutation(${'$'}fieldId: ID!, ${'$'}name: String$optionVariable) {
          updateProjectV2Field(input: {fieldId: ${'$'}fieldId, name: ${'$'}name$optionInput}) {
            projectV2Field {
              ... on ProjectV2FieldCommon { id name dataType }
              ... on ProjectV2SingleSelectField { options { id name color description } }
            }
          }
        }
    """.trimIndent(), variables) ?: return null
    return try {
        data.optJSONObject("updateProjectV2Field")?.optJSONObject("projectV2Field")?.let(::parseProjectV2Field)
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.deleteProjectV2Field(context: Context, fieldId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}fieldId: ID!) {
          deleteProjectV2Field(input: {fieldId: ${'$'}fieldId}) {
            projectV2Field {
              ... on ProjectV2FieldCommon { id }
            }
          }
        }
    """.trimIndent(), JSONObject().apply { put("fieldId", fieldId) })
    return data?.optJSONObject("deleteProjectV2Field")?.optJSONObject("projectV2Field")?.optString("id").orEmpty().isNotBlank()
}

private fun projectV2SingleSelectOptionsJson(options: List<String>): JSONArray =
    JSONArray(options.map {
        JSONObject().apply {
            put("name", it)
            put("description", "")
            put("color", "GRAY")
        }
    })

internal suspend fun GitHubManager.addProjectV2DraftIssue(context: Context, projectId: String, title: String, body: String): GHProjectV2Item? {
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}title: String!, ${'$'}body: String) {
          addProjectV2DraftIssue(input: {projectId: ${'$'}projectId, title: ${'$'}title, body: ${'$'}body}) {
            projectItem {
              id
              type
              archived
              updatedAt
              creator { login }
              content {
                __typename
                ... on DraftIssue { id title body createdAt updatedAt }
              }
              fieldValues(first: 1) { nodes { __typename } }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("title", title)
        put("body", body)
    }) ?: return null
    return try {
        data.optJSONObject("addProjectV2DraftIssue")?.optJSONObject("projectItem")?.let(::parseProjectV2Item)
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.updateProjectV2DraftIssue(context: Context, draftIssueId: String, title: String, body: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}draftIssueId: ID!, ${'$'}title: String!, ${'$'}body: String) {
          updateProjectV2DraftIssue(input: {draftIssueId: ${'$'}draftIssueId, title: ${'$'}title, body: ${'$'}body}) {
            draftIssue { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("draftIssueId", draftIssueId)
        put("title", title)
        put("body", body)
    })
    return data?.optJSONObject("updateProjectV2DraftIssue")?.optJSONObject("draftIssue")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.deleteProjectV2Item(context: Context, projectId: String, itemId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}itemId: ID!) {
          deleteProjectV2Item(input: {projectId: ${'$'}projectId, itemId: ${'$'}itemId}) {
            deletedItemId
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("itemId", itemId)
    })
    return data?.optJSONObject("deleteProjectV2Item")?.optString("deletedItemId").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.archiveProjectV2Item(context: Context, projectId: String, itemId: String, archived: Boolean): Boolean {
    val mutation = if (archived) "archiveProjectV2Item" else "unarchiveProjectV2Item"
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}itemId: ID!) {
          $mutation(input: {projectId: ${'$'}projectId, itemId: ${'$'}itemId}) {
            item { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("itemId", itemId)
    })
    return data?.optJSONObject(mutation)?.optJSONObject("item")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.updateProjectV2ItemFieldValue(context: Context, projectId: String, itemId: String, field: GHProjectV2Field, value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return clearProjectV2ItemFieldValue(context, projectId, itemId, field.id)
    val valueObject = JSONObject()
    when (field.dataType.lowercase()) {
        "number" -> valueObject.put("number", normalized.toDoubleOrNull() ?: return false)
        "date" -> valueObject.put("date", normalized)
        "single_select" -> {
            val option = field.options.firstOrNull { it.id == normalized || it.name.equals(normalized, ignoreCase = true) } ?: return false
            valueObject.put("singleSelectOptionId", option.id)
        }
        else -> valueObject.put("text", normalized)
    }
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}itemId: ID!, ${'$'}fieldId: ID!, ${'$'}value: ProjectV2FieldValue!) {
          updateProjectV2ItemFieldValue(input: {projectId: ${'$'}projectId, itemId: ${'$'}itemId, fieldId: ${'$'}fieldId, value: ${'$'}value}) {
            projectV2Item { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("itemId", itemId)
        put("fieldId", field.id)
        put("value", valueObject)
    })
    return data?.optJSONObject("updateProjectV2ItemFieldValue")?.optJSONObject("projectV2Item")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.clearProjectV2ItemFieldValue(context: Context, projectId: String, itemId: String, fieldId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}itemId: ID!, ${'$'}fieldId: ID!) {
          clearProjectV2ItemFieldValue(input: {projectId: ${'$'}projectId, itemId: ${'$'}itemId, fieldId: ${'$'}fieldId}) {
            projectV2Item { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("itemId", itemId)
        put("fieldId", fieldId)
    })
    return data?.optJSONObject("clearProjectV2ItemFieldValue")?.optJSONObject("projectV2Item")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.moveProjectV2Item(context: Context, projectId: String, itemId: String, afterId: String?): Boolean {
    val data = graphql(context, """
        mutation(${'$'}projectId: ID!, ${'$'}itemId: ID!, ${'$'}afterId: ID) {
          updateProjectV2ItemPosition(input: {projectId: ${'$'}projectId, itemId: ${'$'}itemId, afterId: ${'$'}afterId}) {
            items { totalCount }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("projectId", projectId)
        put("itemId", itemId)
        if (afterId.isNullOrBlank()) put("afterId", JSONObject.NULL) else put("afterId", afterId)
    })
    return data?.optJSONObject("updateProjectV2ItemPosition")?.optJSONObject("items") != null
}

// ─── Парсеры (чистые, без IO) ───

private fun parseProjectV2Detail(j: JSONObject): GHProjectV2Detail {
    val fields = j.optJSONObject("fields")?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectV2Field) }
    }.orEmpty()
    val itemsObject = j.optJSONObject("items")
    val items = itemsObject?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectV2Item) }
    }.orEmpty()
    val viewsObject = j.optJSONObject("views")
    val views = viewsObject?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectV2View) }
    }.orEmpty()
    val workflowsObject = j.optJSONObject("workflows")
    val workflows = workflowsObject?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectV2Workflow) }
    }.orEmpty()
    return GHProjectV2Detail(
        id = j.optString("id"),
        number = j.optInt("number"),
        title = j.optString("title"),
        shortDescription = j.optString("shortDescription", ""),
        readme = j.optString("readme", ""),
        url = j.optString("url", ""),
        closed = j.optBoolean("closed", false),
        isPublic = j.optBoolean("public", false),
        updatedAt = j.optString("updatedAt", ""),
        itemsCount = itemsObject?.optInt("totalCount", items.size) ?: items.size,
        fields = fields,
        items = items,
        views = views,
        workflows = workflows
    )
}

private fun parseProjectV2Field(j: JSONObject): GHProjectV2Field? {
    val id = j.optString("id").takeIf { it.isNotBlank() } ?: return null
    val options = j.optJSONArray("options")?.let { arr ->
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { option ->
                GHProjectV2FieldOption(
                    id = option.optString("id"),
                    name = option.optString("name"),
                    color = option.optString("color", ""),
                    description = option.optString("description", "")
                )
            }
        }
    }.orEmpty()
    return GHProjectV2Field(
        id = id,
        name = j.optString("name"),
        dataType = j.optString("dataType", ""),
        options = options
    )
}

private fun parseProjectV2View(j: JSONObject): GHProjectV2View {
    val fields = j.optJSONObject("fields")?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
        }
    }.orEmpty()
    return GHProjectV2View(
        id = j.optString("id"),
        number = j.optInt("number"),
        name = j.optString("name", ""),
        layout = j.optString("layout", ""),
        filter = j.optString("filter", ""),
        updatedAt = j.optString("updatedAt", ""),
        fields = fields
    )
}

private fun parseProjectV2Workflow(j: JSONObject): GHProjectV2Workflow =
    GHProjectV2Workflow(
        id = j.optString("id"),
        number = j.optInt("number"),
        name = j.optString("name", ""),
        enabled = j.optBoolean("enabled", false),
        updatedAt = j.optString("updatedAt", "")
    )

private fun parseProjectV2Item(j: JSONObject): GHProjectV2Item {
    val content = j.optJSONObject("content")
    val typename = content?.optString("__typename", j.optString("type", "")) ?: j.optString("type", "")
    val fieldValues = j.optJSONObject("fieldValues")?.optJSONArray("nodes")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::parseProjectV2ItemFieldValue) }
    }.orEmpty()
    return GHProjectV2Item(
        id = j.optString("id"),
        type = j.optString("type", typename),
        contentId = content?.optString("id", "") ?: "",
        contentType = typename,
        title = content?.optString("title") ?: "",
        body = content?.optString("body", "") ?: "",
        number = content?.optInt("number", 0) ?: 0,
        state = content?.optString("state", "") ?: "",
        url = content?.optString("url", "") ?: "",
        creator = j.optJSONObject("creator")?.optString("login") ?: "",
        archived = j.optBoolean("archived", false),
        createdAt = content?.optString("createdAt", "") ?: "",
        updatedAt = content?.optString("updatedAt", j.optString("updatedAt", "")) ?: j.optString("updatedAt", ""),
        fieldValues = fieldValues
    )
}

private fun parseProjectV2ItemFieldValue(j: JSONObject): GHProjectV2ItemFieldValue? {
    val field = j.optJSONObject("field") ?: return null
    val fieldId = field.optString("id").takeIf { it.isNotBlank() } ?: return null
    val type = field.optString("dataType", "")
    val value = when (j.optString("__typename")) {
        "ProjectV2ItemFieldTextValue" -> j.optString("text", "")
        "ProjectV2ItemFieldNumberValue" -> j.optDouble("number", 0.0).toString()
        "ProjectV2ItemFieldDateValue" -> j.optString("date", "")
        "ProjectV2ItemFieldSingleSelectValue" -> j.optString("name", "")
        "ProjectV2ItemFieldIterationValue" -> j.optString("title", "")
        "ProjectV2ItemFieldRepositoryValue" -> j.optJSONObject("repository")?.optString("nameWithOwner") ?: ""
        "ProjectV2ItemFieldPullRequestValue" -> "${j.optJSONObject("pullRequests")?.optInt("totalCount", 0) ?: 0} pull requests"
        else -> ""
    }
    return GHProjectV2ItemFieldValue(
        fieldId = fieldId,
        fieldName = field.optString("name", ""),
        dataType = type,
        value = value,
        optionId = j.optString("optionId", ""),
        iterationId = j.optString("iterationId", "")
    )
}
