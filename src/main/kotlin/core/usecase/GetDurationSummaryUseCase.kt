package core.usecase

import core.entity.BuildConfig
import core.isNotGreaterThan

class GetDurationSummaryUseCase(val config : BuildConfig) : GetSummaryUseCase{
    override fun isSuccessful(): Boolean {
        return config.isSuccess && config.duration().isNotGreaterThan(config.maxDuration)
    }

    override fun value(): String? {
        var message = config.completedMessage()
        config.maxDuration?.let { message += ", max is ${it}s" }
        return message
    }

    override fun key(): String {
        return "build"
    }
}
