package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus

/**
 * Promotes confidence signals to hard gates (V4-PLAN.md §11 item 7). These are meta — they read the
 * outcome of the whole run (other gates' statuses, whether a base ref resolved) rather than a single
 * [GateContext], so they can't be ordinary [Gate]s; they run once after all gates evaluate and
 * append ordinary [GateResult]s that flow through `passed` like any other. Both are off by default,
 * so the default exit code is unchanged and confidence stays purely informational.
 */
fun promotedGates(
    config: GateConfig,
    results: List<GateResult>,
    baseRefResolved: Boolean,
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

    return promoted
}
