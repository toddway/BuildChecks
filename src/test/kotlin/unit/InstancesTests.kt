package unit

import core.entity.BuildConfig
import data.Instances
import org.junit.Test

class InstancesTests {
    @Test
    fun `can call public methods`() {
        val config = BuildConfig()
        config.isPostActivated = true
        config.baseUrl = "http://bitbucket"
        config.allowUncommittedChanges = true
        config.statsBaseUrl = "http://stats"
        val di = Instances(config)
        di.handleBuildFinishedUseCase
        di.handleBuildStartedUseCase
    }
}