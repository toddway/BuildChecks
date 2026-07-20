package buildchecks.cli

import buildchecks.gate.OriginKind
import buildchecks.model.IngestedFile
import buildchecks.model.ParsedReport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OriginsTest {

    private fun file(path: String, format: String, tool: String? = null) =
        IngestedFile(path, format, 0, ParsedReport(tool = tool))

    @Test
    fun `origin is the prefix before the build-output marker`() {
        assertEquals("services/auth", origin("services/auth/build/reports/detekt.sarif"))
        assertEquals("modules/a", origin("modules/a/target/site/jacoco/jacoco.xml"))
        assertEquals("frontend", origin("frontend/coverage/lcov.info"))
        assertEquals("frontend", origin("frontend/lcov.info"))
    }

    @Test
    fun `root and aggregated reports collapse to the root origin`() {
        assertEquals(ROOT_ORIGIN, origin("build/reports/detekt.sarif"))
        assertEquals(ROOT_ORIGIN, origin("coverage/lcov.info"))
        assertEquals(ROOT_ORIGIN, origin("lcov.info"))
        // no recognizable marker at all also degrades to the root origin
        assertEquals(ROOT_ORIGIN, origin("weird/place/report.xml"))
    }

    @Test
    fun `kind is the producing tool for findings, else the format`() {
        assertEquals("detekt", kind(file("build/reports/detekt.sarif", "sarif", tool = "detekt")))
        assertEquals("checkstyle", kind(file("build/reports/ktlint.xml", "checkstyle")))
        assertEquals("jacoco", kind(file("build/reports/jacoco.xml", "jacoco")))
    }

    @Test
    fun `a single-module repo collapses to one origin`() {
        val manifest = presentManifest(listOf(
            file("build/reports/detekt.sarif", "sarif", tool = "detekt"),
            file("build/test-results/test/TEST-a.xml", "junit"),
            file("build/reports/jacoco/jacoco.xml", "jacoco"),
        ))
        assertEquals(
            setOf(OriginKind(".", "detekt"), OriginKind(".", "junit"), OriginKind(".", "jacoco")),
            manifest,
        )
    }

    @Test
    fun `multi-origin reports key by their own module`() {
        val manifest = presentManifest(listOf(
            file("services/auth/build/reports/detekt.sarif", "sarif", tool = "detekt"),
            file("services/web/build/reports/detekt.sarif", "sarif", tool = "detekt"),
        ))
        assertEquals(
            setOf(OriginKind("services/auth", "detekt"), OriginKind("services/web", "detekt")),
            manifest,
        )
    }

    @Test
    fun `origin counts group reports per module`() {
        val counts = originCounts(listOf(
            file("services/auth/build/reports/detekt.sarif", "sarif", tool = "detekt"),
            file("services/auth/build/reports/jacoco.xml", "jacoco"),
            file("services/web/build/reports/detekt.sarif", "sarif", tool = "detekt"),
        ))
        assertEquals(mapOf("services/auth" to 2, "services/web" to 1), counts)
    }
}
