package func

import core.entity.Stats
import data.buildRetrofitStatsDatasource
import org.junit.Ignore
import org.junit.Test
import java.util.*

class RetrofitBuildMetricDatasourceIntegrationTests {
    @Ignore
    @Test
    fun `asdf`() {
        val datasource = buildRetrofitStatsDatasource("", "")
        val metrics = Stats(
                1.1,
                1,
                1.1,
                1,
                Date().time,
                "AaaDfsdf",
                "asdfdf"
        )
        val result = datasource.postStats(metrics).blockingFirst()
        assert(result == true)
    }
}