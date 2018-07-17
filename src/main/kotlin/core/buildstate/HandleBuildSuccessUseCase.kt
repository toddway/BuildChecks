package core.buildstate

import core.entity.BuildConfig
import core.entity.Stats
import core.summary.GetCoverageSummaryUseCase
import core.summary.GetLintSummaryUseCase
import core.summary.GetSummaryUseCase
import core.summary.postAll
import core.post.PostStatsUseCase
import core.post.PostStatusUseCase

open class HandleBuildSuccessUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val postStatsUseCase: PostStatsUseCase,
        val config: BuildConfig,
        val summaries : List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.postAll(postStatusUseCase)

        val lintUseCase = summaries.find { it is GetLintSummaryUseCase } as GetLintSummaryUseCase
        val coverageUseCase = summaries.find { it is GetCoverageSummaryUseCase } as GetCoverageSummaryUseCase
        postStatsUseCase.invoke(Stats(
                coverageUseCase.percent() ?: 0.0,
                lintUseCase.asTotal() ?: 0,
                config.duration(),
                coverageUseCase.lines() ?: 0,
                config.git.commitDate,
                config.git.commitHash,
                config.git.commitBranch
        ))
    }
}
