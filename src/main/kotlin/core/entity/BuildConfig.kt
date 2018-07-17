package core.entity

import java.util.*

interface BuildConfig {
    var buildUrl : String
    var androidLintReports : String
    var checkstyleReports : String
    var jacocoReports : String
    var coberturaReports : String
    var maxLintViolations : Int?
    var minCoveragePercent : Double?
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
    var cpdReports: String

    fun duration() = ((Date().time - buildStartTime.time) / THOUSAND).round(2)
    fun startedMessage() = "gradle $taskName - in progress"
    fun completedMessage() = "${duration()}s for gradle $taskName"
    fun reportFiles() = listOf(androidLintReports, checkstyleReports, jacocoReports, coberturaReports, cpdReports).joinToString(",").toFileList()
}

open class BuildConfigDefault : BuildConfig {
    override var cpdReports: String = ""
    override var git: GitConfig = GitConfigDefault()
    override var allowUncommittedChanges: Boolean = false
    override var isSuccess: Boolean = false
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
    override var taskName = ""
    override var isPostActivated = false
    override var isPluginActivated = false
}


