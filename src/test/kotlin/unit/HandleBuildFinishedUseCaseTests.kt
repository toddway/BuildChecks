package unit

import core.entity.BuildConfigDefault
import core.buildstate.HandleBuildFailedUseCase
import core.buildstate.HandleBuildFinishedUseCase
import core.buildstate.HandleBuildSuccessUseCase
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildFinishedUseCaseTests {

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = BuildConfigDefault()
        config.isPluginActivated = false
        config.isSuccess = false
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, mock())

        usecase.invoke()

        VerifyNotCalled on handleBuildFailedUseCase that handleBuildFailedUseCase.invoke() was called
    }

    @Test
    fun `when the build finishes unsuccessfully and plugin is activated, post failure`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = BuildConfigDefault()
        config.isPluginActivated = true
        config.isSuccess = false
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, mutableListOf())

        usecase.invoke()

        Verify on handleBuildFailedUseCase that handleBuildFailedUseCase.invoke() was called
    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success`() {
        val handleBuildFailedUseCase : HandleBuildFailedUseCase =  mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = BuildConfigDefault()
        config.isPluginActivated = true
        config.isSuccess = true
        val usecase = HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, mutableListOf())

        usecase.invoke()

        //Verify on postStatusUseCase that postStatusUseCase.success(any(), any()) was called
        Verify on handleBuildSuccessUseCase that handleBuildSuccessUseCase.invoke() was called
    }
}

