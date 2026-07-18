package buildchecks.parse

import buildchecks.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LcovParserTest {

    private val coverage = LcovParser().parse(Fixtures.text("lcov.info")).coverage!!

    @Test
    fun `line totals match the report's own LF and LH records`() {
        assertEquals(listOf("src/calculator.js", "src/greeter.js"), coverage.files.map { it.path })
        val calculator = coverage.files[0]
        assertEquals(21, calculator.lines.size) // LF:21 — duplicate DA records merged
        assertEquals(15, calculator.linesCovered) // LH:15
        val greeter = coverage.files[1]
        assertEquals(19, greeter.lines.size)
        assertEquals(13, greeter.linesCovered)
    }

    @Test
    fun `reads branch data from BRDA records`() {
        val calculator = coverage.files[0]
        val fullyBranched = calculator.lines.single { it.line == 1 }
        assertEquals(2, fullyBranched.coveredBranches)
        assertEquals(2, fullyBranched.totalBranches)
        val untaken = calculator.lines.single { it.line == 10 }
        assertEquals(0, untaken.coveredBranches)
        assertEquals(1, untaken.totalBranches)
    }
}
