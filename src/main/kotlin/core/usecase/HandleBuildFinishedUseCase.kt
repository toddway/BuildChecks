package core.usecase

import core.entity.BuildConfig
import core.entity.Message

class HandleBuildFinishedUseCase(
        private val postStatusUseCase: PostStatusUseCase,
        private val postStatsUseCase: PostStatsUseCase,
        private val getSummaryUseCases: List<GetSummaryUseCase>,
        private val config: BuildConfig,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        if (config.isPluginActivated) {
            getSummaryUseCases.postAll(postStatusUseCase)
            postStatsUseCase.invoke(config.stats(getSummaryUseCases))
            messageQueue.distinct().forEach { println(it.toString()) }
        }
    }
}
