package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.model.TestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JunitParserTest {

    private val parser = JunitParser()

    @Test
    fun `reads a karma report where every test passes`() {
        val report = parser.parse(Fixtures.text("reports/karma.xml"))
        assertEquals(16, report.tests.size)
        assertTrue(report.tests.all { it.status == TestStatus.PASSED })
        val first = report.tests.first()
        assertEquals("PhantomJS_2_1_1_(Mac_OS_0_0_0)..com.toddway.shelf > ShelfTests", first.suite)
        assertEquals(
            "com.toddway.shelf > ShelfTests when_an_object_is_put_then_the_stored_value_is_equal_to_the_original",
            first.name,
        )
        assertEquals(0.012, first.durationSeconds)
    }

    @Test
    fun `reads failures and skips from a mocha report`() {
        val report = parser.parse(Fixtures.text("mocha-junit.xml"))
        assertEquals(6, report.tests.size)
        assertEquals(3, report.tests.count { it.status == TestStatus.PASSED })
        assertEquals(2, report.tests.count { it.status == TestStatus.FAILED })
        assertEquals(1, report.tests.count { it.status == TestStatus.SKIPPED })

        val failed = report.tests.first { it.status == TestStatus.FAILED }
        assertEquals("calculator divides by zero", failed.name)
        assertEquals("division by zero", failed.message)

        val skipped = report.tests.single { it.status == TestStatus.SKIPPED }
        assertEquals("greeter greets in french", skipped.name)
    }
}
