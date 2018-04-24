
import core.BuildStatusConfig
import core.HandleBuildSuccessUseCase
import core.SetBuildStatusUseCase
import core.toDocumentList
import data.provideBuildStatusDatasources
import org.gradle.api.Plugin
import org.gradle.api.Project


open class BuildStatusPlugin : Plugin<Project> {

    var buildStatus: SetBuildStatusUseCase? = null

    override fun apply(project: Project) {
        val config = project.extensions.create("buildstatus", BuildStatusConfig::class.java)

        project.gradle.taskGraph.whenReady {
            buildStatus = SetBuildStatusUseCase(provideBuildStatusDatasources(config, project.hasProperty("postStatus")))
            buildStatus?.pending(startedMessage(project), "g")
        }

        project.gradle.buildFinished {
            if (it.failure == null) {
                HandleBuildSuccessUseCase(
                        buildStatus,
                        jacocoDocs = config.jacocoReports.toDocumentList(),
                        lintDocs = config.lintReports.toDocumentList(),
                        detektDocs = config.detektReports.toDocumentList(),
                        checkstyleDocs = config.checkStyleReports.toDocumentList()
                ).invoke()
                buildStatus?.success(completedMessage(config, project), "g")
            } else {
                buildStatus?.failure(completedMessage(config, project), "g")
            }
        }
    }

    private fun startedMessage(project: Project)
            = "gradle ${project.taskNameString()} - in progress"

    private fun completedMessage(config : BuildStatusConfig, project: Project)
            = "${config.duration()}s for gradle ${project.taskNameString()}"
}

fun Project.taskNameString() = gradle.startParameter.taskNames.joinToString(" ")
