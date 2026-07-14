package gs.git.vps.ui.screens

import androidx.compose.runtime.saveable.Saver
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHPermissions
import gs.git.vps.data.github.model.GHRepo
import org.json.JSONObject

/**
 * Bundle-safe snapshots for navigation objects that are not Parcelable. Only stable model fields
 * are stored; network payloads and file contents are deliberately reloaded after restoration.
 */
internal val NullableGitHubRepoSaver = Saver<GHRepo?, String>(
    save = { repo ->
        if (repo == null) "" else JSONObject().apply {
            put("name", repo.name)
            put("fullName", repo.fullName)
            put("description", repo.description)
            put("language", repo.language)
            put("stars", repo.stars)
            put("forks", repo.forks)
            put("private", repo.isPrivate)
            put("fork", repo.isFork)
            put("defaultBranch", repo.defaultBranch)
            put("updatedAt", repo.updatedAt)
            put("owner", repo.owner)
            put("htmlUrl", repo.htmlUrl)
            put("archived", repo.isArchived)
            put("template", repo.isTemplate)
            put("id", repo.id)
            put("openIssues", repo.openIssues)
            repo.permissions?.let { permissions ->
                put("permissions", JSONObject().apply {
                    put("admin", permissions.admin)
                    put("maintain", permissions.maintain)
                    put("push", permissions.push)
                    put("triage", permissions.triage)
                    put("pull", permissions.pull)
                })
            }
        }.toString()
    },
    restore = { encoded ->
        if (encoded.isBlank()) null else runCatching {
            val json = JSONObject(encoded)
            val permissions = json.optJSONObject("permissions")?.let {
                GHPermissions(
                    admin = it.optBoolean("admin"),
                    maintain = it.optBoolean("maintain"),
                    push = it.optBoolean("push"),
                    triage = it.optBoolean("triage"),
                    pull = it.optBoolean("pull"),
                )
            }
            GHRepo(
                name = json.getString("name"),
                fullName = json.getString("fullName"),
                description = json.optString("description"),
                language = json.optString("language"),
                stars = json.optInt("stars"),
                forks = json.optInt("forks"),
                isPrivate = json.optBoolean("private"),
                isFork = json.optBoolean("fork"),
                defaultBranch = json.optString("defaultBranch"),
                updatedAt = json.optString("updatedAt"),
                owner = json.getString("owner"),
                htmlUrl = json.optString("htmlUrl"),
                isArchived = json.optBoolean("archived"),
                isTemplate = json.optBoolean("template"),
                id = json.optLong("id"),
                openIssues = json.optInt("openIssues"),
                permissions = permissions,
            )
        }.getOrNull()
    },
)

internal val NullableGitHubContentSaver = Saver<GHContent?, String>(
    save = { content ->
        if (content == null) "" else JSONObject().apply {
            put("name", content.name)
            put("path", content.path)
            put("type", content.type)
            put("size", content.size)
            put("downloadUrl", content.downloadUrl)
            put("sha", content.sha)
        }.toString()
    },
    restore = { encoded ->
        if (encoded.isBlank()) null else runCatching {
            val json = JSONObject(encoded)
            GHContent(
                name = json.getString("name"),
                path = json.getString("path"),
                type = json.optString("type", "file"),
                size = json.optLong("size"),
                downloadUrl = json.optString("downloadUrl"),
                sha = json.optString("sha"),
            )
        }.getOrNull()
    },
)
