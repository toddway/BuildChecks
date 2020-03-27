package gradle
import core.entity.BuildConfigDefault
import core.entity.Log
import data.Registry
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val registry = Registry(project.createBuildChecksConfig())
        listOf(project.createPostChecksTask(), project.createPrintChecksTask()).forEach { it.doLast {
            registry.provideHandleUnsuccessfulSummariesUseCase().invoke()
        } }
        project.createChecksReportTask().registry = registry

        project.gradle.taskGraph.whenReady {
            registry.config.taskName = project.taskNameString()
            registry.config.isPostActivated = project.isPostChecksActivated()
            registry.config.isPluginActivated = project.isPluginActivated()
            registry.config.log = Log(project.logger)
            registry.provideHandleBuildStartedUseCase().invoke()
        }

        project.gradle.buildFinished {
            registry.config.isSuccess = it.failure == null
            registry.provideHandleBuildFinishedUseCase().invoke()
        }
    }
}

fun Project.isPrintChecksActivated() = gradle.taskGraph.allTasks.find { it is PrintChecksTask } != null
fun Project.isPostChecksActivated() = gradle.taskGraph.allTasks.find { it is PostChecksTask } != null
fun Project.isPluginActivated() = isPostChecksActivated() || isPrintChecksActivated()
fun Project.taskNameString() = gradle.startParameter.taskNames.joinToString(" ")
fun Project.createBuildChecksConfig() = extensions.create("buildChecks", BuildConfigDefault::class.java)
fun Project.createPostChecksTask() = tasks.create("postChecks", PostChecksTask::class.java)
fun Project.createPrintChecksTask() = tasks.create("printChecks", PrintChecksTask::class.java)
fun Project.createChecksReportTask() = tasks.create("checksReport", ChecksReportTask::class.java)
