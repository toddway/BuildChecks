package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test


class HandleBuildSuccessUseCaseTests {

    @Test
    fun `when there are no report docs, there are no calls and no errors`() {
        val datasource : BuildStatusDatasource = mock()
        val setBuildStatus = SetBuildStatusUseCase(listOf(datasource))
        val buildStatsDatasource : BuildStatsDatasource = mock()
        val postBuildMetricsUseCase = PostBuildStatsUseCase(listOf(buildStatsDatasource))
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postBuildMetricsUseCase,
                BuildStatusConfig(),
                GetJacocoSummaryUseCase(listOf()) ,
                GetLintSummaryUseCase(listOf()),
                GetDetektSummaryUseCase(listOf()),
                GetCheckstyleSummaryUseCase(listOf())
        )
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling buildStatsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.postSuccessStatus(any(), any())
    }

    @Test
    fun `when there are one or more report docs, post success status for each type and build metrics`() {
        val buildStatusDatasource : BuildStatusDatasource = mock()
        val setBuildStatus = SetBuildStatusUseCase(listOf(buildStatusDatasource))
        val buildStatsDatasource : BuildStatsDatasource = mock()
        val postBuildMetricsUseCase = PostBuildStatsUseCase(listOf(buildStatsDatasource))
        val usecase = HandleBuildSuccessUseCase(
                setBuildStatus,
                postBuildMetricsUseCase,
                BuildStatusConfig(),
                GetJacocoSummaryUseCase(listOf(mock(), mock())),
                GetLintSummaryUseCase(listOf(mock())),
                GetDetektSummaryUseCase("".toDocumentList()),
                GetCheckstyleSummaryUseCase("".toDocumentList())
        )
        When calling buildStatusDatasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)
        When calling buildStatsDatasource.postStats(any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(buildStatusDatasource, times(2)).postSuccessStatus(any(), any())
        Verify on buildStatsDatasource that buildStatsDatasource.postStats(any()) was called
    }
}

