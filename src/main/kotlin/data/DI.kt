package data

import core.datasource.StatsDatasource
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Message
import core.usecase.*


class DI(val config : BuildConfig = BuildConfigDefault()) {

    private val retrofit by lazy { retrofit(config.baseUrl, config.authorization) }

    private val list by lazy {
        listOf(
                ConsoleDatasource(),
                GithubDatasource(retrofit, config),
                BitBucket1Datasource(retrofit, config),
                BitBucket2Datasource(retrofit, config)
        )
    }

    private val postStatusUseCase by lazy {
        PostStatusUseCase(list, config, messageQueue)
    }

    private fun statsDatasources(): List<StatsDatasource> {
        val datasources = mutableListOf<StatsDatasource>()
        if (config.statsBaseUrl.isNotBlank()) {
            val service = createRetrifotBuildStatsService(config.statsBaseUrl, "")
            datasources.add(RetrofitStatsDatasource(service))
        }
        return datasources
    }

    private val handleBuildSuccessUseCase by lazy {
        HandleBuildSuccessUseCase(
                postStatusUseCase,
                PostStatsUseCase(statsDatasources()),
                config,
                getSummaryUseCaseList(config)
        )
    }

    private val handleBuildFailedUseCase by lazy {
        HandleBuildFailedUseCase(postStatusUseCase, getSummaryUseCaseList(config))
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase, config)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase, handleBuildSuccessUseCase, config, messageQueue)
    }

    private val messageQueue = mutableListOf<Message>()
}



