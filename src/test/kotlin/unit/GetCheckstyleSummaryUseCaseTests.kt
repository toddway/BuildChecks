package unit

import core.GetCheckstyleSummaryUseCase
import core.toDocument
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetCheckstyleSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, a summary is returned`() {
        val document = File("./src/test/testFiles/detekt-checkstyle.xml").toDocument()
        val usecase = GetCheckstyleSummaryUseCase(listOf(document))
        val v = usecase.asString()
        println(v)
        v shouldNotBe ""
    }
}

