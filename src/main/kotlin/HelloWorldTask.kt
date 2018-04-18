
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class HelloWorldTask : DefaultTask() {

    @TaskAction
    fun showMessage() {
        println("Hello World")
    }
}