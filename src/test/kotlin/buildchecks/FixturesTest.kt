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

    @Test
    fun `fixture files generated for phase 1 are present`() {
        Fixtures.file("eslint.sarif")                    // SARIF (eslint + @microsoft/eslint-formatter-sarif)
        Fixtures.file("lcov.info")                       // LCOV (c8)
        Fixtures.file("mocha-junit.xml")                 // JUnit XML with failures/skips (mocha-junit-reporter)
    }
}
