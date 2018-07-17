package core.buildstate

import core.summary.GetSummaryUseCase
import core.summary.postAll
import core.post.PostStatusUseCase

open class HandleBuildFailedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val summaries: List<GetSummaryUseCase>
) {
    open fun invoke() {
        summaries.postAll(postStatusUseCase)
    }
}
