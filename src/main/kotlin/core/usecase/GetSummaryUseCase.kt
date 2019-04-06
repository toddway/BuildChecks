package core.usecase

import core.entity.BuildConfig
import core.entity.BuildStatus
import core.toDocumentList

interface GetSummaryUseCase {
    fun value() : String?
    fun key() : String
    fun isSuccessful() : Boolean
    companion object
}

fun List<GetSummaryUseCase>.postAll(postStatusUseCase: PostStatusUseCase) {
    forEach { getSummaryUseCase ->
        getSummaryUseCase.value()?.let {
            if (getSummaryUseCase.isSuccessful())
                postStatusUseCase.post(BuildStatus.SUCCESS, it, getSummaryUseCase.key())
            else
                postStatusUseCase.post(BuildStatus.FAILURE, it, getSummaryUseCase.key())
        }
    }
}

fun BuildConfig.buildGetSummaryUseCases(): List<GetSummaryUseCase> {
    return listOf(
            GetDurationSummaryUseCase(this),
            GetCoverageSummaryUseCase(
                    coberturaReports.toDocumentList(),
                    CreateCoberturaMap(),
                    minCoveragePercent),
            GetCoverageSummaryUseCase(
                    jacocoReports.toDocumentList(),
                    CreateJacocoMap(),
                    minCoveragePercent),
            GetLintSummaryUseCase(
                    androidLintReports.toDocumentList()
                            + checkstyleReports.toDocumentList()
                            + cpdReports.toDocumentList(),
                    maxLintViolations
            )
    )
}
