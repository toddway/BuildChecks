package buildchecks.model

/** Everything one check run produced; the single input every renderer consumes. */
data class CheckSummary(
    val gates: List<GateResult>,
    val findings: List<ReportedFinding>,
    val tests: List<TestResult>,
    val coverage: CoverageData?,
    val files: List<IngestedFile>,
    val notUnderstood: List<String>,
    val freshness: Freshness?,
) {
    val passed: Boolean get() = gates.none { it.status == GateStatus.FAILED }
}

data class ReportedFinding(
    val finding: Finding,
    val fingerprint: String,
    val isNew: Boolean,
)
