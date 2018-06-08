package core

class HandleBuildSuccessUseCase(
        val setBuildStatusUseCase: SetBuildStatusUseCase? = null,
        val postBuildStatsUseCase: PostBuildStatsUseCase,
        val buildStatusConfig: BuildStatusProperties,
        val summaries : List<SummaryUseCase>
) {
    fun invoke() {
        summaries.forEach { summary ->
            summary.summaryString()?.let { setBuildStatusUseCase?.success(it, summary.keyString()) }
        }

        val count = summaries
                .filter { it is GetLintSummaryUseCase }
                .map { it as GetLintSummaryUseCase }
                .sumBy { it.asTotal() ?: 0 }

        val coverageUseCase = summaries.find { it is GetCoverageSummaryUseCase } as GetCoverageSummaryUseCase

        postBuildStatsUseCase.invoke(BuildStats(
                coverageUseCase.percent() ?: 0.0,
                count,
                buildStatusConfig.duration(),
                coverageUseCase.lines() ?: 0,
                buildStatusConfig.commitDate(),
                buildStatusConfig.hash(),
                buildStatusConfig.commitBranch()
        ))
    }
}