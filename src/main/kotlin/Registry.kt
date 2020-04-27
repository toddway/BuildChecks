import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.Message
import core.usecase.*
import data.buildList


class Registry(val config : BuildConfig = BuildConfigDefault()) {
    private val messages = mutableListOf<Message>()

    fun provideGetSummaryUseCases() =
            config.reportFiles().summaries(config)

    fun providePostStatsUseCase() =
            PostStatsUseCase(PostStatsUseCase.Datasource.buildList(config))

    fun providePostStatusUseCase() =
            PostStatusUseCase(PostStatusUseCase.Datasource.buildList(config), config, messages)

    fun provideHandleBuildStartedUseCase() =
            HandleBuildStartedUseCase(providePostStatusUseCase(), config)

    fun provideHandleBuildFinishedUseCase() =
            HandleBuildFinishedUseCase(
                    providePostStatusUseCase(),
                    providePostStatsUseCase(),
                    provideGetSummaryUseCases(),
                    config,
                    messages
            )

    fun provideHandleUnsuccessfulSummariesUseCase() =
            HandleUnsuccessfulSummariesUseCase(provideGetSummaryUseCases())
}
