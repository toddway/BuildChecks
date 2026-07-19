package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/**
 * Self-contained browsable report: inline CSS/JS, no CDN, no external requests
 * (V4-PLAN.md §7). Tool report links point at directories the CLI copied under
 * the output dir, so the whole dir is one portable artifact.
 *
 * Written for a maintainer seeing it for the first time: every section opens with
 * a one-line explanation, jargon carries a hover tooltip, and bulk tables
 * (per-file coverage, the ingested-file inventory) are collapsed behind their
 * aggregate so the first screen is the verdict, not the data.
 */
class HtmlReport : Renderer {

    override val fileName = "index.html"

    override fun render(summary: CheckSummary): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"en\">")
        appendLine("<head>")
        appendLine("<meta charset=\"utf-8\">")
        appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        appendLine("<title>BuildChecks — ${if (summary.passed) "passed" else "failed"}</title>")
        appendLine("<style>$CSS</style>")
        appendLine("</head>")
        appendLine("<body>")
        header(summary)
        freshnessBanner(summary)
        gates(summary)
        findings(summary)
        tests(summary)
        coverage(summary)
        ingested(summary)
        appendLine("<script>$JS</script>")
        appendLine("</body>")
        appendLine("</html>")
    }

    private fun StringBuilder.header(summary: CheckSummary) {
        val badge = if (summary.passed) "<span class=\"badge pass\">PASSED</span>"
        else "<span class=\"badge fail\">FAILED</span>"
        appendLine("<header><h1>BuildChecks $badge</h1>")
        appendLine("<p class=\"muted\">One gated summary of this build's analysis findings, test results, " +
            "and coverage, aggregated from ${summary.files.size} report files your tools wrote.</p></header>")
    }

    private fun StringBuilder.freshnessBanner(summary: CheckSummary) {
        val freshness = summary.freshness?.takeIf { it.stale } ?: return
        appendLine("<div class=\"warning\">⚠️ Ingested reports differ in age by ${freshness.spreadMinutes} minutes " +
            "(tolerance ${freshness.toleranceMinutes}) — some numbers here may come from an earlier build. " +
            "Usual causes: a partial build, or a removed module whose old reports are still on disk. " +
            "Sort the Age column under Report files to find the outliers.</div>")
    }

    private fun StringBuilder.gates(summary: CheckSummary) {
        appendLine("<section><h2>Gates</h2>")
        appendLine("<p class=\"muted\">The pass/fail rules this run was checked against — any FAIL fails the " +
            "build (non-zero exit). Each detail reads <em>measured value (limit)</em>: a plain max/min limit " +
            "comes from this project's configuration; a limit marked <em>baseline</em> comes from the committed " +
            "snapshot of the last accepted state (<code>buildchecks baseline</code>), so those numbers can only " +
            "hold steady or improve. Hover a gate name for what it checks.</p>")
        appendLine("<table>")
        appendLine("<thead><tr><th>Gate</th><th>Status</th><th>Detail</th></tr></thead><tbody>")
        summary.gates.forEach { result ->
            val status = when (result.status) {
                GateStatus.PASSED -> "<td class=\"pass\">PASS</td>"
                GateStatus.FAILED -> "<td class=\"fail\">FAIL</td>"
                GateStatus.SKIPPED -> "<td class=\"skip\" title=\"Not evaluated — the detail column says why.\">SKIP</td>"
            }
            appendLine("<tr><td>${help(result.gate, GATE_EXPLANATIONS[result.gate])}</td>$status<td>${escape(result.detail)}</td></tr>")
        }
        appendLine("</tbody></table></section>")
    }

    // Section-level drill-down links into each tool's own copied HTML report.
    private fun StringBuilder.toolLinks(summary: CheckSummary, formats: Set<String>) {
        val links = summary.files
            .filter { it.format in formats && it.toolReport != null }
            .distinctBy { it.toolReport }
        if (links.isEmpty()) return
        appendLine("<p>Tool reports: " + links.joinToString(" · ") {
            "<a href=\"${escape(it.toolReport!!)}\">${escape(label(it.path))}</a>"
        } + "</p>")
    }

    // "build/reports/jvmTestCoverage/jvmTestCoverage.xml" -> "jvmTestCoverage";
    // "build/test-results/jvmTest/TEST-x.xml" -> "jvmTest"
    private fun label(path: String): String {
        val name = path.substringAfterLast('/').substringBeforeLast('.')
        return if (name.startsWith("TEST-")) path.substringBeforeLast('/').substringAfterLast('/') else name
    }

    private fun StringBuilder.findings(summary: CheckSummary) {
        appendLine("<section><h2>Findings (${summary.findings.size})</h2>")
        appendLine("<p class=\"muted\">Every issue the ingested analysis tools reported. " +
            "${help("NEW", NEW_EXPLANATION)} marks findings introduced since the baseline snapshot.</p>")
        toolLinks(summary, setOf("sarif", "checkstyle", "cpd"))
        if (summary.findings.isEmpty()) {
            appendLine("<p>None of the ingested analysis reports contain findings.</p></section>")
            return
        }
        val tools = summary.findings.map { it.finding.tool }.distinct().sorted()
        appendLine("<div class=\"controls\">")
        appendLine("<input id=\"search\" type=\"search\" placeholder=\"filter by text…\">")
        appendLine("<select id=\"severity\"><option value=\"\">all severities</option>" +
            Severity.entries.joinToString("") { "<option>${it.name}</option>" } + "</select>")
        appendLine("<select id=\"tool\"><option value=\"\">all tools</option>" +
            tools.joinToString("") { "<option>${escape(it)}</option>" } + "</select>")
        appendLine("<label><input id=\"newonly\" type=\"checkbox\"> new only</label>")
        appendLine("</div>")
        appendLine("<table id=\"findings\" class=\"sortable\">")
        appendLine("<thead><tr><th>Severity</th><th>Tool</th><th>Rule</th><th>Location</th><th>Message</th><th>New</th></tr></thead><tbody>")
        summary.findings.forEach { reported ->
            val finding = reported.finding
            val location = finding.location?.let { "${it.path}:${it.line ?: 0}" } ?: ""
            appendLine("<tr data-severity=\"${finding.severity.name}\" data-tool=\"${escape(finding.tool)}\" data-new=\"${reported.isNew}\">" +
                "<td class=\"sev-${finding.severity.name.lowercase()}\">${finding.severity.name}</td>" +
                "<td>${escape(finding.tool)}</td>" +
                "<td>${escape(finding.ruleId)}</td>" +
                "<td class=\"path\">${escape(location)}</td>" +
                "<td>${escape(finding.message)}</td>" +
                "<td>${if (reported.isNew) "<span class=\"badge fail\" title=\"${escape(NEW_EXPLANATION)}\">NEW</span>" else ""}</td></tr>")
        }
        appendLine("</tbody></table></section>")
    }

    private fun StringBuilder.tests(summary: CheckSummary) {
        if (summary.tests.isEmpty()) return
        val failed = summary.tests.filter { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
        val skipped = summary.tests.count { it.status == TestStatus.SKIPPED }
        appendLine("<section><h2>Tests (${summary.tests.size} total, ${failed.size} failed, $skipped skipped)</h2>")
        appendLine("<p class=\"muted\">From the JUnit XML the test runs wrote. Failures are listed here; " +
            "full output is in the tool reports.</p>")
        toolLinks(summary, setOf("junit"))
        if (failed.isEmpty()) {
            appendLine("<p>All tests passed.</p></section>")
            return
        }
        appendLine("<table><thead><tr><th>Suite</th><th>Test</th><th>Message</th></tr></thead><tbody>")
        failed.forEach {
            appendLine("<tr><td>${escape(it.suite)}</td><td>${escape(it.name)}</td><td>${escape(it.message ?: "")}</td></tr>")
        }
        appendLine("</tbody></table></section>")
    }

    private fun StringBuilder.coverage(summary: CheckSummary) {
        val coverage = summary.coverage ?: return
        val percent = coverage.linePercent?.let { "%.2f%%".format(it) } ?: "n/a"
        appendLine("<section><h2>Coverage $percent</h2>")
        appendLine("<p class=\"muted\">${"%,d".format(coverage.linesCovered)} of ${"%,d".format(coverage.linesTotal)} " +
            "executable lines covered, totaled across every ingested coverage report. This is the number the " +
            "coverage gates check. For line-by-line annotated source, open the tool reports.</p>")
        toolLinks(summary, setOf("jacoco", "cobertura", "lcov"))
        appendLine("<details><summary>Per-file line coverage (${coverage.files.size} files)</summary>")
        appendLine("<table class=\"sortable\"><thead><tr><th>File</th><th>Covered</th><th>Total</th><th>%</th></tr></thead><tbody>")
        coverage.files.sortedBy { it.path }.forEach { file ->
            val filePercent = if (file.lines.isEmpty()) "" else "%.1f".format(100.0 * file.linesCovered / file.lines.size)
            appendLine("<tr><td class=\"path\">${escape(file.path)}</td><td>${file.linesCovered}</td>" +
                "<td>${file.lines.size}</td><td>$filePercent</td></tr>")
        }
        appendLine("</tbody></table></details></section>")
    }

    private fun StringBuilder.ingested(summary: CheckSummary) {
        val formatCounts = summary.files.groupingBy { it.format }.eachCount()
            .entries.sortedByDescending { it.value }
            .joinToString(" · ") { "${it.value} ${it.key}" }
        appendLine("<footer><h2>Report files (${summary.files.size} read)</h2>")
        appendLine("<p class=\"muted\">Everything BuildChecks found and understood" +
            (if (formatCounts.isEmpty()) "." else ": $formatCounts.") +
            " Numbers above are computed only from these files.</p>")
        if (summary.files.isNotEmpty()) {
            appendLine("<details><summary>All ${summary.files.size} files</summary>")
            appendLine("<table class=\"sortable\">")
            appendLine("<thead><tr><th>File</th><th>Format</th>" +
                "<th>${help("Age", "Minutes between the report file's last modification and this check run — old reports may be stale.")}</th>" +
                "<th>Tool report</th></tr></thead><tbody>")
            summary.files.forEach { file ->
                val age = summary.freshness?.ageMinutes?.get(file.path)?.let { "$it min" } ?: ""
                // no link = the tool produced no html report next to the ingested file
                val link = file.toolReport?.let { "<a href=\"${escape(it)}\">open</a>" } ?: "—"
                appendLine("<tr><td class=\"path\">${escape(file.path)}</td><td>${escape(file.format)}</td><td>$age</td><td>$link</td></tr>")
            }
            appendLine("</tbody></table></details>")
        }
        if (summary.notUnderstood.isNotEmpty()) {
            appendLine("<details><summary>Found but not understood (${summary.notUnderstood.size})</summary>")
            appendLine("<p class=\"muted\">These matched the report search but no parser recognized their " +
                "content, so they contribute nothing above. Harmless unless a report you expected is listed here — " +
                "if so, check the tool is writing one of the supported formats " +
                "(SARIF, JUnit, JaCoCo, Cobertura, LCOV, Checkstyle, CPD).</p><ul>")
            summary.notUnderstood.forEach { appendLine("<li class=\"path\">${escape(it)}</li>") }
            appendLine("</ul></details>")
        }
        appendLine("</footer>")
    }

    private fun help(text: String, explanation: String?): String =
        if (explanation == null) escape(text)
        else "<span class=\"help\" title=\"${escape(explanation)}\">${escape(text)}</span>"

    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private companion object {
        const val NEW_EXPLANATION = "Not present in the baseline snapshot (buildchecks-baseline.txt) — " +
            "introduced since the last `buildchecks baseline` run."

        val GATE_EXPLANATIONS = mapOf(
            "new findings" to "Findings that are not in the baseline — introduced since the snapshot " +
                "was last taken. The other findings gates compare totals; this one catches each " +
                "individual new issue.",
            "total findings" to "The finding total must not rise above the baseline's total, so the " +
                "backlog can only stay level or shrink. Run `buildchecks baseline` to accept a new level.",
            "coverage" to "Overall line coverage must not drop below the baseline's coverage (less the " +
                "configured tolerance) — it can only stay level or rise.",
            "minimum coverage" to "An absolute floor from your config: overall line coverage must be at " +
                "least this, regardless of the baseline.",
            "errors" to "Error-severity findings must not exceed the configured maximum.",
            "warnings" to "Warning-severity findings must not exceed the configured maximum.",
            "test failures" to "Failed tests must not exceed the configured maximum (0 unless configured).",
            "changed-line coverage" to "Coverage of only the lines added or changed relative to the git " +
                "base ref. Skipped with a notice when git or a base ref isn't available.",
        )

        val CSS = """
            :root { color-scheme: light dark; }
            body { font: 14px/1.5 system-ui, sans-serif; margin: 0 auto; max-width: 72rem; padding: 1rem 2rem 4rem; }
            h1 { font-size: 1.4rem; margin-bottom: .2rem; } h2 { font-size: 1.1rem; margin-top: 2rem; }
            .muted { margin: .2rem 0 .8rem; opacity: .65; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border-bottom: 1px solid color-mix(in srgb, currentColor 20%, transparent); padding: .35rem .6rem; text-align: left; vertical-align: top; }
            th { user-select: none; white-space: nowrap; }
            table.sortable th { cursor: pointer; }
            table.sortable th::after { content: " ⇅"; font-weight: 400; opacity: .35; }
            table.sortable th[data-asc="true"]::after { content: " ▲"; opacity: 1; }
            table.sortable th[data-asc="false"]::after { content: " ▼"; opacity: 1; }
            .help { border-bottom: 1px dotted; cursor: help; }
            details { margin: .8rem 0; }
            summary { cursor: pointer; font-weight: 600; margin-bottom: .4rem; }
            .badge { border-radius: .3rem; font-size: .75em; font-weight: 700; padding: .15rem .5rem; vertical-align: middle; }
            .badge.pass, td.pass { background: #1a7f37; color: #fff; }
            .badge.fail, td.fail { background: #cf222e; color: #fff; }
            td.skip { background: #9a6700; color: #fff; }
            td.pass, td.fail, td.skip { font-weight: 700; text-align: center; width: 4rem; }
            .sev-error { color: #cf222e; font-weight: 700; }
            .sev-warning { color: #9a6700; font-weight: 700; }
            .sev-info { opacity: .75; }
            .path { font-family: ui-monospace, monospace; font-size: .85em; word-break: break-all; }
            .warning { background: #fff8c5; border: 1px solid #d4a72c; border-radius: .4rem; color: #4d3800; margin: 1rem 0; padding: .6rem 1rem; }
            .controls { display: flex; flex-wrap: wrap; gap: .6rem; margin: .8rem 0; }
            .controls input[type=search] { flex: 1; min-width: 12rem; padding: .3rem .5rem; }
        """.trimIndent()

        val JS = """
            const table = document.getElementById('findings');
            if (table) {
              const rows = [...table.tBodies[0].rows];
              const search = document.getElementById('search');
              const severity = document.getElementById('severity');
              const tool = document.getElementById('tool');
              const newonly = document.getElementById('newonly');
              const apply = () => {
                const text = search.value.toLowerCase();
                rows.forEach(row => {
                  row.hidden = (text && !row.textContent.toLowerCase().includes(text))
                    || (severity.value && row.dataset.severity !== severity.value)
                    || (tool.value && row.dataset.tool !== tool.value)
                    || (newonly.checked && row.dataset.new !== 'true');
                });
              };
              [search, severity, tool, newonly].forEach(el => el.addEventListener('input', apply));
            }
            document.querySelectorAll('table.sortable').forEach(sortable => {
              [...sortable.tHead.rows[0].cells].forEach((th, index) => {
                th.addEventListener('click', () => {
                  const asc = th.dataset.asc !== 'true';
                  [...sortable.tHead.rows[0].cells].forEach(cell => delete cell.dataset.asc);
                  th.dataset.asc = asc;
                  const body = sortable.tBodies[0];
                  [...body.rows]
                    .sort((a, b) => {
                      const x = a.cells[index].textContent.trim(), y = b.cells[index].textContent.trim();
                      const nx = parseFloat(x), ny = parseFloat(y);
                      const result = !isNaN(nx) && !isNaN(ny) ? nx - ny : x.localeCompare(y);
                      return asc ? result : -result;
                    })
                    .forEach(row => body.appendChild(row));
                });
              });
            });
        """.trimIndent()
    }
}
