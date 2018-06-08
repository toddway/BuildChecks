package core

import org.w3c.dom.Document
import org.w3c.dom.Element

class GetCoburturaSummaryUseCase(documents: List<Document>) : GetCoverageSummaryUseCase(documents) {
    override fun toReportMap(d : Document): Map<String?, Pair<Int, Int>> {
        return d.toCoburturaMap()
    }
}

fun Document.toCoburturaMap(): Map<String?, Pair<Int, Int>> {
    val root = childNodes.children().find { it is Element }
    var reportMap = mutableMapOf<String?, Pair<Int, Int>>()
    root?.let {
        val covered = (it.attr("lines-covered")?.toInt() ?: 0) + (it.attr("branches-covered")?.toInt() ?: 0)
        val valid = (it.attr("lines-valid")?.toInt() ?: 0) + (it.attr("branches-valid")?.toInt() ?: 0)
        val missed = valid - covered
        reportMap.put("LINE + BRANCH", Pair(covered, missed))
    }
    return reportMap
}



