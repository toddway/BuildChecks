package buildchecks.cli

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Candidate scan (V4-PLAN.md §3). With no configured globs, the zero-config default set:
 * standard report locations, sniffed by content. Config `paths` globs override the
 * locations; parsers still decide by content whether a candidate is understood.
 */
class ReportDiscovery(
    globs: List<String>? = null,
    private val outputDir: String = DEFAULT_OUTPUT_DIR,
) {

    private val matchers = globs?.map { FileSystems.getDefault().getPathMatcher("glob:$it") }

    fun discover(root: File): List<File> = root.walkTopDown()
        .onEnter { dir ->
            // The exclusions apply to descendants only — the root is always entered. Otherwise a
            // checkout whose own directory is named like an excluded dir (e.g. Bitrise's /bitrise/src)
            // would abort the whole walk, finding zero reports.
            dir == root ||
                (dir.name !in excludedDirs &&
                    relative(root, dir) != outputDir &&
                    // Gradle's processed-resources copies of source files are not reports
                    !(dir.name == "resources" && dir.parentFile?.name == "build"))
        }
        .filter { it.isFile && matches(relative(root, it)) }
        .sortedBy { relative(root, it) }
        .toList()

    private fun matches(path: String): Boolean {
        // Extension prefilter applies in both modes: globs choose locations, but only
        // files that could be a supported report format become candidates — a glob
        // ending in ** must not drag a tool's html pages into "not understood".
        if (File(path).extension !in candidateExtensions) return false
        val matchers = matchers ?: return isInDefaultLocation(path)
        return matchers.any { it.matches(Path.of(path)) }
    }

    private fun isInDefaultLocation(path: String) =
        path.contains("build/reports/") ||
            path.contains("build/test-results/") ||
            path.contains("target/site/jacoco/") ||
            path.contains("coverage/") ||
            path.substringAfterLast('/') == "lcov.info"

    private fun relative(root: File, file: File) =
        file.relativeTo(root).path.replace(File.separatorChar, '/')

    companion object {
        const val DEFAULT_OUTPUT_DIR = "build/reports/buildchecks"
        // src is excluded because source trees (incl. test resources) are never report locations
        private val excludedDirs = setOf("node_modules", ".git", "src")
        private val candidateExtensions = setOf("xml", "json", "sarif", "info", "txt")
    }
}
