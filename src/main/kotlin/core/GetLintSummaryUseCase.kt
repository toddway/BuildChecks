package core

import java.io.File

class GetLintSummaryUseCase(val file: File) {
    fun execute(): String {
        val document = GetDocumentUseCase(file).execute()
        val nodeList = document.getElementsByTagName("issue")
        val counterMap = nodeList.children().groupBy { it.attr("severity") }
        val countsList = counterMap.flatMap { entry -> listOf("${entry.value.count()} ${entry.key?.toLowerCase()}s") }
        return countsList.joinToString(", ")
    }

}