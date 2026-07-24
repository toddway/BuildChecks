package buildchecks.cli

import buildchecks.gate.OriginKind
import buildchecks.model.Freshness
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

    @Test
    fun `changed files map to the longest known origin that prefixes them`() {
        val files = listOf(
            file("services/auth/build/reports/detekt.sarif", "sarif", tool = "detekt"),
            file("services/web/build/reports/detekt.sarif", "sarif", tool = "detekt"),
        )
        val changed = setOf(
            "services/auth/src/main/kotlin/Auth.kt",
            "services/web/src/App.kt",
            "README.md", // no module prefix -> root origin
        )
        assertEquals(setOf("services/auth", "services/web", ROOT_ORIGIN), changedOrigins(changed, files))
    }

    @Test
    fun `a module known only from the baseline manifest still counts as touched`() {
        // payments emitted no report this run, but the manifest says it should have one — so a change
        // to it must not collapse into root and look measured
        val files = listOf(file("services/auth/build/reports/detekt.sarif", "sarif", tool = "detekt"))
        assertEquals(
            setOf("services/payments"),
            changedOrigins(setOf("services/payments/src/Pay.kt"), files, manifestOrigins = setOf("services/payments")),
        )
    }

    @Test
    fun `fresh origins are those with a non age-outlier report this run`() {
        val files = listOf(
            file("services/auth/build/reports/detekt.sarif", "sarif", tool = "detekt"),
            file("services/web/build/reports/detekt.sarif", "sarif", tool = "detekt"),
        )
        // web's report is a stale outlier, auth's is fresh, payments produced nothing at all
        val freshness = Freshness(
            mapOf(
                "services/auth/build/reports/detekt.sarif" to 0L,
                "services/web/build/reports/detekt.sarif" to 90L,
            ),
            toleranceMinutes = 15,
        )
        val touched = setOf("services/auth", "services/web", "services/payments")
        assertEquals(setOf("services/auth"), freshChangedOrigins(touched, files, freshness))
    }
}
