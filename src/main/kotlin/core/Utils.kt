package core

import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

fun Pair<Int, Int>.percentage(): Double {
    return if (first == 0 && second == 0) 0.0
        else ((first.toDouble() / (second + first)) * 100)
}

fun Double.round(scale : Int) =
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()

fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

fun String.run() = runCommand(File("."))?.trim()

fun String.toFileList(): List<File> {
    return split(",").map { File(it.trim()) }.filter { it.exists() }
}



fun Int.blankOrNum() : String = if (this == 0) "" else "$this"

