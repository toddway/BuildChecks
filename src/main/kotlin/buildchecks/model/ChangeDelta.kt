package buildchecks.model

/**
 * How this change moved the rulers, relative to the git base ref (V4-PLAN.md §11 item 7, 4.2).
 * Plain facts only — computed at the cli/git boundary (a `git show <ref>:file` of the baseline and
 * config, plus the changed-file set mapped to origins) and handed to [confidence] to phrase and to
 * `promotedGates` to optionally enforce. Present only when a base ref resolved; a null [ChangeDelta]
 * means there was no "before" to compare against, so no delta signal contributes.
 */
data class ChangeDelta(
    /** Origins (module/source groups) this change touched, from the diffed file set. */
    val touchedOrigins: Set<String> = emptySet(),
    /** Of [touchedOrigins], those that produced a fresh (non age-outlier) report this run. */
    val freshOrigins: Set<String> = emptySet(),
    /** Findings newly accepted into the on-disk baseline vs the base ref (would have failed before). */
    val baselineFindingsAccepted: Int = 0,
    /** How far the baseline's recorded coverage floor dropped vs the base ref, or null if it didn't. */
    val baselineCoverageLowered: Double? = null,
    /** Expected-report manifest entries dropped from the baseline vs the base ref (display labels). */
    val baselineReportsDropped: List<String> = emptyList(),
    /** Gate settings loosened in buildchecks.toml vs the base ref (display labels). */
    val configLoosened: List<String> = emptyList(),
) {
    /** Touched origins with no fresh report this run — the change may not have been re-measured. */
    val staleChangedOrigins: Set<String> get() = touchedOrigins - freshOrigins

    val baselineLoosened: Boolean
        get() = baselineFindingsAccepted > 0 || baselineCoverageLowered != null || baselineReportsDropped.isNotEmpty()
}
