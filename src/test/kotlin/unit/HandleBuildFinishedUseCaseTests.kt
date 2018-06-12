package unit

import core.entity.ConfigEntityDefault
import core.usecase.HandleBuildFailedUseCase
import core.usecase.HandleBuildFinishedUseCase
import core.usecase.HandleBuildSuccessUseCase
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildFinishedUseCaseTests {

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPluginActivated = false
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(false)

        VerifyNotCalled on handleBuildFailedUseCase that handleBuildFailedUseCase.invoke() was called
    }

    @Test
    fun `when the build finishes unsuccessfully and plugin is activated, post failure`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(false)

        Verify on handleBuildFailedUseCase that handleBuildFailedUseCase.invoke() was called
    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(true)

        //Verify on postStatusUseCase that postStatusUseCase.success(any(), any()) was called
        Verify on handleBuildSuccessUseCase that handleBuildSuccessUseCase.invoke() was called
    }
}

