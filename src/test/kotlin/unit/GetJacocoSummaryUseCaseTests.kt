package unit

import core.GetJacocoSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Ignore
import org.junit.Test
import java.io.File

class GetJacocoSummaryUseCaseTests {

    @Ignore
    @Test
    fun `when there is a report document, the coverage is returned`() {
        val file = File("/Users/tway/dev/sorry/core/build/reports/coverage/coverage.xml")
        val usecase = GetJacocoSummaryUseCase(file)
        val v = usecase.execute()
        println(v)
        v shouldNotBe ""
    }
}

