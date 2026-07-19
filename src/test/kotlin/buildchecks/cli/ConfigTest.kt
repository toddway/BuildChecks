package buildchecks.cli

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigTest {

    @TempDir
    lateinit var root: File

    private fun write(toml: String): File =
        File(root, "buildchecks.toml").also { it.writeText(toml.trimIndent()) }

    @Test
    fun `no config file means all defaults`() {
        val config = loadConfig(null, root)
        assertEquals(Config(), config)
        assertNull(config.reports.paths)
        assertEquals("build/reports/buildchecks", config.reports.outputDir)
        assertEquals("buildchecks-baseline.txt", config.git.baselineFile)
    }

    @Test
    fun `an empty file is the same as no file`() {
        write("")
        assertEquals(Config(), loadConfig(null, root))
    }

    @Test
    fun `parses the documented example`() {
        write("""
            [reports]
            paths = ["**/build/reports/**", "coverage/lcov.info"]
            output_dir = "build/reports/buildchecks"
            freshness_tolerance_minutes = 30

            [gates]
            min_changed_line_coverage = 80
            max_new_findings = 2
            ratchet = false
            coverage_tolerance = 0.5
            min_coverage_percent = 52.0
            max_errors = 0
            max_warnings = 1000

            [git]
            base_ref = "origin/dev"
            baseline_file = "quality-baseline.txt"
        """)
        val config = loadConfig(null, root)
        assertEquals(listOf("**/build/reports/**", "coverage/lcov.info"), config.reports.paths)
        assertEquals(30, config.reports.freshnessToleranceMinutes)
        assertEquals(80, config.gates.minChangedLineCoverage)
        assertEquals(2, config.gates.maxNewFindings)
        assertEquals(false, config.gates.ratchet)
        assertEquals(0.5, config.gates.coverageTolerance)
        assertEquals(52.0, config.gates.minCoveragePercent)
        assertEquals(0, config.gates.maxErrors)
        assertEquals(1000, config.gates.maxWarnings)
        assertEquals("origin/dev", config.git.baseRef)
        assertEquals("quality-baseline.txt", config.git.baselineFile)
    }

    @Test
    fun `interpolates environment variables in string values`() {
        write("""
            [git]
            base_ref = "origin/${'$'}{TARGET_BRANCH}"
        """)
        val config = loadConfig(null, root) { name -> if (name == "TARGET_BRANCH") "dev" else null }
        assertEquals("origin/dev", config.git.baseRef)
    }

    @Test
    fun `an undefined environment variable is an error, not a blank`() {
        write("""
            [git]
            base_ref = "${'$'}{NOT_DEFINED_ANYWHERE}"
        """)
        val error = assertThrows(IllegalArgumentException::class.java) { loadConfig(null, root) { null } }
        assertTrue(error.message!!.contains("NOT_DEFINED_ANYWHERE"))
    }

    @Test
    fun `a misspelled key is an error, not silently ignored`() {
        write("""
            [gates]
            max_warings = 10
        """)
        assertThrows(IllegalArgumentException::class.java) { loadConfig(null, root) }
    }

    @Test
    fun `an explicit config path wins over the root file`() {
        write("[gates]\nmax_errors = 5")
        val explicit = File(root, "other.toml").also { it.writeText("[gates]\nmax_errors = 7") }
        assertEquals(7, loadConfig(explicit, root).gates.maxErrors)
    }
}
