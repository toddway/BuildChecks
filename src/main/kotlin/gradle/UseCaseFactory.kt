package gradle

import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Message
import core.usecase.*
import data.buildPostStatsDatasources
import data.buildPostStatusDatasources


class UseCaseFactory(val config : BuildConfig = BuildConfigDefault()) {
    private val messages = mutableListOf<Message>()
    private val getSummaryUseCases by lazy { config.buildGetSummaryUseCases() }
    private val postStatsUseCase by lazy { PostStatsUseCase(config.buildPostStatsDatasources()) }

    private val postStatusUseCase by lazy {
        PostStatusUseCase(config.buildPostStatusDatasources(), config, messages)
    }

    private val handleBuildSuccessUseCase by lazy {
        HandleBuildSuccessUseCase(postStatusUseCase, postStatsUseCase, config, getSummaryUseCases)
    }

    private val handleBuildFailedUseCase by lazy {
        HandleBuildFailedUseCase(postStatusUseCase, getSummaryUseCases)
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase, config)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, messages)
    }
}
