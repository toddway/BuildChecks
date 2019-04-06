package unit

import core.entity.BuildConfigDefault
import core.usecase.HandleBuildStartedUseCase
import core.usecase.PostStatusUseCase
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildStartedUseCaseTests {

    @Test
    fun `when the plugin is disabled, nothing is posted`() {
        val postBuildStatus : PostStatusUseCase = mock()
        val config = BuildConfigDefault()
        config.isPluginActivated = false
        val usecase = HandleBuildStartedUseCase(postBuildStatus, config)

        usecase.invoke()

        VerifyNotCalled on postBuildStatus that postBuildStatus.post(any(), any(), any()) was called
    }

    @Test
    fun `when the build is started and the plugin is activated, post a build status`() {
        val postBuildStatus : PostStatusUseCase = mock()
        val config = BuildConfigDefault()
        config.isPluginActivated = true
        config.isPostActivated = true
        val usecase = HandleBuildStartedUseCase(postBuildStatus, config)
        //When calling postBuildStatus.sources itReturns listOf(ConsoleDatasource())

        usecase.invoke()

        Verify on postBuildStatus that postBuildStatus.post(any(), any(), any()) was called
    }
}

