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
        assertTrue(out.any { it == "PASS  new findings: 0 new (max 0)" }, out.joinToString("\n"))
        assertTrue(out.any { it.startsWith("PASS  findings ratchet: 8 findings") }, out.joinToString("\n"))
        assertTrue(out.any { it.startsWith("PASS  coverage ratchet:") }, out.joinToString("\n"))
        assertTrue(out.any { it.startsWith("PASS  test failures: 0 failed of 16") }, out.joinToString("\n"))
    }

    @Test
    fun `check fails against an emptied baseline`() {
        copyPassingReports()
        runBaseline(root) { }
        FingerprintBaseline(File(root, "buildchecks-baseline.txt")).write(emptyList(), null)

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(out.any { it.startsWith("FAIL  new findings: 8 new (max 0)") }, out.joinToString("\n"))
        assertTrue(out.any { it.startsWith("FAIL  findings ratchet: 8 findings (baseline 0)") }, out.joinToString("\n"))
    }

    @Test
    fun `failing tests exit one even without a baseline`() {
        copy("shelf/TEST-com.toddway.shelf.JvmTests.xml", "build/test-results/jvmTest/TEST-JvmTests.xml")

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(out.any { it == "FAIL  test failures: 1 failed of 4 tests (max 0)" }, out.joinToString("\n"))
        assertTrue(out.any { it.startsWith("SKIP  new findings:") }, out.joinToString("\n"))
    }

    @Test
    fun `a project without reports passes with skips`() {
        assertEquals(0, runCheck(root) { out += it })
        assertTrue(out.any { it.startsWith("no report files found") }, out.joinToString("\n"))
    }

    @Test
    fun `unrecognized candidate files are listed, not silently skipped`() {
        File(root, "build/reports").mkdirs()
        File(root, "build/reports/lint-results.xml").writeText(Fixtures.text("lint-results-prodRelease.xml"))

        runCheck(root) { out += it }
        assertTrue(out.any { it == "not understood: build/reports/lint-results.xml" }, out.joinToString("\n"))
    }
}
