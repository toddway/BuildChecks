package unit

import core.entity.StatsEntity
import core.datasource.StatsDatasource
import core.usecase.PostStatsUseCase
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostStatsUseCaseTests {

    @Test
    fun `when metrics, post them`() {
        val datasource : StatsDatasource = mock()
        val usecase = PostStatsUseCase(listOf(datasource))
        val metrics = StatsEntity(
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

