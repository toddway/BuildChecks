package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/** Compact totals table plus one line per gate; always printed. */
class ConsoleSummary {

    fun render(summary: CheckSummary): String = buildString {
        val rows = totals(summary)
        val label = rows.maxOf { it.first.length }
        val value = rows.maxOf { it.second.length }
        appendLine("┌─${"─".repeat(label)}─┬─${"─".repeat(value)}─┐")
        rows.forEach { (name, amount) ->
            appendLine("│ ${name.padEnd(label)} │ ${amount.padStart(value)} │")
        }
        appendLine("└─${"─".repeat(label)}─┴─${"─".repeat(value)}─┘")

        summary.gates.forEach { result ->
            val mark = when (result.status) {
                GateStatus.PASSED -> "PASS"
                GateStatus.FAILED -> "FAIL"
                GateStatus.SKIPPED -> "SKIP"
            }
            appendLine("$mark  ${result.gate}: ${result.detail}")
        }

        // The named finding (4.1): covered-but-not-verified. Called out on its own because two
        // adjacent percentages don't read as the contradiction they are.
        summary.contradiction?.let {
            appendLine()
            appendLine("CONTRADICTION: changed lines are %.0f%% covered but only %.0f%% mutation-killed"
                .format(it.coveragePercent, it.mutationPercent))
            appendLine("  tests execute this change without verifying it — coverage counts the line, " +
                "mutation checks the assertion")
        }

        // The trust axis: how completely the checks ran, separate from pass/fail. Always shown so a
        // full-confidence run reads as deliberate, not silent.
        appendLine()
        appendLine("confidence: ${summary.confidence.level}")
        summary.confidence.reasons.forEach { appendLine("  - ${it.summary}") }

        summary.freshness?.takeIf { it.stale }?.let {
            appendLine()
            appendLine("WARNING: ingested reports differ in age by ${it.spreadMinutes} minutes " +
                "(tolerance ${it.toleranceMinutes}) — possible orphaned reports from removed modules or a partial build")
        }
    }.trimEnd()

    private fun totals(summary: CheckSummary): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        rows += "errors" to summary.findings.count { it.finding.severity == Severity.ERROR }.toString()
        rows += "warnings" to summary.findings.count { it.finding.severity == Severity.WARNING }.toString()
        rows += "info" to summary.findings.count { it.finding.severity == Severity.INFO }.toString()
        rows += "new findings" to summary.findings.count { it.isNew }.toString()
        if (summary.tests.isNotEmpty()) {
            val failed = summary.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
            rows += "tests" to "${summary.tests.size} (${failed} failed)"
        }
        summary.coverage?.linePercent?.let { rows += "coverage" to "%.2f%%".format(it) }
        return rows
    }
}
