package unit

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import core.BuildStatusDatasource
import core.HandleBuildSuccessUseCase
import core.SetBuildStatusUseCase
import core.toDocumentList
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test


class HandleBuildSuccessUseCaseTests {

    @Test
    fun `when there are no report docs, there are no calls and no errors`() {
        val datasource : BuildStatusDatasource = mock()
        val setBuildStatus = SetBuildStatusUseCase(listOf(datasource))
        val usecase = HandleBuildSuccessUseCase(setBuildStatus, listOf(), listOf(), listOf(), listOf())
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)

        usecase.invoke()

        VerifyNotCalled on datasource that datasource.postSuccessStatus(any(), any())
    }

    @Test
    fun `when there are one or more report docs, call build status for each`() {
        val datasource : BuildStatusDatasource = mock()
        val setBuildStatus = SetBuildStatusUseCase(listOf(datasource))
        val usecase = HandleBuildSuccessUseCase(setBuildStatus, listOf(mock(), mock()), listOf(mock()), "".toDocumentList(), "".toDocumentList())
        When calling datasource.postSuccessStatus(any(), any()) itReturns Observable.just(true)

        usecase.invoke()

        verify(datasource, times(2)).postSuccessStatus(any(), any())
    }
}

