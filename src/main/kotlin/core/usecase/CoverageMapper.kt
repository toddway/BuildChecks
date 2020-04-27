package core.usecase

import core.HUNDRED
import org.w3c.dom.Document

interface CoverageMapper {
    fun from(document: Document): Map<String?, CoverageCounts>
}

data class CoverageCounts(val covered : Int, val missed : Int) {
    fun total() = covered + missed
    fun percentCovered(): Double {
        return if (covered == 0 && missed == 0) 0.0
        else ((covered.toDouble() / total()) * HUNDRED)
    }

    override fun toString(): String {
        return "[covered=$covered, missed=$missed, total=${total()}, coverage=${percentCovered()}]"
    }
}

fun List<Document>.toCoverageMaps(coverageMapper: CoverageMapper) : List<Map<String?, CoverageCounts>> =
        map { coverageMapper.from(it) }

fun List<CoverageCounts?>.sumCounts() : CoverageCounts =
        CoverageCounts(sumBy { it?.covered ?: 0}, sumBy { it?.missed ?: 0 })

fun List<Map<String?, CoverageCounts>>.countsOf(countType : String) : CoverageCounts =
        map { it[countType] }.sumCounts()

