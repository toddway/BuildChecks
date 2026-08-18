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
 *
 * Scoped to *live* origins: an entry is only checked when its origin produced some other report
 * this run. An origin that emitted nothing at all wasn't measured — it may simply not have been
 * built, which is routine for an incremental build or a narrower task set than the one that
 * captured the baseline. Inferring a dropped check from that is the mistake
 * [buildchecks.model.ChangeDelta] warns against ("absence is not evidence a check fell"), and it
 * made a baseline captured from one build scope unusable for gating another — a local snapshot
 * would record, say, `junit in common/glassbox` from the one module whose tests the developer ran,
 * then fail every CI build that didn't run them. What survives scoping is the signal actually worth
 * gating: a *kind* vanishing while its origin is still being measured.
 */
class MissingReportGate : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val manifest = context.baseline?.manifest ?: return skip(
            if (context.baseline == null) "no baseline"
            else "baseline predates the origin manifest (re-baseline to enable)",
        )
        val liveOrigins = context.presentOrigins.mapTo(mutableSetOf()) { it.origin }
        val (checked, unbuilt) = manifest.partition { it.origin in liveOrigins }
        val missing = checked.filterNot { it in context.presentOrigins }.sorted()
        // Say what was skipped rather than reporting blanket coverage over origins nobody measured.
        val scope = if (unbuilt.isEmpty()) "" else
            " (${unbuilt.size} not built this run: ${unbuilt.map { it.origin }.distinct().sorted().joinToString(", ")})"
        return if (missing.isEmpty()) {
            listOf(GateResult(NAME, GateStatus.PASSED, "${checked.size} expected report(s) present$scope"))
        } else {
            val named = missing.joinToString(", ") { "${it.kind} in ${it.origin}" }
            listOf(GateResult(NAME, GateStatus.FAILED, "${missing.size} expected report(s) missing: $named$scope"))
        }
    }

    private fun skip(reason: String) = listOf(GateResult(NAME, GateStatus.SKIPPED, reason))

    private companion object {
        const val NAME = "expected reports"
    }
}
