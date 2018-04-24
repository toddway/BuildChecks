package unit

import core.GetJacocoSummaryUseCase
import core.toDocument
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetJacocoSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, a summary is returned`() {
        val document = File("./src/test/testFiles/coverage.xml").toDocument()
        val usecase = GetJacocoSummaryUseCase(listOf(document))
        val v = usecase.asString()
        println(v)
        v shouldNotBe ""
    }
}

