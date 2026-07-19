package buildchecks.cli

import java.io.File

/**
 * Zero-config candidate scan (V4-PLAN.md §3). Finds files under the standard report
 * locations; parsers decide by content whether a candidate is understood. Refined with
 * config-driven globs in phase 4.
 */
class ReportDiscovery(private val outputDir: String = DEFAULT_OUTPUT_DIR) {

    fun discover(root: File): List<File> = root.walkTopDown()
        .onEnter { dir ->
            dir.name !in excludedDirs &&
                relative(root, dir) != outputDir &&
                // Gradle's processed-resources copies of source files are not reports
                !(dir.name == "resources" && dir.parentFile?.name == "build")
        }
        .filter { it.isFile && it.extension in candidateExtensions && isInReportLocation(relative(root, it)) }
        .sortedBy { relative(root, it) }
        .toList()

    private fun isInReportLocation(path: String) =
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
