package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.Freshness
import buildchecks.model.GateResult
import buildchecks.model.GateStatus
import buildchecks.model.IngestedFile
import buildchecks.model.LineCoverage
import buildchecks.model.Location
import buildchecks.model.ParsedReport
import buildchecks.model.ReportedFinding
import buildchecks.model.Severity
import buildchecks.model.TestResult
import buildchecks.model.TestStatus
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Not an assertion suite — a styling loop. Renders a hand-built summary that shows every
 * visual state the real dogfood run (a healthy repo) never does: failed and skipped gates,
 * the FAILED badge, NEW findings, all three severities, test failures, the stale-reports
 * banner, and the not-understood list.
 *
 * Loop while tweaking HtmlReport / report.css / report.js:
 *   ./gradlew -t test --tests buildchecks.render.HtmlReportPreviewTest
 * then reload build/reports/buildchecks-preview/index.html in a browser.
 */
class HtmlReportPreviewTest {

    @Test
    fun `render every visual state for browser preview`() {
        val out = Path.of("build/reports/buildchecks-preview/index.html")
        Files.createDirectories(out.parent)
        Files.writeString(out, HtmlReport().render(previewSummary()))
        // A real check run copies each tool's own HTML under tools/; the preview builds a
        // summary by hand, so write matching stubs to make the drill-down links resolve here.
        stubToolReport(out.parent, "tools/detekt/detekt.html", "detekt")
        stubToolReport(out.parent, "tools/jacoco/index.html", "JaCoCo coverage")
        println("preview: ${out.toAbsolutePath()}")
    }

    private fun stubToolReport(dir: Path, relative: String, tool: String) {
        val file = dir.resolve(relative)
        Files.createDirectories(file.parent)
        Files.writeString(file, "<!DOCTYPE html><title>$tool</title>" +
            "<h1>$tool report</h1><p>Preview stub — a real run copies the tool's own HTML here.</p>" +
            "<p><a href=\"../../index.html\">← back to BuildChecks</a></p>")
    }

    private fun previewSummary(): CheckSummary {
        val findings = listOf(
            Finding("detekt", "MagicNumber", Severity.ERROR,
                "This expression contains a magic number 42.", Location("src/main/kotlin/Shelf.kt", 12)),
            Finding("detekt", "LongMethod", Severity.WARNING,
                "The function load is too long (61 > 60).", Location("src/main/kotlin/Loader.kt", 8)),
            Finding("eslint", "no-unused-vars", Severity.INFO,
                "'result' is assigned a value but never used.", Location("web/app.js", 101)),
            Finding("cpd", "duplication", Severity.WARNING,
                "Found 24 duplicated tokens: x < y && \"quoted\" <tag>", // markup must render escaped
                Location("src/main/kotlin/Copy.kt", 30), duplicatedTokens = 24),
        )
        return CheckSummary(
            gates = listOf(
                GateResult("findings", GateStatus.FAILED, "1 new (max 0), 4 total (baseline max 3)"),
                GateResult("coverage", GateStatus.PASSED, "81.25% (baseline min 80.0%)"),
                GateResult("test failures", GateStatus.FAILED, "1 failed of 3 tests (max 0)"),
                GateResult("changed-line coverage", GateStatus.SKIPPED, "no git base ref available"),
            ),
            findings = findings.mapIndexed { index, finding ->
                // detekt emits an HTML report (Location links into it); eslint/cpd here don't (plain text).
                val report = if (finding.tool == "detekt") "tools/detekt/detekt.html" else null
                ReportedFinding(finding, "fp%04d".format(index), isNew = index == 0, toolReport = report)
            },
            tests = listOf(
                TestResult("com.example.ShelfTest", "stores and loads", TestStatus.PASSED),
                TestResult("com.example.ShelfTest", "expires old entries", TestStatus.FAILED,
                    "expected: <3> but was: <2>"),
                TestResult("com.example.LoaderTest", "slow network", TestStatus.SKIPPED),
            ),
            coverage = CoverageData(listOf(
                FileCoverage("src/main/kotlin/Shelf.kt", (1..40).map { LineCoverage(it, if (it % 5 == 0) 0 else 1) }),
                FileCoverage("src/main/kotlin/Loader.kt", (1..24).map { LineCoverage(it, 1) }),
                FileCoverage("src/main/kotlin/Copy.kt", emptyList()),
            )),
            files = listOf(
                IngestedFile("build/reports/detekt/detekt.xml", "checkstyle", 0, ParsedReport(),
                    toolReport = "tools/detekt/detekt.html"),
                IngestedFile("build/reports/jacoco/test/jacocoTestReport.xml", "jacoco", 0, ParsedReport(),
                    toolReport = "tools/jacoco/index.html"),
                IngestedFile("build/test-results/test/TEST-com.example.ShelfTest.xml", "junit", 0, ParsedReport()),
            ),
            notUnderstood = listOf("build/reports/detekt/detekt.txt"),
            hasBaseline = true,
            freshness = Freshness(
                mapOf(
                    "build/reports/detekt/detekt.xml" to 0,
                    "build/reports/jacoco/test/jacocoTestReport.xml" to 52,
                    "build/test-results/test/TEST-com.example.ShelfTest.xml" to 1,
                ),
                toleranceMinutes = 15,
            ),
        )
    }
}
