package core.usecase

import core.attr
import core.children
import org.w3c.dom.Document
import org.w3c.dom.Element

class GetCoberturaSummaryUseCase(documents: List<Document>) : GetCoverageSummaryUseCase(documents) {
    override fun toCoverageMap(d : Document): Map<String?, Pair<Int, Int>> {
        return d.toCoburturaMap()
    }
}

fun Document.toCoburturaMap(): Map<String?, Pair<Int, Int>> {
    val root = childNodes.children().find { it is Element }
    var reportMap = mutableMapOf<String?, Pair<Int, Int>>()
    root?.let {
        val covered = (it.attr("lines-covered")?.toIntOrNull() ?: 0) + (it.attr("branches-covered")?.toIntOrNull() ?: 0)
        val valid = (it.attr("lines-valid")?.toIntOrNull() ?: 0) + (it.attr("branches-valid")?.toIntOrNull() ?: 0)
        val missed = valid - covered
        reportMap.put("LINE + BRANCH", Pair(covered, missed))
    }
    return reportMap
}



