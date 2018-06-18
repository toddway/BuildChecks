package func

import core.datasource.StatusDatasource
import data.GithubDatasource
import data.createGithubService
import org.amshove.kluent.shouldBe
import org.junit.Ignore
import org.junit.Test

class GithubDatasourceIntegrationTests {

    @Ignore
    @Test
    fun `asdf`() {
        val datasource : StatusDatasource = GithubDatasource(
                createGithubService("https://api.github.com", ""),
                "4aa5558ccb1e002970e1835621d6e9ab56c29458",
                "http://localhost"
        )

        val result = datasource.postPendingStatus("test", "some-key").blockingFirst()

        result shouldBe true
    }
}

