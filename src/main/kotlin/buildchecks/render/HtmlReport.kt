package buildchecks.render

import buildchecks.model.ChangedFileCoverage
import buildchecks.model.ChangedLineCoverage
import buildchecks.model.CheckSummary
import buildchecks.model.ConfidenceWeight
import buildchecks.model.Freshness
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
                gates(summary)
                findings(summary)
                // Changed-line coverage answers "is the work in this diff tested?" — more actionable
                // than the whole-project Tests/Coverage rollups, so it leads them.
                changedCoverage(summary)
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
                +" "
                val level = summary.confidence.level
                span("badge conf-${level.name.lowercase()}") {
                    title = CONFIDENCE_EXPLANATION
                    +"confidence: $level"
                }
            }
            p("muted") {
                +("One gated summary of this build's analysis findings, test results, and coverage, " +
                    "aggregated from ${summary.files.size} report files your tools wrote.")
            }
            confidenceReasons(summary)
        }
    }

    // The trust axis spelled out: PASSED says the metrics held; confidence says how completely they
    // were checked. Informational — never affects the exit code (a signal that should block is
    // promoted to a gate via config). Only rendered when something lowered it below full.
    private fun FlowContent.confidenceReasons(summary: CheckSummary) {
        val reasons = summary.confidence.reasons
        if (reasons.isEmpty()) return
        div("confidence") {
            p("muted") {
                +"Why confidence is "
                em { +summary.confidence.level.name.lowercase() }
                +" — each reason lowers it; none change pass/fail:"
            }
            // MAJOR-first so the reason actually driving the verdict leads, not a benign MINOR below it.
            val ordered = reasons.sortedByDescending { it.weight == ConfidenceWeight.MAJOR }
            ul {
                ordered.forEach { reason ->
                    li {
                        weightBadge(reason.weight)
                        +" "
                        val help = CONFIDENCE_REASON_HELP[reason.signal]
                        if (help != null) span("help") { title = help; +reason.summary }
                        else +reason.summary
                    }
                }
            }
        }
    }

    // Per-reason severity chip: MAJOR (drops the whole verdict to LOW) vs MINOR (caps it at MEDIUM).
    private fun FlowOrPhrasingContent.weightBadge(weight: ConfidenceWeight) {
        val major = weight == ConfidenceWeight.MAJOR
        span("badge ${if (major) "weight-major" else "weight-minor"}") {
            title = if (major)
                "MAJOR — an intended check did not run at all this run, so on its own it drops the " +
                    "overall confidence to LOW."
            else
                "MINOR — the check ran but something makes it worth a little less; on its own it caps " +
                    "confidence at MEDIUM and never forces LOW."
            +weight.name
        }
    }

    // Report-level staleness surfaces per row rather than as one banner: a `stale?` chip on any row
    // (or aggregate) whose source report is an age-outlier, so a reader sees exactly which numbers to
    // trust. Both helpers no-op unless the freshness set as a whole is stale (Freshness.outlier).

    // Chip for a single row, keyed on the ingested report that produced it.
    private fun FlowOrPhrasingContent.staleChip(reportPath: String?, freshness: Freshness?) {
        val path = reportPath ?: return
        if (freshness?.outlier(path) != true) return
        +" "
        span("badge stale") {
            title = "From $path, ${freshness.ageMinutes[path]} min old — much older than the freshest " +
                "report this run, so it may predate the latest build and no longer be current."
            +"stale?"
        }
    }

    // Chip for an aggregate (a section total), flagged when any report of the given formats feeding
    // it is an outlier — the number blends a stale input even if no single visible row shows it.
    private fun FlowOrPhrasingContent.staleAggregateChip(summary: CheckSummary, formats: Set<String>) {
        val freshness = summary.freshness ?: return
        val stale = summary.files.filter { it.format in formats && freshness.outlier(it.path) }.map { it.path }
        if (stale.isEmpty()) return
        +" "
        span("badge stale") {
            title = "This total blends ${stale.size} report(s) much older than the freshest this run " +
                "(${stale.joinToString(", ")}) — some of it may predate the latest build. Any flagged " +
                "rows below show which."
            +"stale?"
        }
    }

    private fun BODY.gates(summary: CheckSummary) {
        section {
            h2 { +"Gates" }
            p("muted") { +"The pass/fail rules this build was checked against — any FAIL fails the build." }
            table {
                thead { tr { th { +"Gate" }; th { +"Status" }; th { help("Detail", DETAIL_EXPLANATION) } } }
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
                // Flag the individual report here, not only on the section total, so a reader sees
                // exactly which of these inputs is the age-outlier.
                staleChip(file.path, summary.freshness)
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
        val newCount = summary.findings.count { it.isNew }
        section {
            h2 {
                if (summary.hasBaseline) +"Findings — $newCount new (${summary.findings.size} total)"
                else +"Findings (${summary.findings.size})"
            }
            p("muted") {
                +"Every issue the ingested analysis tools reported. "
                help("NEW", NEW_EXPLANATION)
                +" marks findings added since the baseline"
                if (summary.hasBaseline) {
                    +"; only these show by default — clear "
                    em { +"new only" }
                    +" to see all."
                } else +"."
            }
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
                    // Default to new-only when there is a baseline: most rows are intentionally
                    // suppressed history, so the new findings are what a reader needs to see.
                    input(InputType.checkBox) {
                        id = "newonly"
                        if (summary.hasBaseline) attributes["checked"] = "checked"
                    }
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
                        val stale = reported.reportPath?.let { summary.freshness?.outlier(it) } == true
                        tr {
                            attributes["data-severity"] = finding.severity.name
                            attributes["data-tool"] = finding.tool
                            attributes["data-new"] = reported.isNew.toString()
                            if (stale) attributes["data-stale"] = "true"
                            td("sev-${finding.severity.name.lowercase()}") { +finding.severity.name }
                            td { +finding.tool }
                            td { +finding.ruleId }
                            td("path") {
                                val loc = finding.location?.let { "${it.path}:${it.line ?: 0}" }
                                when {
                                    loc == null -> {}
                                    reported.toolReport != null ->
                                        a(href = reported.toolReport!!) {
                                            title = "Open ${reported.finding.tool}'s report"
                                            +loc
                                        }
                                    else -> +loc
                                }
                                staleChip(reported.reportPath, summary.freshness)
                            }
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
            h2 {
                +"Tests (${summary.tests.size} total, ${failed.size} failed, $skipped skipped)"
                staleAggregateChip(summary, setOf("junit"))
            }
            p("muted") { +"From the ingested JUnit results — only failures are listed below." }
            toolLinks(summary, setOf("junit"))
            if (failed.isEmpty()) {
                p { +"All tests passed." }
                return@section
            }
            table {
                thead { tr { th { +"Suite" }; th { +"Test" }; th { +"Message" } } }
                tbody {
                    failed.forEach { result ->
                        tr {
                            td { +result.suite }
                            td { +result.name; staleChip(result.reportPath, summary.freshness) }
                            td { +(result.message ?: "") }
                        }
                    }
                }
            }
        }
    }

    private fun BODY.coverage(summary: CheckSummary) {
        val coverage = summary.coverage ?: return
        val percent = coverage.linePercent?.let { "%.2f%%".format(it) } ?: "n/a"
        section {
            h2 {
                +"Coverage $percent"
                staleAggregateChip(summary, setOf("jacoco", "cobertura", "lcov"))
            }
            p("muted") {
                +("${"%,d".format(coverage.linesCovered)} of ${"%,d".format(coverage.linesTotal)} " +
                    "executable lines covered, across every ingested coverage report — the figure the coverage gate checks.")
            }
            toolLinks(summary, setOf("jacoco", "cobertura", "lcov"))
            details {
                summary { +"Per-file line coverage (${coverage.files.size} files)" }
                table(classes = "sortable") {
                    thead { tr { th { +"File" }; th { +"Covered" }; th { +"Total" }; th { +"%" } } }
                    tbody {
                        coverage.files.sortedBy { it.path }.forEach { file ->
                            tr {
                                td("path") { +file.path; staleChip(file.reportPath, summary.freshness) }
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

    // The uncovered changed lines behind the changed-line coverage gate, so a reader can jump
    // straight to the gaps instead of opening every module's coverage report. Rendered only when
    // a diff was measured and something it changed is uncovered.
    private fun BODY.changedCoverage(summary: CheckSummary) {
        val measured = summary.changedLineCoverage as? ChangedLineCoverage.Measured ?: return
        val uncoveredFiles = measured.uncoveredFiles
        if (uncoveredFiles.isEmpty()) return
        section {
            h2 { +"Changed lines not covered (${measured.executableCount - measured.coveredCount})" }
            p("muted") {
                +"Lines this diff added or changed relative to "
                code { +measured.baseRef }
                +(" that no test covered — ${"%.2f".format(measured.percent)}% of " +
                    "${measured.executableCount} changed lines are covered.")
            }
            table(classes = "sortable") {
                thead {
                    tr { th { +"File" }; th { +"Uncovered" }; th { +"Covered" }; th { +"Changed" } }
                }
                tbody {
                    // Worst-first: the files with the most uncovered changed lines are where the
                    // gap is; the specific line numbers live in each file's linked coverage report.
                    uncoveredFiles.sortedWith(compareByDescending<ChangedFileCoverage> { it.uncovered.size }.thenBy { it.path })
                        .forEach { file ->
                            tr {
                                td("path") {
                                    val report = file.toolReport
                                    if (report != null) a(href = report) {
                                        title = "Open this file's coverage report"
                                        +file.path
                                    } else +file.path
                                    staleChip(file.reportPath, summary.freshness)
                                }
                                td { +file.uncovered.size.toString() }
                                td { +file.covered.size.toString() }
                                td { +file.executable.toString() }
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

        const val DETAIL_EXPLANATION = "Reads \"measured value (limit)\". A limit marked \"baseline\" comes " +
            "from the committed snapshot of the last accepted state (buildchecks baseline), so it can only " +
            "hold steady or improve; the rest come from this project's configuration."

        const val CONFIDENCE_EXPLANATION = "How completely the checks actually ran, separate from " +
            "pass/fail. PASSED says the tracked metrics held; confidence says how much that's worth — " +
            "a skipped gate, an unreadable or stale report, or a not-yet-baselined source each lower " +
            "it, as does — vs the git base ref — a touched module that didn't re-run, a loosened " +
            "baseline, or a loosened gate setting in this same change. Informational: it never " +
            "changes the exit code."

        // Per-reason tooltips for confidence signals whose wording needs unpacking. Keyed by the
        // ConfidenceReason.signal id so the model stays free of presentation text.
        val CONFIDENCE_REASON_HELP = mapOf(
            "changed-origins-stale" to "An origin is a module or source group BuildChecks measures, " +
                "grouped by where its reports are written.",
        )

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
            "expected reports" to "Every report present when the baseline was snapshotted must still be " +
                "ingested this run, grouped by origin (module/source). Catches a check silently disabled " +
                "or a source that stopped emitting its report. Run `buildchecks baseline` to accept an " +
                "intentional removal. Skipped against a baseline taken before this was recorded.",
            "no skipped gates" to "Turns any skipped gate into a failure (config fail_on_skipped_gates). " +
                "Off by default — a skipped gate normally only lowers confidence, not the exit code.",
            "base ref required" to "Fails when no git base ref could be resolved for delta analysis " +
                "(config require_base_ref). Off by default. Set --base-ref/git.base_ref or run on a PR build.",
            "baseline not loosened" to "Fails when the baseline was loosened vs the git base ref — " +
                "findings accepted, the coverage floor lowered, or an expected report dropped in this " +
                "same change (config fail_on_baseline_loosened). Off by default; normally only lowers confidence.",
            "changed origins measured" to "Fails when a module this change touched produced only a stale " +
                "report this run — one older than the freshest, so the change may not have been re-measured " +
                "(config require_changed_origins_fresh). A module that emitted no report at all is not " +
                "counted here; use the expected-reports gate to require a report's presence. Off by " +
                "default; normally only lowers confidence.",
        )

        val CSS = resource("report.css")
        val JS = resource("report.js")

        fun resource(name: String): String =
            checkNotNull(HtmlReport::class.java.getResourceAsStream(name)) { "missing classpath resource $name" }
                .bufferedReader().use { it.readText() }
    }
}
