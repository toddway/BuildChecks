package buildchecks.gate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BaselineDeltaTest {

    private fun baseline(fingerprints: Set<String>, coverage: Double? = null, manifest: Set<OriginKind>? = null) =
        Baseline(fingerprints, fingerprints.size, coverage, manifest)

    @Test
    fun `an unchanged baseline is not loosened`() {
        val b = baseline(setOf("a", "b"), coverage = 80.0)
        val delta = baselineDelta(b, b)
        assertFalse(delta.loosened)
        assertEquals(0, delta.findingsAccepted)
        assertNull(delta.coverageLowered)
    }

    @Test
    fun `findings added to the on-disk baseline are counted as accepted`() {
        val delta = baselineDelta(baseline(setOf("a")), baseline(setOf("a", "b", "c")))
        assertEquals(2, delta.findingsAccepted)
        assertTrue(delta.loosened)
    }

    @Test
    fun `removing findings from the baseline is not loosening`() {
        val delta = baselineDelta(baseline(setOf("a", "b", "c")), baseline(setOf("a")))
        assertEquals(0, delta.findingsAccepted)
        assertFalse(delta.loosened)
    }

    @Test
    fun `a lowered coverage floor is reported, a raised one is not`() {
        assertEquals(10.0, baselineDelta(baseline(setOf("a"), 80.0), baseline(setOf("a"), 70.0)).coverageLowered!!, 1e-9)
        assertNull(baselineDelta(baseline(setOf("a"), 70.0), baseline(setOf("a"), 80.0)).coverageLowered)
    }

    @Test
    fun `dropped manifest entries are reported, added ones are not`() {
        val base = baseline(setOf("a"), manifest = setOf(OriginKind("services/web", "junit"), OriginKind(".", "detekt")))
        val current = baseline(setOf("a"), manifest = setOf(OriginKind(".", "detekt")))
        val delta = baselineDelta(base, current)
        assertEquals(listOf("junit in services/web"), delta.reportsDropped)
        assertTrue(delta.loosened)
    }

    @Test
    fun `manifests are uncomparable when either side predates v2`() {
        val base = baseline(setOf("a"), manifest = setOf(OriginKind(".", "detekt")))
        val current = baseline(setOf("a"), manifest = null) // pre-v2 baseline on disk
        assertTrue(baselineDelta(base, current).reportsDropped.isEmpty())
    }
}
