package buildchecks.model

/**
 * Mutation results for only the lines a diff added or changed (V4-PLAN.md §4, 4.1) — the fast,
 * fair PR signal: a full-project mutation run is minutes-to-hours, but the mutants that fall on the
 * lines *this change* touched are few, so a diff-scoped PIT run (or a diff-scoped read of a broader
 * run) stays cheap. Mapped once and consumed by both the changed-line mutation gate (which applies
 * the threshold) and the HTML report (which lists the surviving mutants).
 *
 * Severable by design, exactly like [ChangedLineCoverage]: anything that prevents mapping the diff
 * to mutation data yields [Unavailable] with a human reason, never a measurement.
 */
sealed interface ChangedLineMutation {
    /** No measurement possible; [reason] is the gate's SKIP detail verbatim. */
    data class Unavailable(val reason: String) : ChangedLineMutation

    /**
     * Mutants on changed lines vs [baseRef]. Only produced when at least one mutant fell on a
     * changed line, so [percent] is always well-defined.
     */
    data class Measured(
        val baseRef: String,
        val files: List<ChangedFileMutation>, // only files with >= 1 mutant on a changed line
        val filesWithoutData: Int,            // changed files with no matching PIT entry
    ) : ChangedLineMutation {
        val killedCount: Int get() = files.sumOf { it.killed }
        val mutantCount: Int get() = files.sumOf { it.mutants }
        val percent: Double get() = 100.0 * killedCount / mutantCount
        val survivedFiles: List<ChangedFileMutation> get() = files.filter { it.survivedCount > 0 }
    }
}

data class ChangedFileMutation(
    val path: String,                // git (new-side) path
    val mutants: Int,                // mutants on changed lines
    val killed: Int,                 // detected mutants on changed lines
    val survivedLines: List<Int>,    // sorted, distinct changed lines carrying >= 1 surviving mutant
    val toolReport: String? = null,  // output-dir-relative link to this file's PIT report; attached post-copy
    val reportPath: String? = null,  // repo-relative path of the source PIT report; for the stale flag
) {
    val survivedCount: Int get() = mutants - killed
}

/**
 * Map a diff to mutation data — the mutation twin of [changedLineCoverage]. The `null` case is the
 * gate being unable to resolve a base ref; the [ChangedLines.Unavailable] and empty/no-data cases
 * carry the same skip reasons the changed-line coverage gate has always reported.
 */
fun changedLineMutation(changed: ChangedLines?, mutation: MutationData?): ChangedLineMutation =
    when (changed) {
        null -> ChangedLineMutation.Unavailable(
            "no base ref and no origin/HEAD default branch (set --base-ref or git.base_ref)")
        is ChangedLines.Unavailable -> ChangedLineMutation.Unavailable(changed.reason)
        is ChangedLines.Diff -> measure(changed, mutation)
    }

private fun measure(diff: ChangedLines.Diff, mutation: MutationData?): ChangedLineMutation {
    if (diff.files.values.all { it.isEmpty() })
        return ChangedLineMutation.Unavailable("no changed lines vs ${diff.baseRef}")
    if (mutation == null) return ChangedLineMutation.Unavailable("no mutation data")

    val files = mutableListOf<ChangedFileMutation>()
    var filesWithoutData = 0
    for ((path, lines) in diff.files) {
        if (lines.isEmpty()) continue
        val byLine = mutation.mutantsByLine(path)
        if (byLine == null) {
            filesWithoutData++
            continue
        }
        // Only mutants that fell on a changed line count; changed lines PIT never mutated (blanks,
        // declarations, lines with no mutable behaviour) simply carry none and drop out here.
        val onChanged = lines.flatMap { byLine[it].orEmpty() }
        if (onChanged.isEmpty()) continue
        val killed = onChanged.count { it.detected }
        val survivedLines = onChanged.filter { !it.detected }.map { it.line }.distinct().sorted()
        files += ChangedFileMutation(path, onChanged.size, killed, survivedLines)
    }
    if (files.isEmpty()) {
        val suffix = if (filesWithoutData > 0)
            " ($filesWithoutData changed ${if (filesWithoutData == 1) "file" else "files"} had no matching " +
                "mutation data — e.g. build logic, non-source, or a module PIT didn't cover)"
        else ""
        return ChangedLineMutation.Unavailable("no changed lines carry mutants vs ${diff.baseRef}$suffix")
    }
    return ChangedLineMutation.Measured(diff.baseRef, files, filesWithoutData)
}

/** Mutants keyed by line for one git path, merged across every matching PIT file. */
private fun MutationData.mutantsByLine(gitPath: String): Map<Int, List<Mutation>>? {
    val matches = matching(gitPath)
    if (matches.isEmpty()) return null
    return matches.flatMap { it.mutations }.groupBy { it.line }
}
