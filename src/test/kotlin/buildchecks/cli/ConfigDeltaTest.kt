package buildchecks.cli

import buildchecks.gate.GateConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigDeltaTest {

    private fun cfg(gates: GateConfig) = Config(gates = gates)

    @Test
    fun `identical config is not loosened`() {
        val c = cfg(GateConfig(minCoveragePercent = 80.0, maxErrors = 0))
        assertTrue(configLoosened(c, c).isEmpty())
    }

    @Test
    fun `a lowered floor is loosening, including removing it entirely`() {
        assertEquals(
            listOf("min_coverage_percent 80.0 → 70.0"),
            configLoosened(cfg(GateConfig(minCoveragePercent = 80.0)), cfg(GateConfig(minCoveragePercent = 70.0))),
        )
        assertEquals(
            listOf("min_changed_line_coverage 80 → off"),
            configLoosened(cfg(GateConfig(minChangedLineCoverage = 80)), cfg(GateConfig(minChangedLineCoverage = null))),
        )
    }

    @Test
    fun `a raised cap is loosening, including removing it entirely`() {
        assertEquals(
            listOf("max_errors 0 → 5"),
            configLoosened(cfg(GateConfig(maxErrors = 0)), cfg(GateConfig(maxErrors = 5))),
        )
        assertEquals(
            listOf("max_errors 0 → off"),
            configLoosened(cfg(GateConfig(maxErrors = 0)), cfg(GateConfig(maxErrors = null))),
        )
    }

    @Test
    fun `tightening is not reported`() {
        val tighter = configLoosened(
            cfg(GateConfig(minCoveragePercent = 70.0, maxErrors = 5, minChangedLineCoverage = null)),
            cfg(GateConfig(minCoveragePercent = 80.0, maxErrors = 0, minChangedLineCoverage = 90)),
        )
        assertTrue(tighter.isEmpty(), tighter.toString())
    }

    @Test
    fun `max_test_failures treats null as an effective zero`() {
        // unset (null = fail on any) -> 3 is a loosening; the effective floor rose from 0
        assertEquals(
            listOf("max_test_failures 0 → 3"),
            configLoosened(cfg(GateConfig(maxTestFailures = null)), cfg(GateConfig(maxTestFailures = 3))),
        )
    }

    @Test
    fun `disabling the ratchet, a gate floor, or a promotion is loosening`() {
        val loosened = configLoosened(
            cfg(GateConfig(ratchet = true, failOnSkippedGates = true, requireChangedOriginsFresh = true)),
            cfg(GateConfig(ratchet = false, failOnSkippedGates = false, requireChangedOriginsFresh = false)),
        )
        assertTrue(loosened.contains("ratchet on → off"))
        assertTrue(loosened.contains("fail_on_skipped_gates on → off"))
        assertTrue(loosened.contains("require_changed_origins_fresh on → off"))
    }
}
