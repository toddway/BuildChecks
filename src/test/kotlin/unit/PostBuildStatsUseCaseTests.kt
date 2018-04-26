package unit

import core.BuildStats
import core.BuildStatsDatasource
import core.PostBuildStatsUseCase
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostBuildStatsUseCaseTests {

    @Test
    fun `when metrics, post them`() {
        val datasource : BuildStatsDatasource = mock()
        val usecase = PostBuildStatsUseCase(listOf(datasource))
        val metrics = BuildStats(
                1.1,
                1,
                1.1,
                1,
                2343434,
                "ADfsdf",
                "asdfdf"
        )
        When calling datasource.postStats(metrics) itReturns Observable.just(true)
        usecase.invoke(metrics)
        Verify on datasource that datasource.postStats(metrics) was called
    }
}

