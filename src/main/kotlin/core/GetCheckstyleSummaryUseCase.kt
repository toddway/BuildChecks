package core

import org.w3c.dom.Document

open class GetCheckstyleSummaryUseCase(documents: List<Document>) : GetLintSummaryUseCase(documents) {

    init {
        var suffix = " Checkstyle issues "
        var tagName = "error"
    }
}