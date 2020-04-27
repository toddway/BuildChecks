package func

import org.junit.Test
import java.io.File

class ReportTests {

    @Test
    fun test() {
        val dir = File("./build/reports")
        //println(dir.exists())

        val files = dir.walkTopDown()
        files.filter { it.name.endsWith("xml") }.forEach { println(it.name) }
        //files.filter { it.name.endsWith("html") }.forEach { println(it.name) }
    }
}