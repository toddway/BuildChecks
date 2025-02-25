package unit

import core.copyEachRecursively
import org.junit.Assert
import org.junit.Test
import java.io.File

class FilesTest {

    @Test
    fun `when copyEachRecursively is called, then the expected unique directories are created`() {
        val pathsWithCommonFixes = setOf(
            "project/module1/reports",
            "project/module2/reports",
            "project/features/module1/reports",
            "project/features/module2/reports",
            "project/module3/reports"
        )
        val sourceParentDir = File("./build/testFiles/source")
        val targetParentDir = File("./build/testFiles/target")
        val sourceDirs = pathsWithCommonFixes.map { it -> File(sourceParentDir, it).also{ it.mkdirs() } }
        val expectedUids = setOf("module1", "module2", "features-module1", "features-module2", "module3")
        targetParentDir.deleteRecursively()

        sourceDirs.copyEachRecursively(targetParentDir)

        expectedUids.forEach { Assert.assertTrue(it, File(targetParentDir, it).exists()) }
    }
}