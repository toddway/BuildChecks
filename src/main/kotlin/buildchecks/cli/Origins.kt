package buildchecks.cli

import buildchecks.gate.OriginKind
import buildchecks.model.Freshness
import buildchecks.model.IngestedFile

const val ROOT_ORIGIN = "."

/**
 * Origin (V4-PLAN.md §5.5): a report's source group, derived from its path as the prefix
 * before the build-output marker discovery already keys on. `services/auth/build/reports/…`
 * → `services/auth`; root or aggregated reports collapse to the root origin. A single-module
 * repo has one origin, so the whole feature is inert where the layout is absent. A computed
 * property of the ingested file — no config key, no model entity.
 */
fun origin(path: String): String {
    val segments = path.split('/')
    val marker = segments.indexOfFirst { it in markers }
    return if (marker <= 0) ROOT_ORIGIN else segments.subList(0, marker).joinToString("/")
}

/** kind = the producing tool for findings reports, else the coverage/test format (§5.5). */
fun kind(file: IngestedFile): String = file.report.tool ?: file.format

/** The (origin, kind) set present this run, compared against the baseline manifest. */
fun presentManifest(files: List<IngestedFile>): Set<OriginKind> =
    files.map { OriginKind(origin(it.path), kind(it)) }.toSet()

/** Reports per origin, in origin order — logged so a source count dropping (e.g. 2→1) is visible. */
fun originCounts(files: List<IngestedFile>): Map<String, Int> =
    files.groupingBy { origin(it.path) }.eachCount().toSortedMap()

/**
 * Origins this change touched (V4-PLAN.md §11 4.2, change-scoped freshness). Source paths carry no
 * build-output marker, so they can't feed [origin]; instead each changed path is matched against the
 * origins the tool actually knows about — those present this run plus [manifestOrigins] from the
 * baseline — assigning it to the longest such origin that prefixes it, else the root origin. Matching
 * against the manifest is what lets a touched module that emitted *no* report this run still surface
 * as touched (rather than collapsing into root and looking measured).
 */
fun changedOrigins(changedFiles: Set<String>, files: List<IngestedFile>, manifestOrigins: Set<String> = emptySet()): Set<String> {
    val known = (files.map { origin(it.path) } + manifestOrigins).filter { it != ROOT_ORIGIN }.toSet()
    return changedFiles.map { path ->
        known.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length } ?: ROOT_ORIGIN
    }.toSet()
}

/**
 * Of [origins], those that produced *any* ingested report this run, regardless of age. The dividing
 * line for change-scoped freshness: only an origin BuildChecks actually measured can be judged stale.
 * An origin with no report is simply not evidence of a missed check (BuildChecks doesn't know which
 * origins should emit reports), so it is excluded here rather than flagged — see [ChangeDelta].
 */
fun reportedChangedOrigins(origins: Set<String>, files: List<IngestedFile>): Set<String> =
    origins.filter { o -> files.any { origin(it.path) == o } }.toSet()

/**
 * Of [origins], those that produced a fresh (non age-outlier) report this run. An origin with only
 * stale-outlier reports is absent — the signature of a touched module that did not re-run. Reuses
 * [Freshness.outlier] (§3), so a uniformly-aged build flags nothing.
 */
fun freshChangedOrigins(origins: Set<String>, files: List<IngestedFile>, freshness: Freshness?): Set<String> =
    origins.filter { o -> files.any { origin(it.path) == o && freshness?.outlier(it.path) != true } }.toSet()

private val markers = setOf("build", "target", "coverage", "lcov.info")
