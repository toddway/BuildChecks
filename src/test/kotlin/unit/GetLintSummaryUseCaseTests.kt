package unit

import core.toDocumentList
import core.toFileList
import core.usecase.GetLintSummaryUseCase
import core.usecase.toViolationMap
import core.usecase.toViolationSummary
import org.junit.Test

class GetLintSummaryUseCaseTests {

    @Test
    fun `when there is a valid report document, the summary is returned`() {
        val usecase = GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toFileList().toDocumentList())
        val v = usecase.value()
        println(v)
        assert(v != null)
    }

    @Test
    fun `when a map is created from multiple report files, then the counts of each violation type are accurate`() {
        val docs = "./src/test/testFiles/lint-results-prodRelease.xml, ./src/test/testFiles/detekt-checkstyle.xml, ./src/test/testFiles/cpdCheck-swift.xml".toFileList().toDocumentList()
        val map = docs.toViolationMap()
        assert(map?.get("clones")?.count() == 2)
        assert(map?.get("error")?.count() == 7)
        assert(map?.get("warning")?.count() == 409)
        assert(map?.get("info")?.count() == 5)
        println(map?.toViolationSummary(34, docs.size))
    }

    @Test
    fun `when a pmd map is created, the error count is correct`() {
        val map = "./src/test/testFiles/pmd.xml".toFileList().toDocumentList().toViolationMap()
        assert(map?.get("error")?.count() == 2)
    }
}

