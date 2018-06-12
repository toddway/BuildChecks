package gradle
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

//open class PostStatusTask : DefaultTask() {
//
//    var message = ""
//    var status = ""
//
//    @Option(option="m", description="Message to post along with status", order=1)
//    fun setTheMessage(v : String) {
//        this.message = v
//    }
//
//    @TaskAction
//    fun showMessage() {
//        println(message + status)
//    }
//}