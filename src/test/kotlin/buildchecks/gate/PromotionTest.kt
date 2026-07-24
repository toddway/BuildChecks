package buildchecks.gate

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
}
