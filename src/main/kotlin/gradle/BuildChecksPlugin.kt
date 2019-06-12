package gradle
import core.entity.BuildConfig
import core.entity.Log
import data.Instances
import groovy.lang.GString
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.createPostChecksTask()
        project.createPrintChecksTask()
        project.creatExtension()
        val instances by lazy { Instances(project.toBuildConfig()) }

        project.gradle.taskGraph.whenReady {
            instances.handleBuildStartedUseCase.invoke()
        }

        project.gradle.buildFinished {
            instances.config.isSuccess = it.failure == null
            instances.handleBuildFinishedUseCase.invoke()
        }
    }
}

fun Project.isPrintChecksActivated() = gradle.taskGraph.allTasks.find { it is PrintChecksTask } != null
fun Project.isPostChecksActivated() = gradle.taskGraph.allTasks.find { it is PostChecksTask } != null
fun Project.isPluginActivated() = isPostChecksActivated() || isPrintChecksActivated()
fun Project.taskNameString() = gradle.startParameter.taskNames.joinToString(" ")
fun Project.creatExtension() = extensions.create("buildChecks", BuildChecksExtension::class.java)
fun Project.createPostChecksTask() = tasks.create("postChecks", PostChecksTask::class.java)
fun Project.createPrintChecksTask() = tasks.create("printChecks", PrintChecksTask::class.java)

fun Project.toBuildConfig(): BuildConfig {
    with(extensions.getByType(BuildChecksExtension::class.java)) {
        return BuildConfig(
                baseUrl = baseUrl,
                authorization = authorization,
                buildUrl = buildUrl,
                maxLintViolations = maxLintViolations,
                minCoveragePercent = minCoveragePercent,
                maxDuration = maxDuration,
                jacocoReports = jacocoReports.map { it.toString() },
                coberturaReports = coberturaReports.map { it.toString() },
                checkstyleReports = checkstyleReports.map { it.toString() },
                androidLintReports = androidLintReports.map { it.toString() },
                cpdReports = cpdReports.map { it.toString() },
                allowUncommittedChanges = allowUncommittedChanges,
                taskName = taskNameString(),
                isPostActivated = isPostChecksActivated(),
                isPluginActivated = isPluginActivated(),
                log = Log(logger)
        )
    }
}

open class BuildChecksExtension {
    var baseUrl : String = ""
    var authorization : String = ""
    var buildUrl : String = ""
    var maxLintViolations : Int? = null
    var minCoveragePercent : Double? = null
    var maxDuration : Double? = null
    var jacocoReports : Iterable<GString> = listOf()
    var coberturaReports : Iterable<GString> = listOf()
    var checkstyleReports  : Iterable<GString> = listOf()
    var androidLintReports : Iterable<GString> = listOf()
    var cpdReports : Iterable<GString> = listOf()
    var allowUncommittedChanges : Boolean = false
}
