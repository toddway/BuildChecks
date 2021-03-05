package unit

import Registry
import core.entity.BuildConfigDefault
import core.usecase.HandleBuildFinishedUseCase
import core.usecase.PostStatsUseCase
import core.usecase.PostStatusUseCase
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test

class HandleBuildFinishedUseCaseTests {
    val statsDatasource : PostStatsUseCase.Datasource = mockk()
    val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
    val statusDatasource : PostStatusUseCase.Datasource = mockk()
    val postStatusUseCase = PostStatusUseCase(listOf(statusDatasource), mockk(), mockk())
    val config = BuildConfigDefault().apply { reports = "./src/test/testFiles" }
    val registry = Registry(config)
    val summaries = registry.provideGetSummaryUseCases()


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

//    @Test
//    fun `when plugin is not activated, nothing is posted`() {
//        config.isChecksActivated = false
//        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())
//
//        usecase.invoke()
//
//        VerifyNotCalled on statusDatasource that statusDatasource.post(any(), any(), any()) was called
//    }
//
//    @Test
//    fun `when the build finishes unsuccessfully and plugin is activated, post failure status for each type`() {
//        config.isChecksActivated = true
//        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())
//
//        usecase.invoke()
//
//        verify(statusDatasource, times(summaries.size)).post(any(), any(), any())
//    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success status for each type`() {
        config.isChecksActivated = true
        print("summaries: " + summaries.filter { it.value() != null }.size)
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        io.mockk.verify(exactly = summaries.size) { statusDatasource.post(any(), any(), any())}
        io.mockk.verify { statsDatasource.postStats(any()) }
    }
}

