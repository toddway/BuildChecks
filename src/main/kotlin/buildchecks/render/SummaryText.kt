package buildchecks.render

import buildchecks.model.ChangedLineCoverage
import buildchecks.model.CheckSummary
import buildchecks.model.ConfidenceLevel
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/**
 * A single-line, human-readable gate headline, short enough for a commit-status description
 * (kept within GitHub's ~140-char limit). BuildChecks composes this itself so CI glue can post it
 * verbatim — `-f description="$(cat summary.txt)"` — instead of reconstructing a sentence from
 * summary.json in every project's build script.
 *
 * It packs one compact `<metric> (STATUS)` segment per gate that ran, e.g.
 * `2 new findings (FAIL), 61.16% coverage (FAIL), 0/4990 test failures (PASS), 4 missing reports (FAIL)`.
 * Failed gates come first so the signal a viewer most needs survives truncation; skipped gates carry
 * no measurement (and are already implied by a lowered confidence) so they're left off.
 */
class SummaryText : Renderer {

    override val fileName = "summary.txt"

    override fun render(summary: CheckSummary): String {
        val ran = summary.gates.filter { it.status != GateStatus.SKIPPED }
        // Stable sort keeps each gate's natural order within its status band; FAILED (0) leads.
        val segments = ran.sortedBy { if (it.status == GateStatus.FAILED) 0 else 1 }
            .map { segment(it, summary) }
            .toMutableList()
        if (segments.isEmpty()) segments += "no gates ran"
        // Trust axis: called out only when below full, so a clean run's headline stays terse.
        if (summary.confidence.level != ConfidenceLevel.HIGH) {
            segments += "confidence ${summary.confidence.level.name.lowercase()}"
        }
        return clip(segments.joinToString(", "))
    }

    // Gate names mirror the *Gate.NAME constants; matched as strings so render stays off the gate package.
    private fun segment(gate: buildchecks.model.GateResult, summary: CheckSummary): String {
        val body = when (gate.gate) {
            "findings" -> summary.findings.count { it.isNew }.let { "$it new ${plural(it, "finding")}" }
            "coverage" ->
                "${summary.coverage?.linePercent?.let { "%.2f%%".format(it) } ?: "?"} coverage"
            "test failures" -> {
                val failures = summary.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
                "$failures/${summary.tests.size} test failures"
            }
            "expected reports" -> {
                // "N expected report(s) missing: …" on FAIL; on PASS nothing is missing.
                val missing = if (gate.status == GateStatus.FAILED) leadingInt(gate.detail) else 0
                "$missing missing ${plural(missing, "report")}"
            }
            "changed-line coverage" -> when (val c = summary.changedLineCoverage) {
                is ChangedLineCoverage.Measured -> "${"%.2f%%".format(c.percent)} changed-line coverage"
                else -> "changed-line coverage"
            }
            "errors" -> summary.findings.count { it.finding.severity == Severity.ERROR }
                .let { "$it ${plural(it, "error")}" }
            "warnings" -> summary.findings.count { it.finding.severity == Severity.WARNING }
                .let { "$it ${plural(it, "warning")}" }
            else -> gate.gate // promotions and any future gate: name it as-is
        }
        return "$body (${tag(gate.status)})"
    }

    private fun tag(status: GateStatus) = when (status) {
        GateStatus.FAILED -> "FAIL"
        GateStatus.PASSED -> "PASS"
        GateStatus.SKIPPED -> "SKIP"
    }

    private fun leadingInt(s: String) = Regex("\\d+").find(s)?.value?.toIntOrNull() ?: 0

    private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"

    private fun clip(line: String, max: Int = 140) =
        if (line.length <= max) line else line.take(max - 1).trimEnd() + "…"
}
