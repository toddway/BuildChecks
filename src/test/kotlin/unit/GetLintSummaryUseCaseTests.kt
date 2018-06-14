package unit

import core.toDocumentList
import core.usecase.GetLintSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Test

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val usecase = GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toDocumentList())
        val v = usecase.asString()
        println(v)
        v shouldNotBe null
    }
}

