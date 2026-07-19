package buildchecks.cli

import buildchecks.model.IngestedFile
import java.io.File

/**
 * Copies each tool's own HTML report next to ours so index.html can drill down and the
 * output dir stays one portable artifact (v3's best feature, kept — V4-PLAN.md §7).
 *
 * Heuristics per ingested file: a sibling html/ dir or index.html means the parent dir is
 * an html report root — copy it whole; otherwise a sibling .html with the same basename is
 * copied alone. Generic report roots with neither are skipped.
 */
fun copyToolReports(root: File, files: List<IngestedFile>, outputDir: File): List<IngestedFile> =
    files.map { ingested ->
        val parent = File(root, ingested.path).parentFile ?: return@map ingested
        val destination = File(outputDir, "tools/${ingested.path.substringBeforeLast('/').replace('/', '-')}")
        val htmlRoot = listOf(File(parent, "html"), File(parent, "index.html")).any { it.exists() }
        val sibling = File(parent, File(ingested.path).nameWithoutExtension + ".html")
        when {
            htmlRoot -> {
                copyDir(parent, destination, exclude = outputDir)
                val index = listOf("index.html", "html/index.html").firstOrNull { File(destination, it).isFile }
                ingested.copy(toolReport = index?.let { relative(outputDir, File(destination, it)) })
            }
            sibling.isFile -> {
                destination.mkdirs()
                sibling.copyTo(File(destination, sibling.name), overwrite = true)
                ingested.copy(toolReport = relative(outputDir, File(destination, sibling.name)))
            }
            else -> ingested
        }
    }

// A tool's report root can contain our own output dir; never copy that into itself.
private fun copyDir(source: File, destination: File, exclude: File) {
    source.walkTopDown()
        .onEnter { it.canonicalFile != exclude.canonicalFile }
        .filter { it.isFile }
        .forEach { file ->
            val target = File(destination, file.relativeTo(source).path)
            target.parentFile?.mkdirs()
            file.copyTo(target, overwrite = true)
        }
}

private fun relative(outputDir: File, file: File) =
    file.relativeTo(outputDir).path.replace(File.separatorChar, '/')
