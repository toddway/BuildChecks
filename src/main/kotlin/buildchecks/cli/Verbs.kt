package buildchecks.cli

import buildchecks.gate.Baseline
import buildchecks.gate.FingerprintBaseline
import buildchecks.gate.Fingerprinter
import buildchecks.gate.GateContext
import buildchecks.gate.baselineDelta
import buildchecks.gate.parseBaseline
import buildchecks.gate.promotedGates
import buildchecks.git.GitDiff
import buildchecks.model.ChangeDelta
import buildchecks.model.ChangedLineCoverage
import buildchecks.model.ChangedLines
import buildchecks.model.CheckSummary
import buildchecks.model.FileCoverage
import buildchecks.model.Finding
import buildchecks.model.IngestedFile
import buildchecks.model.ReportedFinding
import buildchecks.model.changedLineCoverage
import buildchecks.model.confidence
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
        Fingerprinter(root, sourceLines(root)).fingerprint(merged.findings)
    }
    val baseline = FingerprintBaseline(File(root, config.git.baselineFile)).read()
    val git = GitDiff(root)
    // Resolve the base ref once (git.defaultBranch() runs at most once): shared by changed-line
    // coverage, the requireBaseRef promotion, and the 4.2 delta signals. Attempted always, since the
    // delta signals feed the always-shown confidence axis — it returns null (no signal) off a PR.
    val baseRef = resolveBaseRef(git, config, baseRefFlag, verbose, env, echo)
    // The raw changed-file set is available whenever a base ref resolved (for change-scoped
    // freshness); the changed-line *coverage* section stays gated on its own knob, unchanged.
    val changedLines = timed("diff", verbose, echo) { baseRef?.let { git.changedLines(it) } }
    val changedCoverage = changedLineCoverage(
        if (config.gates.minChangedLineCoverage != null) changedLines else null,
        merged.coverage,
    )
    val presentOrigins = presentManifest(ingestion.files)
    val reportFreshness = freshness(ingestion.files, nowMillis, config.reports.freshnessToleranceMinutes)
    val delta = timed("delta", verbose, echo) {
        changeDelta(git, baseRef, changedLines, ingestion.files, baseline, reportFreshness, config, env)
    }
    val context = GateContext(fingerprinted, merged.tests, merged.coverage, baseline, changedCoverage, presentOrigins)
    val results = timed("gates", verbose, echo) {
        val evaluated = gates(config.gates).flatMap { it.evaluate(context) }
        evaluated + promotedGates(config.gates, evaluated, baseRefResolved = baseRef != null, delta = delta)
    }
    logOriginCounts(ingestion.files, echo)

    // Report sources ingested this run but not in the baseline manifest — the inverse of
    // MissingReportGate. A notice, not a failure (adding coverage is legitimate); feeds confidence.
    val newReportLabels = baseline?.manifest
        ?.let { manifest -> (presentOrigins - manifest).sorted().map { "${it.kind} in ${it.origin}" } }
        ?: emptyList()

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
        freshness = reportFreshness,
        hasBaseline = baseline != null,
        changedLineCoverage = linkedChangedCoverage,
        confidence = confidence(results, reportFreshness, ingestion.notUnderstood, newReportLabels, delta),
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
    val fingerprinted = Fingerprinter(root, sourceLines(root)).fingerprint(merged.findings)
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

// Base ref resolution order per V4-PLAN.md §4: flag -> config -> CI env -> remote default branch
// -> null (features that need it skip). The default-branch fallback is a guessed-but-named default
// (like zero-config report discovery): it's noted in the output so a reader always knows what the
// diff was against. Returns the resolved ref (or null), so the caller knows whether one was found —
// which requireBaseRef promotes to a gate and changed-line coverage uses to run vs skip.
private fun resolveBaseRef(
    git: GitDiff,
    config: Config,
    baseRefFlag: String?,
    verbose: Boolean,
    env: (String) -> String?,
    echo: (String) -> Unit,
): String? {
    val baseRef = baseRefFlag
        ?: config.git.baseRef
        ?: ciBaseRef(env)
        ?: git.defaultBranch()?.also {
            echo("no base ref set; diffing against $it (default branch) for change analysis — " +
                "override with --base-ref or git.base_ref")
        }
        ?: return null
    if (verbose) echo("base ref: $baseRef")
    return baseRef
}

