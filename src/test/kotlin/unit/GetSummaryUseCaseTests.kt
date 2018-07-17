package unit

import core.coverage.CreateJacocoMap
import core.summary.GetCoverageSummaryUseCase
import core.summary.GetLintSummaryUseCase
import core.summary.GetSummaryUseCase
import core.summary.GetTextSummaryUseCase
import core.entity.toDocument
import core.entity.toDocumentList
import org.amshove.kluent.shouldBe
import org.junit.Assert
import org.junit.Test
import java.io.File

class GetSummaryUseCaseTests {

    @Test
    fun`when there are valid report documents, summaries are generated`() {

        val summaries : List<GetSummaryUseCase> = listOf(
                GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toDocumentList() + "./src/test/testFiles/detekt-checkstyle.xml".toDocumentList()),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/coverage.xml").toDocument()), CreateJacocoMap()),
                GetTextSummaryUseCase(File("./src/test/testFiles/hello.txt"))
        )

        summaries.forEach {
            it.key().isNotBlank() shouldBe true
            Assert.assertTrue("${it.javaClass} summary was null or blank", !it.summary().isNullOrBlank())
            println("${it.key()} - ${it.summary()}")
        }
    }

    @Test
    fun`when there are invalid report documents, summaries are generated`() {
        val summaries : List<GetSummaryUseCase> = listOf(
                GetLintSummaryUseCase(listOf(File("./src/test/testFiles/coverage.xml").toDocument(), File("./src/test/testFiles/coverage.xml").toDocument())),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/cobertura-coverage.xml").toDocument()), CreateJacocoMap()),
                GetTextSummaryUseCase(File("./src/test/testFiles/coverage.xml"))
        )

        summaries.forEach {
            it.key().isNotBlank() shouldBe true
            Assert.assertTrue("${it.javaClass} summary was null or blank", !it.summary().isNullOrBlank())
        }
    }
}