package core.usecase

import core.entity.ConfigEntity

class HandleBuildFinishedUseCase(
        private val handleBuildFailedUseCase: HandleBuildFailedUseCase,
        private val handleBuildSuccessUseCase: HandleBuildSuccessUseCase,
        private val config: ConfigEntity
) {
    fun invoke(isSuccess: Boolean) {
        if (config.isPluginActivated) {
            if (isSuccess) {
                handleBuildSuccessUseCase.invoke()
            } else {
                handleBuildFailedUseCase.invoke()
            }
        }
    }
}
