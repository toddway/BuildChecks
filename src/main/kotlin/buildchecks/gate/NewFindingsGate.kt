package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

class NewFindingsGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val baseline = context.baseline
            ?: return listOf(GateResult(NAME, GateStatus.SKIPPED, "no baseline file — run `buildchecks baseline` to create one"))
        val new = context.findings.filter { it.fingerprint !in baseline.fingerprints }
        val status = if (new.size <= config.maxNewFindings) GateStatus.PASSED else GateStatus.FAILED
        val worst = new.firstOrNull()?.finding?.let { finding ->
            "; e.g. ${finding.ruleId} at ${finding.location?.path ?: "-"}:${finding.location?.line ?: 0}"
        }
        val detail = "${new.size} new (max ${config.maxNewFindings})" +
            if (status == GateStatus.FAILED && worst != null) worst else ""
        return listOf(GateResult(NAME, status, detail))
    }

    private companion object {
        const val NAME = "new findings"
    }
}