// The base-ref delta facts (V4-PLAN.md §11 item 7, 4.2): change-scoped freshness (which touched
// origins produced a fresh report), plus the baseline and config diffs read via `git show`. null
// when no base ref resolved. Best-effort throughout — a blob that isn't at the base ref, or a repo
// that isn't rooted at the git root, simply yields no signal, never an error. The config diff uses
// the conventional buildchecks.toml path; a --config-supplied path elsewhere isn't diffed.
private fun changeDelta(
    git: GitDiff,
    baseRef: String?,
    changedLines: ChangedLines?,
    files: List<IngestedFile>,
    baseline: Baseline?,
    freshness: buildchecks.model.Freshness?,
    config: Config,
    env: (String) -> String?,
): ChangeDelta? {
    if (baseRef == null) return null
    // Use the ref the diff actually resolved to (it may have retried origin/<ref>), so `git show`
    // reads the same "before" the changed-line set came from.
    val ref = (changedLines as? ChangedLines.Diff)?.baseRef ?: baseRef
    val manifestOrigins = baseline?.manifest?.map { it.origin }?.toSet() ?: emptySet()
    val touched = (changedLines as? ChangedLines.Diff)
        ?.let { changedOrigins(it.files.keys, files, manifestOrigins) }
        ?: emptySet()
    val reported = reportedChangedOrigins(touched, files)
    val fresh = freshChangedOrigins(touched, files, freshness)

    val baseBaseline = git.show(ref, config.git.baselineFile)?.let { parseBaseline(it.lines()) }
    val baselineDiff = if (baseBaseline != null && baseline != null) baselineDelta(baseBaseline, baseline) else null

    val baseConfig = git.show(ref, "buildchecks.toml")?.let { parseConfigText(it, env) }
    val configLoosened = if (baseConfig != null) configLoosened(baseConfig, config) else emptyList()

    return ChangeDelta(
        touchedOrigins = touched,
        reportedOrigins = reported,
        freshOrigins = fresh,
        baselineFindingsAccepted = baselineDiff?.findingsAccepted ?: 0,
        baselineCoverageLowered = baselineDiff?.coverageLowered,
        baselineReportsDropped = baselineDiff?.reportsDropped ?: emptyList(),
        configLoosened = configLoosened,
    )
}

// The PR/MR target branch, as each common CI provider exposes it. Every one of these is set
// only on a merge build, so a plain branch build leaves them all empty and the gate skips —
// which means a consumer (Gradle, a shell step, the Action) needs no per-CI base-ref wiring of
// its own. An explicit --base-ref or config base_ref still wins, since both are tried first.
private fun ciBaseRef(env: (String) -> String?): String? {
    for (key in CI_BASE_REF_VARS) {
        val value = env(key)?.takeIf { it.isNotBlank() } ?: continue
        return value.removePrefix("refs/heads/") // Azure DevOps reports the full refs/heads/<branch>
    }
    return null
}

private val CI_BASE_REF_VARS = listOf(
    "GITHUB_BASE_REF",                     // GitHub Actions (pull_request)
    "BITRISEIO_GIT_BRANCH_DEST",           // Bitrise
    "BITBUCKET_PR_DESTINATION_BRANCH",     // Bitbucket Pipelines
    "CI_MERGE_REQUEST_TARGET_BRANCH_NAME", // GitLab CI (merge request)
    "CHANGE_TARGET",                       // Jenkins multibranch
    "SYSTEM_PULLREQUEST_TARGETBRANCH",     // Azure DevOps
)

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
