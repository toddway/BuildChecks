package unit

import core.datasource.StatusDatasource
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import org.junit.Test

class GetPostStatusDatasourceTests {

    @Test
    fun `when`() {
        val config = BuildConfigDefault()
        config.baseUrl = "http://github"
        val usecase = GetPostStatusDatasource(config)
        val datasource = usecase.invoke()
    }
}

class GetPostStatusDatasource(config: BuildConfig) {
    fun invoke() : StatusDatasource? {
        return null
    }
}
