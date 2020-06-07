package unit

import core.entity.Stats
import core.usecase.PostStatsUseCase
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostStatsUseCaseTests {

    @Test
    fun `when metrics, post them`() {
        val datasource : PostStatsUseCase.Datasource = mock()
        val usecase = PostStatsUseCase(listOf(datasource))
        val metrics = Stats(
                1.1,
                1,
                1.1,
                1,
                2343434,
                "ADfsdf",
                "asdfdf"
        )
        When calling datasource.postStats(metrics) itReturns Observable.just(true)
        usecase.post(metrics)
        Verify on datasource that datasource.postStats(metrics) was called
    }
}

