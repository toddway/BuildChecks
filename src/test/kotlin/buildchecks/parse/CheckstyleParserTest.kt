package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CheckstyleParserTest {

    private val parser = CheckstyleParser()

    @Test
    fun `reads every error element with its severity`() {
        val report = parser.parse(Fixtures.text("detekt-checkstyle.xml"))
        assertEquals(48, report.findings.size)
        assertEquals(6, report.findings.count { it.severity == Severity.ERROR })
        assertEquals(37, report.findings.count { it.severity == Severity.WARNING })
        assertEquals(5, report.findings.count { it.severity == Severity.INFO })
    }

    @Test
    fun `reads rule, message, and location`() {
        val first = parser.parse(Fixtures.text("detekt-checkstyle.xml")).findings.first()
        assertEquals("checkstyle", first.tool)
        assertEquals("detekt.LargeClass", first.ruleId)
        assertEquals(Severity.WARNING, first.severity)
        assertEquals("java/vml/com/sorry/app/post/PostActivity.kt", first.location?.path)
        assertEquals(41, first.location?.line)
        assertEquals(1, first.location?.column)
    }

    @Test
    fun `reads a second detekt-produced fixture`() {
        assertEquals(7, parser.parse(Fixtures.text("reports/detekt-checkstyle.xml")).findings.size)
    }
}
