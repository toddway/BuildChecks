package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.entity.BuildConfigDefault
import core.usecase.*
import Registry
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Before
import org.junit.Test

class HandleBuildFinishedUseCaseTests {
    val statsDatasource : PostStatsUseCase.Datasource = mock()
    val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
    val statusDatasource : PostStatusUseCase.Datasource = mock()
    val postStatusUseCase = PostStatusUseCase(listOf(statusDatasource), mock(), mock())
    val config = BuildConfigDefault().apply { reports = "./src/test/testFiles" }
    val registry = Registry(config)
    val summaries = registry.provideGetSummaryUseCases()


    @Before fun before() {
        When calling statusDatasource.name() itReturns "asdf"
        When calling statusDatasource.isActive() itReturns true
        When calling statusDatasource.isRemote() itReturns false
        When calling statusDatasource.post(any(), any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)
        config.artifactsPath = "./build/testFiles/buildChecks"
        config.artifactsBranch = ""
    }

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        config.isPluginActivated = false
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        VerifyNotCalled on statusDatasource that statusDatasource.post(any(), any(), any()) was called
    }

    @Test
    fun `when the build finishes unsuccessfully and plugin is activated, post failure status for each type`() {
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        verify(statusDatasource, times(summaries.size)).post(any(), any(), any())
    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success status for each type`() {
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        verify(statusDatasource, times(summaries.size)).post(any(), any(), any())
        Verify on statsDatasource that statsDatasource.postStats(any()) was called
    }
}

