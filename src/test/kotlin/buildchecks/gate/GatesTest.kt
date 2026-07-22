package buildchecks.gate

import buildchecks.model.ChangedLines
import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.GateStatus
import buildchecks.model.LineCoverage
import buildchecks.model.changedLineCoverage
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
        presentOrigins: Set<OriginKind> = emptySet(),
    ) = GateContext(
        findings, tests, coverage, baseline,
        changedLineCoverage(changedLines, coverage), presentOrigins,
    )

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

    // -- findings --

    @Test
    fun `findings gate skips without a baseline`() {
        val result = FindingsGate(GateConfig()).evaluate(context(listOf(finding()))).single()
        assertEquals(GateStatus.SKIPPED, result.status)
    }

    @Test
    fun `baselined findings pass, unknown fingerprints fail with an example`() {
        val gate = FindingsGate(GateConfig())
        val baseline = Baseline(setOf("aaaa"), 1, null)

        val pass = gate.evaluate(context(listOf(finding()), baseline = baseline)).single()
        assertEquals(GateStatus.PASSED, pass.status)
        assertEquals("0 new (max 0), 1 total (baseline max 1)", pass.detail)

        val fail = gate.evaluate(context(listOf(finding(fingerprint = "bbbb")), baseline = baseline)).single()
        assertEquals(GateStatus.FAILED, fail.status)
        assertTrue(fail.detail.contains("MagicNumber at src/A.kt:7"))
    }

    @Test
    fun `findings gate fails when the total grows even with no new fingerprints`() {
        // two findings sharing the baselined fingerprint: nothing "new", but the total ratchet trips
        val gate = FindingsGate(GateConfig())
        val baseline = Baseline(setOf("aaaa"), 1, null)

        val fail = gate.evaluate(context(listOf(finding(), finding()), baseline = baseline)).single()
        assertEquals(GateStatus.FAILED, fail.status)
        assertEquals("0 new (max 0), 2 total (baseline max 1)", fail.detail)

        // ratchet off: only the new-findings clause remains
        val ratchetOff = FindingsGate(GateConfig(ratchet = false))
            .evaluate(context(listOf(finding(), finding()), baseline = baseline)).single()
        assertEquals(GateStatus.PASSED, ratchetOff.status)
        assertEquals("0 new (max 0)", ratchetOff.detail)
    }

    // -- coverage --

    @Test
    fun `coverage gate holds the baseline floor minus tolerance and names the source`() {
        val gate = CoverageGate(GateConfig(coverageTolerance = 0.1))
        val baseline = Baseline(emptySet(), 0, 80.05)

        val within = gate.evaluate(context(coverage = coverage(80, 100), baseline = baseline)).single()
        assertEquals(GateStatus.PASSED, within.status)
        assertEquals("80.00% (min 79.95%, from baseline)", within.detail)

        val below = gate.evaluate(context(coverage = coverage(79, 100), baseline = baseline)).single()
        assertEquals(GateStatus.FAILED, below.status)
    }

    @Test
    fun `coverage gate uses the higher of baseline and configured minimum`() {
        val baseline = Baseline(emptySet(), 0, 60.0)
        val configHigher = CoverageGate(GateConfig(minCoveragePercent = 75.0))
            .evaluate(context(coverage = coverage(70, 100), baseline = baseline)).single()
        assertEquals(GateStatus.FAILED, configHigher.status)
        assertEquals("70.00% (min 75.00%, configured)", configHigher.detail)

        // ratchet off leaves only the configured minimum
        val ratchetOff = CoverageGate(GateConfig(ratchet = false, minCoveragePercent = 52.0))
            .evaluate(context(coverage = coverage(53, 100), baseline = baseline)).single()
        assertEquals("53.00% (min 52.00%, configured)", ratchetOff.detail)
    }

    @Test
    fun `coverage gate skips without data and vanishes without any limit`() {
        val gate = CoverageGate(GateConfig(minCoveragePercent = 52.0))
        assertEquals(GateStatus.SKIPPED, gate.evaluate(context()).single().status)
        // no baseline coverage, no configured minimum -> nothing to gate against
        assertTrue(CoverageGate(GateConfig()).evaluate(context(coverage = coverage(50, 100))).isEmpty())
    }

    // -- caps --

    @Test
    fun `unconfigured caps yield no results for a project without tests`() {
        assertTrue(CapsGate(GateConfig()).evaluate(context()).isEmpty())
    }

    @Test
    fun `error and warning caps count by severity`() {
        val gate = CapsGate(GateConfig(maxErrors = 0, maxWarnings = 1))
        val results = gate.evaluate(context(listOf(
            finding(Severity.ERROR),
            finding(Severity.WARNING, "bbbb"),
            finding(Severity.INFO, "cccc"),
        )))
        assertEquals(GateStatus.FAILED, results.single { it.gate == "errors" }.status)
        assertEquals(GateStatus.PASSED, results.single { it.gate == "warnings" }.status)
    }

    @Test
    fun `test failures cap defaults to zero when tests are present`() {
        val gate = CapsGate(GateConfig())
        val tests = listOf(
            TestResult("suite", "passes", TestStatus.PASSED),
            TestResult("suite", "fails", TestStatus.FAILED),
        )
        val result = gate.evaluate(context(tests = tests)).single()
        assertEquals(GateStatus.FAILED, result.status)
        assertEquals("1 failed of 2 tests (max 0)", result.detail)
    }

    // -- expected reports (origin presence) --

    private val missingReportGate = MissingReportGate()

    @Test
    fun `expected-reports gate skips without a baseline or against a pre-v2 baseline`() {
        val noBaseline = missingReportGate.evaluate(context()).single()
        assertEquals(GateStatus.SKIPPED, noBaseline.status)
        assertEquals("no baseline", noBaseline.detail)

        val preV2 = Baseline(emptySet(), 0, null, manifest = null)
        val old = missingReportGate.evaluate(context(baseline = preV2)).single()
        assertEquals(GateStatus.SKIPPED, old.status)
        assertTrue(old.detail.contains("re-baseline"), old.detail)
    }

    @Test
    fun `expected-reports gate passes when every manifest entry is present this run`() {
        val manifest = setOf(OriginKind(".", "detekt"), OriginKind("services/auth", "jacoco"))
        val baseline = Baseline(emptySet(), 0, null, manifest = manifest)
        val result = missingReportGate.evaluate(context(baseline = baseline, presentOrigins = manifest)).single()
        assertEquals(GateStatus.PASSED, result.status)
        assertEquals("2 expected report(s) present", result.detail)
    }

    @Test
    fun `expected-reports gate fails naming what stopped being emitted`() {
        val manifest = setOf(OriginKind(".", "detekt"), OriginKind("services/auth", "jacoco"))
        val baseline = Baseline(emptySet(), 0, null, manifest = manifest)
        // services/auth stopped emitting its jacoco report; detekt still present
        val present = setOf(OriginKind(".", "detekt"))
        val result = missingReportGate.evaluate(context(baseline = baseline, presentOrigins = present)).single()
        assertEquals(GateStatus.FAILED, result.status)
        assertEquals("1 expected report(s) missing: jacoco in services/auth", result.detail)
    }

    @Test
    fun `a tool going clean does not read as a missing report`() {
        // detekt is keyed by its driver name (ParsedReport.tool), not its finding count, so a
        // run with zero detekt findings still presents (origin, "detekt") — no false failure.
        val manifest = setOf(OriginKind(".", "detekt"))
        val baseline = Baseline(emptySet(), 0, null, manifest = manifest)
        val result = missingReportGate.evaluate(context(baseline = baseline, presentOrigins = manifest)).single()
        assertEquals(GateStatus.PASSED, result.status)
    }
}
