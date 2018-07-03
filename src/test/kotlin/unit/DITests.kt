package unit

import core.entity.BuildConfigDefault
import data.DI
import org.junit.Test

class DITests {
    @Test
    fun `can call public methods`() {
        val config = BuildConfigDefault()
        config.isPostActivated = true
        config.baseUrl = "http://bitbucket"
        config.allowUncommittedChanges = true
        config.statsBaseUrl = "http://stats"
        val di = DI(config)
        di.handleBuildFinishedUseCase()
        di.handleBuildStartedUseCase()
    }
}