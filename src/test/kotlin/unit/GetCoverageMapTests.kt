package unit

import core.toDocument
import core.usecase.CoverageCoberturaMapper
import core.usecase.CoverageMapper
import core.usecase.CoverageJacocoMapper
import core.usecase.toCoverageMaps
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.Test
import org.w3c.dom.Document
import java.io.File

class GetCoverageMapTests {

    @Test
    fun `when there is a cobertura report file, a coverage map is created`() {
        val usecase : CoverageMapper = CoverageCoberturaMapper()
        val map = usecase.from(File("./src/test/testFiles/cobertura-coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }

    @Test
    fun `when there is a jacoco report file, a coverage map is created`() {
        val usecase : CoverageMapper = CoverageJacocoMapper()
        val map = usecase.from(File("./src/test/testFiles/coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }


    @Test
    fun `when there is an invalid jacoco report file, a coverage map is created`() {
        val usecase : CoverageMapper = CoverageJacocoMapper()
        val map = usecase.from(File("./src/test/testFiles/cobertura-coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }

    @Test
    fun `when there is an invalid cobertura report file, a coverage map is created`() {
        val usecase : CoverageMapper = CoverageCoberturaMapper()
        val map = usecase.from(File("./src/test/testFiles/coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }


    @Test fun `when`() {
        val usecase : CoverageMapper = CoverageJacocoMapper()
        val doc = usecase.from(File("./src/test/testFiles/cobertura-coverage.xml").toDocument())

        println(listOf<Document>()
                .toCoverageMaps(usecase)
                .map { it["LINE + BRANCH"] })
    }
}

