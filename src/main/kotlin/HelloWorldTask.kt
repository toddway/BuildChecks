
import core.BuildStatusExtension
import core.BuildStatusProperties
import core.run
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class HelloWorldTask : DefaultTask() {
    var command : String? = null
    var config : BuildStatusProperties = BuildStatusExtension()

    @TaskAction
    fun showMessage() {

        command?.let {
            println(it.run())
        }
    }
}