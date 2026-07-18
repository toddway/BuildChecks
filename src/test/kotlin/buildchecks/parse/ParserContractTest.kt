package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.cli.reportParsers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Shared contract for all parsers: sniffing is exclusive and every claimed fixture parses. */
class ParserContractTest {

    private val parsers = reportParsers()

    private val claimedFixtures = mapOf(
        "eslint.sarif" to "sarif",
        "reports/karma.xml" to "junit",
        "mocha-junit.xml" to "junit",
        "coverage.xml" to "jacoco",
        "reports/coverage/coverage.xml" to "jacoco",
        "cobertura-coverage.xml" to "cobertura",
        "lcov.info" to "lcov",
        "detekt-checkstyle.xml" to "checkstyle",
        "reports/detekt-checkstyle.xml" to "checkstyle",
        "cpdCheck-swift.xml" to "cpd",
        "cpdCheck-ts.xml" to "cpd",
        "reports/cpd/cpdCheck.xml" to "cpd",
        "shelf/TEST-com.toddway.shelf.JvmTests.xml" to "junit",
        "shelf/TEST-com.toddway.shelf.ShelfTests.xml" to "junit",
        "shelf/detekt.xml" to "checkstyle",
        "shelf/cpdCheck.xml" to "cpd",
        "shelf/jvmTestCoverage.xml" to "jacoco",
    )

    private val unclaimedFixtures = listOf(
        "pmd.xml",                      // PMD's native format is not ingested (SARIF is the lint path)
        "lint-results-prodRelease.xml", // Android Lint's native format is not ingested
        "hello.txt",
        "reports/detekt-plain.txt",
        "reports/cpd/cpdCheck.text",
        "reports/detekt-report.html",
    )

    @Test
    fun `every recognized fixture is claimed by exactly the expected parser`() {
        for ((fixture, expected) in claimedFixtures) {
            val claiming = parsers.filter { it.claims(Fixtures.text(fixture)) }.map { it.format }
            assertEquals(listOf(expected), claiming, fixture)
        }
    }

    @Test
    fun `unrecognized files are claimed by no parser`() {
        for (fixture in unclaimedFixtures) {
            val claiming = parsers.filter { it.claims(Fixtures.text(fixture)) }.map { it.format }
            assertEquals(emptyList<String>(), claiming, fixture)
        }
    }

    @Test
    fun `every claimed fixture parses without error`() {
        for ((fixture, format) in claimedFixtures) {
            parsers.single { it.format == format }.parse(Fixtures.text(fixture))
        }
    }
}
