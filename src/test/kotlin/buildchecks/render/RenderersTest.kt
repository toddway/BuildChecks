package buildchecks.render

import buildchecks.Fixtures
import buildchecks.model.CheckSummary
import buildchecks.model.Finding
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
        return CheckSummary(
            gates = listOf(
                GateResult("new findings", GateStatus.PASSED, "0 new (max 0)"),
                GateResult("test failures", GateStatus.FAILED, "1 failed of 4 tests (max 0)"),
            ),
            findings = reports.findings.mapIndexed { index, finding ->
                ReportedFinding(finding, "fp%04d".format(index), isNew = index == 0)
            },
            tests = reports.tests,
            coverage = reports.coverage,
            files = listOf(
                IngestedFile("build/reports/detekt.xml", "checkstyle", 0, reports, toolReport = "tools/detekt/detekt.html"),
                IngestedFile("build/reports/eslint.sarif", "sarif", 0, reports, content = Fixtures.text("eslint.sarif")),
            ),
            notUnderstood = listOf("build/reports/detekt/detekt.txt"),
            freshness = Freshness(mapOf("build/reports/detekt.xml" to 0, "build/reports/eslint.sarif" to 45), 15),
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
    fun `markdown summary shows gates, totals, and the freshness warning`() {
        val markdown = MarkdownSummary().render(summary)
        assertTrue(markdown.startsWith("## BuildChecks: ❌ failed"))
        assertTrue(markdown.contains("| test failures | ❌ | 1 failed of 4 tests (max 0) |"))
        assertTrue(markdown.contains("differ in age by 45 minutes"))
        assertTrue(markdown.contains("**Findings:** 8 (6 errors, 0 warnings, 2 info, 1 new)"))
        assertTrue(markdown.contains("**Coverage:** 93.24%"))
    }

    @Test
    fun `console summary prints the totals table and gate lines`() {
        val console = ConsoleSummary().render(summary)
        assertTrue(console.contains("│ warnings"))
        assertTrue(console.contains("│ coverage"))
        assertTrue(console.contains("PASS  new findings: 0 new (max 0)"))
        assertTrue(console.contains("FAIL  test failures: 1 failed of 4 tests (max 0)"))
        assertTrue(console.contains("WARNING: ingested reports differ in age by 45 minutes"))
    }

    @Test
    fun `html report is self-contained and complete`() {
        val html = HtmlReport().render(summary)
        assertTrue(html.contains("<style>") && html.contains("<script>"))
        assertFalse(html.contains("http://") || html.contains("https://"), "no external requests")
        assertTrue(html.contains("BuildChecks <span class=\"badge fail\">FAILED</span>"))
        assertTrue(html.contains("Findings (8)"))
        assertTrue(html.contains("data-severity=\"ERROR\""))
        assertTrue(html.contains(">NEW</span>"))
        assertTrue(html.contains("test_with_ktor[jvm]"))
        assertTrue(html.contains("Coverage 93.24%"))
        assertTrue(html.contains("Tool reports: <a href=\"tools/detekt/detekt.html\">detekt</a>"))
        assertTrue(html.contains("differ in age by 45 minutes"))
        assertTrue(html.contains("found but not understood") || html.contains("Found but not understood"))
    }

    @Test
    fun `html report explains itself to a first-time reader`() {
        val html = HtmlReport().render(summary)
        // every gate name carries a hover explanation
        assertTrue(html.contains("<span class=\"help\" title=\"Fails on findings introduced since the baseline"))
        assertTrue(html.contains("title=\"Failed tests may not exceed"))
        // bulk tables are collapsed behind their aggregates
        assertTrue(html.contains("<details><summary>Per-file line coverage (6 files)</summary>"))
        assertTrue(html.contains("<details><summary>All 2 files</summary>"))
        assertTrue(html.contains("<details><summary>Found but not understood (1)</summary>"))
        // the coverage aggregate is stated in lines, not just a percentage
        assertTrue(html.contains("executable lines covered"))
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
