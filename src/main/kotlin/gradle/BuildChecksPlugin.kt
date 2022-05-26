package gradle
import core.Registry
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Log
import core.entity.ProjectConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

open class BuildChecksPlugin @Inject constructor(private val buildEventsListenerRegistry: BuildEventsListenerRegistry) : Plugin<Project> {
    override fun apply(project: Project) {
        val registry = Registry(GradleProjectConfig(project))

        BuildEventService.create(
            project.gradle.sharedServices,
            buildEventsListenerRegistry,
            onBuildStart = { registry.provideBuildStartedUseCase().invoke() },
            onBuildFinish = { registry.provideBuildFinishedUseCase(it.hasNoFailures()).invoke() }
        )
    }
}

class GradleProjectConfig(val project: Project) : ProjectConfig {
    override fun isPrintChecksActivated() = CheckTask.PRINT.hasTask(project)
    override fun isPostChecksActivated() = CheckTask.POST.hasTask(project)
    override fun isPushArtifactsActivated() = CheckTask.PUSH.hasTask(project)
    override fun isOpenChecksActivated() = CheckTask.OPEN.hasTask(project)
    override fun taskNameString() = project.gradle.startParameter.taskNames.joinToString(" ")
    override fun createBuildChecksConfig(): BuildConfig = project.extensions.create("buildChecks", BuildConfigDefault::class.java)
    override fun logger(): Log = Log(project.logger)
    init { CheckTask.values().forEach { it.createTask(project) } }
}

enum class CheckTask(var label: String) {
    POST("postChecks"),
    PRINT("printChecks"),
    OPEN("openChecks"),
    PUSH("pushArtifacts");

    fun createTask(project: Project) { project.tasks.create(label, BuildChecksTask::class.java) }
    fun hasTask(project: Project) = project.gradle.taskGraph.allTasks.find { it.name == label } != null
}

abstract class BuildChecksTask : DefaultTask() {
    @TaskAction
    fun buildChecks() {}
}