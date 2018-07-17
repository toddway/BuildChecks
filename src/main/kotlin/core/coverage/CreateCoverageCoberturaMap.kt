package core.coverage

import core.entity.attr
import core.entity.children
import core.entity.toIntOrZero
import org.w3c.dom.Document
import org.w3c.dom.Element

class CreateCoberturaMap : CreateCoverageMap {
    override fun from(document: Document): Map<String?, Pair<Int, Int>> {
        return document.toCoburturaMap()
    }
}

fun Document.toCoburturaMap(): Map<String?, Pair<Int, Int>> {
    val root = childNodes.children().find { it is Element }
    val reportMap = mutableMapOf<String?, Pair<Int, Int>>()
    root?.let {
        val covered = (it.attr("lines-covered").toIntOrZero()) + (it.attr("branches-covered").toIntOrZero())
        val valid = (it.attr("lines-valid").toIntOrZero()) + (it.attr("branches-valid").toIntOrZero())
        val missed = valid - covered
        reportMap.put("LINE + BRANCH", Pair(covered, missed))
    }
    return reportMap
}
