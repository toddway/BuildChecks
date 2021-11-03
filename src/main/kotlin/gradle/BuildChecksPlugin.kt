package gradle
import Registry
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Log
import core.entity.ProjectConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

open class BuildChecksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val registry = Registry(GradleProjectConfig(project))

        project.gradle.taskGraph.whenReady {
            registry.provideBuildStartedUseCase().invoke()
        }

        project.gradle.buildFinished {
            registry.provideBuildFinishedUseCase(it.failure == null).invoke()
        }
    }
}

class GradleProjectConfig(val project: Project) : ProjectConfig {
    override fun isPrintChecksActivated() = project.gradle.taskGraph.allTasks.find { it is PrintChecksTask } != null
    override fun isPostChecksActivated() = project.gradle.taskGraph.allTasks.find { it is PostChecksTask } != null
    override fun isPushArtifactsActivated() = project.gradle.taskGraph.allTasks.find { it is PushArtifacts } != null
    override fun taskNameString() = project.gradle.startParameter.taskNames.joinToString(" ")
    override fun createBuildChecksConfig(): BuildConfig = project.extensions.create("buildChecks", BuildConfigDefault::class.java)
    override fun initPostChecksTask(doLast : () -> Unit) { project.tasks.create("postChecks", PostChecksTask::class.java).doLast { doLast() } }
    override fun initPrintChecksTask(doLast : () -> Unit) { project.tasks.create("printChecks", PrintChecksTask::class.java).doLast { doLast() } }
    override fun initPushArtifactsTask() { project.tasks.create("pushArtifacts", PushArtifacts::class.java) }
    override fun logger(): Log = Log(project.logger)
}