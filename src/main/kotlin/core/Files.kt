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
                    if (f.exists()) log.debug(InfoMessage("BuildChecks found: ${f.absolutePath}").toString())
                    else log.debug(ErrorMessage("BuildChecks could not find: ${f.absolutePath}").toString())
                }
                f
            }
            .filter { it.exists() }
}

fun String.toShortUid(items: Set<String>) : String {
    val prefix = items.reduceOrNull { acc, path -> path.commonPrefixWith(acc) } ?: ""
    val suffix = items.reduceOrNull { acc, path -> path.commonSuffixWith(acc) } ?: ""
    return removePrefix(prefix).removeSuffix(suffix).replace(File.separator, "-")
}

fun List<File>.copyEachRecursively(targetDir : File) : File {
    val paths = map { it.absolutePath }.toSet()
    forEach { it.copyRecursively(File(targetDir, it.absolutePath.toShortUid(paths)), true) }
    return targetDir
}

fun List<File>.firstDir() = firstOrNull { it.isDirectory }

fun File.toNameAndPathPair(relativeRootDir: File): Pair<String, File> {
    val name = if (isIndexHTML()) parentFile.relativeTo(relativeRootDir).path else relativeTo(relativeRootDir).path
    return name to relativeTo(relativeRootDir)
}
fun File.isReadableFormat() : Boolean = name.toLowerCase().run { endsWith(".html") || endsWith(".txt") || endsWith(".text") || endsWith(".md")}
fun File.isXML() = name.toLowerCase().endsWith(".xml")
fun File.isIndexHTML() = name.toLowerCase() == "index.html"
fun File.containsIndexHTML() = listFiles()?.any { it.isIndexHTML() }  ?: false
fun List<File>.toXmlDocuments() = filter { it.isXML() }.toDocumentList()
fun File.findReportFiles() : List<File> = walkTopDown().onEnter { !it.parentFile.containsIndexHTML() }.filter { !it.isDirectory }.toList()
fun File.subdirs(): Array<File>? = listFiles { it : File -> it.isDirectory }


