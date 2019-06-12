package core.usecase

import core.entity.BuildConfig
import core.isNotGreaterThan
import core.percentage
import core.round
import core.toDocuments
import org.w3c.dom.Document

open class GetCoverageSummaryUseCase(
        val documents: List<Document>,
        val createCoverageMap: CreateCoverageMap,
        val minCoveragePercent : Double? = null) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean = minCoveragePercent?.isNotGreaterThan(percent()) ?: true
    override fun key(): String = "test"

    override fun value(): String? {
        var summary = percent()?.let { "$it% coverage in ${documents.size} reports" }
        if (!summary.isNullOrBlank())
            minCoveragePercent?.let { summary += ", min is $minCoveragePercent%" }
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

    companion object
}


fun GetCoverageSummaryUseCase.Companion.buildCoburtera(config: BuildConfig) : GetCoverageSummaryUseCase = with(config) {
    return GetCoverageSummaryUseCase(coberturaReports.toDocuments(config.log), CreateCoverageCoberturaMap(), minCoveragePercent)
}

fun GetCoverageSummaryUseCase.Companion.buildJacoco(config: BuildConfig) : GetCoverageSummaryUseCase = with(config) {
    return GetCoverageSummaryUseCase(jacocoReports.toDocuments(config.log), CreateCoverageJacocoMap(), minCoveragePercent)
}

