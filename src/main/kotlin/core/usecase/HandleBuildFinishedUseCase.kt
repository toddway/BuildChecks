package core.usecase

import core.entity.ConfigEntity
import core.entity.Message

class HandleBuildFinishedUseCase(
        private val handleBuildFailedUseCase: HandleBuildFailedUseCase,
        private val handleBuildSuccessUseCase: HandleBuildSuccessUseCase,
        private val config: ConfigEntity,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        if (config.isPluginActivated) {
            if (config.isSuccess) {
                handleBuildSuccessUseCase.invoke()
            } else {
                handleBuildFailedUseCase.invoke()
            }
            messageQueue.distinct().forEach { println(it.toString()) }
        }
    }
}
