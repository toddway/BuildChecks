package core.usecase

import core.writeSummaryReports
import core.entity.BuildConfig
import core.entity.Message
import pushArtifacts

class HandleBuildFinishedUseCase(
        private val postStatusUseCase: PostStatusUseCase,
        private val postStatsUseCase: PostStatsUseCase,
        private val getSummaryUseCases: List<GetSummaryUseCase>,
        private val config: BuildConfig,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        if (config.isChecksActivated) {
            getSummaryUseCases.postStatuses(postStatusUseCase)
            postStatsUseCase.invoke(getSummaryUseCases.toStats(config))
            config.writeSummaryReports(messageQueue)
            config.pushArtifacts(messageQueue)
            messageQueue.distinct().forEach { println(it.toString()) }
        }
    }
}

