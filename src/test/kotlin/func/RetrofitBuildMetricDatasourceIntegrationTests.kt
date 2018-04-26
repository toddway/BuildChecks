package func

import core.BuildStats
import data.RetrofitBuildStatsDatasource
import data.createRetrifotBuildStatsService
import org.amshove.kluent.shouldBe
import org.junit.Ignore
import org.junit.Test
import java.util.*

class RetrofitBuildMetricDatasourceIntegrationTests {
    @Ignore
    @Test
    fun `asdf`() {
        val datasource = RetrofitBuildStatsDatasource(createRetrifotBuildStatsService("", ""))
        val metrics = BuildStats(
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