package core.usecase

import core.entity.BuildConfig
import core.entity.BuildStatus

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: BuildConfig
) {
    fun invoke() {
        if (config.isPluginActivated) {
            config.allReports().forEach { it.delete() }
            postStatusUseCase.post(BuildStatus.PENDING, config.startedMessage(), "build")
        }
    }
}
