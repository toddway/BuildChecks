package core.entity

interface ProjectConfig {
    fun isPrintChecksActivated() : Boolean
    fun isPostChecksActivated() : Boolean
    fun isChecksActivated() = isPostChecksActivated() || isPrintChecksActivated() || isPushArtifactsActivated()
    fun isPushArtifactsActivated() : Boolean
    fun taskNameString() : String
    fun createBuildChecksConfig() : BuildConfig
    fun initPostChecksTask(doLast : () -> Unit)
    fun initPrintChecksTask(doLast : () -> Unit)
    fun initPushArtifactsTask()
    fun logger(): Log
}