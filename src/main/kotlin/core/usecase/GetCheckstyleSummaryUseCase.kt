package core.usecase

import org.w3c.dom.Document

open class GetCheckstyleSummaryUseCase(documents: List<Document>) : GetLintSummaryUseCase(documents) {

    init {
        var suffix = " Checkstyle issues "
        var tagName = "error"
    }

    override fun keyString(): String {
        return "ch"
    }
}