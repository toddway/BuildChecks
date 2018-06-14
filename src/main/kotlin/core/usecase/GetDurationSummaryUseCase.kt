package core.usecase

import core.entity.ConfigEntity

class GetDurationSummaryUseCase(val config : ConfigEntity) : GetSummaryUseCase{
    override fun summaryString(): String? {
        return config.completedMessage()
    }

    override fun keyString(): String {
        return "g"
    }
}
