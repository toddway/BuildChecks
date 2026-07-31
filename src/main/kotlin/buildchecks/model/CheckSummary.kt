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
    // Coverage of the lines this diff changed, when a base ref resolved; drives the
    // "changed lines not covered" report section. null when the gate is off.
    val changedLineCoverage: ChangedLineCoverage? = null,
    // Mutation results for the lines this diff changed; drives the "surviving mutants on changed
    // lines" report section and the contradiction. null when the changed-line mutation gate is off.
    val changedLineMutation: ChangedLineMutation? = null,
    // The covered-but-not-verified finding (4.1): high changed-line coverage, low changed-line
    // mutation. null unless both were measured and the gap is unambiguous. Informational.
    val contradiction: Contradiction? = null,
    // How much the verdict is worth — the trust axis (§11 item 7), orthogonal to `passed` and never
    // affecting the exit code. Defaults to full confidence so it stays additive for callers/tests.
    val confidence: Confidence = Confidence(emptyList()),
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
    // Repo-relative path of the ingested report this finding came from, so the renderer can flag
    // findings whose source report is a stale age-outlier (see Freshness.outlier). null if unknown.
    val reportPath: String? = null,
)
