package core.usecase

open class HandleBuildFailedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val summaries: List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.forEach { summary ->
            summary.summaryString()?.let { postStatusUseCase.failure(it, summary.keyString()) }
        }
    }
}