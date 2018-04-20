package unit

import core.GetCheckstyleSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetCheckstyleSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, a summary is returned`() {
        val file = File("./src/test/testFiles/detekt-checkstyle.xml")
        val usecase = GetCheckstyleSummaryUseCase(file)
        val v = usecase.execute()
        println(v)
        v shouldNotBe ""
    }
}

