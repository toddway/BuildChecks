package buildchecks.parse

import buildchecks.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoberturaParserTest {

    private val coverage = CoberturaParser().parse(Fixtures.text("cobertura-coverage.xml")).coverage!!

    @Test
    fun `line totals match the report's own header counters`() {
        // Header of the fixture: lines-valid="149" lines-covered="130"
        assertEquals(7, coverage.files.size)
        assertEquals(149, coverage.linesTotal)
        assertEquals(130, coverage.linesCovered)
    }

    @Test
    fun `reads per-line hits and condition coverage`() {
        val file = coverage.files.single { it.path == "src/core/PostEntity.ts" }
        val line = file.lines.single { it.line == 21 }
        assertEquals(9, line.hits)
        assertEquals(2, line.coveredBranches)
        assertEquals(2, line.totalBranches)
    }

    @Test
    fun `counts branch data only once despite method-level line duplicates`() {
        assertEquals(30, coverage.files.sumOf { f -> f.lines.sumOf { it.coveredBranches } })
        assertEquals(36, coverage.files.sumOf { f -> f.lines.sumOf { it.totalBranches } })
    }
}
