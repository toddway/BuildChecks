package buildchecks.gate

data class GateConfig(
    val maxNewFindings: Int = 0,
    val ratchet: Boolean = true,
    val coverageTolerance: Double = 0.1,
    val minCoveragePercent: Double? = null,
    val maxErrors: Int? = null,
    val maxWarnings: Int? = null,
    val maxTestFailures: Int? = null, // null = 0 whenever test reports are present
)
