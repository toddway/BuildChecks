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
    fun `an enforcing gate that skipped is MAJOR and drops to LOW on its own`() {
        val c = confidence(
            gates = listOf(gate("findings", GateStatus.PASSED), gate("coverage", GateStatus.SKIPPED)),
            freshness = null,
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        val reason = c.reasons.single()
        assertEquals("skipped-gates", reason.signal)
        assertEquals(ConfidenceWeight.MAJOR, reason.weight)
        assertTrue(reason.summary.contains("coverage"))
    }

    @Test
    fun `a skipped changed-line coverage gate does not lower confidence`() {
        // Severable/informational by design: it skips when there's simply nothing to measure
        // (no covered source changed, no base ref), which is not an intended check failing to run.
        val c = confidence(
            gates = listOf(gate("findings", GateStatus.PASSED), gate("changed-line coverage", GateStatus.SKIPPED)),
            freshness = null,
            notUnderstood = emptyList(),
            newReportLabels = emptyList(),
        )
        assertEquals(ConfidenceLevel.HIGH, c.level)
        assertTrue(c.reasons.isEmpty())
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
    fun `a touched origin with only a stale report is MAJOR and drops to LOW`() {
        // services/web was measured (it has a report) but that report is a stale age-outlier.
        val c = confidence(
            gates = emptyList(), freshness = null, notUnderstood = emptyList(), newReportLabels = emptyList(),
            delta = ChangeDelta(
                touchedOrigins = setOf("services/auth", "services/web"),
                reportedOrigins = setOf("services/auth", "services/web"),
                freshOrigins = setOf("services/auth"),
            ),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        val reason = c.reasons.single()
        assertEquals("changed-origins-stale", reason.signal)
        assertEquals(ConfidenceWeight.MAJOR, reason.weight)
        assertTrue(reason.summary.contains("services/web"))
        assertTrue(reason.summary.contains("stale report"))
    }

    @Test
    fun `a touched origin that produced no report at all is not flagged`() {
        // The core narrowing: absence of a report is not evidence a check fell (BuildChecks has no
        // notion of which origins should emit one), so a touched-but-unreported origin contributes
        // nothing here — the expected-reports gate owns the "a report went missing" concern.
        val c = confidence(
            gates = emptyList(), freshness = null, notUnderstood = emptyList(), newReportLabels = emptyList(),
            delta = ChangeDelta(touchedOrigins = setOf("."), reportedOrigins = emptySet(), freshOrigins = emptySet()),
        )
        assertEquals(ConfidenceLevel.HIGH, c.level)
        assertTrue(c.reasons.isEmpty())
    }

    @Test
    fun `every touched-and-measured origin fresh contributes no reason`() {
        val c = confidence(
            emptyList(), null, emptyList(), emptyList(),
            delta = ChangeDelta(touchedOrigins = setOf("a"), reportedOrigins = setOf("a"), freshOrigins = setOf("a")),
        )
        assertEquals(ConfidenceLevel.HIGH, c.level)
    }

    @Test
    fun `a loosened baseline is MAJOR and folds every specific into one reason`() {
        val c = confidence(
            emptyList(), null, emptyList(), emptyList(),
            delta = ChangeDelta(
                baselineFindingsAccepted = 3,
                baselineCoverageLowered = 2.5,
                baselineReportsDropped = listOf("junit in services/web"),
            ),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        val reason = c.reasons.single()
        assertEquals("baseline-loosened", reason.signal)
        assertTrue(reason.summary.contains("3 findings newly accepted"))
        assertTrue(reason.summary.contains("junit in services/web"))
    }

    @Test
    fun `a loosened config is MAJOR`() {
        val c = confidence(
            emptyList(), null, emptyList(), emptyList(),
            delta = ChangeDelta(configLoosened = listOf("min_coverage_percent 80.0 → 70.0")),
        )
        assertEquals(ConfidenceLevel.LOW, c.level)
        assertEquals("config-loosened", c.reasons.single().signal)
        assertTrue(c.reasons.single().summary.contains("min_coverage_percent"))
    }

    @Test
    fun `a null delta contributes no signal`() {
        val c = confidence(emptyList(), null, emptyList(), emptyList(), delta = null)
        assertEquals(ConfidenceLevel.HIGH, c.level)
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
