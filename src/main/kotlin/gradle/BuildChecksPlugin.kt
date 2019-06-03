package gradle
import core.entity.BuildConfigDefault
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.createPostChecksTask()
        project.createPrintChecksTask()
        val config = project.createBuildChecksConfig()
        val factory = UseCaseFactory(config)

        project.gradle.taskGraph.whenReady {
            config.taskName = project.taskNameString()
            config.isPostActivated = project.isPostChecksActivated()
            config.isPluginActivated = project.isPluginActivated()
            factory.handleBuildStartedUseCase().invoke()
        }

        project.gradle.buildFinished {
            config.isSuccess = it.failure == null
            factory.handleBuildFinishedUseCase().invoke()
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
//fun Project.createChecksReportTask() = tasks.create("checks", ChecksReportTask::class.java)
