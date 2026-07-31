package buildchecks.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ContradictionTest {

    // Coverage.Measured with a chosen percent: `covered` hits / (`covered` + `uncovered`).
    private fun coverage(percent: Int) = ChangedLineCoverage.Measured(
        "main",
        listOf(ChangedFileCoverage("a.kt", covered = (1..percent).toList(), uncovered = ((percent + 1)..100).toList())),
        filesWithoutData = 0,
    )

    // Mutation.Measured with a chosen kill percent out of 100 mutants.
    private fun mutation(percent: Int) = ChangedLineMutation.Measured(
        "main",
        listOf(ChangedFileMutation("a.kt", mutants = 100, killed = percent, survivedLines = listOf(1))),
        filesWithoutData = 0,
    )

    @Test
    fun `fires when well-covered changed lines have a low kill rate`() {
        val c = contradiction(coverage(90), mutation(40))
        assertNotNull(c)
        assertEquals(90.0, c!!.coveragePercent, 0.001)
        assertEquals(40.0, c.mutationPercent, 0.001)
        assertEquals(50.0, c.gap, 0.001)
    }

    @Test
    fun `stays silent when coverage is low — that's honestly untested, not contradictory`() {
        // gap is 40 points, but coverage below the threshold isn't the covered-but-not-verified case
        assertNull(contradiction(coverage(50), mutation(10)))
    }

    @Test
    fun `stays silent when the gap is small — ordinary noise, not a finding`() {
        assertNull(contradiction(coverage(95), mutation(85)))
    }

    @Test
    fun `needs both signals measured on the same diff`() {
        assertNull(contradiction(ChangedLineCoverage.Unavailable("no base ref"), mutation(10)))
        assertNull(contradiction(coverage(90), ChangedLineMutation.Unavailable("no mutation data")))
        assertNull(contradiction(null, null))
    }
}
