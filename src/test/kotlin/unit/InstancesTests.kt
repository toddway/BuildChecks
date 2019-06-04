package unit

import core.entity.BuildConfigDefault
import data.Instances
import org.junit.Test

class InstancesTests {
    @Test
    fun `can call public methods`() {
        val config = BuildConfigDefault()
        config.isPostActivated = true
        config.baseUrl = "http://bitbucket"
        config.allowUncommittedChanges = true
        config.statsBaseUrl = "http://stats"
        val di = Instances(config)
        di.handleBuildFinishedUseCase
        di.handleBuildStartedUseCase
    }
}