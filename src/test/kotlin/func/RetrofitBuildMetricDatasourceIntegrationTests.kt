package func

import core.entity.StatsEntity
import data.RetrofitStatsDatasource
import data.createRetrifotBuildStatsService
import org.amshove.kluent.shouldBe
import org.junit.Ignore
import org.junit.Test
import java.util.*

class RetrofitBuildMetricDatasourceIntegrationTests {
    @Ignore
    @Test
    fun `asdf`() {
        val datasource = RetrofitStatsDatasource(createRetrifotBuildStatsService("", ""))
        val metrics = StatsEntity(
                1.1,
                1,
                1.1,
                1,
                Date().time,
                "AaaDfsdf",
                "asdfdf"
        )
        val result = datasource.postStats(metrics).blockingFirst()
        result shouldBe true
    }
}