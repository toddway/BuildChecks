package unit
import core.BuildStatusDatasource
import core.SetBuildStatusUseCase
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostBuildStatusTests {

    val key = "ADfds"

    @Test
    fun `when the status is pending, post the status`() {
        val datasource : BuildStatusDatasource = mock()
        val usecase = SetBuildStatusUseCase(listOf(datasource))
        val message = "build in progress"
        When calling datasource.postPendingStatus(message, key) itReturns Observable.just(true)
        usecase.pending(message, key)
        Verify on datasource that datasource.postPendingStatus(message, key) was called
    }

    @Test
    fun `when the status is set to failure, post the status`() {
        val datasource : BuildStatusDatasource = mock()
        val usecase = SetBuildStatusUseCase(listOf(datasource))
        val message = "build failed"
        When calling datasource.postFailureStatus(message, key) itReturns Observable.just(true)
        usecase.failure(message, key)
        Verify on datasource that datasource.postFailureStatus(message, key) was called
    }

    @Test
    fun `when the status is set to success, post the status`() {
        val datasource : BuildStatusDatasource = mock()
        val usecase = SetBuildStatusUseCase(listOf(datasource))
        val message = "build successful"
        When calling datasource.postSuccessStatus(message, key) itReturns Observable.just(true)
        usecase.success(message, key)
        Verify on datasource that datasource.postSuccessStatus(message, key) was called
    }
}

