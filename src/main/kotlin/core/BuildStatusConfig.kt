package core

import java.io.File
import java.util.*

open class BuildStatusConfig {
    var buildUrl : String = ""
    var lintReports : String = ""
    var jacocoReports : String = ""
    var detektReports : String = ""
    var checkStyleReports : String = ""
    var postBaseUrl : String = ""
    var postAuthorization : String = ""
    var buildStartTime = Date()
    val hash = "git rev-parse HEAD".runCommand(File("."))?.trim() ?: ""
    fun duration() = ((Date().time - buildStartTime.time) / 1000.0).round(2)
}