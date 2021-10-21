package func

import core.commonPrefix
import core.copyInto
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

    fun test2() {
        val targetDir = File("./src/test/results")
        targetDir.deleteRecursively()

        val dirs = listOf(
            File("./src/test/testFiles/reports/coverage"),
            File("./src/test/testFiles/reports/cpd"),
            File("./src/test/testFiles/reports/tests"),
            File("./src/test/testFiles/reports/whatever/coverage")
        )

        dirs.copyInto(targetDir)
    }
}