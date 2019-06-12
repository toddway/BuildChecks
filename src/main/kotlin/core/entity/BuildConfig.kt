package core.entity

import core.THOUSAND
import core.round
import core.toFiles
import java.util.*

data class BuildConfig (
        var baseUrl : String  = "https://google.com/",
        var authorization : String  = "",
        var buildUrl : String = "",

        var jacocoReports: Iterable<String> = listOf(),
        var coberturaReports: Iterable<String> = listOf(),
        var checkstyleReports: Iterable<String> = listOf(),
        var androidLintReports: Iterable<String> = listOf(),
        var cpdReports: Iterable<String> = listOf(),

        var maxDuration: Double? = null,
        var maxLintViolations: Int? = null,
        var minCoveragePercent: Double? = null,

        var log: Log? = null,
        var git: GitConfig = GitConfig(),
        var allowUncommittedChanges: Boolean = false,
        var isSuccess: Boolean = false,

        var buildStartTime : Date = Date(),
        var statsBaseUrl : String = "",
        var taskName : String = "default",
        var isPostActivated : Boolean = false,
        var isPluginActivated : Boolean = false
)  {

    fun allReports() = (jacocoReports + coberturaReports + checkstyleReports + androidLintReports + cpdReports).toFiles()
    fun duration() = ((Date().time - buildStartTime.time) / THOUSAND).round(2)
    fun startedMessage() = "gradle $taskName - in progress"
    fun completedMessage() = "${duration()}s for gradle $taskName"
    fun isAllowedToPost() = allowUncommittedChanges || git.isAllCommitted
}

