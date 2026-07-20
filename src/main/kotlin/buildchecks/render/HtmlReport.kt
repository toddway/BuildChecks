package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.Severity
import buildchecks.model.TestStatus
import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.code
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.em
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.summary
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.ul
import kotlinx.html.unsafe

/**
 * Self-contained browsable report: inline CSS/JS, no CDN, no external requests
 * (V4-PLAN.md §7). Tool report links point at directories the CLI copied under
 * the output dir, so the whole dir is one portable artifact.
 *
 * Written for a maintainer seeing it for the first time: every section opens with
 * a one-line explanation, jargon carries a hover tooltip, and bulk tables
 * (per-file coverage, the ingested-file inventory) are collapsed behind their
 * aggregate so the first screen is the verdict, not the data.
 *
 * Markup is the kotlinx-html DSL (text and attributes escaped by construction);
 * the CSS/JS live next to this class as report.css / report.js and are inlined
 * at render time. To eyeball changes without a full check run, see
 * HtmlReportPreviewTest.
 */
class HtmlReport : Renderer {

    override val fileName = "index.html"

    override fun render(summary: CheckSummary): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendHTML(prettyPrint = false).html {
            lang = "en"
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title("BuildChecks — ${if (summary.passed) "passed" else "failed"}")
                style { unsafe { raw(CSS) } }
            }
            body {
                pageHeader(summary)
                freshnessBanner(summary)
                gates(summary)
                findings(summary)
                tests(summary)
                coverage(summary)
                ingested(summary)
                script { unsafe { raw(JS) } }
            }
        }
    }

    private fun BODY.pageHeader(summary: CheckSummary) {
        header {
            h1 {
                +"BuildChecks "
                if (summary.passed) span("badge pass") { +"PASSED" }
                else span("badge fail") { +"FAILED" }
            }
            p("muted") {
                +("One gated summary of this build's analysis findings, test results, and coverage, " +
                    "aggregated from ${summary.files.size} report files your tools wrote.")
            }
        }
    }

    private fun BODY.freshnessBanner(summary: CheckSummary) {
        val freshness = summary.freshness?.takeIf { it.stale } ?: return
        div("warning") {
            +("⚠️ Ingested reports differ in age by ${freshness.spreadMinutes} minutes " +
                "(tolerance ${freshness.toleranceMinutes}) — some numbers here may come from an earlier build. " +
                "Usual causes: a partial build, or a removed module whose old reports are still on disk. " +
                "Sort the Age column under Report files to find the outliers.")
        }
    }

    private fun BODY.gates(summary: CheckSummary) {
        section {
            h2 { +"Gates" }
            p("muted") {
                +("The pass/fail rules this run was checked against — any FAIL fails the build " +
                    "(non-zero exit). Each detail reads ")
                em { +"measured value (limit)" }
                +". Limits marked "
                em { +"baseline" }
                +" come from the committed snapshot of the last accepted state ("
                code { +"buildchecks baseline" }
                +("), so those numbers can only hold steady or improve; the rest are from this " +
                    "project's configuration. Hover a gate name for what it checks.")
            }
            table {
                thead { tr { th { +"Gate" }; th { +"Status" }; th { +"Detail" } } }
                tbody {
                    summary.gates.forEach { result ->
                        tr {
                            td { help(result.gate, GATE_EXPLANATIONS[result.gate]) }
                            when (result.status) {
                                GateStatus.PASSED -> td("pass") { +"PASS" }
                                GateStatus.FAILED -> td("fail") { +"FAIL" }
                                GateStatus.SKIPPED -> td("skip") {
                                    title = "Not evaluated — the detail column says why."
                                    +"SKIP"
                                }
                            }
                            td { +result.detail }
                        }
                    }
                }
            }
        }
    }

    // Section-level drill-down links into each tool's own copied HTML report.
    private fun FlowContent.toolLinks(summary: CheckSummary, formats: Set<String>) {
        val links = summary.files
            .filter { it.format in formats && it.toolReport != null }
            .distinctBy { it.toolReport }
        if (links.isEmpty()) return
        p {
            +"Tool reports: "
            links.forEachIndexed { index, file ->
                if (index > 0) +" · "
                a(href = file.toolReport!!) { +linkLabel(file.path) }
            }
        }
    }

    // "build/reports/jvmTestCoverage/jvmTestCoverage.xml" -> "jvmTestCoverage";
    // "build/test-results/jvmTest/TEST-x.xml" -> "jvmTest"
    private fun linkLabel(path: String): String {
        val name = path.substringAfterLast('/').substringBeforeLast('.')
        return if (name.startsWith("TEST-")) path.substringBeforeLast('/').substringAfterLast('/') else name
    }

    private fun BODY.findings(summary: CheckSummary) {
        section {
            h2 { +"Findings (${summary.findings.size})" }
            p("muted") {
                +"Every issue the ingested analysis tools reported. "
                help("NEW", NEW_EXPLANATION)
                +" marks findings introduced since the baseline snapshot."
            }
            toolLinks(summary, setOf("sarif", "checkstyle", "cpd"))
            if (summary.findings.isEmpty()) {
                p { +"None of the ingested analysis reports contain findings." }
                return@section
            }
            val tools = summary.findings.map { it.finding.tool }.distinct().sorted()
            div("controls") {
                input(InputType.search) { id = "search"; placeholder = "filter by text…" }
                select {
                    id = "severity"
                    option { value = ""; +"all severities" }
                    Severity.entries.forEach { option { +it.name } }
                }
                select {
                    id = "tool"
                    option { value = ""; +"all tools" }
                    tools.forEach { option { +it } }
                }
                label {
                    input(InputType.checkBox) { id = "newonly" }
                    +" new only"
                }
            }
            table(classes = "sortable") {
                id = "findings"
                thead {
                    tr {
                        th { +"Severity" }; th { +"Tool" }; th { +"Rule" }
                        th { +"Location" }; th { +"Message" }; th { +"New" }
                    }
                }
                tbody {
                    summary.findings.forEach { reported ->
                        val finding = reported.finding
                        tr {
                            attributes["data-severity"] = finding.severity.name
                            attributes["data-tool"] = finding.tool
                            attributes["data-new"] = reported.isNew.toString()
                            td("sev-${finding.severity.name.lowercase()}") { +finding.severity.name }
                            td { +finding.tool }
                            td { +finding.ruleId }
                            td("path") { +(finding.location?.let { "${it.path}:${it.line ?: 0}" } ?: "") }
                            td { +finding.message }
                            td {
                                if (reported.isNew) span("badge fail") {
                                    title = NEW_EXPLANATION
                                    +"NEW"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun BODY.tests(summary: CheckSummary) {
        if (summary.tests.isEmpty()) return
        val failed = summary.tests.filter { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR }
        val skipped = summary.tests.count { it.status == TestStatus.SKIPPED }
        section {
            h2 { +"Tests (${summary.tests.size} total, ${failed.size} failed, $skipped skipped)" }
            p("muted") {
                +("From the JUnit XML the test runs wrote. Failures are listed here; " +
                    "full output is in the tool reports.")
            }
            toolLinks(summary, setOf("junit"))
            if (failed.isEmpty()) {
                p { +"All tests passed." }
                return@section
            }
            table {
                thead { tr { th { +"Suite" }; th { +"Test" }; th { +"Message" } } }
                tbody {
                    failed.forEach { result ->
                        tr { td { +result.suite }; td { +result.name }; td { +(result.message ?: "") } }
                    }
                }
            }
        }
    }

    private fun BODY.coverage(summary: CheckSummary) {
        val coverage = summary.coverage ?: return
        val percent = coverage.linePercent?.let { "%.2f%%".format(it) } ?: "n/a"
        section {
            h2 { +"Coverage $percent" }
            p("muted") {
                +("${"%,d".format(coverage.linesCovered)} of ${"%,d".format(coverage.linesTotal)} " +
                    "executable lines covered, totaled across every ingested coverage report. This is the " +
                    "number the coverage gates check. For line-by-line annotated source, open the tool reports.")
            }
            toolLinks(summary, setOf("jacoco", "cobertura", "lcov"))
            details {
                summary { +"Per-file line coverage (${coverage.files.size} files)" }
                table(classes = "sortable") {
                    thead { tr { th { +"File" }; th { +"Covered" }; th { +"Total" }; th { +"%" } } }
                    tbody {
                        coverage.files.sortedBy { it.path }.forEach { file ->
                            tr {
                                td("path") { +file.path }
                                td { +file.linesCovered.toString() }
                                td { +file.lines.size.toString() }
                                td {
                                    if (file.lines.isNotEmpty()) {
                                        +"%.1f".format(100.0 * file.linesCovered / file.lines.size)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun BODY.ingested(summary: CheckSummary) {
        val formatCounts = summary.files.groupingBy { it.format }.eachCount()
            .entries.sortedByDescending { it.value }
            .joinToString(" · ") { "${it.value} ${it.key}" }
        footer {
            h2 { +"Report files (${summary.files.size} read)" }
            p("muted") {
                +("Everything BuildChecks found and understood" +
                    (if (formatCounts.isEmpty()) "." else ": $formatCounts.") +
                    " Numbers above are computed only from these files.")
            }
            if (summary.files.isNotEmpty()) {
                details {
                    summary { +"All ${summary.files.size} files" }
                    table(classes = "sortable") {
                        thead {
                            tr {
                                th { +"File" }
                                th { +"Format" }
                                th {
                                    help("Age", "Minutes between the report file's last modification " +
                                        "and this check run — old reports may be stale.")
                                }
                                th { +"Tool report" }
                            }
                        }
                        tbody {
                            summary.files.forEach { file ->
                                tr {
                                    td("path") { +file.path }
                                    td { +file.format }
                                    td { +(summary.freshness?.ageMinutes?.get(file.path)?.let { "$it min" } ?: "") }
                                    // no link = the tool produced no html report next to the ingested file
                                    td {
                                        val link = file.toolReport
                                        if (link != null) a(href = link) { +"open" } else +"—"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (summary.notUnderstood.isNotEmpty()) {
                details {
                    summary { +"Found but not understood (${summary.notUnderstood.size})" }
                    p("muted") {
                        +("These matched the report search but no parser recognized their content, " +
                            "so they contribute nothing above. Harmless unless a report you expected is " +
                            "listed here — if so, check the tool is writing one of the supported formats " +
                            "(SARIF, JUnit, JaCoCo, Cobertura, LCOV, Checkstyle, CPD).")
                    }
                    ul { summary.notUnderstood.forEach { li("path") { +it } } }
                }
            }
        }
    }

    private fun FlowOrPhrasingContent.help(text: String, explanation: String?) {
        if (explanation == null) text(text)
        else span("help") { title = explanation; +text }
    }

    private companion object {
        const val NEW_EXPLANATION = "Not present in the baseline snapshot (buildchecks-baseline.txt) — " +
            "introduced since the last `buildchecks baseline` run."

        val GATE_EXPLANATIONS = mapOf(
            "findings" to "Checked two ways against the baseline: no new findings beyond the allowed " +
                "max (each finding is fingerprinted, so pre-existing ones don't count), and the total " +
                "must not rise above the baseline's total. Run `buildchecks baseline` to accept the " +
                "current state.",
            "coverage" to "Overall line coverage must stay at or above the limit shown — the higher of " +
                "the baseline's coverage minus a small tolerance (never worse than the last accepted " +
                "state) or the configured minimum (the worst the project will ever accept).",
            "errors" to "Error-severity findings must not exceed the configured maximum.",
            "warnings" to "Warning-severity findings must not exceed the configured maximum.",
            "test failures" to "Failed tests must not exceed the configured maximum (0 unless configured).",
            "changed-line coverage" to "Coverage of only the lines added or changed relative to the git " +
                "base ref. Skipped with a notice when git or a base ref isn't available.",
        )

        val CSS = resource("report.css")
        val JS = resource("report.js")

        fun resource(name: String): String =
            checkNotNull(HtmlReport::class.java.getResourceAsStream(name)) { "missing classpath resource $name" }
                .bufferedReader().use { it.readText() }
    }
}
