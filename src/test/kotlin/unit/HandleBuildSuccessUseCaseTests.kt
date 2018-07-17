package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.buildstate.HandleBuildSuccessUseCase
import core.coverage.CreateCoberturaMap
import core.coverage.CreateJacocoMap
import core.post.StatsDatasource
import core.post.StatusDatasource
import core.entity.BuildConfigDefault
import core.post.PostStatsUseCase
import core.post.PostStatusUseCase
import core.summary.GetCoverageSummaryUseCase
import core.summary.GetLintSummaryUseCase
import core.entity.toDocumentList
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test


class HandleBuildSuccessUseCaseTests {

    @Test
    fun `when there are no report docs, there are no calls and no errors`() {
        val datasource : StatusDatasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(datasource), mock())
        val statsDatasource : StatsDatasource = mock()
        val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(), CreateJacocoMap()),
                GetLintSummaryUseCase(listOf()),
                GetCoverageSummaryUseCase(listOf(), CreateCoberturaMap())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postStatsUseCase,
                BuildConfigDefault(),
                summaries
        )
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.postSuccessStatus(any(), any())
    }

    @Test
    fun `when there are one or more report docs, post success status for each type and build metrics`() {
        val statusDatasource : StatusDatasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(statusDatasource), mock())
        val statsDatasource : StatsDatasource = mock()
        val postStatsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(mock(), mock()), CreateJacocoMap()),
                GetLintSummaryUseCase(listOf(mock())),
                GetCoverageSummaryUseCase("".toDocumentList(), CreateCoberturaMap())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postStatsUseCase,
                BuildConfigDefault(),
                summaries
        )
        When calling statusDatasource.name() itReturns "asdf"
        When calling statusDatasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(statusDatasource, times(2)).postSuccessStatus(any(), any())
        Verify on statsDatasource that statsDatasource.postStats(any()) was called
    }
}

