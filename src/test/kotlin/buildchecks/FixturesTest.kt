package buildchecks

import org.junit.jupiter.api.Test

class FixturesTest {

    @Test
    fun `fixture files carried over from v3 are present`() {
        Fixtures.file("coverage.xml")                    // JaCoCo
        Fixtures.file("cobertura-coverage.xml")          // Cobertura
        Fixtures.file("detekt-checkstyle.xml")           // Checkstyle (via detekt)
        Fixtures.file("lint-results-prodRelease.xml")    // Android Lint
        Fixtures.file("pmd.xml")                         // PMD
        Fixtures.file("cpdCheck-swift.xml")              // CPD
        Fixtures.file("reports/karma.xml")               // JUnit XML (karma)
        Fixtures.file("reports")                         // report tree for discovery tests
    }
}
