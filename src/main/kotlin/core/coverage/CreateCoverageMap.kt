package core.coverage

import org.w3c.dom.Document

interface CreateCoverageMap {
    fun from(document: Document): Map<String?, Pair<Int, Int>>
}
