package buildchecks.parse

import buildchecks.Fixtures
import buildchecks.model.TestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Reports harvested from a full Gradle build of the Shelf project (~/dev/Shelf), July 2026. */
class ShelfReportsTest {

    @Test
    fun `detekt checkstyle report from a real gradle build`() {
        val report = CheckstyleParser().parse(Fixtures.text("shelf/detekt.xml"))
        assertEquals(8, report.findings.size)
        assertTrue(report.findings.first().location!!.path.endsWith("src/commonMain/kotlin/com/toddway/shelf/Shelf.kt"))
    }

    @Test
    fun `cpd report with no duplications`() {
        assertEquals(0, CpdParser().parse(Fixtures.text("shelf/cpdCheck.xml")).findings.size)
    }

    @Test
    fun `gradle junit results including a real failure`() {
        val jvmTests = JunitParser().parse(Fixtures.text("shelf/TEST-com.toddway.shelf.JvmTests.xml"))
        assertEquals(4, jvmTests.tests.size)
        val failed = jvmTests.tests.single { it.status == TestStatus.FAILED }
        assertEquals("test_with_ktor[jvm]", failed.name)
        assertTrue(failed.message!!.contains("401 Unauthorized"))

        val shelfTests = JunitParser().parse(Fixtures.text("shelf/TEST-com.toddway.shelf.ShelfTests.xml"))
        assertEquals(16, shelfTests.tests.size)
        assertTrue(shelfTests.tests.all { it.status == TestStatus.PASSED })
    }

    @Test
    fun `jacoco report from a real gradle build`() {
        val coverage = JacocoParser().parse(Fixtures.text("shelf/jvmTestCoverage.xml")).coverage!!
        assertEquals(6, coverage.files.size)
        assertEquals(74, coverage.linesTotal)
        assertEquals(69, coverage.linesCovered)
        assertTrue(coverage.files.any { it.path == "com/toddway/shelf/Shelf.kt" })
    }
}
