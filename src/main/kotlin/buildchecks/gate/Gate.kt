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
)

interface Gate {
    fun evaluate(context: GateContext): List<GateResult>
}
