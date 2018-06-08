package unit

import core.GetCoburturaSummaryUseCase
import core.toDocument
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetCoburturaSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, a summary is returned`() {
        val document = File("./src/test/testFiles/cobertura-coverage.xml").toDocument()
        val usecase = GetCoburturaSummaryUseCase(listOf(document))
        val v = usecase.percentAsString()
        println(v)
        v shouldNotBe ""
    }
}

