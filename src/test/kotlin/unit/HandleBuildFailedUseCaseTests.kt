package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.datasource.StatusDatasource
import core.toDocumentList
import core.usecase.*
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class HandleBuildFailedUseCaseTests {

    @Test
    fun `when there are no summaries, there are no calls and no errors`() {
        val datasource : StatusDatasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(datasource), ShowMessageUseCase())
        val summaries = listOf(
                GetJacocoSummaryUseCase(listOf()),
                GetLintSummaryUseCase(listOf()),
                GetCoberturaSummaryUseCase(listOf())
        )
        val usecase = HandleBuildFailedUseCase(setBuildStatus, summaries)
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.postSuccessStatus(any(), any())
    }

    @Test
    fun `when there are one or more summaries, post failed status for each type`() {
        val statusDatasource : StatusDatasource = mock()
        val setBuildStatus = PostStatusUseCase(listOf(statusDatasource), ShowMessageUseCase())
        val summaries = listOf(
                GetJacocoSummaryUseCase(listOf(mock(), mock())),
                GetLintSummaryUseCase(listOf(mock())),
                GetCoberturaSummaryUseCase("".toDocumentList())
        )
        val usecase = HandleBuildFailedUseCase(setBuildStatus, summaries)
        When calling statusDatasource.name() itReturns "asdf"
        When calling statusDatasource.postFailureStatus(any(), any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(statusDatasource, times(2)).postFailureStatus(any(), any())

    }
}

