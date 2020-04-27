package core.entity

import core.findReportFiles
import core.round
import core.toFileList
import java.io.File
import java.util.*

interface BuildConfig {
    var buildUrl : String
    var reports : String
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
    fun isAllCommitted() = allowUncommittedChanges || git.isAllCommitted
    fun reportDirs() = reports.toFileList(log)
    fun reportFiles() : List<File> = reportDirs().flatMap { it.findReportFiles() }
}

//gradle config cannot be a data class
open class BuildConfigDefault : BuildConfig {
    override var reports: String = ""
    override var log: Log? = null
    override var maxDuration: Double? = null
    override var git: GitConfig = GitConfigDefault()
    override var allowUncommittedChanges: Boolean = true
    override var isSuccess: Boolean = true
    override var maxLintViolations: Int? = null
    override var minCoveragePercent: Double? = null
    override var buildUrl  = ""
    override var baseUrl  = ""
    override var authorization  = ""
    override var buildStartTime = Date()
    override var statsBaseUrl = ""
    override var taskName = "default"
    override var isPostActivated = false
    override var isPluginActivated = false
}


