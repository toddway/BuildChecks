package buildchecks.cli

import buildchecks.gate.OriginKind
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

private val markers = setOf("build", "target", "coverage", "lcov.info")
