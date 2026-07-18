package buildchecks.parse

import buildchecks.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class JacocoParserTest {

    private val coverage = JacocoParser().parse(Fixtures.text("coverage.xml")).coverage!!

    @Test
    fun `aggregates line coverage across all packages`() {
        assertEquals(31, coverage.files.size)
        assertEquals(431, coverage.linesTotal)
        assertEquals(132, coverage.linesCovered)
    }

    @Test
    fun `builds file paths from package and sourcefile names`() {
        assertNotNull(coverage.files.find { it.path == "vml/com/sorry/app/article/SettingsPresenter.kt" })
    }

    @Test
    fun `reads per-line hit and branch data`() {
        val file = coverage.files.single { it.path == "vml/com/sorry/core/postlist/FeedPresenter.kt" }
        val line = file.lines.single { it.line == 124 }
        assertEquals(1, line.hits)
        assertEquals(1, line.coveredBranches)
        assertEquals(2, line.totalBranches)
    }
}
