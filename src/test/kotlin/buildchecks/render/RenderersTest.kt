package buildchecks.render

import buildchecks.Fixtures
import buildchecks.model.ChangedFileCoverage
import buildchecks.model.ChangedLineCoverage
import buildchecks.model.CheckSummary
import buildchecks.model.confidence
import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.LineCoverage
import buildchecks.model.ParsedReport
import buildchecks.model.Freshness
import buildchecks.model.GateResult
import buildchecks.model.GateStatus
import buildchecks.model.IngestedFile
import buildchecks.model.Location
import buildchecks.model.ReportedFinding
import buildchecks.model.Severity
import buildchecks.model.TestResult
import buildchecks.model.TestStatus
import buildchecks.model.merged
import buildchecks.parse.CheckstyleParser
import buildchecks.parse.JacocoParser
import buildchecks.parse.JunitParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** All renderers against one summary assembled from the real Shelf reports. */
class RenderersTest {

    private val summary = shelfSummary()

    private fun shelfSummary(): CheckSummary {
        val reports = listOf(
            CheckstyleParser().parse(Fixtures.text("shelf/detekt.xml")),
            JacocoParser().parse(Fixtures.text("shelf/jvmTestCoverage.xml")),
            JunitParser().parse(Fixtures.text("shelf/TEST-com.toddway.shelf.JvmTests.xml")),
        ).merged()
        val gates = listOf(
            GateResult("findings", GateStatus.PASSED, "0 new (max 0), 8 total (baseline max 8)"),
            GateResult("test failures", GateStatus.FAILED, "1 failed of 4 tests (max 0)"),
        )
        // eslint.sarif is 45 min old vs detekt.xml at 0 (tolerance 15), so the set is stale; plus one
        // not-understood report. Two MINOR signals, no skipped gate -> MEDIUM confidence.
        val notUnderstood = listOf("build/reports/detekt/detekt.txt")
        val freshness = Freshness(mapOf("build/reports/detekt.xml" to 0, "build/reports/eslint.sarif" to 45), 15)
        return CheckSummary(
            gates = gates,
            findings = reports.findings.mapIndexed { index, finding ->
                ReportedFinding(finding, "fp%04d".format(index), isNew = index == 0, toolReport = "tools/detekt/detekt.html")
            },
            tests = reports.tests,
            coverage = reports.coverage,
            files = listOf(
                IngestedFile("build/reports/detekt.xml", "checkstyle", 0, reports, toolReport = "tools/detekt/detekt.html"),
                IngestedFile("build/reports/eslint.sarif", "sarif", 0, reports, content = Fixtures.text("eslint.sarif")),
            ),
            notUnderstood = notUnderstood,
            freshness = freshness,
            confidence = confidence(gates, freshness, notUnderstood, emptyList()),
        )
    }

