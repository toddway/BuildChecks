package buildchecks.gate

import buildchecks.model.ChangeDelta
import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Promotes confidence signals to hard gates (V4-PLAN.md §11 item 7). These are meta — they read the
 * outcome of the whole run (other gates' statuses, whether a base ref resolved, the base-ref delta)
 * rather than a single [GateContext], so they can't be ordinary [Gate]s; they run once after all
 * gates evaluate and append ordinary [GateResult]s that flow through `passed` like any other. All
 * are off by default, so the default exit code is unchanged and confidence stays purely informational.
 */
fun promotedGates(
    config: GateConfig,
    results: List<GateResult>,
    baseRefResolved: Boolean,
    delta: ChangeDelta? = null,
): List<GateResult> {
    val promoted = mutableListOf<GateResult>()

    if (config.failOnSkippedGates) {
        val skipped = results.filter { it.status == GateStatus.SKIPPED }.map { it.gate }
        promoted += if (skipped.isEmpty()) {
            GateResult("no skipped gates", GateStatus.PASSED, "every configured gate ran")
        } else {
            GateResult(
                "no skipped gates", GateStatus.FAILED,
                "${skipped.size} gate(s) skipped: ${skipped.joinToString(", ")}",
            )
        }
    }

    if (config.requireBaseRef) {
        promoted += if (baseRefResolved) {
            GateResult("base ref required", GateStatus.PASSED, "a git base ref resolved for delta analysis")
        } else {
            GateResult(
                "base ref required", GateStatus.FAILED,
                "no git base ref resolved — set --base-ref, git.base_ref, or run on a PR/merge build",
            )
        }
    }

    if (config.failOnBaselineLoosened) {
        promoted += when {
            delta == null ->
                GateResult("baseline not loosened", GateStatus.PASSED, "no base ref to compare the baseline against")
            delta.baselineLoosened -> {
                val bits = buildList {
                    if (delta.baselineFindingsAccepted > 0) add("${delta.baselineFindingsAccepted} finding(s) accepted")
                    if (delta.baselineCoverageLowered != null) add("coverage floor lowered")
                    if (delta.baselineReportsDropped.isNotEmpty()) add("${delta.baselineReportsDropped.size} expected report(s) dropped")
                }
                GateResult("baseline not loosened", GateStatus.FAILED, "loosened vs the base ref: ${bits.joinToString(", ")}")
            }
            else -> GateResult("baseline not loosened", GateStatus.PASSED, "the baseline was not loosened vs the base ref")
        }
    }

    if (config.requireChangedOriginsFresh) {
        val stale = delta?.staleChangedOrigins ?: emptySet()
        promoted += when {
            delta == null ->
                GateResult("changed origins measured", GateStatus.PASSED, "no base ref to identify changed origins")
            stale.isEmpty() ->
                GateResult("changed origins measured", GateStatus.PASSED, "every measured changed origin produced a fresh report")
            else -> GateResult(
                "changed origins measured", GateStatus.FAILED,
                "${stale.size} changed origin(s) produced only a stale report: ${stale.sorted().joinToString(", ")}",
            )
        }
    }

    return promoted
}
