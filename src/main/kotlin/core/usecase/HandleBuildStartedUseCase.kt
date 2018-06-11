package core.usecase

import core.entity.ConfigEntity

class HandleBuildStartedUseCase(val setStatusUseCase: SetStatusUseCase, val config: ConfigEntity) {
    fun invoke() {
        if (config.isPostEnabled) {
            setStatusUseCase.pending(config.startedMessage(), "g")
        }
    }
}