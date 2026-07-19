package buildchecks.gate

import buildchecks.model.ChangedLines
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
        changedLines: ChangedLines? = null,
    ) = GateContext(findings, tests, coverage, baseline, changedLines)

    // -- changed-line coverage --

    private val changedLineGate = ChangedLineCoverageGate(GateConfig(minChangedLineCoverage = 80))

    // JaCoCo-style package path; git sees the full repo-relative path.
    private fun perLineCoverage(vararg lines: Pair<Int, Int>) = CoverageData(listOf(
        FileCoverage("com/example/Greeter.kt", lines.map { LineCoverage(it.first, it.second) }),
    ))

    private fun diff(vararg lines: Int) = ChangedLines.Diff(
        "origin/dev",
        mapOf("src/main/kotlin/com/example/Greeter.kt" to lines.toSet()),
    )

    @Test
    fun `changed-line gate is off without a configured minimum`() {
        assertTrue(ChangedLineCoverageGate(GateConfig()).evaluate(context(changedLines = diff(1))).isEmpty())
    }

    @Test
    fun `changed-line gate skips with a hint when no base ref resolved`() {
        val result = changedLineGate.evaluate(context(coverage = perLineCoverage(1 to 1))).single()
        assertEquals(GateStatus.SKIPPED, result.status)
        assertTrue(result.detail.contains("GITHUB_BASE_REF"), result.detail)
    }

    @Test
    fun `changed-line gate surfaces why git was unavailable`() {
        val unavailable = ChangedLines.Unavailable("git not available: No such file or directory")
        val result = changedLineGate.evaluate(context(changedLines = unavailable)).single()
        assertEquals(GateStatus.SKIPPED, result.status)
        assertEquals("git not available: No such file or directory", result.detail)
    }

    @Test
    fun `changed lines gate on per-line hits across path conventions`() {
        // 3 of 4 changed executable lines covered = 75% < 80
        val coverage = perLineCoverage(5 to 1, 6 to 1, 7 to 1, 8 to 0)
        val fail = changedLineGate.evaluate(context(coverage = coverage, changedLines = diff(5, 6, 7, 8))).single()
        assertEquals(GateStatus.FAILED, fail.status)
        assertEquals("75.00% of 4 changed lines vs origin/dev (min 80%)", fail.detail)

        val pass = changedLineGate.evaluate(context(coverage = coverage, changedLines = diff(5, 6, 7))).single()
        assertEquals(GateStatus.PASSED, pass.status)
    }

    @Test
    fun `absolute report paths still match repo-relative git paths`() {
        val coverage = CoverageData(listOf(
            FileCoverage("/ci/workspace/src/main/kotlin/com/example/Greeter.kt", listOf(LineCoverage(5, 1))),
        ))
        val result = changedLineGate.evaluate(context(coverage = coverage, changedLines = diff(5))).single()
        assertEquals(GateStatus.PASSED, result.status)
    }

    @Test
    fun `non-executable changed lines are excluded from the ratio`() {
        // only line 5 is in the report; lines 6-7 are blanks/comments
        val coverage = perLineCoverage(5 to 1)
        val result = changedLineGate.evaluate(context(coverage = coverage, changedLines = diff(5, 6, 7))).single()
        assertEquals(GateStatus.PASSED, result.status)
        assertTrue(result.detail.startsWith("100.00% of 1 changed lines"), result.detail)
    }

    @Test
    fun `changed files without coverage data are noted, not failed`() {
        val changed = ChangedLines.Diff("main", mapOf(
            "src/main/kotlin/com/example/Greeter.kt" to setOf(5),
            "README.md" to setOf(1, 2),
        ))
        val result = changedLineGate.evaluate(context(coverage = perLineCoverage(5 to 1), changedLines = changed)).single()
        assertEquals(GateStatus.PASSED, result.status)
        assertTrue(result.detail.endsWith("; 1 changed file(s) without coverage data"), result.detail)
    }

    @Test
    fun `changed-line gate skips on empty diffs and missing coverage`() {
        val emptyDiff = ChangedLines.Diff("main", emptyMap())
        assertEquals(
            GateStatus.SKIPPED,
            changedLineGate.evaluate(context(coverage = perLineCoverage(1 to 1), changedLines = emptyDiff)).single().status,
        )
        val noCoverage = changedLineGate.evaluate(context(changedLines = diff(5))).single()
        assertEquals(GateStatus.SKIPPED, noCoverage.status)
        assertEquals("no coverage data", noCoverage.detail)
        // changed lines exist but none map to line data (e.g. only generated code changed)
        val noData = changedLineGate.evaluate(context(coverage = perLineCoverage(1 to 1), changedLines = diff(99))).single()
        assertEquals(GateStatus.SKIPPED, noData.status)
    }

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
