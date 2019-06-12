package unit

import core.entity.BuildConfig
import core.entity.GitConfig
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.util.*

class BuildConfigTests {
    @Test
    fun `set all config props`() {
        val config = BuildConfig()
        config.allowUncommittedChanges = false
        config.authorization = "Asdfd"
        config.baseUrl = "Asdfd"
        config.buildUrl = "ASdfsadf"
        config.isPostActivated = true
        config.isPluginActivated = true
        config.isSuccess = false
        config.isPostActivated = true
        config.maxLintViolations = 435
        config.minCoveragePercent = 54.0
        config.statsBaseUrl = "adsfadsf"
        config.git = GitConfig()
        config.taskName = "adfs"
        config.buildStartTime = Date()
    }

    @Test
    fun `when valid file paths are configured, reportFiles returns a list of File objects for each`() {
        val config = BuildConfig()
        config.jacocoReports = listOf(
                "./src/test/testFiles/lint-results-prodRelease.xml",
                "./src/test/testFiles/detekt-checkstyle.xml",
                "./src/test/testFiles/cpdCheck-swift.xml"
        )
        config.allReports().count() shouldEqual 3
    }

    @Test
    fun `when invalid file paths are configured, reportFiles returns no File objects`() {
        val config = BuildConfig()
        config.jacocoReports = "./src/test/testFiles/sdfdf.xml, ./src/test/testFiles/sdfd.xml, ".split(",")
        config.allReports().count() shouldEqual 0
    }

    @Test
    fun `when completedMessage is called, it returns a string message`() {
        val config = BuildConfig()
        config.completedMessage() shouldNotBe null
    }
}
