package buildchecks.gate

import buildchecks.model.ChangeDelta
import buildchecks.model.GateResult
import buildchecks.model.GateStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromotionTest {

    private fun gate(name: String, status: GateStatus) = GateResult(name, status, "detail")

    @Test
    fun `both promotions are off by default so nothing is appended`() {
        val results = listOf(gate("changed-line coverage", GateStatus.SKIPPED))
        assertTrue(promotedGates(GateConfig(), results, baseRefResolved = false).isEmpty())
    }

    @Test
    fun `failOnSkippedGates turns a skipped gate into a failure`() {
        val results = listOf(gate("findings", GateStatus.PASSED), gate("expected reports", GateStatus.SKIPPED))
        val promoted = promotedGates(GateConfig(failOnSkippedGates = true), results, baseRefResolved = true)
        val result = promoted.single()
        assertEquals("no skipped gates", result.gate)
        assertEquals(GateStatus.FAILED, result.status)
        assertTrue(result.detail.contains("expected reports"))
    }

    @Test
    fun `failOnSkippedGates passes when every gate ran`() {
        val results = listOf(gate("findings", GateStatus.PASSED), gate("coverage", GateStatus.FAILED))
        val promoted = promotedGates(GateConfig(failOnSkippedGates = true), results, baseRefResolved = true)
        assertEquals(GateStatus.PASSED, promoted.single().status)
    }

    @Test
    fun `requireBaseRef fails when no base ref resolved and passes when one did`() {
        val failed = promotedGates(GateConfig(requireBaseRef = true), emptyList(), baseRefResolved = false).single()
        assertEquals("base ref required", failed.gate)
        assertEquals(GateStatus.FAILED, failed.status)

        val passed = promotedGates(GateConfig(requireBaseRef = true), emptyList(), baseRefResolved = true).single()
        assertEquals(GateStatus.PASSED, passed.status)
    }

    @Test
    fun `both knobs can fire together`() {
        val results = listOf(gate("changed-line coverage", GateStatus.SKIPPED))
        val promoted = promotedGates(
            GateConfig(failOnSkippedGates = true, requireBaseRef = true), results, baseRefResolved = false,
        )
        assertEquals(2, promoted.size)
        assertTrue(promoted.all { it.status == GateStatus.FAILED })
    }

    @Test
    fun `failOnBaselineLoosened fails on a loosened baseline and passes otherwise`() {
        val loose = ChangeDelta(baselineFindingsAccepted = 2)
        val failed = promotedGates(GateConfig(failOnBaselineLoosened = true), emptyList(), true, delta = loose).single()
        assertEquals("baseline not loosened", failed.gate)
        assertEquals(GateStatus.FAILED, failed.status)
        assertTrue(failed.detail.contains("2 finding(s) accepted"))

        val tight = ChangeDelta()
        val passed = promotedGates(GateConfig(failOnBaselineLoosened = true), emptyList(), true, delta = tight).single()
        assertEquals(GateStatus.PASSED, passed.status)
    }

    @Test
    fun `failOnBaselineLoosened passes when there is no base ref to compare`() {
        val passed = promotedGates(GateConfig(failOnBaselineLoosened = true), emptyList(), false, delta = null).single()
        assertEquals(GateStatus.PASSED, passed.status)
        assertTrue(passed.detail.contains("no base ref"))
    }

    @Test
    fun `requireChangedOriginsFresh fails when a touched origin produced no fresh report`() {
        val delta = ChangeDelta(touchedOrigins = setOf("a", "b"), freshOrigins = setOf("a"))
        val failed = promotedGates(GateConfig(requireChangedOriginsFresh = true), emptyList(), true, delta = delta).single()
        assertEquals("changed origins measured", failed.gate)
        assertEquals(GateStatus.FAILED, failed.status)
        assertTrue(failed.detail.contains("b"))
    }

    @Test
    fun `requireChangedOriginsFresh passes when every touched origin is fresh`() {
        val delta = ChangeDelta(touchedOrigins = setOf("a"), freshOrigins = setOf("a"))
        val passed = promotedGates(GateConfig(requireChangedOriginsFresh = true), emptyList(), true, delta = delta).single()
        assertEquals(GateStatus.PASSED, passed.status)
    }
}
