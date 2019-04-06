package core.usecase

import core.entity.BuildConfig

class GetDurationSummaryUseCase(val config : BuildConfig) : GetSummaryUseCase{
    override fun isSuccessful(): Boolean {
        return config.isSuccess
    }

    override fun value(): String? {
        return config.completedMessage()
    }

    override fun key(): String {
        return "build"
    }
}
