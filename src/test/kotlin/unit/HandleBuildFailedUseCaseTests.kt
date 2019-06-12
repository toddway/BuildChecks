package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.entity.BuildConfig
import core.toDocuments
import core.usecase.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildFailedUseCaseTests {

    @Test
    fun `when there are no summaries, there are no calls and no errors`() {
        val datasource : PostStatusUseCase.Datasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(datasource), BuildConfig(), mock())
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(), CreateCoverageJacocoMap()),
                GetLintSummaryUseCase(listOf())
        )
        val usecase = HandleBuildFailedUseCase(setBuildStatus, summaries)
        When calling datasource.name() itReturns "asdf"
        When calling datasource.post(any(), any(), any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.post(any(), any(), any())
    }

    @Test
    fun `when there are one or more summaries, post status for each type`() {
        val setBuildStatus : PostStatusUseCase = mock()
        val summaries = listOf(
                GetCoverageSummaryUseCase(listOf(mock(), mock()), CreateCoverageJacocoMap()),
                GetLintSummaryUseCase(listOf(mock())),
                GetCoverageSummaryUseCase(listOf("").toDocuments(), CreateCoverageCoberturaMap())
        )
        val usecase = HandleBuildFailedUseCase(setBuildStatus, summaries)

        usecase.invoke()

        verify(setBuildStatus, times(2)).post(any(), any(), any())

    }
}

