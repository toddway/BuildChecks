package core

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

fun Pair<Double, Double>.percentage(): Double {
    return if (first == 0.0 && second == 0.0) 0.0
        else ((first / (second + first)) * 100)
}

fun Double.round(scale : Int) =
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun String.toFileList(): List<File> {
    return split(",").map { File(it.trim()) }.filter { it.exists() }
}

fun File.toDocument() : Document {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    db.setEntityResolver { _, systemId ->
        if (systemId.contains("report.dtd")) {
            InputSource(StringReader(""))
        } else {
            null
        }
    }

    return db.parse(this)
}

fun String.toDocumentList(): List<Document> {
    return toFileList().map { it.toDocument() }
}

fun Int.blankOrNum() : String = if (this == 0) "" else "$this"