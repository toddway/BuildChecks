package buildchecks.cli

import buildchecks.gate.FingerprintBaseline
import buildchecks.gate.Fingerprinter
import buildchecks.gate.GateContext
import buildchecks.git.GitDiff
import buildchecks.model.ChangedLines
import buildchecks.model.CheckSummary
import buildchecks.model.IngestedFile
import buildchecks.model.ReportedFinding
import buildchecks.model.freshness
import buildchecks.model.merged
import buildchecks.render.ConsoleSummary
import java.io.File

/** ingest -> gate -> render; returns the process exit code (0 pass, 1 gate failure). */
fun runCheck(
    root: File,
    config: Config = Config(),
    baseRefFlag: String? = null,
    verbose: Boolean = false,
    nowMillis: Long = System.currentTimeMillis(),
    env: (String) -> String? = System::getenv,
    echo: (String) -> Unit,
): Int {
    val outputDir = File(root, config.reports.outputDir)
    val ingestion = ingestReports(root, config, verbose, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    val baseline = FingerprintBaseline(File(root, config.git.baselineFile)).read()
    val changedLines = changedLines(root, config, baseRefFlag, verbose, env, echo)
    val presentOrigins = presentManifest(ingestion.files)
    val context = GateContext(fingerprinted, merged.tests, merged.coverage, baseline, changedLines, presentOrigins)
    val results = gates(config.gates).flatMap { it.evaluate(context) }
    logOriginCounts(ingestion.files, echo)

    val summary = CheckSummary(
        gates = results,
        findings = fingerprinted.map {
            ReportedFinding(it.finding, it.fingerprint, baseline != null && it.fingerprint !in baseline.fingerprints)
        },
        tests = merged.tests,
        coverage = merged.coverage,
        files = copyToolReports(root, ingestion.files, outputDir),
        notUnderstood = ingestion.notUnderstood,
        freshness = freshness(ingestion.files, nowMillis, config.reports.freshnessToleranceMinutes),
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
fun runBaseline(
    root: File,
    config: Config = Config(),
    verbose: Boolean = false,
    echo: (String) -> Unit,
): Int {
    val ingestion = ingestReports(root, config, verbose, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    val coveragePercent = merged.coverage?.linePercent
    val manifest = presentManifest(ingestion.files)
    val file = File(root, config.git.baselineFile)
    FingerprintBaseline(file).write(fingerprinted, coveragePercent, manifest)
    logOriginCounts(ingestion.files, echo)
    echo("")
    echo("baseline written: ${file.name} (${fingerprinted.size} findings" +
        (coveragePercent?.let { ", coverage %.2f%%".format(it) } ?: "") +
        ", ${manifest.size} expected report(s))")
    return 0
}

// Per-origin source counts (V4-PLAN.md §5.5): so a dropped report (e.g. 2→1 under one origin)
// stays visible even where the presence gate can't individually distinguish same-kind reports.
private fun logOriginCounts(files: List<IngestedFile>, echo: (String) -> Unit) {
    if (files.isEmpty()) return
    val counts = originCounts(files)
    echo("origins: " + counts.entries.joinToString(", ") { "${it.key} (${it.value} report(s))" })
}

private fun ingestReports(root: File, config: Config, verbose: Boolean, echo: (String) -> Unit): Ingestion {
    val candidates = ReportDiscovery(config.reports.paths, config.reports.outputDir).discover(root)
    if (verbose) {
        echo("root: ${root.absolutePath}")
        echo("report paths: ${config.reports.paths ?: "zero-config defaults"}")
        echo("baseline file: ${config.git.baselineFile}")
        candidates.forEach { echo("candidate: ${it.relativeTo(root).path}") }
    }
    val ingestion = ingest(root, candidates, reportParsers())
    ingestion.files.forEach { echo("ingested: ${it.path} (${it.format})") }
    ingestion.notUnderstood.forEach { echo("not understood: $it") }
    if (ingestion.files.isEmpty()) echo("no report files found under ${root.absolutePath}")
    return ingestion
}

// Base ref resolution order per V4-PLAN.md §4: flag -> config -> GITHUB_BASE_REF -> gate skips.
private fun changedLines(
    root: File,
    config: Config,
    baseRefFlag: String?,
    verbose: Boolean,
    env: (String) -> String?,
    echo: (String) -> Unit,
): ChangedLines? {
    if (config.gates.minChangedLineCoverage == null) return null
    val baseRef = baseRefFlag
        ?: config.git.baseRef
        ?: env("GITHUB_BASE_REF")?.takeIf { it.isNotBlank() }
        ?: return null
    if (verbose) echo("base ref: $baseRef")
    return GitDiff(root).changedLines(baseRef)
}

// Report paths may be absolute (JVM tools) or repo-relative (JS/TS tools).
private fun sourceLines(root: File): (String) -> List<String>? = { path ->
    val file = File(path).takeIf { it.isAbsolute && it.isFile } ?: File(root, path)
    if (file.isFile) file.readLines() else null
}
