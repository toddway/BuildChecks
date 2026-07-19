package buildchecks.cli

import buildchecks.Fixtures
import buildchecks.gate.FingerprintBaseline
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** End-to-end: a temp project assembled from real Shelf reports, through both verbs. */
class VerbsTest {

    @TempDir
    lateinit var root: File

    private val out = mutableListOf<String>()
    private val text: String get() = out.joinToString("\n")

    private fun copy(fixture: String, dest: String) {
        Fixtures.file(fixture).copyTo(File(root, dest))
    }

    private fun copyPassingReports() {
        copy("shelf/detekt.xml", "build/reports/detekt.xml")
        copy("shelf/jvmTestCoverage.xml", "build/reports/jvmTestCoverage/jvmTestCoverage.xml")
        copy("shelf/TEST-com.toddway.shelf.ShelfTests.xml", "build/test-results/jvmTest/TEST-ShelfTests.xml")
    }

    @Test
    fun `baseline then check passes with exit code zero`() {
        copyPassingReports()
        assertEquals(0, runBaseline(root) { out += it })
        assertTrue(File(root, "buildchecks-baseline.txt").isFile)

        assertEquals(0, runCheck(root) { out += it })
        assertTrue(text.contains("PASS  new findings: 0 new (max 0)"), text)
        assertTrue(text.contains("PASS  findings must not increase: 8 findings"), text)
        assertTrue(text.contains("PASS  coverage must not decrease:"), text)
        assertTrue(text.contains("PASS  test failures: 0 failed of 16"), text)
    }

    @Test
    fun `check writes every output file`() {
        copyPassingReports()
        copy("eslint.sarif", "build/reports/eslint.sarif")
        // a tool html report next to the ingested xml, as jacoco lays it out
        File(root, "build/reports/jvmTestCoverage/html").mkdirs()
        File(root, "build/reports/jvmTestCoverage/html/index.html").writeText("<html>jacoco report</html>")
        // Gradle's test html report, in its conventional location
        File(root, "build/reports/tests/jvmTest").mkdirs()
        File(root, "build/reports/tests/jvmTest/index.html").writeText("<html>test report</html>")
        runBaseline(root) { }
        assertEquals(0, runCheck(root) { out += it })

        val outputDir = File(root, ReportDiscovery.DEFAULT_OUTPUT_DIR)
        for (name in listOf("index.html", "summary.md", "summary.json", "findings.json", "codeclimate.json", "merged.sarif")) {
            assertTrue(File(outputDir, name).isFile, "$name missing")
        }
        assertTrue(File(outputDir, "summary.json").readText().contains("\"passed\": true"))
        assertTrue(File(outputDir, "merged.sarif").readText().contains("ESLint"))
        // jacoco html/ dir sits next to the ingested xml -> copied and linked
        assertTrue(File(outputDir, "tools/build-reports-jvmTestCoverage/html/index.html").isFile)
        val html = File(outputDir, "index.html").readText()
        assertTrue(html.contains("tools/build-reports-jvmTestCoverage/html/index.html"))
        // junit xml under test-results/jvmTest -> Gradle's reports/tests/jvmTest html
        assertTrue(File(outputDir, "tools/build-reports-tests-jvmTest/index.html").isFile)
        assertTrue(html.contains("<a href=\"tools/build-reports-tests-jvmTest/index.html\">jvmTest</a>"), html)
    }

    @Test
    fun `check fails against an emptied baseline`() {
        copyPassingReports()
        runBaseline(root) { }
        FingerprintBaseline(File(root, "buildchecks-baseline.txt")).write(emptyList(), null)

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(text.contains("FAIL  new findings: 8 new (max 0)"), text)
        assertTrue(text.contains("FAIL  findings must not increase: 8 findings (baseline 0)"), text)
        assertTrue(File(root, "${ReportDiscovery.DEFAULT_OUTPUT_DIR}/summary.json").readText()
            .contains("\"passed\": false"))
    }

    @Test
    fun `failing tests exit one even without a baseline`() {
        copy("shelf/TEST-com.toddway.shelf.JvmTests.xml", "build/test-results/jvmTest/TEST-JvmTests.xml")

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(text.contains("FAIL  test failures: 1 failed of 4 tests (max 0)"), text)
        assertTrue(text.contains("SKIP  new findings:"), text)
    }

    @Test
    fun `a project without reports passes with skips`() {
        assertEquals(0, runCheck(root) { out += it })
        assertTrue(text.contains("no report files found"), text)
    }

    @Test
    fun `unrecognized candidate files are listed, not silently skipped`() {
        File(root, "build/reports").mkdirs()
        File(root, "build/reports/lint-results.xml").writeText(Fixtures.text("lint-results-prodRelease.xml"))

        runCheck(root) { out += it }
        assertTrue(text.contains("not understood: build/reports/lint-results.xml"), text)
    }

    @Test
    fun `config drives gates, output dir, and baseline file`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [reports]
            output_dir = "out/quality"
            [gates]
            max_errors = 0
            [git]
            baseline_file = "quality-baseline.txt"
        """.trimIndent())
        val config = loadConfig(null, root)

        runBaseline(root, config) { }
        assertTrue(File(root, "quality-baseline.txt").isFile)

        assertEquals(1, runCheck(root, config) { out += it })
        assertTrue(text.contains("FAIL  errors: 6 errors (max 0)"), text) // shelf detekt has 6 errors
        assertTrue(File(root, "out/quality/summary.json").isFile)
    }

    @Test
    fun `configured report globs narrow what is ingested`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [reports]
            paths = ["**/test-results/**"]
        """.trimIndent())

        runCheck(root, loadConfig(null, root)) { out += it }
        assertTrue(text.contains("ingested: build/test-results/jvmTest/TEST-ShelfTests.xml (junit)"), text)
        assertTrue(!text.contains("detekt.xml"), text)
    }

    @Test
    fun `stale sibling reports produce a freshness warning`() {
        copyPassingReports()
        File(root, "build/reports/detekt.xml").setLastModified(System.currentTimeMillis() - 60 * 60_000)

        runCheck(root) { out += it }
        assertTrue(text.contains("WARNING: ingested reports differ in age by"), text)
    }
}
