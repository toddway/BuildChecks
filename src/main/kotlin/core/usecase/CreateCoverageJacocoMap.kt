package core.usecase

import core.attr
import core.children
import org.w3c.dom.Document
import org.w3c.dom.NodeList

class CreateJacocoMap : CreateCoverageMap {
    override fun from(document: Document): Map<String?, Pair<Int, Int>> {
        return document.toJacocoMap()
    }
}

fun Document.toJacocoMap(): Map<String?, Pair<Int, Int>> {
    val nodeList : NodeList = getElementsByTagName("method") ?: return mapOf()

    val counterMap = nodeList.children().flatMap { it.childNodes.children() }.groupBy { it.attr("type") }

    val statsMap = counterMap.mapValues {entry ->
        val covered = entry.value.sumBy { it.attr("covered")?.toIntOrNull() ?: 0 }
        val missed = entry.value.sumBy { it.attr("missed")?.toIntOrNull()  ?: 0 }
        Pair(covered, missed)
    }

    val linePlusBranchMap = statsMap.filter { entry -> entry.key == "LINE" || entry.key == "BRANCH" }
    val linePlusBranchPair = Pair(
            linePlusBranchMap.values.sumBy { it.first },
            linePlusBranchMap.values.sumBy { it.second }
    )

    val reportMap = statsMap.toMutableMap()
    reportMap.put("LINE + BRANCH", linePlusBranchPair)
    return reportMap
}
