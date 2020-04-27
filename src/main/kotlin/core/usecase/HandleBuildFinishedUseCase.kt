package core.usecase

import core.entity.*
import core.findReportFiles
import core.isIndexHTML
import core.isXML
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
            getSummaryUseCases.postAll(postStatusUseCase)
            postStatsUseCase.invoke(config.stats(getSummaryUseCases))
            messageQueue.distinct().forEach { println(it.toString()) }
            config.writeReports()
        }
    }
}

fun BuildConfig.writeReports() {
    reportDirs().forEach { dir ->
        val reportFiles = dir.findReportFiles()
        val htmlPairs = reportFiles.filter { !it.isXML() }.toNameToPathPairs(dir)
        val xmlPairs = reportFiles.filter { it.isXML() }.toNameToPathPairs(dir)
        val messages = reportFiles.summaries(this).toMessages()

        val buildChecksHtmlFile = File(dir, "buildChecks.html")
        buildChecksHtmlFile.writeText(htmlReport(messages, htmlPairs, xmlPairs, git))
        println(InfoMessage("Browse reports at " + buildChecksHtmlFile.absoluteFile.toURI()))
    }
}

fun List<File>.toNameToPathPairs(relativeRootDir : File) = map {
    val name = if (it.isIndexHTML()) it.parentFile.relativeTo(relativeRootDir).path else it.name
    name to it.relativeTo(relativeRootDir)
}

fun List<GetSummaryUseCase>.toMessages() = filter { it.value() != null }.map { s ->
    s.value()?.let {
        if (s.isSuccessful()) InfoMessage(it)
        else ErrorMessage(it)
    }
}

fun htmlReport(messages: List<Message?>, htmlPairs: List<Pair<String, File>>, xmlPairs: List<Pair<String, File>>, git: GitConfig) = """
<html>
<style>body {font-family: Helvetica, Arial, sans-serif;line-height:160%;padding:20px} a:link{text-decoration:none}</style>
<body>
<h1>Build Checks</h1>
${messages.joinToString("\n") { "$it<br/>" }}
<ul>${htmlPairs.joinToString("\n") { "<li><a href=\"${it.second}\">${it.first}</a></li>" }}</ul>
From ${git.summary()}<br/>
${xmlPairs.map { "${it.second}" }.joinToString(", ")}
</body>
</html>
""".trimIndent()