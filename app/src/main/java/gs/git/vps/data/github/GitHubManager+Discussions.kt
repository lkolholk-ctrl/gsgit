package gs.git.vps.data.github

import android.content.Context
import gs.git.vps.data.github.model.GHComment
import gs.git.vps.data.github.model.GHDiscussion
import gs.git.vps.data.github.model.GHDiscussionCategory
import org.json.JSONArray
import org.json.JSONObject

/**
 * Домен Discussions слоя GitHub API — целиком GraphQL (обсуждения, категории, комментарии,
 * пометка ответа, upvote). Нарезан по эталону Releases (см. docs/decomposition-log.md).
 * Сеть — через ядро `graphql()`; парсинг — чистые `parseDiscussion*`. Сигнатуры вызовов не менялись.
 *
 * Комментарии обсуждений возвращают GHComment (домен Issues). `getRepositoryNodeId` — приватный
 * helper только этого домена (резолвит node-id репо для createDiscussion).
 */

internal suspend fun GitHubManager.getDiscussions(context: Context, owner: String, repo: String): List<GHDiscussion> {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            discussions(first: 50, orderBy: {field: UPDATED_AT, direction: DESC}) {
              nodes {
                id
                number
                title
                body
                createdAt
                updatedAt
                closed
                locked
                url
                upvoteCount
                viewerHasUpvoted
                answer { id }
                category { id name emoji isAnswerable }
                author { login avatarUrl }
                comments { totalCount }
              }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
    }) ?: return emptyList()
    return try {
        val nodes = data.optJSONObject("repository")?.optJSONObject("discussions")?.optJSONArray("nodes") ?: return emptyList()
        parseDiscussionNodes(nodes)
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getDiscussionCategories(context: Context, owner: String, repo: String): List<GHDiscussionCategory> {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            discussionCategories(first: 50) {
              nodes {
                id
                name
                slug
                emoji
                description
                isAnswerable
              }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
    }) ?: return emptyList()
    return try {
        val nodes = data.optJSONObject("repository")?.optJSONObject("discussionCategories")?.optJSONArray("nodes") ?: return emptyList()
        (0 until nodes.length()).mapNotNull { i ->
            nodes.optJSONObject(i)?.let { j ->
                GHDiscussionCategory(
                    id = j.optString("id"),
                    name = j.optString("name"),
                    slug = j.optString("slug"),
                    emoji = j.optString("emoji"),
                    description = j.optString("description", ""),
                    isAnswerable = j.optBoolean("isAnswerable", false)
                )
            }
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.getDiscussionDetail(context: Context, owner: String, repo: String, discussionNumber: Int): GHDiscussion? {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!, ${'$'}number: Int!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            discussion(number: ${'$'}number) {
              id
              number
              title
              body
              createdAt
              updatedAt
              closed
              locked
              url
              upvoteCount
              viewerHasUpvoted
              answer { id }
              category { id name emoji isAnswerable }
              author { login avatarUrl }
              comments { totalCount }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
        put("number", discussionNumber)
    }) ?: return null
    return try {
        data.optJSONObject("repository")?.optJSONObject("discussion")?.let(::parseDiscussion)
    } catch (e: Exception) { null }
}

internal suspend fun GitHubManager.createDiscussion(context: Context, owner: String, repo: String, title: String, body: String, categoryId: String): Boolean {
    val repositoryId = getRepositoryNodeId(context, owner, repo) ?: return false
    val data = graphql(context, """
        mutation(${'$'}repositoryId: ID!, ${'$'}categoryId: ID!, ${'$'}title: String!, ${'$'}body: String!) {
          createDiscussion(input: {repositoryId: ${'$'}repositoryId, categoryId: ${'$'}categoryId, title: ${'$'}title, body: ${'$'}body}) {
            discussion { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("repositoryId", repositoryId)
        put("categoryId", categoryId)
        put("title", title)
        put("body", body)
    })
    return data?.optJSONObject("createDiscussion")?.optJSONObject("discussion")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.updateDiscussion(context: Context, discussionId: String, title: String, body: String, categoryId: String? = null): Boolean {
    val data = graphql(context, """
        mutation(${'$'}discussionId: ID!, ${'$'}title: String!, ${'$'}body: String!, ${'$'}categoryId: ID) {
          updateDiscussion(input: {discussionId: ${'$'}discussionId, title: ${'$'}title, body: ${'$'}body, categoryId: ${'$'}categoryId}) {
            discussion { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("discussionId", discussionId)
        put("title", title)
        put("body", body)
        if (categoryId.isNullOrBlank()) put("categoryId", JSONObject.NULL) else put("categoryId", categoryId)
    })
    return data?.optJSONObject("updateDiscussion")?.optJSONObject("discussion")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.deleteDiscussion(context: Context, discussionId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}discussionId: ID!) {
          deleteDiscussion(input: {id: ${'$'}discussionId}) {
            discussion { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("discussionId", discussionId)
    })
    return data?.optJSONObject("deleteDiscussion")?.optJSONObject("discussion")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.getDiscussionComments(context: Context, owner: String, repo: String, discussionNumber: Int): List<GHComment> {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!, ${'$'}number: Int!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            discussion(number: ${'$'}number) {
              comments(first: 100) {
                nodes {
                  id
                  databaseId
                  body
                  createdAt
                  author { login avatarUrl }
                }
              }
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
        put("number", discussionNumber)
    }) ?: return emptyList()
    return try {
        val nodes = data.optJSONObject("repository")
            ?.optJSONObject("discussion")
            ?.optJSONObject("comments")
            ?.optJSONArray("nodes") ?: return emptyList()
        (0 until nodes.length()).mapNotNull { i ->
            nodes.optJSONObject(i)?.let { j ->
                val author = j.optJSONObject("author")
                GHComment(
                    id = j.optLong("databaseId", 0L),
                    body = j.optString("body"),
                    author = author?.optString("login") ?: "",
                    avatarUrl = author?.optString("avatarUrl") ?: "",
                    createdAt = j.optString("createdAt"),
                    nodeId = j.optString("id", "")
                )
            }
        }
    } catch (e: Exception) { emptyList() }
}

internal suspend fun GitHubManager.addDiscussionComment(context: Context, discussionId: String, body: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}discussionId: ID!, ${'$'}body: String!) {
          addDiscussionComment(input: {discussionId: ${'$'}discussionId, body: ${'$'}body}) {
            comment { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("discussionId", discussionId)
        put("body", body)
    })
    return data?.optJSONObject("addDiscussionComment")?.optJSONObject("comment")?.optString("id").orEmpty().isNotBlank()
}

internal suspend fun GitHubManager.markDiscussionCommentAsAnswer(context: Context, commentId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}id: ID!) {
          markDiscussionCommentAsAnswer(input: {id: ${'$'}id}) {
            discussion { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("id", commentId)
    })
    return data?.optJSONObject("markDiscussionCommentAsAnswer")?.optJSONObject("discussion") != null
}

internal suspend fun GitHubManager.unmarkDiscussionCommentAsAnswer(context: Context, commentId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}id: ID!) {
          unmarkDiscussionCommentAsAnswer(input: {id: ${'$'}id}) {
            discussion { id }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("id", commentId)
    })
    return data?.optJSONObject("unmarkDiscussionCommentAsAnswer")?.optJSONObject("discussion") != null
}

