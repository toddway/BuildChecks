package core.usecase

import core.attr
import core.children
import org.w3c.dom.Document
import org.w3c.dom.Element

class CoverageCoberturaMapper : CoverageMapper {
    override fun from(document: Document): Map<String?, CoverageCounts> {
        return document.toCoburturaMap()
    }
}

fun Document.toCoburturaMap(): Map<String?, CoverageCounts> {
    val root = childNodes.children().find { it is Element }
    val reportMap = mutableMapOf<String?, CoverageCounts>()
    root?.let {
        val covered = (it.attr("lines-covered")?.toIntOrNull() ?: 0) + (it.attr("branches-covered")?.toIntOrNull() ?: 0)
        val valid = (it.attr("lines-valid")?.toIntOrNull() ?: 0) + (it.attr("branches-valid")?.toIntOrNull() ?: 0)
        val missed = valid - covered
        reportMap.put("LINE + BRANCH", CoverageCounts(covered, missed))
    }
    return reportMap
}
