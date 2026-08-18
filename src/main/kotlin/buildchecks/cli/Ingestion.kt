package buildchecks.cli

import buildchecks.model.IngestedFile
import buildchecks.model.Location
import buildchecks.model.ParsedReport
import buildchecks.parse.ReportParser
import java.io.File

data class Ingestion(
    val files: List<IngestedFile>,
    val notUnderstood: List<String>,
)

fun ingest(root: File, candidates: List<File>, parsers: List<ReportParser>): Ingestion {
    val files = mutableListOf<IngestedFile>()
    val notUnderstood = mutableListOf<String>()
    val prefixes = rootPrefixes(root)
    // One existence check per distinct path, shared across every report in the run.
    val seen = HashMap<String, Boolean>()
    val exists: (String) -> Boolean = { p -> seen.getOrPut(p) { File(root, p).isFile } }
    for (candidate in candidates) {
        val content = candidate.readText()
        val path = candidate.relativeTo(root).path.replace(File.separatorChar, '/')
        val parser = parsers.firstOrNull { it.claims(content) }
        if (parser == null) {
            notUnderstood += path
        } else {
            val parsed = parser.parse(content).rebasedOnRoot(origin(path), prefixes, exists)
            files += IngestedFile(path, parser.format, candidate.lastModified(), parsed, content)
        }
    }
    return Ingestion(files, notUnderstood)
}

/**
 * Rewrite this report's finding paths to repo-root-relative (see [repoRelativeSource]). Done at
 * ingest because that is the only point where a finding is still tied to the report it came from,
 * and so to the origin its paths are relative to — [ParsedReport.merged] flattens that away.
 */
private fun ParsedReport.rebasedOnRoot(
    reportOrigin: String,
    prefixes: List<String>,
    exists: (String) -> Boolean,
): ParsedReport {
    if (findings.isEmpty()) return this
    fun rebase(location: Location) =
        location.copy(path = repoRelativeSource(location.path, reportOrigin, prefixes, exists))
    return copy(
        findings = findings.map { finding ->
            finding.copy(
                location = finding.location?.let(::rebase),
                relatedLocations = finding.relatedLocations.map(::rebase),
            )
        },
    )
}
