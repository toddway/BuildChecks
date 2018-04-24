package core

import org.w3c.dom.Document
import org.w3c.dom.NodeList

class GetJacocoSummaryUseCase(val documents: List<Document>) {
    fun asString(): String? {
        return asNumber()?.let { "$it% test coverage" }
    }

    fun asNumber(): Double? {
        if (documents.isEmpty()) return null
        else return documents.let {
            val list = it.map { it.toJacocoMap() }.map { it["LINE + BRANCH"] }
            val sums = Pair(
                    list.sumByDouble { it?.first ?: 0.0},
                    list.sumByDouble { it?.second ?: 0.0 }
            )
            sums.percentage().round(2)
        }
    }
}

fun Document.toJacocoMap(): Map<String?, Pair<Double, Double>> {
    val nodeList : NodeList = getElementsByTagName("method") ?: return mapOf()

    val counterMap = nodeList.children().flatMap { it.childNodes.children() }.groupBy { it.attr("type") }

    val metricMap = counterMap.mapValues {entry ->
        val covered = entry.value.sumByDouble { it.attr("covered")?.toDouble() ?: 0.0}
        val missed = entry.value.sumByDouble { it.attr("missed")?.toDouble()  ?: 0.0 }
        Pair(covered, missed)
    }

    val linePlusBranchMap = metricMap.filter { entry -> entry.key == "LINE" || entry.key == "BRANCH" }
    val linePlusBranchPair = Pair(linePlusBranchMap.values.sumByDouble { it.first }, linePlusBranchMap.values.sumByDouble { it.second })

    val reportMap = metricMap.toMutableMap()
    reportMap.put("LINE + BRANCH", linePlusBranchPair)
    return reportMap
}



