package buildchecks.model

/**
 * The signal-hierarchy contradiction (V4-PLAN.md §11 4.1, render item 4): on the lines this change
 * touched, coverage is high (tests execute them) yet the mutation kill rate is much lower (those
 * tests would not notice if the lines' behaviour changed). Covered-but-not-verified is the exact
 * failure BuildChecks exists to catch — a change whose tests raise the coverage number without
 * actually checking the new behaviour — so it is surfaced as one *named* finding rather than two
 * adjacent percentages a reader has to correlate for themselves.
 *
 * This is the first real exercise of the signal hierarchy the tool has asserted all along: coverage
 * and mutation are not two independent metrics but a claim and its audit, and here the audit
 * disagrees with the claim. It is informational — it never sets the exit code (the changed-line
 * mutation gate does the blocking) — but it is rendered prominently wherever a human looks.
 */
data class Contradiction(
    val coveragePercent: Double, // changed-line coverage
    val mutationPercent: Double, // changed-line mutation kill rate
) {
    /** How far the audit fell short of the claim — the size of the "covered but not verified" gap. */
    val gap: Double get() = coveragePercent - mutationPercent
}

/**
 * Fire only when both signals were measured on the same diff and the pattern is unambiguous: the
 * changed lines are well covered ([COVERED_THRESHOLD]) yet the mutation kill rate trails coverage by
 * a wide margin ([VERIFICATION_GAP]). Both guards matter — a low-coverage change is honestly
 * incomplete, not contradictory, and a small gap is ordinary noise, not a finding worth a headline.
 */
fun contradiction(coverage: ChangedLineCoverage?, mutation: ChangedLineMutation?): Contradiction? {
    val cov = coverage as? ChangedLineCoverage.Measured ?: return null
    val mut = mutation as? ChangedLineMutation.Measured ?: return null
    val finding = Contradiction(cov.percent, mut.percent)
    return finding.takeIf { cov.percent >= COVERED_THRESHOLD && it.gap >= VERIFICATION_GAP }
}

// Coverage this high means "the tests plainly run these lines", so a low kill rate can't be excused
// as untested code — it's untested behaviour. A 20-point gap is well outside re-measurement noise.
private const val COVERED_THRESHOLD = 80.0
private const val VERIFICATION_GAP = 20.0
