package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/** Gate summary for $GITHUB_STEP_SUMMARY and PR comments. */
class MarkdownSummary : Renderer {

    override val fileName = "summary.md"

    override fun render(summary: CheckSummary): String = buildString {
        appendLine("## BuildChecks: ${if (summary.passed) "✅ passed" else "❌ failed"}")
        appendLine()

        summary.freshness?.takeIf { it.stale }?.let {
            appendLine("> ⚠️ Ingested reports differ in age by ${it.spreadMinutes} minutes " +
                "(tolerance ${it.toleranceMinutes}) — possible orphaned reports.")
            appendLine()
        }

        appendLine("| Gate | Status | Detail |")
        appendLine("|---|---|---|")
        summary.gates.forEach { result ->
            val mark = when (result.status) {
                GateStatus.PASSED -> "✅"
                GateStatus.FAILED -> "❌"
                GateStatus.SKIPPED -> "⏭️"
            }
            appendLine("| ${result.gate} | $mark | ${cell(result.detail)} |")
        }
        appendLine()

        val counts = Severity.entries.associateWith { severity ->
            summary.findings.count { it.finding.severity == severity }
        }
        val newCount = summary.findings.count { it.isNew }
        append("**Findings:** ${summary.findings.size} " +
            "(${counts[Severity.ERROR]} errors, ${counts[Severity.WARNING]} warnings, " +
            "${counts[Severity.INFO]} info, $newCount new)")
        if (summary.tests.isNotEmpty()) {
            val failed = summary.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
            append(" · **Tests:** ${summary.tests.size} ($failed failed)")
        }
        summary.coverage?.linePercent?.let { append(" · **Coverage:** ${"%.2f%%".format(it)}") }
        appendLine()
    }

    private fun cell(text: String) = text.replace("|", "\\|").replace("\n", " ")
}
