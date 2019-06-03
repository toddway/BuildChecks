package unit

import core.toDocument
import core.usecase.CreateCoverageCoberturaMap
import core.usecase.CreateCoverageMap
import core.usecase.CreateCoverageJacocoMap
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.Test
import java.io.File

class GetCoverageMapTests {

    @Test
    fun `when there is a cobertura report file, a coverage map is created`() {
        val usecase : CreateCoverageMap = CreateCoverageCoberturaMap()
        val map = usecase.from(File("./src/test/testFiles/cobertura-coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }

    @Test
    fun `when there is a jacoco report file, a coverage map is created`() {
        val usecase : CreateCoverageMap = CreateCoverageJacocoMap()
        val map = usecase.from(File("./src/test/testFiles/coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }


    @Test
    fun `when there is an invalid jacoco report file, a coverage map is created`() {
        val usecase : CreateCoverageMap = CreateCoverageJacocoMap()
        val map = usecase.from(File("./src/test/testFiles/cobertura-coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }

    @Test
    fun `when there is an invalid cobertura report file, a coverage map is created`() {
        val usecase : CreateCoverageMap = CreateCoverageCoberturaMap()
        val map = usecase.from(File("./src/test/testFiles/coverage.xml").toDocument())
        map.size shouldBeGreaterThan 0
    }

}

