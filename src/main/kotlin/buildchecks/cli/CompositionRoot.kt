package buildchecks.cli

import buildchecks.parse.CheckstyleParser
import buildchecks.parse.CoberturaParser
import buildchecks.parse.CpdParser
import buildchecks.parse.JacocoParser
import buildchecks.parse.JunitParser
import buildchecks.parse.LcovParser
import buildchecks.parse.ReportParser
import buildchecks.parse.SarifParser

// Manual composition root (V4-PLAN.md §2): explicit ordered lists, no framework.
// Grows as phases land; today it declares the parser set.
fun reportParsers(): List<ReportParser> = listOf(
    SarifParser(),
    JunitParser(),
    JacocoParser(),
    CoberturaParser(),
    LcovParser(),
    CheckstyleParser(),
    CpdParser(),
)
