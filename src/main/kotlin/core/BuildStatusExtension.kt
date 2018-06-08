package core

import java.util.*

open class BuildStatusExtension : BuildStatusProperties {
    override var buildUrl  = ""
    override var lintReports  = ""
    override var jacocoReports  = ""
    override var detektReports  = ""
    override var checkStyleReports  = ""
    override var statusBaseUrl  = ""
    override var statusAuthorization  = ""
    override var buildStartTime = Date()
    override var statsBaseUrl = ""
    override var taskName = ""
    override var isPostStatus = false
}

