package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * One row for the overall-coverage metric, gated against the effective limit: the higher
 * of the baseline's coverage minus the tolerance (the ratchet, V4-PLAN.md §4 — never worse
 * than the last accepted state) and the configured minimum (the worst ever accepted).
 * The detail names which of the two set the limit.
 */
class CoverageGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val baselineFloor = context.baseline?.coveragePercent
            ?.takeIf { config.ratchet }
            ?.let { it - config.coverageTolerance }
        val configuredMin = config.minCoveragePercent
        val limit = listOfNotNull(baselineFloor, configuredMin).maxOrNull()
            ?: return emptyList() // nothing to gate against

        val percent = context.coverage?.linePercent
            ?: return listOf(GateResult(NAME, GateStatus.SKIPPED, "no coverage data"))

        val source = if (limit == baselineFloor) "from baseline" else "configured"
        return listOf(GateResult(
            NAME,
            if (percent >= limit) GateStatus.PASSED else GateStatus.FAILED,
            "%.2f%% (min %.2f%%, %s)".format(percent, limit, source),
        ))
    }

    private companion object {
        const val NAME = "coverage"
    }
}
