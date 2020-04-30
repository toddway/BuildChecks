package gradle
import core.entity.BuildConfigDefault
import core.entity.Log
import Registry
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        Registry(project.createBuildChecksConfig()).apply {
            listOf(project.createPostChecksTask(), project.createPrintChecksTask()).forEach {
                it.doLast { provideHandleUnsuccessfulSummariesUseCase().invoke() }
            }
            project.createPushArtifactsTask().registry = this

            project.gradle.taskGraph.whenReady {
                config.taskName = project.taskNameString()
                config.isPostActivated = project.isPostChecksActivated()
                config.isPluginActivated = project.isPluginActivated()
                config.log = Log(project.logger)
                provideHandleBuildStartedUseCase().invoke()
            }

            project.gradle.buildFinished {
                config.isSuccess = it.failure == null
                provideHandleBuildFinishedUseCase().invoke()
            }
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
fun Project.createPushArtifactsTask() = tasks.create("pushArtifacts", PushArtifacts::class.java)
