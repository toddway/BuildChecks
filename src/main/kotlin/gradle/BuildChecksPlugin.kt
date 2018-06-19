package gradle
import core.entity.BuildConfigDefault
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.createPostChecksTask()
        project.createPrintChecksTask()
        val di = DI(project.createBuildChecksConfig())

        project.gradle.taskGraph.whenReady {
            di.config.taskName = project.taskNameString()
            di.config.isPostActivated = project.isPostChecksActivated()
            di.config.isPluginActivated = project.isPluginActivated()
            di.handleBuildStartedUseCase().invoke()
        }

        project.gradle.buildFinished {
            di.config.isSuccess = it.failure == null
            di.handleBuildFinishedUseCase().invoke()
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



//        project.tasks.create("postPending", gradle.PostStatusTask::class.java).status = "pending"
//        project.tasks.create("postSuccess", gradle.PostStatusTask::class.java).status = "success"
//        project.tasks.create("postFailed", gradle.PostStatusTask::class.java).status = "success"
