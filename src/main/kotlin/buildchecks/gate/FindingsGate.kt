package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * One row for the findings metric, checked two ways against the baseline: no new
 * fingerprints beyond the allowed max, and (the ratchet, V4-PLAN.md §4) the total may not
 * rise above the baseline's total — the attribution-free backstop for any fingerprint miss.
 */
class FindingsGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val baseline = context.baseline
            ?: return listOf(GateResult(NAME, GateStatus.SKIPPED, "no baseline file — run `buildchecks baseline` to create one"))

        val new = context.findings.filter { it.fingerprint !in baseline.fingerprints }
        val newOk = new.size <= config.maxNewFindings
        val totalOk = !config.ratchet || context.findings.size <= baseline.findingCount

        var detail = "${new.size} new (max ${config.maxNewFindings})"
        if (config.ratchet) detail += ", ${context.findings.size} total (baseline max ${baseline.findingCount})"
        if (!newOk) {
            val worst = new.first().finding
            detail += "; e.g. ${worst.ruleId} at ${worst.location?.path ?: "-"}:${worst.location?.line ?: 0}"
        }
        val status = if (newOk && totalOk) GateStatus.PASSED else GateStatus.FAILED
        return listOf(GateResult(NAME, status, detail))
    }

    private companion object {
        const val NAME = "findings"
    }
}
