package gs.git.vps.data.github

import android.content.Context

internal data class CodeDraftRebaseResult(
    val changes: List<CodeChange>,
    val bases: Map<String, String>,
    val conflictedPaths: List<String>,
    val error: String? = null,
)

/** Returns null only for a real 404; transport/API failures throw so callers never assume delete. */
internal suspend fun GitHubManager.getCodeRemoteText(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    path: String,
): String? {
    val remote = getCodeRemoteBytes(
        context = context,
        owner = owner,
        repo = repo,
        branch = branch,
        path = path,
        maxBytes = CODE_TEXT_MAX_BYTES,
    ) ?: return null
    return decodeCodeText(remote.bytes)
}

/** Rebase a local A/M/D/R draft onto the latest branch contents without dropping either side. */
internal suspend fun GitHubManager.rebaseCodeDraft(
    context: Context,
    owner: String,
    repo: String,
    branch: String,
    changes: Collection<CodeChange>,
    bases: Map<String, String>,
): CodeDraftRebaseResult {
    val rebased = mutableListOf<CodeChange>()
    val nextBases = bases.toMutableMap()
    val conflicts = mutableListOf<String>()

    return try {
        for (change in changes) {
            when (change) {
                is CodeAdd -> {
                    val remote = getCodeRemoteText(context, owner, repo, branch, change.path)
                    if (remote == null) {
                        rebased += change
                        nextBases[change.path] = ""
                    } else {
                        val local = change.content ?: change.sourcePath?.let {
                            getFileContent(context, owner, repo, it, branch)
                        }.orEmpty()
                        val merged = mergeCodeText(nextBases[change.path].orEmpty(), local, remote)
                        rebased += change.copy(content = merged.text, sourcePath = null, sourceSha = null)
                        nextBases[change.path] = remote
                        if (merged.hasConflicts) conflicts += change.path
                    }
                }
                is CodeModify -> {
                    val remote = getCodeRemoteText(context, owner, repo, branch, change.path).orEmpty()
                    val base = nextBases[change.path]
                    val merged = if (base != null) mergeCodeText(base, change.content, remote)
                    else unknownAncestorConflict(change.content, remote)
                    rebased += change.copy(content = merged.text)
                    nextBases[change.path] = remote
                    if (merged.hasConflicts) conflicts += change.path
                }
                is CodeDelete -> {
                    val remote = getCodeRemoteText(context, owner, repo, branch, change.path)
                    val base = nextBases[change.path]
                    when {
                        remote == null || (base != null && remote == base) -> {
                            rebased += change
                            nextBases[change.path] = remote.orEmpty()
                        }
                        base != null -> {
                            // A changed remote file is restored as a text conflict instead of being
                            // silently deleted. The user can resolve it and delete again explicitly.
                            val merged = mergeCodeText(base, "", remote)
                            rebased += CodeModify(change.path, merged.text)
                            nextBases[change.path] = remote
                            conflicts += change.path
                        }
                        else -> {
                            val merged = unknownAncestorConflict("", remote)
                            rebased += CodeModify(change.path, merged.text)
                            nextBases[change.path] = remote
                            conflicts += change.path
                        }
                    }
                }
                is CodeRename -> {
                    val remote = getCodeRemoteText(context, owner, repo, branch, change.oldPath).orEmpty()
                    val base = nextBases[change.path] ?: nextBases[change.oldPath]
                    val local = change.content ?: base ?: remote
                    val merged = if (base != null) mergeCodeText(base, local, remote)
                    else unknownAncestorConflict(local, remote)
                    rebased += change.copy(content = merged.text, sourceSha = null)
                    nextBases.remove(change.oldPath)
                    nextBases[change.path] = remote
                    if (merged.hasConflicts) conflicts += change.path
                }
            }
        }
        CodeDraftRebaseResult(rebased, nextBases, conflicts.distinct())
    } catch (e: Exception) {
        CodeDraftRebaseResult(changes.toList(), bases, emptyList(), e.message ?: "rebase failed")
    }
}

private fun unknownAncestorConflict(local: String, remote: String): CodeThreeWayMergeResult =
    CodeThreeWayMergeResult(
        text = buildString {
            append("<<<<<<< LOCAL DRAFT\n")
            append(local)
            append("\n||||||| BASE (unavailable)\n")
            append("=======\n")
            append(remote)
            append("\n>>>>>>> REMOTE HEAD")
        },
        hasConflicts = true,
    )
