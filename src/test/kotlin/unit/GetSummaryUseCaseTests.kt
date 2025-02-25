package unit

import core.entity.BuildConfig
import core.toDocument
import core.toDocumentList
import core.toFileList
import core.usecase.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.io.File

class GetSummaryUseCaseTests {

    @Test
    fun`when there are valid report documents, summaries are generated`() {
        val summaries : List<GetSummaryUseCase> = listOf(
                GetLintSummaryUseCase("./src/test/testFiles/lint-results-prodRelease.xml".toFileList().toDocumentList() +  "./src/test/testFiles/detekt-checkstyle.xml".toFileList().toDocumentList()),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/coverage.xml").toDocument()), CoverageJacocoMapper()),
                GetCoverageSummaryUseCase(listOf(File("./src/test/testFiles/cobertura-coverage.xml").toDocument()), CoverageCoberturaMapper()),
                GetTextSummaryUseCase(File("./src/test/testFiles/hello.txt"))
        )

        summaries.forEach {
            assert(it.key().isNotBlank()==  true)
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
            assert(it.key().isNotBlank()==  true)
            //Assert.assertTrue("${it.javaClass} summary was null or blank", !it.value().isNullOrBlank())
        }
    }

    @Test
    fun `when the build was successful, the duration summary isSuccessful is true`() {
        val config : BuildConfig = mockk()
        every {  config.isSuccess } returns true
        val summary = GetDurationSummaryUseCase(config)
        assert(summary.isSuccessful()==  true)
    }

    @Test
    fun `when the build was not successful, the duration summary isSuccessful is false`() {
        val config : BuildConfig = mockk()
        every { config.isSuccess } returns false
        val summary = GetDurationSummaryUseCase(config)
        assert(summary.isSuccessful() == false)
    }
}