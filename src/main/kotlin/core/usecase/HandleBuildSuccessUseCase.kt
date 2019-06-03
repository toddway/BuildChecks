package core.usecase

import core.entity.BuildConfig

open class HandleBuildSuccessUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val postStatsUseCase: PostStatsUseCase,
        val config: BuildConfig,
        val summaries : List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.postAll(postStatusUseCase)
        postStatsUseCase.invoke(config.stats(summaries))
    }
}
