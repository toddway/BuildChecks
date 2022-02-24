package core.usecase

import core.entity.BuildConfig
import core.entity.Message
import core.entity.printAll
import pushArtifacts

class HandleBuildFinishedUseCase(
        private val postStatusUseCase: PostStatusUseCase,
        private val postStatsUseCase: PostStatsUseCase,
        private val summaries: List<GetSummaryUseCase>,
        private val config: BuildConfig,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        config.log?.info("${this::class.simpleName} invoked")
        if (config.isChecksActivated) {
            summaries.postStatuses(postStatusUseCase)
            summaries.postStats(config, postStatsUseCase)
            config.writeBuildReports(messageQueue)
            messageQueue.printAll()
            summaries.throwIfUnsuccessful(config)
            config.pushArtifacts(messageQueue)
        }
    }
}
