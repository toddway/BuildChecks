package buildchecks.gate

import buildchecks.model.GateResult
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/** Optional absolute floors (V4-PLAN.md §4); each configured floor yields one result. */
class FloorsGate(private val config: GateConfig) : Gate {

    override fun evaluate(context: GateContext): List<GateResult> {
        val results = mutableListOf<GateResult>()

        config.minCoveragePercent?.let { floor ->
            val percent = context.coverage?.linePercent
            results += if (percent == null) {
                GateResult("coverage floor", GateStatus.SKIPPED, "no coverage data")
            } else {
                GateResult(
                    "coverage floor",
                    if (percent >= floor) GateStatus.PASSED else GateStatus.FAILED,
                    "%.2f%% (min %.1f%%)".format(percent, floor),
                )
            }
        }

        config.maxErrors?.let { max ->
            val errors = context.findings.count { it.finding.severity == Severity.ERROR }
            results += GateResult(
                "errors",
                if (errors <= max) GateStatus.PASSED else GateStatus.FAILED,
                "$errors errors (max $max)",
            )
        }

        config.maxWarnings?.let { max ->
            val warnings = context.findings.count { it.finding.severity == Severity.WARNING }
            results += GateResult(
                "warnings",
                if (warnings <= max) GateStatus.PASSED else GateStatus.FAILED,
                "$warnings warnings (max $max)",
            )
        }

        if (context.tests.isNotEmpty()) {
            val max = config.maxTestFailures ?: 0
            val failures = context.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
            results += GateResult(
                "test failures",
                if (failures <= max) GateStatus.PASSED else GateStatus.FAILED,
                "$failures failed of ${context.tests.size} tests (max $max)",
            )
        }

        return results
    }
}
