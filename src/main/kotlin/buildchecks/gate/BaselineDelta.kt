package buildchecks.gate

/**
 * How the on-disk baseline differs from the baseline at the git base ref, in the *loosening*
 * direction only (V4-PLAN.md §11 item 7, 4.2): findings newly accepted, a lowered coverage floor,
 * and dropped expected-report manifest entries — each a way a check that would have failed at the
 * base ref now passes. Tightening (findings removed, coverage raised) is not a confidence concern,
 * so it is deliberately not reported here.
 */
data class BaselineDelta(
    val findingsAccepted: Int,
    val coverageLowered: Double?, // magnitude of the drop in the recorded coverage floor, or null
    val reportsDropped: List<String>, // manifest entries present at the base ref but not on disk
) {
    val loosened: Boolean get() = findingsAccepted > 0 || coverageLowered != null || reportsDropped.isNotEmpty()
}

/** [base] = the baseline at the git base ref; [current] = the on-disk baseline this run gated with. */
fun baselineDelta(base: Baseline, current: Baseline): BaselineDelta {
    val accepted = (current.fingerprints - base.fingerprints).size
    val coverageLowered = base.coveragePercent
        ?.takeIf { current.coveragePercent != null && current.coveragePercent < it - 1e-9 }
        ?.let { it - current.coveragePercent!! }
    // Both manifests must exist to compare; a pre-v2 baseline on either side leaves it uncomparable.
    val dropped = if (base.manifest != null && current.manifest != null) {
        (base.manifest - current.manifest).sorted().map { "${it.kind} in ${it.origin}" }
    } else {
        emptyList()
    }
    return BaselineDelta(accepted, coverageLowered, dropped)
}
