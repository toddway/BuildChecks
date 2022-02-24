package core.entity

interface ProjectConfig {
    fun isPrintChecksActivated() : Boolean
    fun isPostChecksActivated() : Boolean
    fun isChecksActivated() = isPostChecksActivated() || isPrintChecksActivated() || isPushArtifactsActivated()
    fun isPushArtifactsActivated() : Boolean
    fun isOpenChecksActivated() : Boolean
    fun taskNameString() : String
    fun createBuildChecksConfig() : BuildConfig
    fun logger(): Log
}