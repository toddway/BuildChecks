package core.usecase

import core.isNotGreaterThan
import core.round
import org.w3c.dom.Document
import java.text.NumberFormat

open class GetCoverageSummaryUseCase(
        val documents: List<Document>,
        val coverageMapper: CoverageMapper,
        val minCoveragePercent : Double? = null) : GetSummaryUseCase {
    val map by lazy { documents.toCoverageMaps(coverageMapper) }
    override fun isSuccessful(): Boolean = minCoveragePercent?.isNotGreaterThan(percent()) ?: true
    override fun key(): String = "test"

    override fun value(): String? {
        return percent()?.let { percent ->
            val count = NumberFormat.getIntegerInstance().format(map.countsOf(LINE_PLUS_BRANCH).total())
            val prefix = "$percent% coverage ($count lines+branches)"
            minCoveragePercent?.let { "$prefix, min is $it%" } ?: prefix
        }
    }

    fun percent(): Double? {
        return if (documents.isEmpty()) null else {
            map.countsOf(LINE_PLUS_BRANCH)
                    .percentCovered()
                    .let {
                        val rounded = it.round(2)
                        if (rounded <= 0) null else rounded
                    }
        }
    }

    fun linesPlusBranches(): Int? {
        return if (documents.isEmpty()) null else {
            map.countsOf(LINE_PLUS_BRANCH).total()
        }
    }
}

fun List<Document>.buildCobertura(minCoveragePercent: Double?) : GetCoverageSummaryUseCase {
    return GetCoverageSummaryUseCase(this, CoverageCoberturaMapper(), minCoveragePercent)
}

fun List<Document>.buildJacoco(minCoveragePercent: Double?) : GetCoverageSummaryUseCase {
    return GetCoverageSummaryUseCase(this, CoverageJacocoMapper(), minCoveragePercent)
}


fun List<Document>.buildCoverage(minCoveragePercent: Double?) : GetCoverageSummaryUseCase {
    val mappers = listOf(CoverageJacocoMapper(), CoverageCoberturaMapper())
    return GetCoverageSummaryUseCase(this, CoverageMultiMapper(mappers), minCoveragePercent)
}

const val LINE_PLUS_BRANCH = "LINE + BRANCH"
