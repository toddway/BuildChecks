package core.usecase

import core.entity.BuildConfig
import core.entity.BuildStatus

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: BuildConfig
) {
    fun invoke() {
        config.log?.info("${this::class.simpleName} invoked")
        if (config.isChecksActivated) {
            config.artifactsDir().runCatching { deleteRecursively() }
            postStatusUseCase.post(BuildStatus.PENDING, config.startedMessage(), "build")
        } else {
            config.log = null
        }
    }
}
