package core.usecase

interface GetSummaryUseCase {
    fun summaryString() : String?
    fun keyString() : String
}
