package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus

/**
 * Self-contained browsable report: inline CSS/JS, no CDN, no external requests
 * (V4-PLAN.md §7). Tool report links point at directories the CLI copied under
 * the output dir, so the whole dir is one portable artifact.
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
        appendLine("<header><h1>BuildChecks $badge</h1></header>")
    }

    private fun StringBuilder.freshnessBanner(summary: CheckSummary) {
        val freshness = summary.freshness?.takeIf { it.stale } ?: return
        appendLine("<div class=\"warning\">⚠️ Ingested reports differ in age by ${freshness.spreadMinutes} minutes " +
            "(tolerance ${freshness.toleranceMinutes}) — possible orphaned reports from removed modules or a partial build.</div>")
    }

    private fun StringBuilder.gates(summary: CheckSummary) {
        appendLine("<section><h2>Gates</h2><table>")
        appendLine("<thead><tr><th>Gate</th><th>Status</th><th>Detail</th></tr></thead><tbody>")
        summary.gates.forEach { result ->
            val status = when (result.status) {
                GateStatus.PASSED -> "<td class=\"pass\">PASS</td>"
                GateStatus.FAILED -> "<td class=\"fail\">FAIL</td>"
                GateStatus.SKIPPED -> "<td class=\"skip\">SKIP</td>"
            }
            appendLine("<tr><td>${escape(result.gate)}</td>$status<td>${escape(result.detail)}</td></tr>")
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
        toolLinks(summary, setOf("sarif", "checkstyle", "cpd"))
        if (summary.findings.isEmpty()) {
            appendLine("<p>No findings.</p></section>")
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
                "<td>${if (reported.isNew) "<span class=\"badge fail\">NEW</span>" else ""}</td></tr>")
        }
        appendLine("</tbody></table></section>")
    }

    private fun StringBuilder.tests(summary: CheckSummary) {
        if (summary.tests.isEmpty()) return
        val failed = summary.tests.filter { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
        val skipped = summary.tests.count { it.status == TestStatus.SKIPPED }
        appendLine("<section><h2>Tests (${summary.tests.size} total, ${failed.size} failed, $skipped skipped)</h2>")
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
        toolLinks(summary, setOf("jacoco", "cobertura", "lcov"))
        appendLine("<table class=\"sortable\"><thead><tr><th>File</th><th>Covered</th><th>Total</th><th>%</th></tr></thead><tbody>")
        coverage.files.sortedBy { it.path }.forEach { file ->
            val filePercent = if (file.lines.isEmpty()) "" else "%.1f".format(100.0 * file.linesCovered / file.lines.size)
            appendLine("<tr><td class=\"path\">${escape(file.path)}</td><td>${file.linesCovered}</td>" +
                "<td>${file.lines.size}</td><td>$filePercent</td></tr>")
        }
        appendLine("</tbody></table></section>")
    }

    private fun StringBuilder.ingested(summary: CheckSummary) {
        appendLine("<footer><h2>Ingested files</h2><table>")
        appendLine("<thead><tr><th>File</th><th>Format</th><th>Age</th><th>Tool report</th></tr></thead><tbody>")
        summary.files.forEach { file ->
            val age = summary.freshness?.ageMinutes?.get(file.path)?.let { "$it min" } ?: ""
            val link = file.toolReport?.let { "<a href=\"${escape(it)}\">open</a>" } ?: ""
            appendLine("<tr><td class=\"path\">${escape(file.path)}</td><td>${escape(file.format)}</td><td>$age</td><td>$link</td></tr>")
        }
        appendLine("</tbody></table>")
        if (summary.notUnderstood.isNotEmpty()) {
            appendLine("<h2>Found but not understood</h2><ul>")
            summary.notUnderstood.forEach { appendLine("<li class=\"path\">${escape(it)}</li>") }
            appendLine("</ul>")
        }
        appendLine("</footer>")
    }

    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private companion object {
        val CSS = """
            :root { color-scheme: light dark; }
            body { font: 14px/1.5 system-ui, sans-serif; margin: 0 auto; max-width: 72rem; padding: 1rem 2rem 4rem; }
            h1 { font-size: 1.4rem; } h2 { font-size: 1.1rem; margin-top: 2rem; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border-bottom: 1px solid color-mix(in srgb, currentColor 20%, transparent); padding: .35rem .6rem; text-align: left; vertical-align: top; }
            th { cursor: pointer; user-select: none; white-space: nowrap; }
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
