package data

import core.entity.BuildConfig
import core.entity.Message
import core.usecase.*


class Instances(var config : BuildConfig) {
    private val messages = mutableListOf<Message>()

    private val getSummaryUseCases by lazy {
        GetSummaryUseCase.buildList(config)
    }

    private val postStatsUseCase by lazy {
        PostStatsUseCase(PostStatsUseCase.Datasource.buildList(config))
    }

    private val postStatusUseCase by lazy {
        PostStatusUseCase(PostStatusUseCase.Datasource.buildList(config), config, messages)
    }

    private val handleBuildSuccessUseCase by lazy {
        HandleBuildSuccessUseCase(postStatusUseCase, postStatsUseCase, config, getSummaryUseCases)
    }

    private val handleBuildFailedUseCase by lazy {
        HandleBuildFailedUseCase(postStatusUseCase, getSummaryUseCases)
    }

    val handleBuildStartedUseCase by lazy {
        HandleBuildStartedUseCase(postStatusUseCase, config)
    }

    val handleBuildFinishedUseCase by lazy {
        HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, messages)
    }
}
