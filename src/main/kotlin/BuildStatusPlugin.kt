
import core.*
import data.BitBucketDatasource
import data.ConsoleDatasource
import data.createBitBucketService
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*


open class BuildStatusPlugin : Plugin<Project> {

    var buildStatus: SetBuildStatusUseCase? = null
    val key = "buildstatus"

    override fun apply(project: Project) {
        val ext = project.extensions.create(key, BuildStatusExtension::class.java)

        project.gradle.taskGraph.whenReady {
            buildStatus = getBuildStatusUseCase(ext, project.hasProperty("postStatus"))
            buildStatus?.pending("started ${ext.buildStartTime}", key)
        }

        project.gradle.buildFinished {
            if (it.failure == null) {
                ext.jacocoReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(GetJacocoSummaryUseCase(file).execute(), "$key-jacoco-$index")
                }

                ext.lintReports.toFileList().forEachIndexed { index, file ->
                    buildStatus?.success(GetLintSummaryUseCase(file).execute(), "$key-lint-$index")
                }

                buildStatus?.success(ext.durationMessage(), key)
            } else {
                buildStatus?.failure(ext.durationMessage(), key)
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
            } else if (ext.postBaseUrl.contentEquals("github")) {
                //TODO
            }
        }
        return SetBuildStatusUseCase(datasources)
    }
}


open class BuildStatusExtension {
    var buildUrl : String = ""
    var lintReports : String = ""
    var jacocoReports : String = ""
    //var detektReports : String = ""
    //var checkStyleReports : String = ""
    var postBaseUrl : String = ""
    var postAuthorization : String = ""


    var buildStartTime = Date()
    val hash = "git rev-parse HEAD".runCommand(File("."))?.trim() ?: ""

    fun durationMessage(): String {
        val buildDuration = ((Date().time - buildStartTime.time) / 1000.0).round(2)
        return "${buildDuration}s duration"
    }
}
