package core

import core.entity.ErrorMessage
import core.entity.InfoMessage
import core.entity.Log
import java.io.File

fun String.toFileList(log: Log? = null): List<File> {
    return split(",")
            .filter { it.trim().isNotEmpty() }
            .map {
                val f = File(it.trim())
                if (log != null) {
                    if (f.exists()) log.info(InfoMessage("BuildChecks found: ${f.absolutePath}").toString())
                    else log.error(ErrorMessage("BuildChecks could not find: ${f.absolutePath}").toString())
                }
                f
            }
            .filter { it.exists() }
}


fun List<File>.copyInto(targetDir : File, subdirPrefix : String = "dir") : File {
    forEachIndexed { index, file ->
        if (file.isDirectory) {
            val subdir = File(targetDir, "$subdirPrefix${index + 1}")
            subdir.deleteRecursively()
            file.listFiles()?.forEach {
                if (it.name != subdir.name && it.name != targetDir.name)
                    it.copyRecursively(File(subdir, it.name))
            }
        }
    }
    return targetDir
}

fun List<File>.firstDir() = firstOrNull { it.isDirectory }

fun List<File>.toNameAndPathPairs(relativeRootDir : File) = map {
    val name = if (it.isIndexHTML()) it.parentFile.relativeTo(relativeRootDir).path else it.name
    name to it.relativeTo(relativeRootDir)
}

fun File.isReadableFormat() : Boolean = name.toLowerCase().run { endsWith(".html") || endsWith(".txt") || endsWith(".text") || endsWith(".md")}
fun File.isXML() = name.toLowerCase().endsWith(".xml")
fun File.isIndexHTML() = name.toLowerCase() == "index.html"
fun File.containsIndexHTML() = listFiles()?.any { it.isIndexHTML() }  ?: false
fun List<File>.toXmlDocuments() = filter { it.isXML() }.toDocumentList()
fun File.findReportFiles() : List<File> = walkTopDown().onEnter { !it.parentFile.containsIndexHTML() }.filter { !it.isDirectory }.toList()