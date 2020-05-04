package unit

import core.entity.BuildConfigDefault
import core.entity.Log
import core.usecase.HandleBuildStartedUseCase
import core.usecase.PostStatusUseCase
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Test

class HandleBuildStartedUseCaseTests {

    @Test
    fun `when the plugin is disabled, nothing is posted`() {
        val postBuildStatus : PostStatusUseCase = mock()
        val config = BuildConfigDefault()
        config.isChecksActivated = false
        config.log = Log(mock())
        val usecase = HandleBuildStartedUseCase(postBuildStatus, config)

        usecase.invoke()

        VerifyNotCalled on postBuildStatus that postBuildStatus.post(any(), any(), any()) was called
        Assert.assertEquals(null, config.log)
    }

    @Test
    fun `when the build is started and the plugin is activated, post a build status`() {
        val postBuildStatus : PostStatusUseCase = mock()
        val config = BuildConfigDefault()
        config.isChecksActivated = true
        config.isPostActivated = true
        config.log = Log(mock())
        val usecase = HandleBuildStartedUseCase(postBuildStatus, config)

        usecase.invoke()

        Verify on postBuildStatus that postBuildStatus.post(any(), any(), any()) was called
        Assert.assertNotEquals(null, config.log)
    }
}

