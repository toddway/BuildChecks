package gradle

import core.run
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ChecksReportTask : DefaultTask() {

    @TaskAction
    fun taskAction() {
        val v = "git --no-pager log --pretty=format:%H".run() ?: "error"
        val a = v.lines()
        a.forEach { println("hash: $it") }
    }
}