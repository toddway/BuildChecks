package unit

import Registry
import core.entity.BuildConfigDefault
import core.entity.ProjectConfig
import core.usecase.HandleBuildFinishedUseCase
import core.usecase.PostStatsUseCase
import core.usecase.PostStatusUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test

class HandleBuildFinishedUseCaseTests {
    val statsDatasource : PostStatsUseCase.Datasource = mockk()
    val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
    val statusDatasource : PostStatusUseCase.Datasource = mockk()
    val postStatusUseCase = PostStatusUseCase(listOf(statusDatasource), mockk(), mockk())
    val config = BuildConfigDefault().apply {
        reports = "./src/test/testFiles"
        log = mockk()
    }
    val projectConfig = mockk<ProjectConfig>().apply { every { createBuildChecksConfig() } returns config }
    val registry = Registry(projectConfig)
    val summaries = registry.provideGetSummaryUseCases()
    val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())


    @Before fun before() {
        every { statusDatasource.name() } returns "asdf"
        every { statusDatasource.isActive() } returns true
        every { statusDatasource.isRemote() } returns false
        every { statusDatasource.post(any(), any(), any()) } returns Observable.just(true)
        every { statsDatasource.postStats(any()) } returns Observable.just(true)
        every { statsDatasource.postStats(any()) } returns Observable.just(true)
        config.artifactsPath = "./build/testFiles/buildChecks"
        config.artifactsBranch = ""
    }

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        config.isChecksActivated = false

        usecase.invoke()

        verify(exactly = 0) { statusDatasource.post(any(), any(), any()) }
    }

    @Test
    fun `when the build finishes unsuccessfully and plugin is activated, post failure status for each type`() {
        config.isChecksActivated = true

        usecase.invoke()

        verify(exactly = summaries.size) { statusDatasource.post(any(), any(), any()) }
    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success status for each type`() {
        config.isChecksActivated = true

        usecase.invoke()

        verify(exactly = summaries.size) { statusDatasource.post(any(), any(), any())}
        verify { statsDatasource.postStats(any()) }
    }

    @Test
    fun `when usecase is invoked, then the info log is called with the expected message`() {
        val expectedMessage = "${usecase::class.simpleName} invoked"

        usecase.invoke()

        verify { config.log?.info(expectedMessage) }
    }
}

