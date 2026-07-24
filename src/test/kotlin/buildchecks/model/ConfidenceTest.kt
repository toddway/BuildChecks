package buildchecks.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfidenceTest {

    private fun gate(name: String, status: GateStatus) = GateResult(name, status, "detail")

    @Test
    fun `no signals is full confidence`() {
        val c = confidence(
            gates = listOf(gate("findings", GateStatus.PASSED)),
            freshness = null,
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.HIGH, c.level)
        assertTrue(c.reasons.isEmpty())
    }

    @Test
    fun `a skipped gate is MAJOR and drops to LOW on its own`() {
        val c = confidence(
            gates = listOf(gate("findings", GateStatus.PASSED), gate("changed-line coverage", GateStatus.SKIPPED)),
            freshness = null,
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        val reason = c.reasons.single()
        assertEquals("skipped-gates", reason.signal)
        assertEquals(ConfidenceWeight.MAJOR, reason.weight)
        assertTrue(reason.summary.contains("changed-line coverage"))
    }

    @Test
    fun `stale reports are only MINOR, capping at MEDIUM`() {
        // absolute-age staleness is legitimate on an unchanged module in an incremental build,
        // so it nudges confidence, never forces LOW (that's 4.2's change-scoped freshness)
        val c = confidence(
            gates = listOf(gate("findings", GateStatus.PASSED)),
            freshness = Freshness(mapOf("a" to 0L, "b" to 60L), toleranceMinutes = 15),
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.MEDIUM, c.level)
        assertEquals("stale-reports", c.reasons.single().signal)
        assertEquals(ConfidenceWeight.MINOR, c.reasons.single().weight)
    }

    @Test
    fun `a fresh set contributes no staleness reason`() {
        val c = confidence(
            gates = emptyList(),
            freshness = Freshness(mapOf("a" to 0L, "b" to 5L), toleranceMinutes = 15),
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.HIGH, c.level)
    }

    @Test
    fun `not-understood and new-report are MINOR with counts folded in`() {
        val c = confidence(
            gates = emptyList(),
            freshness = null,
            notUnderstood = listOf("a.txt", "b.txt"),
            newReportLabels = listOf("junit in services/auth"),
        )
        assertEquals(ConfidenceLevel.MEDIUM, c.level)
        val bySignal = c.reasons.associateBy { it.signal }
        assertTrue(bySignal["not-understood"]!!.summary.contains("2 report files"))
        assertTrue(bySignal["new-report"]!!.summary.contains("junit in services/auth"))
        assertTrue(c.reasons.all { it.weight == ConfidenceWeight.MINOR })
    }

    @Test
    fun `any MAJOR reason forces LOW regardless of MINOR reasons`() {
        val c = confidence(
            gates = listOf(gate("expected reports", GateStatus.SKIPPED)),
            freshness = Freshness(mapOf("a" to 0L, "b" to 60L), toleranceMinutes = 15),
            notUnderstood = listOf("a.txt"),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        assertEquals(3, c.reasons.size)
    }
}
