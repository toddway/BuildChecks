package unit

import core.entity.BuildConfigDefault
import core.Registry
import core.entity.ProjectConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class RegistryTests {
    val config = BuildConfigDefault().apply {
        isPostActivated =  true
        baseUrl = "http://bitbucket"
        allowUncommittedChanges = true
        statsBaseUrl = "http://stats"
    }
    val projectConfig = mockk<ProjectConfig>(relaxed = true).apply {
        every { createBuildChecksConfig() } returns config
    }
    val registry = Registry(projectConfig)

//    @Test
//    fun `when registry is initialized, then tasks are initialized`() {
//        verify {
//            projectConfig.initPostChecksTask()
//            projectConfig.initPrintChecksTask()
//            projectConfig.initPushArtifactsTask()
//        }
//    }

    @Test
    fun `when provideBuildStarted is called, then config is initialized with project config`() {
        registry.provideBuildStartedUseCase()

        config.apply {
            Assert.assertEquals(projectConfig.taskNameString(), taskName)
            Assert.assertEquals(projectConfig.isPostChecksActivated(), isPostActivated)
            Assert.assertEquals(projectConfig.isChecksActivated(), isChecksActivated)
            Assert.assertEquals(projectConfig.isPushArtifactsActivated(), isPushActivated)
            Assert.assertEquals(projectConfig.logger(), log)
        }
    }

    @Test
    fun `when provideBuildFinished is called, then config isSuccess has the expected value`(){
        registry.provideBuildFinishedUseCase(true)
        Assert.assertEquals(true, config.isSuccess)

        registry.provideBuildFinishedUseCase(false)
        Assert.assertEquals(false, config.isSuccess)
    }
}