package buildchecks.cli

import buildchecks.model.IngestedFile
import java.io.File

/**
 * Copies each tool's own HTML report next to ours so index.html can drill down and the
 * output dir stays one portable artifact (v3's best feature, kept — V4-PLAN.md §7).
 *
 * Heuristics per ingested file: a sibling html/ dir or index.html means the parent dir is
 * an html report root — copy it whole; a sibling .html with the same basename is copied
 * alone; JUnit XML under `test-results/<name>/` links Gradle's html report at
 * `reports/tests/<name>/`. Report files with none of these are skipped.
 */
fun copyToolReports(root: File, files: List<IngestedFile>, outputDir: File): List<IngestedFile> =
    files.map { ingested ->
        val parent = File(root, ingested.path).parentFile ?: return@map ingested
        val destination = File(outputDir, "tools/${ingested.path.substringBeforeLast('/').replace('/', '-')}")
        val htmlRoot = listOf(File(parent, "html"), File(parent, "index.html")).any { it.exists() }
        val sibling = File(parent, File(ingested.path).nameWithoutExtension + ".html")
        val gradleTests = gradleTestsHtml(root, ingested.path)
        when {
            htmlRoot -> {
                copyDir(parent, destination, exclude = outputDir)
                val index = listOf("index.html", "html/index.html").firstOrNull { File(destination, it).isFile }
                ingested.copy(toolReport = index?.let { relative(outputDir, File(destination, it)) })
            }
            sibling.isFile -> {
                copyFile(sibling, File(destination, sibling.name))
                ingested.copy(toolReport = relative(outputDir, File(destination, sibling.name)))
            }
            gradleTests != null -> {
                val testsDestination = File(outputDir, "tools/${relative(root, gradleTests).replace('/', '-')}")
                copyDir(gradleTests, testsDestination, exclude = outputDir)
                ingested.copy(toolReport = relative(outputDir, File(testsDestination, "index.html")))
            }
            else -> ingested
        }
    }

// Gradle writes JUnit XML to build/test-results/<name>/ and html to build/reports/tests/<name>/.
private fun gradleTestsHtml(root: File, ingestedPath: String): File? {
    val dir = ingestedPath.substringBeforeLast('/')
    if (!dir.contains("test-results/")) return null
    val html = File(root, dir.replaceFirst("test-results/", "reports/tests/"))
    return html.takeIf { File(it, "index.html").isFile }
}

// A tool's report root can contain our own output dir; never copy that into itself.
private fun copyDir(source: File, destination: File, exclude: File) {
    source.walkTopDown()
        .onEnter { it.canonicalFile != exclude.canonicalFile }
        .filter { it.isFile }
        .forEach { file -> copyFile(file, File(destination, file.relativeTo(source).path)) }
}

// Tool reports are styled for white backgrounds but rarely declare it, so dark-mode
// browsers paint their canvas black. Pin the copies to light.
private const val LIGHT_PIN = """<style>:root{color-scheme:only light;background:#fff}</style>"""

private fun copyFile(source: File, target: File) {
    target.parentFile?.mkdirs()
    if (source.extension.lowercase() !in setOf("html", "htm")) {
        source.copyTo(target, overwrite = true)
        return
    }
    val text = source.readText()
    val head = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(text)
    target.writeText(
        if (head == null) LIGHT_PIN + text
        else text.replaceRange(head.range.last + 1, head.range.last + 1, LIGHT_PIN)
    )
}

private fun relative(outputDir: File, file: File) =
    file.relativeTo(outputDir).path.replace(File.separatorChar, '/')
