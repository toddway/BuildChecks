package buildchecks.cli

import buildchecks.gate.CapsGate
import buildchecks.gate.ChangedLineCoverageGate
import buildchecks.gate.CoverageGate
import buildchecks.gate.FindingsGate
import buildchecks.gate.Gate
import buildchecks.gate.GateConfig
import buildchecks.parse.CheckstyleParser
import buildchecks.parse.CoberturaParser
import buildchecks.parse.CpdParser
import buildchecks.parse.JacocoParser
import buildchecks.parse.JunitParser
import buildchecks.parse.LcovParser
import buildchecks.parse.ReportParser
import buildchecks.parse.SarifParser
import buildchecks.render.CodeClimateReport
import buildchecks.render.FindingsJson
import buildchecks.render.HtmlReport
import buildchecks.render.MarkdownSummary
import buildchecks.render.MergedSarif
import buildchecks.render.Renderer
import buildchecks.render.SummaryJson

// Manual composition root (V4-PLAN.md §2): explicit ordered lists, no framework.
fun reportParsers(): List<ReportParser> = listOf(
    SarifParser(),
    JunitParser(),
    JacocoParser(),
    CoberturaParser(),
    LcovParser(),
    CheckstyleParser(),
    CpdParser(),
)

// Evaluation order per V4-PLAN.md §4.
fun gates(config: GateConfig): List<Gate> = listOf(
    ChangedLineCoverageGate(config),
    FindingsGate(config),
    CoverageGate(config),
    CapsGate(config),
)

// Every file written to the output dir on each check run (V4-PLAN.md §7).
fun renderers(): List<Renderer> = listOf(
    HtmlReport(),
    MarkdownSummary(),
    SummaryJson(),
    FindingsJson(),
    CodeClimateReport(),
    MergedSarif(),
)
