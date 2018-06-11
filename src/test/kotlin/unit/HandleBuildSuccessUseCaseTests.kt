package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.datasource.StatsDatasource
import core.datasource.StatusDatasource
import core.entity.ConfigEntityDefault
import core.toDocumentList
import core.usecase.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test


class HandleBuildSuccessUseCaseTests {

    @Test
    fun `when there are no report docs, there are no calls and no errors`() {
        val datasource : StatusDatasource = mock()
        val setBuildStatus = SetStatusUseCase(listOf(datasource))
        val statsDatasource : StatsDatasource = mock()
        val postBuildMetricsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetJacocoSummaryUseCase(listOf()),
                GetLintSummaryUseCase(listOf()),
                GetDetektSummaryUseCase(listOf()),
                GetCheckstyleSummaryUseCase(listOf())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postBuildMetricsUseCase,
                ConfigEntityDefault(),
                summaries
        )
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.postSuccessStatus(any(), any())
    }

    @Test
    fun `when there are one or more report docs, post success status for each type and build metrics`() {
        val statusDatasource : StatusDatasource = mock()
        val setBuildStatus = SetStatusUseCase(listOf(statusDatasource))
        val statsDatasource : StatsDatasource = mock()
        val postBuildMetricsUseCase = PostStatsUseCase(listOf(statsDatasource))
        val summaries = listOf(
                GetJacocoSummaryUseCase(listOf(mock(), mock())),
                GetLintSummaryUseCase(listOf(mock())),
                GetDetektSummaryUseCase("".toDocumentList()),
                GetCheckstyleSummaryUseCase("".toDocumentList())
        )
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postBuildMetricsUseCase,
                ConfigEntityDefault(),
                summaries
        )
        When calling statusDatasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling statsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(statusDatasource, times(2)).postSuccessStatus(any(), any())
        Verify on statsDatasource that statsDatasource.postStats(any()) was called
    }
}

