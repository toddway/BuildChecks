package buildchecks

import java.io.File

/**
 * Golden-file fixtures for parser and discovery tests: real report files produced by real
 * tools (carried over from v3, extended per format in phase 1).
 */
object Fixtures {
    val root = File("src/test/resources/fixtures")

    fun file(path: String): File = File(root, path).also {
        require(it.exists()) { "Missing fixture: ${it.path}" }
    }
}
