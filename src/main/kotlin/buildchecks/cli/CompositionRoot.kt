package buildchecks.cli

import buildchecks.gate.CapsGate
import buildchecks.gate.ChangedLineCoverageGate
import buildchecks.gate.ChangedLineMutationGate
import buildchecks.gate.CoverageGate
import buildchecks.gate.FindingsGate
import buildchecks.gate.Gate
import buildchecks.gate.GateConfig
import buildchecks.gate.MissingReportGate
import buildchecks.parse.CheckstyleParser
import buildchecks.parse.CoberturaParser
import buildchecks.parse.CpdParser
import buildchecks.parse.JacocoParser
import buildchecks.parse.JunitParser
import buildchecks.parse.LcovParser
import buildchecks.parse.PitParser
import buildchecks.parse.ReportParser
import buildchecks.parse.SarifParser
import buildchecks.render.CodeClimateReport
import buildchecks.render.FindingsJson
import buildchecks.render.HtmlReport
import buildchecks.render.MarkdownSummary
import buildchecks.render.MergedSarif
import buildchecks.render.Renderer
import buildchecks.render.SummaryJson
import buildchecks.render.SummaryText

// Manual composition root (V4-PLAN.md §2): explicit ordered lists, no framework.
fun reportParsers(): List<ReportParser> = listOf(
    SarifParser(),
    JunitParser(),
    JacocoParser(),
    CoberturaParser(),
    LcovParser(),
    CheckstyleParser(),
    CpdParser(),
    PitParser(),
)

// Order gates are evaluated and, since renderers iterate this list, displayed in: findings, then the
// coverage family (whole-project, then the diff-scoped coverage and mutation views), then caps
// (errors/warnings/test failures), then expected reports. Gates are independent, so the order is for
// legibility only (V4-PLAN.md §4).
fun gates(config: GateConfig): List<Gate> = listOf(
    FindingsGate(config),
    CoverageGate(config),
    ChangedLineCoverageGate(config),
    ChangedLineMutationGate(config),
    CapsGate(config),
    MissingReportGate(),
)

// Every file written to the output dir on each check run (V4-PLAN.md §7).
fun renderers(): List<Renderer> = listOf(
    HtmlReport(),
    MarkdownSummary(),
    SummaryJson(),
    SummaryText(),
    FindingsJson(),
    CodeClimateReport(),
    MergedSarif(),
)
