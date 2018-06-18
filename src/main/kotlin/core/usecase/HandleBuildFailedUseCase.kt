package core.usecase

open class HandleBuildFailedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val summaries: List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.postAll(postStatusUseCase)
    }
}
