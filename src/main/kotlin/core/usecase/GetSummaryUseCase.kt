package core.usecase

interface GetSummaryUseCase {
    fun summary() : String?
    fun key() : String
    fun isSuccessful() : Boolean
}

fun List<GetSummaryUseCase>.postAll(postStatusUseCase: PostStatusUseCase) {
    forEach { s ->
        s.summary()?.let {
            if (s.isSuccessful())
                postStatusUseCase.success(it, s.key())
            else
                postStatusUseCase.failure(it, s.key())
        }
    }
}
