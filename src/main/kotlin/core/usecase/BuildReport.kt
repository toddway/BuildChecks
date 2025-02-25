package core.usecase

import core.*
import core.entity.BuildConfig
import core.entity.InfoMessage
import core.entity.Message
import core.entity.Stats
import getCommitLog
import java.io.File

data class BuildReport(
    val messages : List<Message?>,
    val xmlLinks : List<Link>,
    val directoryReports: List<DirectoryReport>,
    val gitSummary : String,
    val stats : Stats,
    val statsHistory : List<Map<String, String>>?
)

data class DirectoryReport(
    val directoryLink : Link,
    val messages : List<Message?>,
    val readableLinks : List<Link>
)

data class Link(val name : String, val file : File)

fun BuildConfig.toBuildReport() : BuildReport {
    val artifactsDir = reportDirs().copyEachRecursively(artifactsDir())
    log?.info(InfoMessage("Building summary reports in ${artifactsDir}...").toString())
    val allReportFiles = artifactsDir.findReportFiles()
    val buildSummaries = allReportFiles.toSummaries(this)
    val stats = buildSummaries.toStats(this)
    return BuildReport(
        messages = buildSummaries.toMessages(),
        xmlLinks = allReportFiles.filter { it.isXML() }.map { it.asLink(artifactsDir) },
        directoryReports = toDirectoryReports(),
        gitSummary = git.summary(),
        stats = stats,
        statsHistory = statsHistory(stats)
    )
}

fun BuildConfig.toDirectoryReports() : List<DirectoryReport> {
    return artifactsDir().subdirs()?.map { dir ->
        val reportFiles = dir.findReportFiles()
        DirectoryReport(
            directoryLink = dir.asLink(artifactsDir()),
            messages = reportFiles.toSummaries(this).filter { it !is GetDurationSummaryUseCase }.toMessages(),
            readableLinks = reportFiles.readables().map { it.asLink(artifactsDir()) }
        )
    } ?: listOf()
}

fun List<File>.readables() = filter { it.isReadableFormat() }
fun File.asLink(relativeRootDir : File) : Link = toNameAndPathPair(relativeRootDir).let { Link(it.first, it.second) }

fun BuildConfig.statsHistory(currentStats: Stats) : List<Map<String, String>>? =
    if (artifactsBranch.isNotBlank() && isPushActivated) {
        log?.info(InfoMessage("Building history chart from '$artifactsBranch' branch log...").toString())
        val commits = getCommitLog(tempDir(), artifactsBranch, log)
        log?.info(InfoMessage("Extract stat history from csv commit messages...").toString())
        val mapsList = csvToMapsList(commits + "\n" + currentStats.toString())
        mapsList.forEach { log?.info(InfoMessage("$it").toString()) }
        mapsList
    } else null
