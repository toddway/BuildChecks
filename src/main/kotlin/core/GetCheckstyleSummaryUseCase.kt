package core

import java.io.File

class GetCheckstyleSummaryUseCase(private val file: File) {
    fun execute(): String {
        val document = GetDocumentUseCase(file).execute()
        val nodeList = document.getElementsByTagName("error")

        val counterMap = nodeList.children().groupBy { it.attr("severity") }
        val countsList = counterMap.flatMap { entry -> listOf("${entry.value.count()} ${entry.key}s") }
        return countsList.joinToString(", ")
    }

}