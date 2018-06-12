package unit
import core.datasource.StatusDatasource
import core.usecase.PostStatusUseCase
import core.usecase.ShowMessageUseCase
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostStatusUseCaseTests {

    val key = "ADfds"

    @Test
    fun `when the status is pending, post the status`() {
        val datasource : StatusDatasource = mock()
        val usecase = PostStatusUseCase(listOf(datasource), ShowMessageUseCase())
        val message = "build in progress"
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postPendingStatus(message, key) itReturns Observable.just(true)
        usecase.pending(message, key)
        Verify on datasource that datasource.postPendingStatus(message, key) was called
    }

    @Test
    fun `when the status is set to failure, post the status`() {
        val datasource : StatusDatasource = mock()
        val usecase = PostStatusUseCase(listOf(datasource), ShowMessageUseCase())
        val message = "build failed"
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postFailureStatus(message, key) itReturns Observable.just(true)
        usecase.failure(message, key)
        Verify on datasource that datasource.postFailureStatus(message, key) was called
    }

    @Test
    fun `when the status is set to success, post the status`() {
        val datasource : StatusDatasource = mock()
        val usecase = PostStatusUseCase(listOf(datasource), ShowMessageUseCase())
        val message = "build successful"
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postSuccessStatus(message, key) itReturns Observable.just(true)
        usecase.success(message, key)
        Verify on datasource that datasource.postSuccessStatus(message, key) was called
    }

    @Test
    fun `when the status is set to success but the datasource returns false, print a message`() {
        val datasource : StatusDatasource = mock()
        val usecase = PostStatusUseCase(listOf(datasource), ShowMessageUseCase())
        val message = "build successful"
        When calling datasource.name() itReturns "asdf"
        When calling datasource.postSuccessStatus(message, key) itReturns Observable.just(false)
        usecase.success(message, key)
        Verify on datasource that datasource.postSuccessStatus(message, key) was called
    }
}

