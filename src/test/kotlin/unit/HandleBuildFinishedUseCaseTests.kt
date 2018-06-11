package unit

import core.entity.ConfigEntityDefault
import core.usecase.HandleBuildFinishedUseCase
import core.usecase.HandleBuildSuccessUseCase
import core.usecase.SetStatusUseCase
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildFinishedUseCaseTests {

    @Test
    fun `when post is disabled, nothing is posted`() {
        val setStatusUseCase : SetStatusUseCase = mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPostEnabled = false
        val usecase = HandleBuildFinishedUseCase(setStatusUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(false)

        VerifyNotCalled on setStatusUseCase that setStatusUseCase.failure(any(), any()) was called
    }

    @Test
    fun `when the build finishes unsuccessfully and post is enabled, post failure`() {
        val setStatusUseCase : SetStatusUseCase = mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPostEnabled = true
        val usecase = HandleBuildFinishedUseCase(setStatusUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(false)

        Verify on setStatusUseCase that setStatusUseCase.failure(any(), any()) was called
    }

    @Test
    fun `when the build finishes successfully and post is enabled, handle success`() {
        val setStatusUseCase : SetStatusUseCase = mock()
        val handleBuildSuccessUseCase : HandleBuildSuccessUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPostEnabled = true
        val usecase = HandleBuildFinishedUseCase(setStatusUseCase, handleBuildSuccessUseCase, config)

        usecase.invoke(true)

        Verify on setStatusUseCase that setStatusUseCase.success(any(), any()) was called
        Verify on handleBuildSuccessUseCase that handleBuildSuccessUseCase.invoke() was called
    }
}

