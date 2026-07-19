package buildchecks.cli

import buildchecks.gate.FingerprintBaseline
import buildchecks.gate.Fingerprinter
import buildchecks.gate.GateConfig
import buildchecks.gate.GateContext
import buildchecks.model.CheckSummary
import buildchecks.model.GateStatus
import buildchecks.model.ReportedFinding
import buildchecks.model.freshness
import buildchecks.model.merged
import buildchecks.render.ConsoleSummary
import java.io.File

/** ingest -> gate -> render; returns the process exit code (0 pass, 1 gate failure). */
fun runCheck(
    root: File,
    config: GateConfig = GateConfig(),
    nowMillis: Long = System.currentTimeMillis(),
    echo: (String) -> Unit,
): Int {
    val outputDir = File(root, ReportDiscovery.DEFAULT_OUTPUT_DIR)
    val ingestion = ingestReports(root, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    val baseline = FingerprintBaseline(baselineFile(root)).read()
    val context = GateContext(fingerprinted, merged.tests, merged.coverage, baseline)
    val results = gates(config).flatMap { it.evaluate(context) }

    val summary = CheckSummary(
        gates = results,
        findings = fingerprinted.map {
            ReportedFinding(it.finding, it.fingerprint, baseline != null && it.fingerprint !in baseline.fingerprints)
        },
        tests = merged.tests,
        coverage = merged.coverage,
        files = copyToolReports(root, ingestion.files, outputDir),
        notUnderstood = ingestion.notUnderstood,
        freshness = freshness(ingestion.files, nowMillis),
    )

    outputDir.mkdirs()
    renderers().forEach { renderer ->
        File(outputDir, renderer.fileName).writeText(renderer.render(summary))
    }

    echo("")
    echo(ConsoleSummary().render(summary))
    echo("")
    echo("report: ${File(outputDir, "index.html").absolutePath}")
    return if (summary.passed) 0 else 1
}

/** ingest -> snapshot the fingerprint baseline file. */
fun runBaseline(root: File, echo: (String) -> Unit): Int {
    val ingestion = ingestReports(root, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    val coveragePercent = merged.coverage?.linePercent
    val file = baselineFile(root)
    FingerprintBaseline(file).write(fingerprinted, coveragePercent)
    echo("")
    echo("baseline written: ${file.name} (${fingerprinted.size} findings" +
        (coveragePercent?.let { ", coverage %.2f%%".format(it) } ?: "") + ")")
    return 0
}

private fun ingestReports(root: File, echo: (String) -> Unit): Ingestion {
    val candidates = ReportDiscovery().discover(root)
    val ingestion = ingest(root, candidates, reportParsers())
    ingestion.files.forEach { echo("ingested: ${it.path} (${it.format})") }
    ingestion.notUnderstood.forEach { echo("not understood: $it") }
    if (ingestion.files.isEmpty()) echo("no report files found under ${root.absolutePath}")
    return ingestion
}

private fun baselineFile(root: File) = File(root, "buildchecks-baseline.txt")

// Report paths may be absolute (JVM tools) or repo-relative (JS/TS tools).
private fun sourceLines(root: File): (String) -> List<String>? = { path ->
    val file = File(path).takeIf { it.isAbsolute && it.isFile } ?: File(root, path)
    if (file.isFile) file.readLines() else null
}
