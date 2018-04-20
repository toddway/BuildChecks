package unit

import core.GetLintSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val file = File("./src/test/testFiles/lint-results-prodRelease.xml")
        val usecase = GetLintSummaryUseCase(file)
        val v = usecase.execute()
        println(v)
        v shouldNotBe null
    }
}

