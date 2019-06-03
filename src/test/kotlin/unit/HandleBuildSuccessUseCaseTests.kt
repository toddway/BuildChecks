package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.entity.BuildConfigDefault
import core.toDocumentList
import core.usecase.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test


class HandleBuildSuccessUseCaseTests {

    @Test
    fun `when there are no report docs, there are no calls and no errors`() {
        val datasource : PostStatusUseCase.Datasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(datasource), mock(), mock())
        val statsDatasource : PostStatsUseCase.Datasource = mock()
        val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(), CreateCoverageJacocoMap()),
                GetLintSummaryUseCase(listOf()),
                GetCoverageSummaryUseCase(listOf(), CreateCoverageCoberturaMap())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postStatsUseCase,
                BuildConfigDefault(),
                summaries
        )
        When calling datasource.name() itReturns "asdf"
        When calling datasource.post(any(), any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.post(any(), any(), any())
    }

    @Test
    fun `when there are one or more report docs, post success status for each type and build metrics`() {
        val statusDatasource : PostStatusUseCase.Datasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(statusDatasource), mock(), mock())
        val statsDatasource : PostStatsUseCase.Datasource = mock()
        val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(mock(), mock()), CreateCoverageJacocoMap()),
                GetLintSummaryUseCase(listOf(mock())),
                GetCoverageSummaryUseCase("".toDocumentList(), CreateCoverageCoberturaMap())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postStatsUseCase,
                BuildConfigDefault(),
                summaries
        )
        When calling statusDatasource.name() itReturns "asdf"
        When calling statusDatasource.isActive() itReturns true
        When calling statusDatasource.isRemote() itReturns false
        When calling statusDatasource.post(any(), any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(statusDatasource, times(2)).post(any(), any(), any())
        Verify on statsDatasource that statsDatasource.postStats(any()) was called
    }
}

