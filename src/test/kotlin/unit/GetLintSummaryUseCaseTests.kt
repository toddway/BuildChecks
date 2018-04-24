package unit

import core.GetLintSummaryUseCase
import core.toDocument
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import java.io.File

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val document = File("./src/test/testFiles/lint-results-prodRelease.xml").toDocument()
        val usecase = GetLintSummaryUseCase(listOf(document))
        val v = usecase.asString()
        println(v)
        v shouldNotBe null
    }
}

