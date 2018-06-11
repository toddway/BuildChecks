package core.usecase

import core.entity.ConfigEntity

class HandleBuildFinishedUseCase(
        private val setStatusUseCase: SetStatusUseCase,
        private val handleBuildSuccessUseCase: HandleBuildSuccessUseCase,
        private val config: ConfigEntity
) {
    fun invoke(isSuccess: Boolean) {
        if (config.isPostEnabled) {
            if (isSuccess) {
                handleBuildSuccessUseCase.invoke()
                setStatusUseCase.success(config.completedMessage(), "g")
            } else {
                setStatusUseCase.failure(config.completedMessage(), "g")
            }
        }
    }
}