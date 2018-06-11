package func

import core.datasource.StatusDatasource
import data.BitBucketDatasource
import data.createBitBucketService
import org.amshove.kluent.shouldBe
import org.junit.Ignore
import org.junit.Test

class BitBucketDatasourceIntegrationTests {

    @Ignore
    @Test
    fun `asdf`() {
        val datasource : StatusDatasource = BitBucketDatasource(
                createBitBucketService("https://bitbucket.uhub.biz", ""),
                "547b9f82420bb7c56b590d34abd84cf95cb538a3",
                "http://localhost"
        )
        val result = datasource.postPendingStatus("test", "some-key").blockingFirst()
        result shouldBe true
    }
}

