package core.usecase

import core.entity.BuildConfig
import core.entity.Message
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
            postStatsUseCase.post(summaries.toStats(config))
            config.writeBuildReports(config.toBuildReport(), messageQueue)
            config.pushArtifacts(messageQueue)
            messageQueue.distinct().forEach { println(it.toString()) }
        }
    }
}
