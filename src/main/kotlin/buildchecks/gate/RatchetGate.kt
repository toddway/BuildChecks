package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Attribution-free backstop (V4-PLAN.md §4): totals must not regress vs the baseline header,
 * catching deleted tests and any fingerprint miss.
 */
class RatchetGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        if (!config.ratchet) return emptyList()
        val baseline = context.baseline
            ?: return listOf(GateResult(FINDINGS, GateStatus.SKIPPED, "no baseline file"))

        val results = mutableListOf<GateResult>()
        val total = context.findings.size
        results += GateResult(
            FINDINGS,
            if (total <= baseline.findingCount) GateStatus.PASSED else GateStatus.FAILED,
            "$total (baseline max ${baseline.findingCount})",
        )

        val percent = context.coverage?.linePercent
        val baselinePercent = baseline.coveragePercent
        if (percent != null && baselinePercent != null) {
            val floor = baselinePercent - config.coverageTolerance
            results += GateResult(
                COVERAGE,
                if (percent >= floor) GateStatus.PASSED else GateStatus.FAILED,
                "%.2f%% (baseline min %.2f%%)".format(percent, floor),
            )
        }
        return results
    }

    private companion object {
        // "Ratchet" internally (V4-PLAN.md §4); names are plain metrics, the rule lives in
        // the report tooltip, and a "baseline"-prefixed limit marks where it came from.
        const val FINDINGS = "total findings"
        const val COVERAGE = "coverage"
    }
}
