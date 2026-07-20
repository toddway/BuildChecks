package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Origin-presence gate (V4-PLAN.md §4 rule 5, §5.5): every (origin, kind) in the baseline
 * manifest must have a matching report ingested this run. Catches a check silently disabled
 * or a source that stopped emitting its report — a regression that otherwise reads as fewer
 * findings. Severable: skips with a notice when there is no baseline, or the baseline predates
 * the manifest (pre-v2); re-baseline to a v2 snapshot to enable it. Intentional removal is
 * accepted the same way new findings are — a visible re-baseline in the same PR.
 */
class MissingReportGate : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val manifest = context.baseline?.manifest ?: return skip(
            if (context.baseline == null) "no baseline"
            else "baseline predates the origin manifest (re-baseline to enable)",
        )
        val missing = manifest.filterNot { it in context.presentOrigins }.sorted()
        return if (missing.isEmpty()) {
            listOf(GateResult(NAME, GateStatus.PASSED, "${manifest.size} expected report(s) present"))
        } else {
            val named = missing.joinToString(", ") { "${it.kind} in ${it.origin}" }
            listOf(GateResult(NAME, GateStatus.FAILED, "${missing.size} expected report(s) missing: $named"))
        }
    }

    private fun skip(reason: String) = listOf(GateResult(NAME, GateStatus.SKIPPED, reason))

    private companion object {
        const val NAME = "expected reports"
    }
}
