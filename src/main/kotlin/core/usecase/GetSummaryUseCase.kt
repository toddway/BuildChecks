package core.usecase

import core.entity.BuildStatus

interface GetSummaryUseCase {
    fun value() : String?
    fun key() : String
    fun isSuccessful() : Boolean
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
