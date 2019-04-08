package unit

import core.entity.BuildConfigDefault
import gradle.UseCaseFactory
import org.junit.Test

class UseCaseFactoryTests {
    @Test
    fun `can call public methods`() {
        val config = BuildConfigDefault()
        config.isPostActivated = true
        config.baseUrl = "http://bitbucket"
        config.allowUncommittedChanges = true
        config.statsBaseUrl = "http://stats"
        val di = UseCaseFactory(config)
        di.handleBuildFinishedUseCase()
        di.handleBuildStartedUseCase()
    }
}