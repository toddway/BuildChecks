package core.usecase

import core.entity.ConfigEntity
import core.entity.StatsEntity

open class HandleBuildSuccessUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val postStatsUseCase: PostStatsUseCase,
        val config: ConfigEntity,
        val summaries : List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.forEach { summary ->
            summary.summaryString()?.let { postStatusUseCase.success(it, summary.keyString()) }
        }

        val count = summaries
                .filter { it is GetLintSummaryUseCase }
                .map { it as GetLintSummaryUseCase }
                .sumBy { it.asTotal() ?: 0 }

        val coverageUseCase = summaries.find { it is GetCoverageSummaryUseCase } as GetCoverageSummaryUseCase

        postStatsUseCase.invoke(StatsEntity(
                coverageUseCase.percent() ?: 0.0,
                count,
                config.duration(),
                coverageUseCase.lines() ?: 0,
                config.commitDate(),
                config.hash(),
                config.commitBranch()
        ))
    }
}