package core.entity

import core.round
import core.toFileList
import java.util.*

interface BuildConfig {
    var buildUrl : String
    var androidLintReports : String
    var checkstyleReports : String
    var jacocoReports : String
    var coberturaReports : String
    var cpdReports: String
    var allowUncommittedChanges : Boolean
    var baseUrl : String
    var authorization : String
    var buildStartTime : Date
    var statsBaseUrl: String
    var taskName : String
    var isPostActivated : Boolean
    var isPluginActivated: Boolean
    var isSuccess: Boolean
    var git : GitConfig

    var maxLintViolations : Int?
    var minCoveragePercent : Double?
    var maxDuration : Double?
    var log: Log?

    fun duration() = ((Date().time - buildStartTime.time) / core.THOUSAND).round(2)
    fun startedMessage() = "gradle $taskName - in progress"
    fun completedMessage() = "${duration()}s for gradle $taskName"
    fun reportFiles() = listOf(
            androidLintReports,
            checkstyleReports,
            jacocoReports,
            coberturaReports,
            cpdReports
    ).joinToString(",").toFileList()
    fun isAllCommitted() = allowUncommittedChanges || git.isAllCommitted
}

//gradle config cannot be a data class
open class BuildConfigDefault : BuildConfig {
    override var log: Log? = null
    override var maxDuration: Double? = null
    override var cpdReports: String = ""
    override var git: GitConfig = GitConfigDefault()
    override var allowUncommittedChanges: Boolean = false
    override var isSuccess: Boolean = true
    override var maxLintViolations: Int? = null
    override var minCoveragePercent: Double? = null
    override var buildUrl  = ""
    override var androidLintReports  = ""
    override var jacocoReports  = ""
    override var checkstyleReports  = ""
    override var coberturaReports = ""
    override var baseUrl  = ""
    override var authorization  = ""
    override var buildStartTime = Date()
    override var statsBaseUrl = ""
    override var taskName = "default"
    override var isPostActivated = false
    override var isPluginActivated = false
}


