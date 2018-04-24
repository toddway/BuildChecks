package core

import org.w3c.dom.Document


class GetDetektSummaryUseCase(documents: List<Document>) : GetLintSummaryUseCase(documents) {

    init{
        suffix = " Detekt issues "
        tagName = "error"
    }
}