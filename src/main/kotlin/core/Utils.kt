package core

import core.entity.ErrorMessage
import core.entity.InfoMessage
import core.entity.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

const val THOUSAND = 1000.0
const val HUNDRED = 100
const val COMMAND_TIMEOUT : Long = 60

fun Double.round(scale : Int) =
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()

fun String.runCommand(workingDir: File, log : Log?): String? {
    return try {
        val proc = ProcessBuilder(*toCommandArgs())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
        proc.waitFor(COMMAND_TIMEOUT, TimeUnit.MINUTES)
        val output = proc.inputStream.readAndClose()
        logCommandStreams(this, output, proc.errorStream.readAndClose(), log)
        output
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

fun logCommandStreams(command : String, output: String?, error : String?, log : Log?) {
    log?.let {
        it.info(InfoMessage("Running command: $command").toString())
        output?.let { o -> it.debug(InfoMessage(o).toString()) }
        error?.let { e -> it.info(ErrorMessage(e.trim()).toString()) }
    }
}

fun InputStream.readAndClose() : String? {
    val v = bufferedReader().readText()
    bufferedReader().close()
    return if (v.isBlank()) null else v
}

fun String.toCommandArgs() : Array<String> {
    val parts = mutableListOf<String>()
    val m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(this)
    while (m.find()) parts.add(m.group(1).replace("\"", ""))
    return parts.toTypedArray()
}

fun String.run() = runCommand(File("."), null)?.trim()

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
