package core

import java.util.*

open class BuildStatusConfig {
    var buildUrl  = ""
    var lintReports  = ""
    var jacocoReports  = ""
    var detektReports  = ""
    var checkStyleReports  = ""
    var statusBaseUrl  = ""
    var statusAuthorization  = ""
    var buildStartTime = Date()
    val hash = "git rev-parse HEAD".run() ?: ""
    val shortHash = "git rev-parse --short HEAD".run() ?: ""
    val commitDate : Long = "git show -s --format=%ct".run()?.toLong() ?: 0
    val commitBranch = "git symbolic-ref -q --short HEAD".run() ?: ""
    var statsBaseUrl = ""
    fun duration() = ((Date().time - buildStartTime.time) / 1000.0).round(2)

}