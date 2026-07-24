package buildchecks.cli

/**
 * Gate settings loosened in [current] vs [base] — the config-diff confidence signal (V4-PLAN.md §11
 * item 7, 4.2). "Loosened" = moved in the permissive direction (a floor lowered, a cap raised, a
 * gate or promotion turned off): the change that lets a check pass now that would have failed at the
 * base ref. Returns display labels like "min_coverage_percent 80.0 → 70.0"; empty when nothing was
 * loosened. Compares only [GateConfig][buildchecks.gate.GateConfig] settings — a threshold moved or
 * a gate disabled — not report-discovery or freshness knobs, which don't gate.
 */
fun configLoosened(base: Config, current: Config): List<String> {
    val b = base.gates
    val c = current.gates
    return buildList {
        if (c.maxNewFindings > b.maxNewFindings) add("max_new_findings ${b.maxNewFindings} → ${c.maxNewFindings}")
        if (b.ratchet && !c.ratchet) add("ratchet on → off")
        if (c.coverageTolerance > b.coverageTolerance) add("coverage_tolerance ${b.coverageTolerance} → ${c.coverageTolerance}")
        loweredFloor("min_coverage_percent", b.minCoveragePercent, c.minCoveragePercent)?.let { add(it) }
        loweredFloorInt("min_changed_line_coverage", b.minChangedLineCoverage, c.minChangedLineCoverage)?.let { add(it) }
        raisedCap("max_errors", b.maxErrors, c.maxErrors)?.let { add(it) }
        raisedCap("max_warnings", b.maxWarnings, c.maxWarnings)?.let { add(it) }
        // max_test_failures has no unbounded state: null means an effective 0 (fail on any failure
        // when tests are present), so compare the effective values rather than treating null as "off".
        val bTf = b.maxTestFailures ?: 0
        val cTf = c.maxTestFailures ?: 0
        if (cTf > bTf) add("max_test_failures $bTf → $cTf")
        if (b.failOnSkippedGates && !c.failOnSkippedGates) add("fail_on_skipped_gates on → off")
        if (b.requireBaseRef && !c.requireBaseRef) add("require_base_ref on → off")
        if (b.failOnBaselineLoosened && !c.failOnBaselineLoosened) add("fail_on_baseline_loosened on → off")
        if (b.requireChangedOriginsFresh && !c.requireChangedOriginsFresh) add("require_changed_origins_fresh on → off")
    }
}

// A minimum floor is looser when it drops or is removed entirely (null = no floor). No signal when
// the base had no floor — you can't loosen what wasn't there.
private fun loweredFloor(key: String, base: Double?, current: Double?): String? = when {
    base == null -> null
    current == null -> "$key $base → off"
    current < base -> "$key $base → $current"
    else -> null
}

private fun loweredFloorInt(key: String, base: Int?, current: Int?): String? = when {
    base == null -> null
    current == null -> "$key $base → off"
    current < base -> "$key $base → $current"
    else -> null
}

// A maximum cap is looser when it rises or is removed entirely (null = unbounded).
private fun raisedCap(key: String, base: Int?, current: Int?): String? = when {
    base == null -> null
    current == null -> "$key $base → off"
    current > base -> "$key $base → $current"
    else -> null
}
