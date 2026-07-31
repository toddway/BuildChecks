package buildchecks.gate

import buildchecks.model.ChangedLineMutation
import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Diff-aware mutation gate (V4-PLAN.md §4, 4.1): a configured fraction of the mutants on changed
 * lines must be killed. The mutation twin of [ChangedLineCoverageGate] — coverage checks the lines
 * ran, this checks the tests that ran them actually assert something. The diff↔mutation mapping
 * lives in [ChangedLineMutation] (shared with the report); this gate only applies the threshold.
 * Severable by design — any [ChangedLineMutation.Unavailable] skips with a visible notice, never fails.
 */
class ChangedLineMutationGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val min = config.minChangedLineMutation ?: return emptyList()
        return when (val mutation = context.changedLineMutation) {
            null -> emptyList() // feature off: no changed-line mutation was computed this run
            is ChangedLineMutation.Unavailable ->
                listOf(GateResult(NAME, GateStatus.SKIPPED, mutation.reason))
            is ChangedLineMutation.Measured -> {
                val withoutData = if (mutation.filesWithoutData > 0)
                    "; ${mutation.filesWithoutData} changed file(s) without mutation data" else ""
                listOf(GateResult(
                    NAME,
                    if (mutation.percent >= min) GateStatus.PASSED else GateStatus.FAILED,
                    "%.2f%% of %d changed-line mutants killed vs %s (min %d%%)%s"
                        .format(mutation.percent, mutation.mutantCount, mutation.baseRef, min, withoutData),
                ))
            }
        }
    }

    private companion object {
        const val NAME = "changed-line mutation"
    }
}
