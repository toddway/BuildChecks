package core

import core.entity.ErrorMessage
import core.entity.InfoMessage
import core.entity.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


const val COMMAND_TIMEOUT : Long = 60

fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(COMMAND_TIMEOUT, TimeUnit.MINUTES)
        val v = proc.inputStream.bufferedReader().readText()
        proc.inputStream.bufferedReader().close()
        //println("${this} exit value: ${proc.exitValue()}")
        v
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

fun String.run() = runCommand(File("."))?.trim()

fun Iterable<String>.toFiles(log: Log? = null): Iterable<File> {
    return filter { it.trim().isNotEmpty() }
            .map { File(it.trim()) }
            .filter {
                log?.let { log ->
                    if (it.exists())
                        log.info(InfoMessage("BuildChecks found: file://${it.absolutePath}").toString())
                    else
                        log.error(ErrorMessage("BuildChecks did not find: file://${it.absolutePath}").toString())
                }
                it.exists()
            }
}
