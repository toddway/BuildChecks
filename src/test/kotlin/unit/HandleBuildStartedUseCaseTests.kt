package unit

import core.entity.ConfigEntityDefault
import core.usecase.HandleBuildStartedUseCase
import core.usecase.SetStatusUseCase
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildStartedUseCaseTests {

    @Test
    fun `when post is disabled, nothing is posted`() {
        val setBuildStatus : SetStatusUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPostEnabled = false
        val usecase = HandleBuildStartedUseCase(setBuildStatus, config)

        usecase.invoke()

        VerifyNotCalled on setBuildStatus that setBuildStatus.pending(any(), any()) was called
    }

    @Test
    fun `when the build is started and isPost is true, post a pending build status`() {
        val setBuildStatus : SetStatusUseCase = mock()
        val config = ConfigEntityDefault()
        config.isPostEnabled = true
        val usecase = HandleBuildStartedUseCase(setBuildStatus, config)

        usecase.invoke()

        Verify on setBuildStatus that setBuildStatus.pending(any(), any()) was called
    }
}

