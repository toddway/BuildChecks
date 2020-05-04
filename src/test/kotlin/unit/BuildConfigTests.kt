package unit

import core.entity.BuildConfigDefault
import core.entity.GitConfigDefault
import core.toXmlDocuments
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.util.*

class BuildConfigTests {
    @Test
    fun `set all config props`() {
        val config = BuildConfigDefault()
        config.allowUncommittedChanges = false
        config.authorization = "Asdfd"
        config.baseUrl = "Asdfd"
        config.buildUrl = "ASdfsadf"
        config.isPostActivated = true
        config.isChecksActivated = true
        config.isSuccess = false
        config.isPostActivated = true
        config.maxLintViolations = 435
        config.minCoveragePercent = 54.0
        config.statsBaseUrl = "adsfadsf"
        config.git = GitConfigDefault()
        config.taskName = "adfs"
        config.buildStartTime = Date()
        config.reports = "./src/test/testFiles"
        config.artifactsPath = "./build/testFiles/buildChecks"
    }

    @Test
    fun `when valid file paths are configured, reportFiles returns a list of File objects for each`() {
        val config = BuildConfigDefault()
        config.reports = "./src/test/testFiles"
        config.reportFiles().toXmlDocuments().size shouldEqual 11
    }

    @Test
    fun `when invalid file paths are configured, reportFiles returns no File objects`() {
        val config = BuildConfigDefault()
        config.reports = "./src/test/testFiles/nowhere"
        config.reportFiles().toXmlDocuments().size shouldEqual 0
    }

    @Test
    fun `when completedMessage is called, it returns a string message`() {
        val config = BuildConfigDefault()
        config.completedMessage() shouldNotBe null
    }

    @Test fun test() {
        val config = BuildConfigDefault()
        //println(config.reportDirs().copyInto(config.artifactsDir()).path)
        //val history = config.csvToMapsList()
        //println(history.jsArrayItemsFrom("coverage"))
    }
}





