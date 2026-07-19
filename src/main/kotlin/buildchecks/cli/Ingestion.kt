package buildchecks.cli

import buildchecks.model.IngestedFile
import buildchecks.parse.ReportParser
import java.io.File

data class Ingestion(
    val files: List<IngestedFile>,
    val notUnderstood: List<String>,
)

fun ingest(root: File, candidates: List<File>, parsers: List<ReportParser>): Ingestion {
    val files = mutableListOf<IngestedFile>()
    val notUnderstood = mutableListOf<String>()
    for (candidate in candidates) {
        val content = candidate.readText()
        val path = candidate.relativeTo(root).path.replace(File.separatorChar, '/')
        val parser = parsers.firstOrNull { it.claims(content) }
        if (parser == null) {
            notUnderstood += path
        } else {
            files += IngestedFile(path, parser.format, candidate.lastModified(), parser.parse(content))
        }
    }
    return Ingestion(files, notUnderstood)
}
