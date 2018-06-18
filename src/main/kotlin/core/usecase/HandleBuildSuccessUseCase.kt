package core.usecase

import core.commitBranch
import core.commitDate
import core.entity.ConfigEntity
import core.entity.StatsEntity
import core.commitHash

open class HandleBuildSuccessUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val postStatsUseCase: PostStatsUseCase,
        val config: ConfigEntity,
        val summaries : List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.postAll(postStatusUseCase)

        val lintUseCase = summaries.find { it is GetLintSummaryUseCase } as GetLintSummaryUseCase
        val coverageUseCase = summaries.find { it is GetCoverageSummaryUseCase } as GetCoverageSummaryUseCase
        postStatsUseCase.invoke(StatsEntity(
                coverageUseCase.percent() ?: 0.0,
                lintUseCase.asTotal() ?: 0,
                config.duration(),
                coverageUseCase.lines() ?: 0,
                commitDate(),
                commitHash(),
                commitBranch()
        ))
    }
}
