package core.usecase

import core.attr
import core.children
import org.w3c.dom.Document
import org.w3c.dom.NodeList

class CoverageJacocoMapper : CoverageMapper {
    override fun from(document: Document): Map<String?, CoverageCounts> {
        return document.toJacocoMap()
    }
}

fun Document.toJacocoMap(): Map<String?, CoverageCounts> {
    val nodeList : NodeList = getElementsByTagName("method") ?: return mapOf()

    val counterMap = nodeList.children().flatMap { it.childNodes.children() }.groupBy { it.attr("type") }

    val statsMap = counterMap.mapValues {entry ->
        val covered = entry.value.sumBy { it.attr("covered")?.toIntOrNull() ?: 0 }
        val missed = entry.value.sumBy { it.attr("missed")?.toIntOrNull()  ?: 0 }
        CoverageCounts(covered, missed)
    }

    val linePlusBranchMap = statsMap.filter { entry -> entry.key == "LINE" || entry.key == "BRANCH" }
    val linePlusBranchPair = CoverageCounts(
            linePlusBranchMap.values.sumBy { it.covered },
            linePlusBranchMap.values.sumBy { it.missed }
    )

    val reportMap = statsMap.toMutableMap()
    reportMap.put("LINE + BRANCH", linePlusBranchPair)
    return reportMap
}
