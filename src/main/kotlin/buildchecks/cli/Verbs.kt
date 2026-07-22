package buildchecks.cli

import buildchecks.gate.FingerprintBaseline
import buildchecks.gate.Fingerprinter
import buildchecks.gate.GateContext
import buildchecks.git.GitDiff
import buildchecks.model.ChangedLineCoverage
import buildchecks.model.ChangedLines
import buildchecks.model.CheckSummary
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.IngestedFile
import buildchecks.model.ReportedFinding
import buildchecks.model.changedLineCoverage
import buildchecks.model.freshness
import buildchecks.model.matching
import buildchecks.model.merged
import buildchecks.render.ConsoleSummary
import java.io.File
import java.util.IdentityHashMap

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
    val overall = Stopwatch()
    val outputDir = File(root, config.reports.outputDir)
    val ingestion = ingestReports(root, config, verbose, echo)
    val merged = ingestion.files.map { it.report }.merged()
    val fingerprinted = timed("fingerprint", verbose, echo) {
        Fingerprinter(sourceLines(root)).fingerprint(merged.findings)
    }
    val baseline = FingerprintBaseline(File(root, config.git.baselineFile)).read()
    val changedLines = timed("diff", verbose, echo) {
        changedLines(root, config, baseRefFlag, verbose, env, echo)
    }
    val changedCoverage = changedLineCoverage(changedLines, merged.coverage)
    val presentOrigins = presentManifest(ingestion.files)
    val context = GateContext(fingerprinted, merged.tests, merged.coverage, baseline, changedCoverage, presentOrigins)
    val results = timed("gates", verbose, echo) { gates(config.gates).flatMap { it.evaluate(context) } }
    logOriginCounts(ingestion.files, echo)

    val copiedFiles = timed("copy-reports", verbose, echo) { copyToolReports(root, ingestion.files, outputDir) }
    // Each finding links to the copied HTML report of the file that produced it. copyToolReports
    // returns shallow copies, so the Finding instances are shared with `fingerprinted` and match
    // by identity (structurally-equal findings from different files stay distinct here).
    val findingReports = IdentityHashMap<Finding, String?>()
    copiedFiles.forEach { file -> file.report.findings.forEach { findingReports[it] = file.toolReport } }
    // Same identity trick to remember which ingested report each finding/test/coverage-file came
    // from, so the report can flag rows whose source is a stale age-outlier (Freshness.outlier
    // keys on this path). merged() flat-maps the rows, so instances are shared by identity.
    val findingSources = IdentityHashMap<Finding, String>()
    copiedFiles.forEach { file -> file.report.findings.forEach { findingSources[it] = file.path } }
    val testSources = IdentityHashMap<buildchecks.model.TestResult, String>()
    copiedFiles.forEach { file -> file.report.tests.forEach { testSources[it] = file.path } }
    val coverageSources = IdentityHashMap<FileCoverage, String>()
    copiedFiles.forEach { file -> file.report.coverage?.files?.forEach { coverageSources[it] = file.path } }
    // Same identity trick for coverage: link each changed file to the copied coverage report that
    // measured it (FileCoverage instances are shared between merged.coverage and copiedFiles).
    val coverageReports = IdentityHashMap<FileCoverage, String?>()
    copiedFiles.forEach { file -> file.report.coverage?.files?.forEach { coverageReports[it] = file.toolReport } }
    val linkedChangedCoverage = when (changedCoverage) {
        is ChangedLineCoverage.Measured -> changedCoverage.copy(files = changedCoverage.files.map { changed ->
            val matches = merged.coverage?.matching(changed.path)
            changed.copy(
                toolReport = matches?.firstNotNullOfOrNull { coverageReports[it] },
                reportPath = matches?.firstNotNullOfOrNull { coverageSources[it] },
            )
        })
        else -> changedCoverage
    }
    val summary = CheckSummary(
        gates = results,
        findings = fingerprinted.map {
            ReportedFinding(
                it.finding,
                it.fingerprint,
                baseline != null && it.fingerprint !in baseline.fingerprints,
                findingReports[it.finding],
                findingSources[it.finding],
            )
        },
        tests = merged.tests.map { it.copy(reportPath = testSources[it]) },
        coverage = merged.coverage?.let { cov ->
            cov.copy(files = cov.files.map { it.copy(reportPath = coverageSources[it]) })
        },
        files = copiedFiles,
        notUnderstood = ingestion.notUnderstood,
        freshness = freshness(ingestion.files, nowMillis, config.reports.freshnessToleranceMinutes),
        hasBaseline = baseline != null,
        changedLineCoverage = linkedChangedCoverage,
    )

    outputDir.mkdirs()
    timed("render", verbose, echo) {
        renderers().forEach { renderer ->
            File(outputDir, renderer.fileName).writeText(renderer.render(summary))
        }
    }
    if (verbose) echo("timing: total ${overall.elapsedMillis()}ms")

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
    val candidates = timed("discover", verbose, echo) {
        ReportDiscovery(config.reports.paths, config.reports.outputDir).discover(root)
    }
    if (verbose) {
        echo("root: ${root.absolutePath}")
        echo("report paths: ${config.reports.paths ?: "zero-config defaults"}")
        echo("baseline file: ${config.git.baselineFile}")
        candidates.forEach { echo("candidate: ${it.relativeTo(root).path}") }
    }
    val ingestion = timed("ingest", verbose, echo) { ingest(root, candidates, reportParsers()) }
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

// Wall-clock timing per phase, printed only under --verbose so we can spot where a large
// project spends the run (e.g. copy-reports on a many-module Android build). nanoTime is
// monotonic and independent of the freshness clock (nowMillis).
private inline fun <T> timed(label: String, verbose: Boolean, echo: (String) -> Unit, block: () -> T): T {
    if (!verbose) return block()
    val stopwatch = Stopwatch()
    val result = block()
    echo("timing: $label ${stopwatch.elapsedMillis()}ms")
    return result
}

private class Stopwatch {
    private val start = System.nanoTime()
    fun elapsedMillis(): Long = (System.nanoTime() - start) / 1_000_000
}

// Report paths may be absolute (JVM tools) or repo-relative (JS/TS tools).
private fun sourceLines(root: File): (String) -> List<String>? = { path ->
    val file = File(path).takeIf { it.isAbsolute && it.isFile } ?: File(root, path)
    if (file.isFile) file.readLines() else null
}
