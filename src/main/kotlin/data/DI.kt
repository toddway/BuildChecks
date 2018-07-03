package data

import core.datasource.StatsDatasource
import core.datasource.StatusDatasource
import core.entity.*
import core.toDocumentList
import core.usecase.*


class DI(val config : BuildConfig = BuildConfigDefault()) {

    private fun statusDatasources() : List<StatusDatasource> {
        val datasources = mutableListOf<StatusDatasource>(ConsoleDatasource())

        if (config.isPostActivated) {
            findRemoteStatusDatasource(config)?.let {
                val name = it.name().toUpperCase()
                if (config.allowUncommittedChanges || config.git.isAllCommitted) {
                    datasources.add(it)
                    messageQueue.add(InfoMessage("Posting to $name"))
                } else {
                    messageQueue.add(ErrorMessage("You must commit all changes before posting checks to $name"))
                }
            } ?: messageQueue.add(ErrorMessage("No recognized post config was found"))
        }

        return datasources
    }

    private fun statsDatasources(): List<StatsDatasource> {
        val datasources = mutableListOf<StatsDatasource>()
        if (config.statsBaseUrl.isNotBlank()) {
            val service = createRetrifotBuildStatsService(config.statsBaseUrl, "")
            datasources.add(RetrofitStatsDatasource(service))
        }
        return datasources
    }

    private fun postStatusUseCase() : PostStatusUseCase {
        return PostStatusUseCase(statusDatasources(), messageQueue)
    }

    private fun handleBuildSuccessUseCase() : HandleBuildSuccessUseCase {
        return HandleBuildSuccessUseCase(
                postStatusUseCase(),
                PostStatsUseCase(statsDatasources()),
                config,
                summariesUseCases()
        )
    }

    private fun handleBuildFailedUseCase() : HandleBuildFailedUseCase {
        return HandleBuildFailedUseCase(postStatusUseCase(), summariesUseCases())
    }

    private fun summariesUseCases(): List<GetSummaryUseCase> {
        return listOf(
                GetDurationSummaryUseCase(config),
                GetCoverageSummaryUseCase(
                        config.coberturaReports.toDocumentList(),
                        CreateCoberturaMap(),
                        config.minCoveragePercent),
                GetCoverageSummaryUseCase(
                        config.jacocoReports.toDocumentList(),
                        CreateJacocoMap(),
                        config.minCoveragePercent),
                GetLintSummaryUseCase(
                        config.androidLintReports.toDocumentList()
                                + config.checkstyleReports.toDocumentList()
                                + config.cpdReports.toDocumentList(),
                        config.maxLintViolations
                )
        )
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase(), config)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase(), handleBuildSuccessUseCase(), config, messageQueue)
    }

    private val messageQueue = mutableListOf<Message>()
}



