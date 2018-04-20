package core

import java.io.File

class GetJacocoSummaryUseCase(val file : File) {
    fun execute(): String {
        val v = getReportMap()["LINE + BRANCH"]?.percentage()?.round(1) ?: 0.0
        return "$v% test coverage"
    }

    fun getReportMap(): MutableMap<String?, Pair<Double, Double>> {
        val document = GetDocumentUseCase(file).execute()
        val nodeList = document.getElementsByTagName("method")
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
}



