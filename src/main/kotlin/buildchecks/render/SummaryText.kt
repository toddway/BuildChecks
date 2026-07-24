package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.ConfidenceLevel
import buildchecks.model.GateStatus
import buildchecks.model.TestStatus

/**
 * A single-line, human-readable gate headline, short enough for a commit-status description
 * (kept within GitHub's ~140-char limit). BuildChecks composes this itself so CI glue can post it
 * verbatim — `-f description="$(cat summary.txt)"` — instead of reconstructing a sentence from
 * summary.json in every project's build script. When per-gate enforcement lands (V4-PLAN §11
 * roadmap #6) the required-vs-advisory distinction is expressed here, in the one place that knows
 * the gate model.
 */
class SummaryText : Renderer {

    override val fileName = "summary.txt"

    override fun render(summary: CheckSummary): String {
        val failed = summary.gates.filter { it.status == GateStatus.FAILED }.map { it.gate }
        val parts = mutableListOf(
            if (failed.isEmpty()) "all gates passed" else "gates failed: ${failed.joinToString(", ")}",
        )
        summary.coverage?.linePercent?.let { parts += "coverage ${"%.2f%%".format(it)}" }
        if (summary.tests.isNotEmpty()) {
            val failures = summary.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
            parts += "${summary.tests.size} ${plural(summary.tests.size, "test")}, $failures failed"
        }
        val newFindings = summary.findings.count { it.isNew }
        parts += "$newFindings new ${plural(newFindings, "finding")}"
        // Trust axis: called out only when it's below full, so a clean run's headline stays terse.
        if (summary.confidence.level != ConfidenceLevel.HIGH) {
            parts += "confidence ${summary.confidence.level.name.lowercase()}"
        }

        return clip(parts.joinToString(" · "))
    }

    private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"

    private fun clip(line: String, max: Int = 140) =
        if (line.length <= max) line else line.take(max - 1).trimEnd() + "…"
}
