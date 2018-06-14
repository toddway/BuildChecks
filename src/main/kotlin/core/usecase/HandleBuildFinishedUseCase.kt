package core.usecase

import core.entity.ConfigEntity

class HandleBuildFinishedUseCase(
        private val handleBuildFailedUseCase: HandleBuildFailedUseCase,
        private val handleBuildSuccessUseCase: HandleBuildSuccessUseCase,
        private val config: ConfigEntity
) {
    fun invoke() {
        if (config.isPluginActivated) {
            if (config.isSuccess) {
                handleBuildSuccessUseCase.invoke()
            } else {
                handleBuildFailedUseCase.invoke()
            }
        }
    }
}
