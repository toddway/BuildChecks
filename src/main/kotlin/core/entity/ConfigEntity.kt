package core.entity

import core.round
import core.run
import java.util.*

interface ConfigEntity {
    var buildUrl : String
    var androidLintReports : String
    var checkstyleReports : String
    var jacocoReports : String
    var coberturaReports : String
    var maxLintViolations : Int?
    var minCoveragePercent : Double?
    var baseUrl : String
    var authorization : String
    var buildStartTime : Date
    var statsBaseUrl: String
    var taskName : String
    var isPostActivated : Boolean
    var isPluginActivated: Boolean
    var isSuccess: Boolean

    fun hash() = "git rev-parse HEAD".run() ?: ""
    fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
    fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
    fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
    fun duration() = ((Date().time - buildStartTime.time) / core.THOUSAND).round(2)
    fun startedMessage() = "gradle $taskName - in progress"
    fun completedMessage() = "${duration()}s for gradle $taskName"
}

