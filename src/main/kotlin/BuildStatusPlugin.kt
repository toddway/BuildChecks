
import core.BuildStatusExtension
import di.DI
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildStatusPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val config = project.extensions.create("buildstatus", BuildStatusExtension::class.java)
        val di = DI(config)

        project.tasks.create("postStatus", HelloWorldTask::class.java).config = config
//        project.tasks.create("postPending", PostStatusTask::class.java).status = "pending"
//        project.tasks.create("postSuccess", PostStatusTask::class.java).status = "success"
//        project.tasks.create("postFailed", PostStatusTask::class.java).status = "success"


        project.gradle.taskGraph.whenReady {
            config.taskName = project.taskNameString()
            config.isPostStatus = project.isPost()
            di.setBuildStatusUseCase().pending(config.startedMessage(), "g")
        }

        project.gradle.buildFinished {
            if (it.failure == null) {
                di.handleBuildSuccessUseCase().invoke()
                di.setBuildStatusUseCase().success(config.completedMessage(), "g")
            } else {
                di.setBuildStatusUseCase().failure(config.completedMessage(), "g")
            }
        }
    }
}

fun Project.isPost() = hasProperty("postStatus")
        || hasProperty("post")
        || gradle.taskGraph.allTasks.find { it is HelloWorldTask } != null
fun Project.taskNameString() = gradle.startParameter.taskNames.joinToString(" ")
