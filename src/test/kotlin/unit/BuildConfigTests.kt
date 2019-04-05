package unit

import core.entity.BuildConfigDefault
import core.entity.GitConfigDefault
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.util.*

class BuildConfigTests {
    @Test
    fun `set all config props`() {
        val config = BuildConfigDefault()
        config.allowUncommittedChanges = false
        config.androidLintReports = "asdf"
        config.authorization = "Asdfd"
        config.baseUrl = "Asdfd"
        config.buildUrl = "ASdfsadf"
        config.checkstyleReports = "adsf"
        config.coberturaReports = "Asdf"
        config.cpdReports = "ADSf"
        config.isPostActivated = true
        config.isPluginActivated = true
        config.isSuccess = false
        config.isPostActivated = true
        config.maxLintViolations = 435
        config.minCoveragePercent = 54.0
        config.statsBaseUrl = "adsfadsf"
        config.git = GitConfigDefault()
        config.taskName = "adfs"
        config.buildStartTime = Date()

    }

    @Test
    fun `when valid file paths are configured, reportFiles returns a list of File objects for each`() {
        val config = BuildConfigDefault()
        config.jacocoReports = "./src/test/testFiles/lint-results-prodRelease.xml, ./src/test/testFiles/detekt-checkstyle.xml, ./src/test/testFiles/cpdCheck-swift.xml"
        config.reportFiles().count() shouldEqual 3
    }

    @Test
    fun `when invalid file paths are configured, reportFiles returns no File objects`() {
        val config = BuildConfigDefault()
        config.jacocoReports = "./src/test/testFiles/sdfdf.xml, ./src/test/testFiles/sdfd.xml, "
        config.reportFiles().count() shouldEqual 0
    }

    @Test
    fun `when completedMessage is called, it returns a string message`() {
        val config = BuildConfigDefault()
        config.completedMessage() shouldNotBe null
    }
}
