package unit

import core.entity.BuildConfigDefault
import data.Registry
import org.junit.Test

class RegistryTests {
    val config = BuildConfigDefault().apply {
        isPostActivated =  true
        baseUrl = "http://bitbucket"
        allowUncommittedChanges = true
        statsBaseUrl = "http://stats"
    }
    val registry = Registry(config)

    @Test
    fun `can call public methods`() {
        registry.provideHandleBuildFinishedUseCase()
        registry.provideHandleBuildStartedUseCase()
    }
}