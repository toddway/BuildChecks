package core.usecase

import core.entity.ConfigEntity

class GetDurationSummaryUseCase(val config : ConfigEntity) : GetSummaryUseCase{
    override fun isSuccessful(): Boolean {
        return true
    }

    override fun summary(): String? {
        return config.completedMessage()
    }

    override fun key(): String {
        return "gradle"
    }
}
