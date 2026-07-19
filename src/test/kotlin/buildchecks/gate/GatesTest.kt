package buildchecks.gate

import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.GateStatus
import buildchecks.model.LineCoverage
import buildchecks.model.Location
import buildchecks.model.Severity
import buildchecks.model.TestResult
import buildchecks.model.TestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GatesTest {

    private fun finding(severity: Severity = Severity.WARNING, fingerprint: String = "aaaa") = FingerprintedFinding(
        Finding("detekt", "MagicNumber", severity, "A magic number", Location("src/A.kt", 7)),
        fingerprint,
    )

    private fun coverage(covered: Int, total: Int) = CoverageData(listOf(
        FileCoverage("src/A.kt", (1..total).map { LineCoverage(it, if (it <= covered) 1 else 0) }),
    ))

    private fun context(
        findings: List<FingerprintedFinding> = emptyList(),
        tests: List<TestResult> = emptyList(),
        coverage: CoverageData? = null,
        baseline: Baseline? = null,
    ) = GateContext(findings, tests, coverage, baseline)

    // -- new findings --

    @Test
    fun `new findings gate skips without a baseline`() {
        val result = NewFindingsGate(GateConfig()).evaluate(context(listOf(finding()))).single()
        assertEquals(GateStatus.SKIPPED, result.status)
    }

    @Test
    fun `baselined findings pass, unknown fingerprints fail`() {
        val gate = NewFindingsGate(GateConfig())
        val baseline = Baseline(setOf("aaaa"), 1, null)

        val pass = gate.evaluate(context(listOf(finding()), baseline = baseline)).single()
        assertEquals(GateStatus.PASSED, pass.status)

        val fail = gate.evaluate(context(listOf(finding(fingerprint = "bbbb")), baseline = baseline)).single()
        assertEquals(GateStatus.FAILED, fail.status)
        assertTrue(fail.detail.contains("MagicNumber at src/A.kt:7"))
    }

    // -- ratchets --

    @Test
    fun `findings ratchet fails when the total grows`() {
        val gate = RatchetGate(GateConfig())
        val baseline = Baseline(setOf("aaaa"), 1, null)

        val pass = gate.evaluate(context(listOf(finding()), baseline = baseline))
        assertEquals(listOf(GateStatus.PASSED), pass.map { it.status })

        val fail = gate.evaluate(context(listOf(finding(), finding(fingerprint = "bbbb")), baseline = baseline))
        assertEquals(listOf(GateStatus.FAILED), fail.map { it.status })
    }

    @Test
    fun `coverage ratchet allows the tolerance and no more`() {
        val gate = RatchetGate(GateConfig(coverageTolerance = 0.1))
        val baseline = Baseline(emptySet(), 0, 80.05)

        val within = gate.evaluate(context(coverage = coverage(80, 100), baseline = baseline))
        assertEquals(GateStatus.PASSED, within.single { it.gate == "coverage must not decrease" }.status)

        val below = gate.evaluate(context(coverage = coverage(79, 100), baseline = baseline))
        assertEquals(GateStatus.FAILED, below.single { it.gate == "coverage must not decrease" }.status)
    }

    @Test
    fun `ratchet gate is disabled by config and skips without a baseline`() {
        assertTrue(RatchetGate(GateConfig(ratchet = false)).evaluate(context()).isEmpty())
        assertEquals(
            GateStatus.SKIPPED,
            RatchetGate(GateConfig()).evaluate(context()).single().status,
        )
    }

    // -- floors --

    @Test
    fun `unconfigured floors yield no results for a project without tests`() {
        assertTrue(FloorsGate(GateConfig()).evaluate(context()).isEmpty())
    }

    @Test
    fun `coverage floor gates the percentage and skips without data`() {
        val gate = FloorsGate(GateConfig(minCoveragePercent = 52.0))
        assertEquals(GateStatus.PASSED, gate.evaluate(context(coverage = coverage(52, 100))).single().status)
        assertEquals(GateStatus.FAILED, gate.evaluate(context(coverage = coverage(51, 100))).single().status)
        assertEquals(GateStatus.SKIPPED, gate.evaluate(context()).single().status)
    }

    @Test
    fun `error and warning floors count by severity`() {
        val gate = FloorsGate(GateConfig(maxErrors = 0, maxWarnings = 1))
        val results = gate.evaluate(context(listOf(
            finding(Severity.ERROR),
            finding(Severity.WARNING, "bbbb"),
            finding(Severity.INFO, "cccc"),
        )))
        assertEquals(GateStatus.FAILED, results.single { it.gate == "errors" }.status)
        assertEquals(GateStatus.PASSED, results.single { it.gate == "warnings" }.status)
    }

    @Test
    fun `test failures floor defaults to zero when tests are present`() {
        val gate = FloorsGate(GateConfig())
        val tests = listOf(
            TestResult("suite", "passes", TestStatus.PASSED),
            TestResult("suite", "fails", TestStatus.FAILED),
        )
        val result = gate.evaluate(context(tests = tests)).single()
        assertEquals(GateStatus.FAILED, result.status)
        assertEquals("1 failed of 2 tests (max 0)", result.detail)
    }
}
