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
    // Whether a baseline snapshot was read this run; drives whether `isNew` is meaningful,
    // so renderers can default to showing only new findings when there is a baseline.
    val hasBaseline: Boolean = false,
) {
    val passed: Boolean get() = gates.none { it.status == GateStatus.FAILED }
}

data class ReportedFinding(
    val finding: Finding,
    val fingerprint: String,
    val isNew: Boolean,
    // Output-dir-relative link to the HTML report of the tool that produced this finding
    // (the human-readable sibling of the parsed SARIF/XML), or null if none was copied.
    val toolReport: String? = null,
)
