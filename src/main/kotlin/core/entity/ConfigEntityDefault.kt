package core.entity

import java.util.*

open class ConfigEntityDefault : ConfigEntity {
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
