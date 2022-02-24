package unit

import core.Registry
import core.entity.*
import core.usecase.*
import io.mockk.*
import org.junit.Before
import org.junit.Test
import pushArtifacts

class HandleBuildFinishedUseCaseTests {
    val postStatsUseCase = mockk<PostStatsUseCase>()
    val statusDatasource : PostStatusUseCase.Datasource = mockk()
    val postStatusUseCase = PostStatusUseCase(listOf(statusDatasource), mockk(), mockk())
    val config = BuildConfigDefault().apply {
        reports = "./src/test/testFiles"
        log = mockk()
        artifactsPath = "./build/testFiles/buildChecks"
        artifactsBranch = ""
    }
    val projectConfig = mockk<ProjectConfig>().apply { every { createBuildChecksConfig() } returns config }
    val registry = Registry(projectConfig)
    val summaries = registry.provideGetSummaryUseCases()
    val messageQueue = mutableListOf<Message>()
    val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, messageQueue)


    @Before fun before() {
        mockkStatic("core.usecase.GetSummaryUseCaseKt")
        mockkStatic("core.usecase.HtmlReportKt").also {
            every { config.writeBuildReports(any(), any()) } answers {}
        }
        mockkStatic("core.entity.MessageKt")
        mockkStatic("GitsKt")
    }

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        config.isChecksActivated = false

        usecase.invoke()

        verify(exactly = 0) {
            summaries.throwIfUnsuccessful(config)
            summaries.postStatuses(postStatusUseCase)
            summaries.postStats(config, postStatsUseCase)
            config.pushArtifacts(messageQueue)
            messageQueue.printAll()
        }
    }

    @Test
    fun `when usecase is invoked and plugin is activated, then`() {
        config.isChecksActivated = true

        usecase.invoke()

        verify(ordering = Ordering.ORDERED) {
            summaries.postStatuses(postStatusUseCase)
            summaries.postStats(config, postStatsUseCase)
            //config.writeBuildReports(any())
            messageQueue.printAll()
            summaries.throwIfUnsuccessful(config)
            config.pushArtifacts(messageQueue)
        }
    }

    @Test
    fun `when usecase is invoked, then the info log is called with the expected message`() {
        val expectedMessage = "${usecase::class.simpleName} invoked"

        usecase.invoke()

        verify { config.log?.info(expectedMessage) }
    }

    @Test
    fun `when usecase is invoked and open task is active, then open`() {
        usecase.invoke()


    }
}

