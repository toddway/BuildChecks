package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CpdParserTest {

    private val parser = CpdParser()

    @Test
    fun `reads each duplication as one finding with all its locations`() {
        val report = parser.parse(Fixtures.text("cpdCheck-swift.xml"))
        assertEquals(2, report.findings.size)

        val first = report.findings.first()
        assertEquals("cpd", first.tool)
        assertEquals("duplicated-code", first.ruleId)
        assertEquals(Severity.WARNING, first.severity)
        assertEquals("12 duplicated lines (48 tokens) in 2 places", first.message)
        assertEquals(48, first.duplicatedTokens)
        assertEquals("/Users/tway/dev/qt-ios/QuikTrip/Shared/RoundCorner.swift", first.location?.path)
        assertEquals(5, first.location?.line)
        assertEquals(listOf(16), first.relatedLocations.map { it.line })
        assertTrue(first.snippet!!.contains("class RoundCornerButton: UIButton, RoundCorner"))
    }

    @Test
    fun `reads a typescript cpd report`() {
        assertEquals(9, parser.parse(Fixtures.text("cpdCheck-ts.xml")).findings.size)
    }

    @Test
    fun `a report with no duplications yields no findings`() {
        assertEquals(0, parser.parse(Fixtures.text("reports/cpd/cpdCheck.xml")).findings.size)
    }
}
