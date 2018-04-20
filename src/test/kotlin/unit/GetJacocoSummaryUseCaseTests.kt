package unit

import core.GetJacocoSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetJacocoSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, a summary is returned`() {
        val file = File("./src/test/testFiles/coverage.xml")
        val usecase = GetJacocoSummaryUseCase(file)
        val v = usecase.execute()
        println(v)
        v shouldNotBe ""
    }
}

