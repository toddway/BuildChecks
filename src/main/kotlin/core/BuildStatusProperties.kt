package core

import java.util.*

interface BuildStatusProperties {
    var buildUrl : String
    var lintReports : String
    var jacocoReports : String
    var detektReports : String
    var checkStyleReports : String
    var coberturaReports : String
    var statusBaseUrl : String
    var statusAuthorization : String
    var buildStartTime : Date
    var statsBaseUrl: String
    var taskName : String
    var isPostStatus : Boolean
    fun hash() = "git rev-parse HEAD".run() ?: ""
    fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
    fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
    fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
    fun duration() = ((Date().time - buildStartTime.time) / 1000.0).round(2)
    fun startedMessage() = "gradle $taskName - in progress"
    fun completedMessage() = "${duration()}s for gradle $taskName"
}