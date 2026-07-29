package buildchecks.cli

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ReportDiscoveryTest {

    @TempDir
    lateinit var root: File

    private fun touch(path: String) {
        File(root, path).also { it.parentFile.mkdirs() }.writeText("x")
    }

    private fun discovered(discovery: ReportDiscovery) =
        discovery.discover(root).map { it.relativeTo(root).path.replace(File.separatorChar, '/') }

    @Test
    fun `zero-config defaults scan report locations but never sources or own output`() {
        touch("build/reports/detekt.xml")
        touch("build/test-results/test/TEST-a.xml")
        touch("coverage/lcov.info")
        touch("module/target/site/jacoco/jacoco.xml")
        touch("build/reports/buildchecks/summary.json")     // own output dir
        touch("src/test/resources/fixtures/lcov.info")      // source tree
        touch("build/resources/test/fixtures/coverage/c.xml") // processed resources
        touch("node_modules/pkg/build/reports/x.xml")
        touch("build/reports/tests/index.html")             // non-candidate extension
        touch("README.md")

        assertEquals(
            listOf(
                "build/reports/detekt.xml",
                "build/test-results/test/TEST-a.xml",
                "coverage/lcov.info",
                "module/target/site/jacoco/jacoco.xml",
            ),
            discovered(ReportDiscovery()),
        )
    }

    @Test
    fun `root directory named like an excluded dir is still scanned`() {
        // Bitrise checks out at /bitrise/src; the root's own name ("src") must not abort the walk.
        val srcRoot = File(root, "src").also { it.mkdirs() }
        File(srcRoot, "app/build/reports/detekt.xml").also { it.parentFile.mkdirs() }.writeText("x")
        File(srcRoot, "app/src/main/Foo.kt").also { it.parentFile.mkdirs() }.writeText("x")

        assertEquals(
            listOf("app/build/reports/detekt.xml"),
            ReportDiscovery().discover(srcRoot)
                .map { it.relativeTo(srcRoot).path.replace(File.separatorChar, '/') },
        )
    }

    @Test
    fun `exclude globs drop matching candidates from the default scan`() {
        touch("build/reports/detekt.xml")
        touch("gradle-plugins/build/reports/plugin-development/validation-report.json") // included build's own report

        assertEquals(
            listOf("build/reports/detekt.xml"),
            discovered(ReportDiscovery(excludeGlobs = listOf("gradle-plugins/**"))),
        )
    }

    @Test
    fun `configured globs replace the default locations`() {
        touch("out/lint.xml")
        touch("out/deep/tests.xml")
        touch("build/reports/detekt.xml") // not in the globs -> not a candidate
        touch("exact/lcov.info")

        assertEquals(
            listOf("exact/lcov.info", "out/deep/tests.xml", "out/lint.xml"),
            discovered(ReportDiscovery(globs = listOf("out/**", "exact/lcov.info"))),
        )
    }

    @Test
    fun `globs only pick locations - non-report extensions are never candidates`() {
        touch("out/detekt.xml")
        touch("out/tests/index.html")            // a tool's own html report
        touch("out/binary/results.bin")          // gradle's binary test results
        touch("out/tests/style.css")

        assertEquals(
            listOf("out/detekt.xml"),
            discovered(ReportDiscovery(globs = listOf("out/**"))),
        )
    }

    @Test
    fun `globs still never match the tool's own output dir`() {
        touch("out/report.xml")
        touch("out/buildchecks/summary.json")

        assertEquals(
            listOf("out/report.xml"),
            discovered(ReportDiscovery(globs = listOf("out/**"), outputDir = "out/buildchecks")),
        )
    }
}
