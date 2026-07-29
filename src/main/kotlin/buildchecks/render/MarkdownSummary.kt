package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/** Gate summary for $GITHUB_STEP_SUMMARY and PR comments. */
class MarkdownSummary : Renderer {

    override val fileName = "summary.md"

    override fun render(summary: CheckSummary): String = buildString {
        appendLine("## BuildChecks: ${if (summary.passed) "✅ passed" else "❌ failed"} " +
            "· confidence: ${summary.confidence.level}")
        appendLine()

        // The trust axis, spelled out: what makes this verdict worth less than a clean run.
        if (summary.confidence.reasons.isNotEmpty()) {
            appendLine("_Confidence — how completely the checks ran (informational, not a gate):_")
            summary.confidence.reasons.forEach { appendLine("- ${cell(it.summary)}") }
            appendLine()
        }

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

        // The new/unbaselined findings themselves, so a reviewer can triage from the comment without
        // opening the full report. Capped so a large regression can't produce an unbounded comment.
        val newFindings = summary.findings.filter { it.isNew }
        if (newFindings.isNotEmpty()) {
            appendLine()
            appendLine("### 🆕 New findings (${newFindings.size})")
            appendLine("| Severity | Check | Location | Message |")
            appendLine("|---|---|---|---|")
            newFindings.take(NEW_FINDINGS_SHOWN).forEach { reported ->
                val f = reported.finding
                val severity = when (f.severity) {
                    Severity.ERROR -> "🛑 error"
                    Severity.WARNING -> "⚠️ warning"
                    Severity.INFO -> "ℹ️ info"
                }
                val location = f.location?.let { it.path + (it.line?.let { line -> ":$line" } ?: "") } ?: "—"
                val check = listOf(f.tool, f.ruleId).filter { it.isNotBlank() }.joinToString(" · ")
                appendLine("| $severity | ${cell(check)} | ${cell(location)} | ${cell(f.message.take(160))} |")
            }
            if (newFindings.size > NEW_FINDINGS_SHOWN)
                appendLine("| … | | | _${newFindings.size - NEW_FINDINGS_SHOWN} more — see the full report_ |")
        }
    }

    private fun cell(text: String) = text.replace("|", "\\|").replace("\n", " ")

    private companion object {
        const val NEW_FINDINGS_SHOWN = 20
    }
}