    @Test
    fun `summary json is the stable scripting contract`() {
        val json = Json.parseToJsonElement(SummaryJson().render(summary)).jsonObject
        assertEquals(1, json["schemaVersion"]!!.jsonPrimitive.content.toInt())
        assertFalse(json["passed"]!!.jsonPrimitive.boolean)
        assertEquals(2, json["gates"]!!.jsonArray.size)
        val findings = json["findings"]!!.jsonObject
        assertEquals(8, findings["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, findings["new"]!!.jsonPrimitive.content.toInt())
        val tests = json["tests"]!!.jsonObject
        assertEquals(4, tests["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, tests["failed"]!!.jsonPrimitive.content.toInt())
        assertEquals(93.24, json["coveragePercent"]!!.jsonPrimitive.content.toDouble(), 0.01)
        // the confidence axis rides alongside `passed` in the same stable contract
        val confidence = json["confidence"]!!.jsonObject
        assertEquals("MEDIUM", confidence["level"]!!.jsonPrimitive.content)
        val signals = confidence["reasons"]!!.jsonArray.map { it.jsonObject["signal"]!!.jsonPrimitive.content }
        assertEquals(listOf("stale-reports", "not-understood"), signals)
    }

    @Test
    fun `findings json carries the full model`() {
        val json = Json.parseToJsonElement(FindingsJson().render(summary)).jsonObject
        assertEquals(8, json["findings"]!!.jsonArray.size)
        val first = json["findings"]!!.jsonArray.first().jsonObject
        assertEquals("fp0000", first["fingerprint"]!!.jsonPrimitive.content)
        assertTrue(first["new"]!!.jsonPrimitive.boolean)
        assertEquals(6, json["coverage"]!!.jsonObject["files"]!!.jsonArray.size)
        val lines = json["coverage"]!!.jsonObject["files"]!!.jsonArray.first()
            .jsonObject["lines"]!!.jsonArray
        assertEquals(4, lines.first().jsonArray.size) // [line, hits, coveredBranches, totalBranches]
    }

    @Test
    fun `codeclimate issues map severities for the gitlab widget`() {
        val issues = Json.parseToJsonElement(CodeClimateReport().render(summary)).jsonArray
        assertEquals(8, issues.size)
        val severities = issues.groupingBy { it.jsonObject["severity"]!!.jsonPrimitive.content }.eachCount()
        assertEquals(mapOf("major" to 6, "info" to 2), severities) // shelf detekt: 6 errors, 2 info
        val first = issues.first().jsonObject
        assertTrue(first["location"]!!.jsonObject["path"]!!.jsonPrimitive.content.endsWith(".kt"))
        assertEquals("fp0000", first["fingerprint"]!!.jsonPrimitive.content)
    }

    @Test
    fun `merged sarif combines every ingested sarif run`() {
        val sarif = Json.parseToJsonElement(MergedSarif().render(summary)).jsonObject
        assertEquals("2.1.0", sarif["version"]!!.jsonPrimitive.content)
        assertEquals(1, sarif["runs"]!!.jsonArray.size)
        val empty = MergedSarif().render(summary.copy(files = emptyList()))
        assertEquals(0, Json.parseToJsonElement(empty).jsonObject["runs"]!!.jsonArray.size)
    }

    @Test
    fun `summary text is a one-line gate headline for commit statuses`() {
        val line = SummaryText().render(summary)
        assertEquals(
            "gates failed: test failures · coverage 93.24% · 4 tests, 1 failed · 1 new finding · confidence medium",
            line,
        )
        assertFalse(line.contains("\n"), "must be a single line")
        assertTrue(line.length <= 140, "must fit a commit-status description")
    }

    @Test
    fun `markdown summary shows gates, totals, and the freshness warning`() {
        val markdown = MarkdownSummary().render(summary)
        assertTrue(markdown.startsWith("## BuildChecks: ❌ failed"))
        assertTrue(markdown.contains("| test failures | ❌ | 1 failed of 4 tests (max 0) |"))
        assertTrue(markdown.contains("differ in age by 45 minutes"))
        assertTrue(markdown.contains("**Findings:** 8 (6 errors, 0 warnings, 2 info, 1 new)"))
        assertTrue(markdown.contains("**Coverage:** 93.24%"))
        // the trust axis is in the header and its reasons are spelled out
        assertTrue(markdown.startsWith("## BuildChecks: ❌ failed · confidence: MEDIUM"))
        assertTrue(markdown.contains("found but not understood"))
    }

    @Test
    fun `console summary prints the totals table and gate lines`() {
        val console = ConsoleSummary().render(summary)
        assertTrue(console.contains("│ warnings"))
        assertTrue(console.contains("│ coverage"))
        assertTrue(console.contains("PASS  findings: 0 new (max 0), 8 total (baseline max 8)"))
        assertTrue(console.contains("FAIL  test failures: 1 failed of 4 tests (max 0)"))
        assertTrue(console.contains("WARNING: ingested reports differ in age by 45 minutes"))
        assertTrue(console.contains("confidence: MEDIUM"))
        assertTrue(console.contains("- 1 report file found but not understood"))
    }

    @Test
    fun `html report is self-contained and complete`() {
        val html = HtmlReport().render(summary)
        assertTrue(html.contains("<style>") && html.contains("<script>"))
        assertFalse(html.contains("http://") || html.contains("https://"), "no external requests")
        assertTrue(html.contains("BuildChecks <span class=\"badge fail\">FAILED</span>"))
        // the confidence badge rides next to the pass/fail badge, with its reasons below
        assertTrue(html.contains("<span class=\"badge conf-medium\""))
        assertTrue(html.contains("confidence: MEDIUM"))
        assertTrue(html.contains("<div class=\"confidence\">"))
        assertTrue(html.contains("Findings (8)"))
        assertTrue(html.contains("data-severity=\"ERROR\""))
        assertTrue(html.contains(">NEW</span>"))
        assertTrue(html.contains("test_with_ktor[jvm]"))
        assertTrue(html.contains("Coverage 93.24%"))
        // each finding's Location drills into the report of the tool that produced it
        assertTrue(html.contains("<td class=\"path\"><a href=\"tools/detekt/detekt.html\""))
        // staleness is surfaced per-row (stale? chips), not as a top-of-report banner
        assertFalse(html.contains("differ in age"))
        assertTrue(html.contains("found but not understood") || html.contains("Found but not understood"))
    }

    @Test
    fun `html report explains itself to a first-time reader`() {
        val html = HtmlReport().render(summary)
        // every gate name carries a hover explanation
        assertTrue(html.contains("<span class=\"help\" title=\"Checked two ways against the baseline"))
        assertTrue(html.contains("title=\"Failed tests must not exceed"))
        // bulk tables are collapsed behind their aggregates
        assertTrue(html.contains("<details><summary>Per-file line coverage (6 files)</summary>"))
        assertTrue(html.contains("<details><summary>All 2 files</summary>"))
        assertTrue(html.contains("<details><summary>Found but not understood (1)</summary>"))
        // the coverage aggregate is stated in lines, not just a percentage
        assertTrue(html.contains("executable lines covered"))
    }

    @Test
    fun `with a baseline the findings table defaults to new-only`() {
        val html = HtmlReport().render(summary.copy(hasBaseline = true))
        assertTrue(html.contains("Findings — 1 new (8 total)"))
        assertTrue(html.contains("id=\"newonly\" checked=\"checked\""))
        // without a baseline the count is a plain total and the box is unchecked
        val noBaseline = HtmlReport().render(summary)
        assertTrue(noBaseline.contains("Findings (8)"))
        assertFalse(noBaseline.contains("checked=\"checked\""))
    }

    @Test
    fun `html counts uncovered changed lines worst-first and links files with a coverage report`() {
        val measured = ChangedLineCoverage.Measured(
            baseRef = "origin/main",
            files = listOf(
                // fewer uncovered, listed first in the input — must sort BELOW Shelf.kt in output
                ChangedFileCoverage("web/app.js", covered = emptyList(),
                    uncovered = listOf(3), toolReport = null), // no HTML report -> plain text
                ChangedFileCoverage("src/main/kotlin/Shelf.kt", covered = listOf(10),
                    uncovered = listOf(12, 15), toolReport = "tools/jacoco/index.html"),
            ),
            filesWithoutData = 0,
        )
        val html = HtmlReport().render(summary.copy(changedLineCoverage = measured))
        assertTrue(html.contains("Changed lines not covered (3)")) // 3 uncovered lines total
        assertTrue(html.contains("origin/main"))
        // a file with a coverage report links into it; a file without one is plain text
        assertTrue(html.contains("<a href=\"tools/jacoco/index.html\""))
        assertTrue(html.contains("<td class=\"path\">web/app.js</td>"))
        // the table shows the uncovered COUNT (not the raw line numbers), worst-first
        assertFalse(html.contains("12, 15"))
        assertTrue(html.indexOf("Shelf.kt") < html.indexOf("web/app.js")) // 2 uncovered before 1
    }

    @Test
    fun `html omits the changed-coverage section when nothing changed is uncovered`() {
        assertFalse(HtmlReport().render(summary).contains("Changed lines not covered"))
        val fullyCovered = ChangedLineCoverage.Measured(
            "origin/main",
            listOf(ChangedFileCoverage("a.kt", covered = listOf(1, 2), uncovered = emptyList())),
            filesWithoutData = 0,
        )
        assertFalse(HtmlReport().render(summary.copy(changedLineCoverage = fullyCovered))
            .contains("Changed lines not covered"))
    }

    @Test
    fun `only findings from a stale age-outlier report are flagged`() {
        // In the fixture freshness, eslint.sarif is 45 min old vs detekt.xml at 0 (tolerance 15),
        // so a finding sourced from eslint.sarif is an outlier and one from detekt.xml is not.
        val stale = ReportedFinding(
            Finding("eslint", "no-unused-vars", Severity.INFO, "unused", Location("web/app.js", 1)),
            "fpStale", isNew = false, reportPath = "build/reports/eslint.sarif",
        )
        val fresh = ReportedFinding(
            Finding("detekt", "MagicNumber", Severity.ERROR, "magic", Location("A.kt", 1)),
            "fpFresh", isNew = false, reportPath = "build/reports/detekt.xml",
        )
        val html = HtmlReport().render(summary.copy(findings = listOf(stale, fresh)))
        // exactly one of the two finding rows is flagged (the outlier), the fresh one is not
        assertEquals(1, Regex("data-stale=\"true\"").findAll(html).count())
        assertTrue(html.contains("class=\"badge stale\""))
    }

    @Test
    fun `stale age-outlier reports flag test and coverage rows and their aggregates`() {
        val s = CheckSummary(
            gates = emptyList(),
            findings = emptyList(),
            tests = listOf(TestResult("Suite", "staleTest", TestStatus.FAILED, "boom",
                reportPath = "build/test-results/old/TEST-x.xml")),
            coverage = CoverageData(listOf(
                FileCoverage("Cov.kt", listOf(LineCoverage(1, 0)), reportPath = "build/reports/jacoco/old.xml"))),
            files = listOf(
                IngestedFile("build/reports/jacoco/old.xml", "jacoco", 0, ParsedReport()),
                IngestedFile("build/test-results/old/TEST-x.xml", "junit", 0, ParsedReport()),
                IngestedFile("build/reports/detekt.xml", "checkstyle", 0, ParsedReport()),
            ),
            notUnderstood = emptyList(),
            freshness = Freshness(
                mapOf(
                    "build/reports/detekt.xml" to 0,           // freshest
                    "build/reports/jacoco/old.xml" to 90,      // outlier -> coverage row + aggregate
                    "build/test-results/old/TEST-x.xml" to 90, // outlier -> test row + aggregate
                ),
                toleranceMinutes = 15,
            ),
        )
        val html = HtmlReport().render(s)
        // per-row chips carry the source report + age in their tooltip
        assertTrue(html.contains("From build/test-results/old/TEST-x.xml, 90 min old"))
        assertTrue(html.contains("From build/reports/jacoco/old.xml, 90 min old"))
        // the blended Tests/Coverage totals are flagged too (per-file coverage table is collapsed)
        assertTrue(html.contains("This total blends"))
    }

    @Test
    fun `changed-line coverage renders above tests and whole-project coverage`() {
        val measured = ChangedLineCoverage.Measured(
            "origin/main",
            listOf(ChangedFileCoverage("a.kt", covered = emptyList(), uncovered = listOf(1))),
            filesWithoutData = 0,
        )
        val html = HtmlReport().render(summary.copy(changedLineCoverage = measured))
        val changed = html.indexOf("Changed lines not covered")
        val tests = html.indexOf("Tests (")
        val coverage = html.indexOf("Coverage 93.24%")
        assertTrue(changed in 0 until tests, "changed-coverage should precede Tests")
        assertTrue(tests < coverage, "Tests should precede whole-project Coverage")
    }

    @Test
    fun `html escapes markup in tool data`() {
        val hostile = CheckSummary(
            gates = emptyList(),
            findings = listOf(ReportedFinding(
                Finding("t", "<script>", Severity.ERROR, "x < y & \"z\"", Location("a<b>.kt", 1)),
                "fp", false,
            )),
            tests = listOf(TestResult("s", "n", TestStatus.FAILED, "<img src=x>")),
            coverage = null,
            files = emptyList(),
            notUnderstood = emptyList(),
            freshness = null,
        )
        val html = HtmlReport().render(hostile)
        assertFalse(html.contains("<script>x") || html.contains("<img src=x>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("x &lt; y &amp; &quot;z&quot;"))
    }
}
