package buildchecks.gate

import buildchecks.model.ChangedLineCoverage
import buildchecks.model.ChangedLineMutation
import buildchecks.model.CoverageData
import buildchecks.model.GateResult
import buildchecks.model.TestResult

data class GateContext(
    val findings: List<FingerprintedFinding>,
    val tests: List<TestResult>,
    val coverage: CoverageData?,
    val baseline: Baseline?,
    val changedLineCoverage: ChangedLineCoverage?, // null = gate off (no changed-line coverage computed)
    val presentOrigins: Set<OriginKind> = emptySet(), // (origin, kind) derived from this run's reports
    val changedLineMutation: ChangedLineMutation? = null, // null = gate off (no changed-line mutation computed)
)

interface Gate {
    fun evaluate(context: GateContext): List<GateResult>
}
