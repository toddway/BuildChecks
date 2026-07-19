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
            ?: return listOf(GateResult("findings ratchet", GateStatus.SKIPPED, "no baseline file"))

        val results = mutableListOf<GateResult>()
        val total = context.findings.size
        results += GateResult(
            "findings ratchet",
            if (total <= baseline.findingCount) GateStatus.PASSED else GateStatus.FAILED,
            "$total findings (baseline ${baseline.findingCount})",
        )

        val percent = context.coverage?.linePercent
        val baselinePercent = baseline.coveragePercent
        if (percent != null && baselinePercent != null) {
            val floor = baselinePercent - config.coverageTolerance
            results += GateResult(
                "coverage ratchet",
                if (percent >= floor) GateStatus.PASSED else GateStatus.FAILED,
                "%.2f%% (baseline %.2f%%, tolerance %.1f)".format(percent, baselinePercent, config.coverageTolerance),
            )
        }
        return results
    }
}
