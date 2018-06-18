package unit

import core.datasource.StatusDatasource
import core.entity.ConfigEntity
import core.entity.ConfigEntityDefault
import org.junit.Test

class GetPostStatusDatasourceTests {

    @Test
    fun `when`() {
        val config = ConfigEntityDefault()
        config.baseUrl = "http://github"
        val usecase = GetPostStatusDatasource(config)
        val datasource = usecase.invoke()
    }
}

class GetPostStatusDatasource(config: ConfigEntity) {
    fun invoke() : StatusDatasource? {
        return null
    }
}
