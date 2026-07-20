package buildchecks.gate

import buildchecks.model.ChangedLines
import buildchecks.model.CoverageData
import buildchecks.model.GateResult
import buildchecks.model.TestResult

data class GateContext(
    val findings: List<FingerprintedFinding>,
    val tests: List<TestResult>,
    val coverage: CoverageData?,
    val baseline: Baseline?,
    val changedLines: ChangedLines?, // null = no base ref was resolvable (or the gate is off)
    val presentOrigins: Set<OriginKind> = emptySet(), // (origin, kind) derived from this run's reports
)

interface Gate {
    fun evaluate(context: GateContext): List<GateResult>
}
