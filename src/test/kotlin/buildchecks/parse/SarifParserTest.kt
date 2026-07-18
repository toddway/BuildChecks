package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SarifParserTest {

    private val report = SarifParser().parse(Fixtures.text("eslint.sarif"))

    @Test
    fun `reads every result from the eslint run`() {
        assertEquals(7, report.findings.size)
        assertTrue(report.findings.all { it.tool == "ESLint" })
        assertEquals(4, report.findings.count { it.severity == Severity.ERROR })
        assertEquals(3, report.findings.count { it.severity == Severity.WARNING })
    }

    @Test
    fun `reads rule, message, and location with the file scheme stripped`() {
        val first = report.findings.first()
        assertEquals("eqeqeq", first.ruleId)
        assertEquals(Severity.WARNING, first.severity)
        assertEquals("Expected '===' and instead saw '=='.", first.message)
        assertEquals("/work/sample/src/calculator.js", first.location?.path)
        assertEquals(10, first.location?.line)
        assertEquals(9, first.location?.column)
    }

    @Test
    fun `reads error-level results`() {
        val last = report.findings.last()
        assertEquals("no-undef", last.ruleId)
        assertEquals(Severity.ERROR, last.severity)
        assertEquals("'undefinedVariable' is not defined.", last.message)
        assertEquals(15, last.location?.line)
    }
}
