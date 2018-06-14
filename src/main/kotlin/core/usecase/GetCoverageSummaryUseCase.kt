package core.usecase

import core.percentage
import core.round
import org.w3c.dom.Document

open class GetCoverageSummaryUseCase(val documents: List<Document>) : GetSummaryUseCase {
    override fun keyString(): String {
        return "coverage"
    }

    override fun summaryString(): String? {
        return percentAsString()
    }

    fun percentAsString(): String? {
        return percent()?.let { "$it% test coverage" }
    }

    fun percent(): Double? {
        return if (documents.isEmpty()) null
        else documents.let {
            val list = it.map { toCoverageMap(it) }.map { it["LINE + BRANCH"] }
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
            val list = it.map { toCoverageMap(it) }.map { it["LINE"] }
            val sums = Pair(
                    list.sumBy { it?.first ?: 0 },
                    list.sumBy { it?.second ?: 0 }
            )
            sums.first + sums.second
        }
    }

    open fun toCoverageMap(d : Document): Map<String?, Pair<Int, Int>> {
        return mapOf()
    }
}
