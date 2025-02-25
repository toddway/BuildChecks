package unit

import core.entity.Stats
import core.usecase.PostStatsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.junit.Test

class PostStatsUseCaseTests {

    @Test
    fun `when metrics, post them`() {
        val datasource : PostStatsUseCase.Datasource = mockk()
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
        every {  datasource.postStats(metrics) } returns Observable.just(true)
        usecase.post(metrics)
        verify { datasource.postStats(metrics) }
    }
}

