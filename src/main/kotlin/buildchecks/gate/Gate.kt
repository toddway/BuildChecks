package buildchecks.gate

import buildchecks.model.CoverageData
import buildchecks.model.GateResult
import buildchecks.model.TestResult

data class GateContext(
    val findings: List<FingerprintedFinding>,
    val tests: List<TestResult>,
    val coverage: CoverageData?,
    val baseline: Baseline?,
)

interface Gate {
    fun evaluate(context: GateContext): List<GateResult>
}
