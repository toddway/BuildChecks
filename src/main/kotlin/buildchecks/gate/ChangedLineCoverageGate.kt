package buildchecks.gate

import buildchecks.model.ChangedLineCoverage
import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Diff-aware coverage gate (V4-PLAN.md §4, rule 1): changed lines must be covered at or above
 * the configured minimum. The diff↔coverage mapping lives in [ChangedLineCoverage] (shared with
 * the report); this gate only applies the threshold. Severable by design — any [ChangedLineCoverage.Unavailable]
 * skips with a visible notice, never fails.
 */
class ChangedLineCoverageGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val min = config.minChangedLineCoverage ?: return emptyList()
        return when (val coverage = context.changedLineCoverage) {
            null -> emptyList() // feature off: no changed-line coverage was computed this run
            is ChangedLineCoverage.Unavailable ->
                listOf(GateResult(NAME, GateStatus.SKIPPED, coverage.reason))
            is ChangedLineCoverage.Measured -> {
                val withoutData = if (coverage.filesWithoutData > 0)
                    "; ${coverage.filesWithoutData} changed file(s) without coverage data" else ""
                listOf(GateResult(
                    NAME,
                    if (coverage.percent >= min) GateStatus.PASSED else GateStatus.FAILED,
                    "%.2f%% of %d changed lines vs %s (min %d%%)%s"
                        .format(coverage.percent, coverage.executableCount, coverage.baseRef, min, withoutData),
                ))
            }
        }
    }

    private companion object {
        const val NAME = "changed-line coverage"
    }
}
