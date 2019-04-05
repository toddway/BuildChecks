package core.usecase

import core.entity.BuildConfig
import core.toDocumentList

fun getSummaryUseCaseList(config : BuildConfig): List<GetSummaryUseCase> {
    return listOf(
            GetDurationSummaryUseCase(config),
            GetCoverageSummaryUseCase(
                    config.coberturaReports.toDocumentList(),
                    CreateCoberturaMap(),
                    config.minCoveragePercent),
            GetCoverageSummaryUseCase(
                    config.jacocoReports.toDocumentList(),
                    CreateJacocoMap(),
                    config.minCoveragePercent),
            GetLintSummaryUseCase(
                    config.androidLintReports.toDocumentList()
                            + config.checkstyleReports.toDocumentList()
                            + config.cpdReports.toDocumentList(),
                    config.maxLintViolations
            )
    )
}
