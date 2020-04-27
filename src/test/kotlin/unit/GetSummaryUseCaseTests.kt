package unit

import core.entity.BuildConfig
import core.toDocument
import core.toDocumentList
import core.usecase.*
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Test
import java.io.File

class GetSummaryUseCaseTests {

    @Test
    fun`when there are valid report documents, summaries are generated`() {
        val summaries : List<GetSummaryUseCase> = listOf(
                GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toDocumentList() +  "./src/test/testFiles/detekt-checkstyle.xml".toDocumentList()),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/coverage.xml").toDocument()), CoverageJacocoMapper()),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/cobertura-coverage.xml").toDocument()), CoverageCoberturaMapper()),
                GetTextSummaryUseCase(File("./src/test/testFiles/hello.txt"))
        )

        summaries.forEach {
            it.key().isNotBlank() shouldBe true
            Assert.assertTrue("${it.javaClass} summary was null or blank", !it.value().isNullOrBlank())
            println("${it.key()} - ${it.value()}")
        }
    }

    @Test
    fun`when there are invalid report documents, summaries are generated`() {
        val summaries : List<GetSummaryUseCase> = listOf(
                GetLintSummaryUseCase(listOf(File("./src/test/testFiles/coverage.xml").toDocument(), File("./src/test/testFiles/coverage.xml").toDocument())),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/cobertura-coverage.xml").toDocument()), CoverageJacocoMapper()),
                GetTextSummaryUseCase(File("./src/test/testFiles/coverage.xml"))
        )

        summaries.forEach {
            it.key().isNotBlank() shouldBe true
            //Assert.assertTrue("${it.javaClass} summary was null or blank", !it.value().isNullOrBlank())
        }
    }

    @Test
    fun `when the build was successful, the duration summary isSuccessful is true`() {
        val config : BuildConfig = mock()
        When calling config.isSuccess itReturns true
        val summary = GetDurationSummaryUseCase(config)
        summary.isSuccessful() shouldBe true
    }

    @Test
    fun `when the build was not successful, the duration summary isSuccessful is false`() {
        val config : BuildConfig = mock()
        When calling config.isSuccess itReturns false
        val summary = GetDurationSummaryUseCase(config)
        summary.isSuccessful() shouldBe false
    }
}