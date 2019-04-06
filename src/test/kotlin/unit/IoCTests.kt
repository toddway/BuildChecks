package unit

import core.entity.BuildConfigDefault
import data.IoC
import org.junit.Test

class IoCTests {
    @Test
    fun `can call public methods`() {
        val config = BuildConfigDefault()
        config.isPostActivated = true
        config.baseUrl = "http://bitbucket"
        config.allowUncommittedChanges = true
        config.statsBaseUrl = "http://stats"
        val di = IoC(config)
        di.handleBuildFinishedUseCase()
        di.handleBuildStartedUseCase()
    }
}