package core.usecase

import core.*
import core.entity.*
import java.io.File

class HandleBuildFinishedUseCase(
        private val postStatusUseCase: PostStatusUseCase,
        private val postStatsUseCase: PostStatsUseCase,
        private val getSummaryUseCases: List<GetSummaryUseCase>,
        private val config: BuildConfig,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        if (config.isPluginActivated) {
            getSummaryUseCases.postStatuses(postStatusUseCase)
            getSummaryUseCases.toStats(config).let { postStatsUseCase.invoke(it) }
            messageQueue.distinct().forEach { println(it.toString()) }
            config.buildSummaryReports()
        }
    }
}

fun BuildConfig.buildSummaryReports() {
    val dirs = reportDirs()
    val artifactsDir = dirs.copyInto(artifactsDir())
    val files = artifactsDir.findReportFiles()
    val xmlPairs = files.filter { it.isXML() }.toNameAndPathPairs(artifactsDir)
    val htmlPairs = files.filter { it.isHtmlOrTxt() }.toNameAndPathPairs(artifactsDir)
    val summaries = files.toSummaries(this)

    File(artifactsDir, "buildChecks.html").apply {
        writeText(htmlReport(summaries.toMessages(), htmlPairs, xmlPairs, git))
        println(InfoMessage("Browse reports at " + absoluteFile.toURI()))
    }

    File(artifactsDir, "buildChecks.csv").apply {
        writeText(summaries.toStats(this@buildSummaryReports).toString())
    }
}


fun htmlReport(messages: List<Message?>, htmlPairs: List<Pair<String, File>>, xmlPairs: List<Pair<String, File>>, git: GitConfig) = """
<html>
<style>body {font-family: Helvetica, Arial, sans-serif;line-height:160%;padding:20px} a:link{text-decoration:none}</style>
<body>
<h1>Build Checks Summary Report</h1>
${messages.joinToString("\n") { "$it<br/>" }}
<ul>${htmlPairs.joinToString("\n") { "<li><a href=\"${it.second}\">${it.first}</a></li>" }}</ul>
From ${git.summary()}<br/>
${xmlPairs.map { "${it.second}" }.joinToString(", ")}
</body>
</html>
""".trimIndent()