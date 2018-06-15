package core.usecase

import core.entity.ConfigEntity
import core.entity.Message

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: ConfigEntity,
        val messageQueue: MutableList<Message>
) {
    fun invoke() {
        if (config.isPluginActivated) {
            postStatusUseCase.pending(config.startedMessage(), "gradle")
        }
    }
}
