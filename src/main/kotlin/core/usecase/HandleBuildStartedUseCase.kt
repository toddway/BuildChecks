package core.usecase

import core.entity.BuildConfig
import core.entity.BuildStatus
import java.util.*

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: BuildConfig
) {
    fun invoke() {
        config.log?.info("${this::class.simpleName} invoked")
        config.buildStartTime = Date()
        if (config.isChecksActivated) {
            config.artifactsDir().runCatching {
                config.log?.info("Deleting $absolutePath")
                deleteRecursively()
            }
            postStatusUseCase.post(BuildStatus.PENDING, config.startedMessage(), "build")
        } else {
            config.log = null
        }
    }
}
