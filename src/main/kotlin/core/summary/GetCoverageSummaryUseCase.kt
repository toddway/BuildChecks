package core.summary

import core.entity.isNotGreaterThan
import core.entity.percentage
import core.entity.round
import core.coverage.CreateCoverageMap
import org.w3c.dom.Document

open class GetCoverageSummaryUseCase(
        val documents: List<Document>,
        val createCoverageMap: CreateCoverageMap,
        val minCoveragePercent : Double? = null) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean {
        return minCoveragePercent?.isNotGreaterThan(percent()) ?: true
    }

    override fun key(): String {
        return "test"
    }

    override fun summary(): String? {
        var summary = percent()?.let { "$it% coverage" }
        if (!summary.isNullOrBlank()) minCoveragePercent?.let { summary += ", threshold is $minCoveragePercent%" }
        return summary
    }

    fun percent(): Double? {
        return if (documents.isEmpty()) null
        else documents.let {
            val list = it.map { createCoverageMap.from(it) }.map { it["LINE + BRANCH"] }
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
            val list = it.map { createCoverageMap.from(it) }.map { it["LINE"] }
            val sums = Pair(
                    list.sumBy { it?.first ?: 0 },
                    list.sumBy { it?.second ?: 0 }
            )
            sums.first + sums.second
        }
    }
}
