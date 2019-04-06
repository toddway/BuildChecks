package data

import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Message
import core.usecase.*


class IoC(val config : BuildConfig = BuildConfigDefault()) {
    private val messageQueue = mutableListOf<Message>()
    private val retrofit by lazy { retrofit(config.baseUrl, config.authorization) }
    private val postStatusUseCase by lazy {
        PostStatusUseCase(config.buildPostStatusDatasources(retrofit), config, messageQueue)
    }
    private val getSummaryUseCases by lazy { config.buildGetSummaryUseCases() }
    private val handleBuildFailedUseCase by lazy {
        HandleBuildFailedUseCase(postStatusUseCase, getSummaryUseCases) }
    private val postStatsUseCase by lazy { PostStatsUseCase(config.buildPostStatsDatasources())
    }
    private val handleBuildSuccessUseCase by lazy {
        HandleBuildSuccessUseCase(postStatusUseCase, postStatsUseCase, config, getSummaryUseCases)
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase, config)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, messageQueue)
    }
}
