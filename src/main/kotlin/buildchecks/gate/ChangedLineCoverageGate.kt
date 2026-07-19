package buildchecks.gate

import buildchecks.model.ChangedLines
import buildchecks.model.CoverageData
import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Diff-aware coverage gate (V4-PLAN.md §4, rule 1): changed lines must be covered at or above
 * the configured minimum. Severable by design — anything that prevents mapping the diff to
 * coverage data skips with a visible notice, never fails.
 */
class ChangedLineCoverageGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val min = config.minChangedLineCoverage ?: return emptyList()
        return when (val changed = context.changedLines) {
            null -> skip("no base ref (set --base-ref, git.base_ref, or GITHUB_BASE_REF)")
            is ChangedLines.Unavailable -> skip(changed.reason)
            is ChangedLines.Diff -> evaluate(changed, context.coverage, min)
        }
    }

    private fun evaluate(diff: ChangedLines.Diff, coverage: CoverageData?, min: Int): List<GateResult> {
        if (diff.files.values.all { it.isEmpty() }) return skip("no changed lines vs ${diff.baseRef}")
        if (coverage == null) return skip("no coverage data")

        var covered = 0
        var total = 0
        var filesWithoutData = 0
        for ((path, lines) in diff.files) {
            if (lines.isEmpty()) continue
            val hits = hitsByLine(coverage, path)
            if (hits == null) {
                filesWithoutData++
                continue
            }
            // Changed lines absent from the report are non-executable (blanks, comments, imports).
            val executable = lines.filter { it in hits }
            total += executable.size
            covered += executable.count { hits.getValue(it) > 0 }
        }
        if (total == 0) return skip("no executable changed lines vs ${diff.baseRef}")

        val percent = 100.0 * covered / total
        val withoutData = if (filesWithoutData > 0) "; $filesWithoutData changed file(s) without coverage data" else ""
        return listOf(GateResult(
            NAME,
            if (percent >= min) GateStatus.PASSED else GateStatus.FAILED,
            "%.2f%% of %d changed lines vs %s (min %d%%)%s".format(percent, total, diff.baseRef, min, withoutData),
        ))
    }

    /**
     * Git paths are repo-relative; report paths vary by tool (JaCoCo emits package paths,
     * LCOV can be absolute). Segment-aligned suffix matching bridges them, and multiple
     * matching entries (merged reports) combine by max hits per line.
     */
    private fun hitsByLine(coverage: CoverageData, gitPath: String): Map<Int, Int>? {
        val gitSegments = gitPath.split('/')
        val matches = coverage.files.filter { file ->
            val segments = file.path.split('/').filter { it.isNotEmpty() && it != "." }
            if (segments.size <= gitSegments.size) gitSegments.takeLast(segments.size) == segments
            else segments.takeLast(gitSegments.size) == gitSegments
        }
        if (matches.isEmpty()) return null
        val byLine = mutableMapOf<Int, Int>()
        for (file in matches) for (line in file.lines) byLine.merge(line.line, line.hits, ::maxOf)
        return byLine
    }

    private fun skip(reason: String) = listOf(GateResult(NAME, GateStatus.SKIPPED, reason))

    private companion object {
        const val NAME = "changed-line coverage"
    }
}
