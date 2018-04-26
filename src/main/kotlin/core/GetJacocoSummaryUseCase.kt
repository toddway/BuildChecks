package core

import org.w3c.dom.Document
import org.w3c.dom.NodeList

class GetJacocoSummaryUseCase(val documents: List<Document>) {
    fun percentAsString(): String? {
        return percent()?.let { "$it% test coverage" }
    }

    fun percent(): Double? {
        return if (documents.isEmpty()) null
        else documents.let {
            val list = it.map { it.toJacocoMap() }.map { it["LINE + BRANCH"] }
            val sums = Pair(
                    list.sumBy { it?.first ?: 0},
                    list.sumBy { it?.second ?: 0 }
            )
            sums.percentage().round(2)
        }
    }

    fun lines(): Int? {
        return if (documents.isEmpty()) null
        else documents.let {
            val list = it.map { it.toJacocoMap() }.map { it["LINE"] }
            val sums = Pair(
                    list.sumBy { it?.first ?: 0 },
                    list.sumBy { it?.second ?: 0 }
            )
            sums.first + sums.second
        }
    }
}

fun Document.toJacocoMap(): Map<String?, Pair<Int, Int>> {
    val nodeList : NodeList = getElementsByTagName("method") ?: return mapOf()

    val counterMap = nodeList.children().flatMap { it.childNodes.children() }.groupBy { it.attr("type") }

    val statsMap = counterMap.mapValues {entry ->
        val covered = entry.value.sumBy { it.attr("covered")?.toInt() ?: 0 }
        val missed = entry.value.sumBy { it.attr("missed")?.toInt()  ?: 0 }
        Pair(covered, missed)
    }

    val linePlusBranchMap = statsMap.filter { entry -> entry.key == "LINE" || entry.key == "BRANCH" }
    val linePlusBranchPair = Pair(linePlusBranchMap.values.sumBy { it.first }, linePlusBranchMap.values.sumBy { it.second })

    val reportMap = statsMap.toMutableMap()
    reportMap.put("LINE + BRANCH", linePlusBranchPair)
    return reportMap
}



