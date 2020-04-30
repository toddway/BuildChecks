package core

import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

const val THOUSAND = 1000.0
const val HUNDRED = 100
const val COMMAND_TIMEOUT : Long = 60

fun Double.round(scale : Int) =
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()

fun String.runCommand(workingDir: File): String? {
    return try {
        val proc = ProcessBuilder(*toCommandArgs())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(COMMAND_TIMEOUT, TimeUnit.MINUTES)
        val v = proc.inputStream.bufferedReader().readText()
        proc.inputStream.bufferedReader().close()
        val e = proc.errorStream.bufferedReader().readText()
        proc.errorStream.bufferedReader().close()
        //println("${this} exit value: ${proc.exitValue()} $e")
        //println(v)
        v
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

fun String.toCommandArgs() : Array<String> {
    val parts = mutableListOf<String>()
    val m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(this)
    while (m.find()) parts.add(m.group(1))
    return parts.toTypedArray()
}

fun String.run() = runCommand(File("."))?.trim()

fun Number.isNotGreaterThan(number: Number?): Boolean {
    if (number == null) return true
    return this.toDouble() <= number.toDouble()
}

fun Number.isNotLessThan(number: Number?): Boolean {
    if (number == null) return true
    return this.toDouble() >= number.toDouble()
}

fun Int.blankOrNum() : String = if (this == 0) "" else "$this"

fun <T, V> Map<V?, List<T>>.entryChildrenSum(): Int {
    return flatMap { entry -> listOf(entry.value.count()) }.sum()
}
