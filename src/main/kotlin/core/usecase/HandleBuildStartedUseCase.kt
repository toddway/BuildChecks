package core.usecase

import core.entity.ConfigEntity

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: ConfigEntity
) {
    fun invoke() {
        if (config.isPluginActivated) {
            postStatusUseCase.pending(config.startedMessage(), "build")
        }
    }
}
