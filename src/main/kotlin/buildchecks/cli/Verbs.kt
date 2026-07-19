package buildchecks.cli

import buildchecks.gate.FingerprintBaseline
import buildchecks.gate.Fingerprinter
import buildchecks.gate.GateConfig
import buildchecks.gate.GateContext
import buildchecks.model.GateStatus
import buildchecks.model.merged
import java.io.File

/** ingest -> gate -> report; returns the process exit code (0 pass, 1 gate failure). */
fun runCheck(root: File, config: GateConfig = GateConfig(), echo: (String) -> Unit): Int {
    val ingestion = ingestReports(root, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    val baseline = FingerprintBaseline(baselineFile(root)).read()
    val context = GateContext(fingerprinted, merged.tests, merged.coverage, baseline)

    val results = gates(config).flatMap { it.evaluate(context) }
    echo("")
    results.forEach { result ->
        val mark = when (result.status) {
            GateStatus.PASSED -> "PASS"
            GateStatus.FAILED -> "FAIL"
            GateStatus.SKIPPED -> "SKIP"
        }
        echo("$mark  ${result.gate}: ${result.detail}")
    }
    return if (results.any { it.status == GateStatus.FAILED }) 1 else 0
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
