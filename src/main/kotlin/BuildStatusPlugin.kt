
import core.*
import data.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*


open class BuildStatusPlugin : Plugin<Project> {

    var buildStatus: SetBuildStatusUseCase? = null

    override fun apply(project: Project) {
        val ext = project.extensions.create("buildstatus", BuildStatusExtension::class.java)
        val taskNames = project.gradle.startParameter.taskNames.joinToString(" ")

        project.gradle.taskGraph.whenReady {
            buildStatus = getBuildStatusUseCase(ext, project.hasProperty("postStatus"))
            buildStatus?.pending("started ${ext.buildStartTime}", "g")
        }

        project.gradle.buildFinished {
            if (it.failure == null) {
                ext.jacocoReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(
                            GetJacocoSummaryUseCase(file).execute(),
                            "j${blankOrInt(index)}"
                    )
                }

                ext.lintReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(
                            GetLintSummaryUseCase(file).execute() + " from Lint",
                            "l${blankOrInt(index)}"
                    )
                }

                ext.detektReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(
                            GetCheckstyleSummaryUseCase(file).execute() + " from Detekt",
                            "d${blankOrInt(index)}"
                    )
                }

                ext.checkStyleReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(
                            GetCheckstyleSummaryUseCase(file).execute() + " from Checkstyle",
                            "c${blankOrInt(index)}"
                    )
                }

                buildStatus?.success(ext.durationMessage() + " for gradle $taskNames", "g")
            } else {
                buildStatus?.failure(ext.durationMessage() + " for gradle $taskNames", "g")
            }
        }

        //project.tasks.create("hello", HelloWorldTask::class.java)
    }

    fun getBuildStatusUseCase(ext : BuildStatusExtension, postStatus : Boolean): SetBuildStatusUseCase {

        val datasources = mutableListOf<BuildStatusDatasource>(ConsoleDatasource(ext.hash))
        if (postStatus) {
            if (ext.postBaseUrl.contains("bitbucket")) {
                val service = createBitBucketService(ext.postBaseUrl, ext.postAuthorization)
                datasources.add(BitBucketDatasource(service, ext.hash, ext.buildUrl))
            } else if (ext.postBaseUrl.contains("github")) {
                val service = createGithubService(ext.postBaseUrl, ext.postAuthorization)
                datasources.add(GithubDatasource(service, ext.hash, ext.buildUrl))
            }
        }
        return SetBuildStatusUseCase(datasources)
    }
}

fun blankOrInt(i : Int): String = if (i == 0) "" else "$i"

open class BuildStatusExtension {
    var buildUrl : String = ""
    var lintReports : String = ""
    var jacocoReports : String = ""
    var detektReports : String = ""
    var checkStyleReports : String = ""
    var postBaseUrl : String = ""
    var postAuthorization : String = ""


    var buildStartTime = Date()
    val hash = "git rev-parse HEAD".runCommand(File("."))?.trim() ?: ""

    fun durationMessage(): String {
        val buildDuration = ((Date().time - buildStartTime.time) / 1000.0).round(2)
        return "${buildDuration}s"
    }
}