internal suspend fun GitHubManager.addDiscussionUpvote(context: Context, subjectId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}subjectId: ID!) {
          addUpvote(input: {subjectId: ${'$'}subjectId}) {
            subject {
              id
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("subjectId", subjectId)
    })
    return data?.optJSONObject("addUpvote")?.optJSONObject("subject") != null
}

internal suspend fun GitHubManager.removeDiscussionUpvote(context: Context, subjectId: String): Boolean {
    val data = graphql(context, """
        mutation(${'$'}subjectId: ID!) {
          removeUpvote(input: {subjectId: ${'$'}subjectId}) {
            subject {
              id
            }
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("subjectId", subjectId)
    })
    return data?.optJSONObject("removeUpvote")?.optJSONObject("subject") != null
}

private suspend fun GitHubManager.getRepositoryNodeId(context: Context, owner: String, repo: String): String? {
    val data = graphql(context, """
        query(${'$'}owner: String!, ${'$'}repo: String!) {
          repository(owner: ${'$'}owner, name: ${'$'}repo) {
            id
          }
        }
    """.trimIndent(), JSONObject().apply {
        put("owner", owner)
        put("repo", repo)
    }) ?: return null
    return data.optJSONObject("repository")?.optString("id")?.takeIf { it.isNotBlank() }
}

private fun parseDiscussionNodes(nodes: JSONArray): List<GHDiscussion> =
    (0 until nodes.length()).mapNotNull { i -> nodes.optJSONObject(i)?.let(::parseDiscussion) }

private fun parseDiscussion(j: JSONObject): GHDiscussion {
    val author = j.optJSONObject("author")
    val category = j.optJSONObject("category")
    return GHDiscussion(
        id = j.optString("id"),
        number = j.optInt("number"),
        title = j.optString("title"),
        body = j.optString("body", ""),
        author = author?.optString("login") ?: "",
        avatarUrl = author?.optString("avatarUrl") ?: "",
        state = if (j.optBoolean("closed", false)) "closed" else "open",
        comments = j.optJSONObject("comments")?.optInt("totalCount", 0) ?: 0,
        createdAt = j.optString("createdAt", ""),
        updatedAt = j.optString("updatedAt", ""),
        categoryId = category?.optString("id") ?: "",
        categoryName = category?.optString("name") ?: "",
        categoryEmoji = category?.optString("emoji") ?: "",
        isAnswerable = category?.optBoolean("isAnswerable", false) ?: false,
        isAnswered = j.optJSONObject("answer") != null,
        locked = j.optBoolean("locked", false),
        upvotes = j.optInt("upvoteCount", 0),
        viewerHasUpvoted = j.optBoolean("viewerHasUpvoted", false),
        htmlUrl = j.optString("url", "")
    )
}
