package core

import core.entity.*
import core.usecase.toMessages
import core.usecase.toStats
import core.usecase.toSummaries
import java.io.File

fun String.toFileList(log: Log? = null): List<File> {
    return split(",")
            .filter { it.trim().isNotEmpty() }
            .map {
                val f = File(it.trim())
                if (log != null) {
                    if (f.exists()) log.info(InfoMessage("BuildChecks found: ${f.absolutePath}").toString())
                    else log.error(ErrorMessage("BuildChecks could not find: ${f.absolutePath}").toString())
                }
                f
            }
            .filter { it.exists() }
}


fun List<File>.copyInto(targetDir : File, subdirPrefix : String = "dir") : File {
    forEachIndexed { index, file ->
        if (file.isDirectory) {
            val subdir = File(targetDir, "$subdirPrefix${index + 1}")
            subdir.deleteRecursively()
            file.listFiles()?.forEach {
                if (it.name != subdir.name && it.name != targetDir.name)
                    it.copyRecursively(File(subdir, it.name))
            }
        }
    }
    return targetDir
}

fun List<File>.firstDir() = firstOrNull { it.isDirectory }

fun List<File>.toNameAndPathPairs(relativeRootDir : File) = map {
    val name = if (it.isIndexHTML()) it.parentFile.relativeTo(relativeRootDir).path else it.name
    name to it.relativeTo(relativeRootDir)
}

fun File.isReadableFormat() : Boolean = name.toLowerCase().run { endsWith(".html") || endsWith(".txt") || endsWith(".text") || endsWith(".md")}
fun File.isXML() = name.toLowerCase().endsWith(".xml")
fun File.isIndexHTML() = name.toLowerCase() == "index.html"
fun File.containsIndexHTML() = listFiles()?.any { it.isIndexHTML() }  ?: false
fun List<File>.toXmlDocuments() = filter { it.isXML() }.toDocumentList()
fun File.findReportFiles() : List<File> = walkTopDown().onEnter { !it.parentFile.containsIndexHTML() }.filter { !it.isDirectory }.toList()


fun BuildConfig.writeSummaryReports(messageQueue: MutableList<Message>) {
    log?.info(InfoMessage("Building summary reports in ${artifactsDir()}...").toString())
    val dirs = reportDirs()
    val artifactsDir = dirs.copyInto(artifactsDir())
    val files = artifactsDir.findReportFiles()
    val xmlPairs = files.filter { it.isXML() }.toNameAndPathPairs(artifactsDir)
    val htmlPairs = files.filter { it.isReadableFormat() }.toNameAndPathPairs(artifactsDir)
    val summaries = files.toSummaries(this)
    val stats = summaries.toStats(this@writeSummaryReports).toString()

    File(artifactsDir, "index.html").apply {
        writeText(htmlReport(summaries.toMessages(), htmlPairs, xmlPairs, chartHtml(stats)))
        messageQueue.add(InfoMessage("Browse reports at " + absoluteFile.toURI()))
    }

    File(artifactsDir, "stats.csv").apply { writeText(stats) }
}
