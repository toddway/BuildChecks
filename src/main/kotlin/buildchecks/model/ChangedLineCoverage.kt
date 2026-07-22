package buildchecks.model

/**
 * Coverage of only the lines a diff added or changed, mapped once and consumed by both the
 * changed-line coverage gate (which applies the threshold) and the HTML report (which lists the
 * uncovered lines). Computed by [changedLineCoverage]; severable by design — anything that
 * prevents mapping the diff to coverage data yields [Unavailable] with a human reason, never a
 * measurement.
 */
sealed interface ChangedLineCoverage {
    /** No measurement possible; [reason] is the gate's SKIP detail verbatim. */
    data class Unavailable(val reason: String) : ChangedLineCoverage

    /**
     * Executable changed lines vs [baseRef]. Only produced when at least one executable changed
     * line was measured, so [percent] is always well-defined.
     */
    data class Measured(
        val baseRef: String,
        val files: List<ChangedFileCoverage>, // only files with >= 1 executable changed line
        val filesWithoutData: Int,            // changed files with no matching coverage entry
    ) : ChangedLineCoverage {
        val coveredCount: Int get() = files.sumOf { it.covered.size }
        val executableCount: Int get() = files.sumOf { it.executable }
        val percent: Double get() = 100.0 * coveredCount / executableCount
        val uncoveredFiles: List<ChangedFileCoverage> get() = files.filter { it.uncovered.isNotEmpty() }
    }
}

data class ChangedFileCoverage(
    val path: String,               // git (new-side) path
    val covered: List<Int>,         // sorted executable changed lines that were hit
    val uncovered: List<Int>,       // sorted executable changed lines not hit
    val toolReport: String? = null, // output-dir-relative link to this file's coverage report; attached post-copy
    val reportPath: String? = null, // repo-relative path of the source coverage report; for the stale flag
) {
    val executable: Int get() = covered.size + uncovered.size
}

/**
 * Map a diff to coverage data (V4-PLAN.md §4, rule 1). The `null` case is the gate being unable
 * to resolve a base ref; the [ChangedLines.Unavailable] and empty/no-data cases carry the same
 * skip reasons the gate has always reported.
 */
fun changedLineCoverage(changed: ChangedLines?, coverage: CoverageData?): ChangedLineCoverage =
    when (changed) {
        null -> ChangedLineCoverage.Unavailable(
            "no base ref (set --base-ref, git.base_ref, or GITHUB_BASE_REF)")
        is ChangedLines.Unavailable -> ChangedLineCoverage.Unavailable(changed.reason)
        is ChangedLines.Diff -> measure(changed, coverage)
    }

private fun measure(diff: ChangedLines.Diff, coverage: CoverageData?): ChangedLineCoverage {
    if (diff.files.values.all { it.isEmpty() })
        return ChangedLineCoverage.Unavailable("no changed lines vs ${diff.baseRef}")
    if (coverage == null) return ChangedLineCoverage.Unavailable("no coverage data")

    val files = mutableListOf<ChangedFileCoverage>()
    var filesWithoutData = 0
    for ((path, lines) in diff.files) {
        if (lines.isEmpty()) continue
        val hits = coverage.hitsByLine(path)
        if (hits == null) {
            filesWithoutData++
            continue
        }
        // Changed lines absent from the report are non-executable (blanks, comments, imports).
        val executable = lines.filter { it in hits }.sorted()
        if (executable.isEmpty()) continue
        val (covered, uncovered) = executable.partition { hits.getValue(it) > 0 }
        files += ChangedFileCoverage(path, covered, uncovered)
    }
    if (files.isEmpty())
        return ChangedLineCoverage.Unavailable("no executable changed lines vs ${diff.baseRef}")
    return ChangedLineCoverage.Measured(diff.baseRef, files, filesWithoutData)
}

/**
 * Git paths are repo-relative; report paths vary by tool (JaCoCo emits package paths, LCOV can be
 * absolute). Segment-aligned suffix matching bridges them; used both to find a file's coverage
 * entries (for links) and, folded into [hitsByLine], to combine merged reports by max hits/line.
 */
fun CoverageData.matching(gitPath: String): List<FileCoverage> {
    val gitSegments = gitPath.split('/')
    return files.filter { file ->
        val segments = file.path.split('/').filter { it.isNotEmpty() && it != "." }
        if (segments.size <= gitSegments.size) gitSegments.takeLast(segments.size) == segments
        else segments.takeLast(gitSegments.size) == gitSegments
    }
}

private fun CoverageData.hitsByLine(gitPath: String): Map<Int, Int>? {
    val matches = matching(gitPath)
    if (matches.isEmpty()) return null
    val byLine = mutableMapOf<Int, Int>()
    for (file in matches) for (line in file.lines) byLine.merge(line.line, line.hits, ::maxOf)
    return byLine
}
