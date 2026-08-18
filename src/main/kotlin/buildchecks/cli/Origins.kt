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

/**
 * Rebase one finding path onto the repo root, so [buildchecks.gate.Fingerprinter] can find the
 * violating source and hash it (V4-PLAN.md §5).
 *
 * Tools disagree about what a finding's path is relative to. JVM tools invoked per module emit
 * *module*-relative paths — detekt's SARIF says so outright, tagging every URI
 * `uriBaseId: "%SRCROOT%"` where SRCROOT is the module dir, not the repo. Others (checkstyle) emit
 * absolute machine paths. Left as-is, a module-relative path resolves against neither the root nor
 * the cwd, the fingerprinter finds no source, and it falls back to hashing the *message* — which for
 * a formatting rule ("Needless blank line(s)") is identical repo-wide, collapsing hundreds of
 * distinct findings onto one hash that only an occurrence index separates. Absolute paths, meanwhile,
 * bake the capturing machine's home dir into the baseline.
 *
 * [reportOrigin] is the report's own origin ([origin]) — for
 * `feature/storelocator/build/reports/detekt/detekt.sarif` that is `feature/storelocator`, exactly
 * the SRCROOT the URIs are relative to. [exists] is the repo-root-relative existence check, injected
 * so this is testable without a filesystem.
 */
fun repoRelativeSource(path: String, reportOrigin: String, rootPrefixes: List<String>, exists: (String) -> Boolean): String {
    val slashed = path.replace('\\', '/')
    // Absolute inside this checkout: strip the root so the baseline is machine-independent.
    rootPrefixes.firstOrNull { slashed.startsWith(it) }?.let { return slashed.removePrefix(it) }
    // Absolute somewhere else entirely — nothing sound to rebase onto; leave it for the caller.
    if (slashed.startsWith("/")) return slashed
    // Already repo-relative.
    if (exists(slashed)) return slashed
    // Module-relative (SARIF %SRCROOT%): rebase onto the report's own origin.
    if (reportOrigin != ROOT_ORIGIN) {
        val rebased = "$reportOrigin/$slashed"
        if (exists(rebased)) return rebased
    }
    return slashed
}

/** The checkout-root prefixes to strip from absolute paths — both real and symlink-resolved. */
fun rootPrefixes(root: java.io.File): List<String> =
    listOfNotNull(root.absolutePath, runCatching { root.canonicalPath }.getOrNull())
        .distinct()
        .map { it.removeSuffix("/") + "/" }
