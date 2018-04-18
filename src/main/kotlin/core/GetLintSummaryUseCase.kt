package core

import java.io.File

class GetLintSummaryUseCase(val file: File) {
    fun execute(): String {
        val document = GetDocumentUseCase(file).execute()
        val nodeList = document.getElementsByTagName("issue")
        val counterMap = nodeList.children().groupBy { it.attr("severity") }
        return "${counterMap["Fatal"]?.count()} lint errors, ${counterMap["Warning"]?.count()} warnings"
    }

}