
import core.*
import di.provideBuildStatsDatasources
import di.provideBuildStatusDatasources
import org.gradle.api.Plugin
import org.gradle.api.Project


open class BuildStatusPlugin : Plugin<Project> {

    var setBuildStatus: SetBuildStatusUseCase? = null

    override fun apply(project: Project) {
        val config = project.extensions.create("buildstatus", BuildStatusConfig::class.java)

        project.gradle.taskGraph.whenReady {
            setBuildStatus = SetBuildStatusUseCase(provideBuildStatusDatasources(config, project.isPost()))
            setBuildStatus?.pending(startedMessage(project), "g")
        }

        project.gradle.buildFinished {
            if (it.failure == null) {
                HandleBuildSuccessUseCase(
                        setBuildStatus,
                        PostBuildStatsUseCase(provideBuildStatsDatasources(config, project.isPost())),
                        config,
                        GetJacocoSummaryUseCase(config.jacocoReports.toDocumentList()),
                        GetLintSummaryUseCase(config.lintReports.toDocumentList()),
                        GetDetektSummaryUseCase(config.detektReports.toDocumentList()),
                        GetCheckstyleSummaryUseCase(config.checkStyleReports.toDocumentList())
                ).invoke()
                setBuildStatus?.success(completedMessage(config, project), "g")
            } else {
                setBuildStatus?.failure(completedMessage(config, project), "g")
            }
        }
    }

    private fun startedMessage(project: Project)
            = "gradle ${project.taskNameString()} - in progress"

    private fun completedMessage(config : BuildStatusConfig, project: Project)
            = "${config.duration()}s for gradle ${project.taskNameString()}"
}

fun Project.isPost() = hasProperty("postStatus") || hasProperty("post")

fun Project.taskNameString() = gradle.startParameter.taskNames.joinToString(" ")
