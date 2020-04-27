package unit

import core.toDocumentList
import core.usecase.GetLintSummaryUseCase
import core.usecase.toViolationMap
import core.usecase.toViolationSummary
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.junit.Test

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val usecase = GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toDocumentList())
        val v = usecase.value()
        println(v)
        v shouldNotBe null
    }

    @Test
    fun `when a map is created from multiple report files, then the counts of each violation type are accurate`() {
        val docs = "./src/test/testFiles/lint-results-prodRelease.xml, ./src/test/testFiles/detekt-checkstyle.xml, ./src/test/testFiles/cpdCheck-swift.xml".toDocumentList()
        val map = docs.toViolationMap()
        map?.get("clones")?.count() shouldEqual 2
        map?.get("error")?.count() shouldEqual 7
        map?.get("warning")?.count() shouldEqual 409
        map?.get("info")?.count() shouldEqual 5
        println(map?.toViolationSummary(34, docs.size))
    }

    @Test
    fun `when a pmd map is created, the error count is correct`() {
        val map = "./src/test/testFiles/pmd.xml".toDocumentList().toViolationMap()
        map?.get("error")?.count() shouldEqual 2
    }
}

