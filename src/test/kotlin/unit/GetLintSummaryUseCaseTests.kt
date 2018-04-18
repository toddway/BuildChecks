package unit

import core.GetLintSummaryUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Ignore
import org.junit.Test
import java.io.File

class GetLintSummaryUseCaseTests {

    @Ignore
    @Test
    fun `when there is a report document, the summary is returned`() {
        val file = File("/Users/tway/dev/sorry/app/build/reports/lint-results-devDebug.xml")
        val usecase = GetLintSummaryUseCase(file)
        val v = usecase.execute()
        println(v)
        v shouldNotBe null
    }
}

