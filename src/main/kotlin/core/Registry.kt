package core

import core.entity.*
import core.usecase.*
import data.buildList

class Registry(val projectConfig: ProjectConfig) {
    private val messages = mutableListOf<Message>()
    val config = projectConfig.createBuildChecksConfig()

    fun provideGetSummaryUseCases() =
            config.reportFiles().toSummaries(config)

    fun providePostStatsUseCase() =
            PostStatsUseCase(PostStatsUseCase.Datasource.buildList(config))

    fun providePostStatusUseCase() =
            PostStatusUseCase(PostStatusUseCase.Datasource.buildList(config), config, messages)

    fun provideBuildStartedUseCase(): HandleBuildStartedUseCase {
        config.apply {
            taskName = projectConfig.taskNameString()
            isPostActivated = projectConfig.isPostChecksActivated()
            isChecksActivated = projectConfig.isChecksActivated()
            isPushActivated = projectConfig.isPushArtifactsActivated()
            isOpenActivated = projectConfig.isOpenChecksActivated()
            log = projectConfig.logger()
        }
        return HandleBuildStartedUseCase(providePostStatusUseCase(), config)
    }

    fun provideBuildFinishedUseCase(isSuccess: Boolean): HandleBuildFinishedUseCase {
        config.isSuccess = isSuccess
        return HandleBuildFinishedUseCase(
            providePostStatusUseCase(),
            providePostStatsUseCase(),
            provideGetSummaryUseCases(),
            config,
            messages
        )
    }
}
