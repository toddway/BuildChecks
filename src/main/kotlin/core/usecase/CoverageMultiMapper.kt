package core.usecase

import org.w3c.dom.Document

class CoverageMultiMapper(val mappers : List<CoverageMapper>) : CoverageMapper {
    override fun from(document: Document): Map<String?, CoverageCounts> {
        return mappers
                .map { it.from(document) }
                .reduce { acc, map ->
                    acc.mapValues {
                        CoverageCounts(
                                it.value.covered + (map[it.key]?.covered ?: 0),
                                it.value.missed + (map[it.key]?.missed ?: 0)
                        )
                    }
                }
    }
}