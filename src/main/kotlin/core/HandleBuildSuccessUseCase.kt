package core

import org.w3c.dom.Document

class HandleBuildSuccessUseCase(
        val buildStatus: SetBuildStatusUseCase? = null,
        val jacocoDocs: List<Document>,
        val lintDocs: List<Document>,
        val detektDocs: List<Document>,
        val checkstyleDocs: List<Document>
) {
    fun invoke() {
        GetJacocoSummaryUseCase(jacocoDocs).asString()?.let { buildStatus?.success(it, "j") }
        GetLintSummaryUseCase(lintDocs).asString()?.let { buildStatus?.success(it, "l")}
        GetDetektSummaryUseCase(detektDocs).asString()?.let { buildStatus?.success(it, "d") }
        GetCheckstyleSummaryUseCase(checkstyleDocs).asString()?.let { buildStatus?.success(it, "c") }
    }
}