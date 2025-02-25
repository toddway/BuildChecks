package unit

import core.entity.BuildConfigDefault
import core.entity.Log
import core.usecase.HandleBuildStartedUseCase
import core.usecase.PostStatusUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test

//class HandleBuildStartedUseCaseTests {
//    val postBuildStatus : PostStatusUseCase = mockk()
//    val logger = mockk<Log>()
//    val config = BuildConfigDefault().apply { log = logger }
//    val usecase = HandleBuildStartedUseCase(postBuildStatus, config)
//
//    @Test
//    fun `when the plugin is disabled, nothing is posted`() {
//        config.isChecksActivated = false
//
//        usecase.invoke()
//
//        verify(exactly = 0) { postBuildStatus.post(any(), any(), any()) }
//        Assert.assertEquals(null, config.log)
//    }
//
//    @Test
//    fun `when the build is started and the plugin is activated, post a build status`() {
//        config.isChecksActivated = true
//        config.isPostActivated = true
//
//        usecase.invoke()
//
//        verify { postBuildStatus.post(any(), any(), any()) }
//        Assert.assertNotEquals(null, config.log)
//    }
//
//    @Test
//    fun `when usecase is invoked, then the info log is called with the expected message`() {
//        val expectedMessage = "${usecase::class.simpleName} invoked"
//
//        usecase.invoke()
//
//        verify { logger.info(expectedMessage) }
//    }
//}
//
