package gs.git.vps.data.github

internal data class CodeThreeWayMergeResult(
    val text: String,
    val hasConflicts: Boolean,
)

private data class LineChange(
    val start: Int,
    val end: Int,
    val replacement: List<String>,
)

/**
 * Conservative line-based diff3. Independent single-region changes are combined automatically;
 * overlapping edits are kept as LOCAL/BASE/REMOTE markers so no side is silently discarded.
 */
internal fun mergeCodeText(base: String, local: String, remote: String): CodeThreeWayMergeResult {
    if (local == remote) return CodeThreeWayMergeResult(local, false)
    if (remote == base) return CodeThreeWayMergeResult(local, false)
    if (local == base) return CodeThreeWayMergeResult(remote, false)

    val baseLines = base.split('\n')
    val localChange = singleLineChange(baseLines, local.split('\n'))
    val remoteChange = singleLineChange(baseLines, remote.split('\n'))
    if (localChange == remoteChange) return CodeThreeWayMergeResult(local, false)

    if (!changesOverlap(localChange, remoteChange)) {
        val merged = baseLines.toMutableList()
        listOf(localChange, remoteChange)
            .sortedByDescending { it.start }
            .forEach { change ->
                for (index in change.end - 1 downTo change.start) merged.removeAt(index)
                merged.addAll(change.start, change.replacement)
            }
        return CodeThreeWayMergeResult(merged.joinToString("\n"), false)
    }

    val unionStart = minOf(localChange.start, remoteChange.start)
    val unionEnd = maxOf(localChange.end, remoteChange.end)
    val localPart = applyChangeToRange(baseLines, localChange, unionStart, unionEnd)
    val remotePart = applyChangeToRange(baseLines, remoteChange, unionStart, unionEnd)
    if (localPart == remotePart) {
        val merged = baseLines.toMutableList().apply {
            for (index in unionEnd - 1 downTo unionStart) removeAt(index)
            addAll(unionStart, localPart)
        }
        return CodeThreeWayMergeResult(merged.joinToString("\n"), false)
    }

    val conflict = buildList {
        addAll(baseLines.subList(0, unionStart))
        add("<<<<<<< LOCAL DRAFT")
        addAll(localPart)
        add("||||||| BASE")
        addAll(baseLines.subList(unionStart, unionEnd))
        add("=======")
        addAll(remotePart)
        add(">>>>>>> REMOTE HEAD")
        addAll(baseLines.subList(unionEnd, baseLines.size))
    }
    return CodeThreeWayMergeResult(conflict.joinToString("\n"), true)
}

private fun singleLineChange(base: List<String>, changed: List<String>): LineChange {
    var prefix = 0
    while (prefix < base.size && prefix < changed.size && base[prefix] == changed[prefix]) prefix++
    var suffix = 0
    while (
        suffix < base.size - prefix &&
        suffix < changed.size - prefix &&
        base[base.lastIndex - suffix] == changed[changed.lastIndex - suffix]
    ) suffix++
    return LineChange(
        start = prefix,
        end = base.size - suffix,
        replacement = changed.subList(prefix, changed.size - suffix),
    )
}

private fun changesOverlap(first: LineChange, second: LineChange): Boolean {
    val firstInsert = first.start == first.end
    val secondInsert = second.start == second.end
    return when {
        firstInsert && secondInsert -> first.start == second.start
        firstInsert -> first.start >= second.start && first.start < second.end
        secondInsert -> second.start >= first.start && second.start < first.end
        else -> maxOf(first.start, second.start) < minOf(first.end, second.end)
    }
}

private fun applyChangeToRange(
    base: List<String>,
    change: LineChange,
    rangeStart: Int,
    rangeEnd: Int,
): List<String> = buildList {
    addAll(base.subList(rangeStart, change.start))
    addAll(change.replacement)
    addAll(base.subList(change.end, rangeEnd))
}
