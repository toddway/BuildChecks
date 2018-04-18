package unit

import core.GetDocumentUseCase
import org.amshove.kluent.shouldNotBe
import org.junit.Ignore
import org.junit.Test
import java.io.File

class GetDocumentUseCaseTests {

    @Ignore
    @Test
    fun `when an XML file is provided, a Document is returned`() {
        val file = File("/Users/tway/dev/sorry/core/build/reports/coverage/coverage.xml")
        val usecase = GetDocumentUseCase(file)
        val doc = usecase.execute()
        doc shouldNotBe null
    }
}

