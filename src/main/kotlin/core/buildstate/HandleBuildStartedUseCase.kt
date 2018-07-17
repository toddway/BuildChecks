package core.buildstate

import core.entity.BuildConfig
import core.post.PostStatusUseCase

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: BuildConfig
) {
    fun invoke() {
        if (config.isPluginActivated) {
            config.reportFiles().forEach { it.delete() }
            postStatusUseCase.pending(config.startedMessage(), "build")
        }
    }
}
