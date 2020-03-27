package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.entity.BuildConfigDefault
import core.toDocumentList
import core.usecase.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HandleBuildFinishedUseCaseTests {
    val statsDatasource : PostStatsUseCase.Datasource = mock()
    val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
    val statusDatasource : PostStatusUseCase.Datasource = mock()
    val postStatusUseCase = PostStatusUseCase(listOf(statusDatasource), mock(), mock())
    val summaries = listOf(
            GetCoverageSummaryUseCase(listOf(mock(), mock()), CreateCoverageJacocoMap()),
            GetLintSummaryUseCase(listOf(mock())),
            GetCoverageSummaryUseCase("".toDocumentList(), CreateCoverageCoberturaMap())
    )

    @Before fun before() {
        When calling statusDatasource.name() itReturns "asdf"
        When calling statusDatasource.isActive() itReturns true
        When calling statusDatasource.isRemote() itReturns false
        When calling statusDatasource.post(any(), any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)
    }

    @Test
    fun `when plugin is not activated, nothing is posted`() {
        val config = BuildConfigDefault()
        config.isPluginActivated = false
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        VerifyNotCalled on statusDatasource that statusDatasource.post(any(), any(), any()) was called
    }

    @Test
    fun `when the build finishes unsuccessfully and plugin is activated, post failure status for each type`() {

        val config = BuildConfigDefault()
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        verify(statusDatasource, times(2)).post(any(), any(), any())
    }

    @Test
    fun `when the build finishes successfully and plugin is activated, handle success status for each type`() {
        val config = BuildConfigDefault()
        config.isPluginActivated = true
        val usecase = HandleBuildFinishedUseCase(postStatusUseCase, postStatsUseCase, summaries, config, mutableListOf())

        usecase.invoke()

        verify(statusDatasource, times(2)).post(any(), any(), any())
        Verify on statsDatasource that statsDatasource.postStats(any()) was called
    }

    //@Test fun test() { Assert.assertEquals(true, false) }
}

