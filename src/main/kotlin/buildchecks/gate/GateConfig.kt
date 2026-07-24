package buildchecks.gate

data class GateConfig(
    val maxNewFindings: Int = 0,
    val ratchet: Boolean = true,
    val coverageTolerance: Double = 0.1,
    val minCoveragePercent: Double? = null,
    val maxErrors: Int? = null,
    val maxWarnings: Int? = null,
    val maxTestFailures: Int? = null, // null = 0 whenever test reports are present
    val minChangedLineCoverage: Int? = null, // consumed by the changed-line gate (phase 5)
    // Confidence promotions (V4-PLAN §11 item 7, 4.1): off by default. Each turns a confidence
    // signal into an ordinary FAILED gate, so it blocks — the exit code stays a function of gates.
    val failOnSkippedGates: Boolean = false, // any SKIPPED gate becomes a failure
    val requireBaseRef: Boolean = false, // no resolvable git base ref becomes a failure
)
