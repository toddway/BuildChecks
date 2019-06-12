package unit

import core.toDocuments
import core.usecase.GetLintSummaryUseCase
import core.usecase.toViolationMap
import core.usecase.toViolationSummary
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Test

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val usecase = GetLintSummaryUseCase(listOf("./src/test/testFiles/lint-results-prodRelease.xml").toDocuments())
        val v = usecase.value()
        println(v)
        v shouldNotBe null
    }

    @Test
    fun `when it`() {
        val docs = listOf(
                "./src/test/testFiles/lint-results-prodRelease.xml",
                "./src/test/testFiles/detekt-checkstyle.xml",
                "./src/test/testFiles/cpdCheck-swift.xml").toDocuments()
        val map = docs.toViolationMap()
        map?.get("clones")?.count() shouldEqual 2
        map?.get("error")?.count() shouldEqual 7
        map?.get("warning")?.count() shouldEqual 409
        map?.get("info")?.count() shouldEqual 5
        println(map?.toViolationSummary(34, docs.size))
    }
}

