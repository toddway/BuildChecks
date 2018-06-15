package gradle

import core.datasource.StatsDatasource
import core.datasource.StatusDatasource
import core.entity.*
import core.toDocumentList
import core.usecase.*
import data.ConsoleDatasource
import data.RetrofitStatsDatasource
import data.createRetrifotBuildStatsService
import data.findRemoteStatusDatasource


class DI(val config : ConfigEntity = ConfigEntityDefault()) {

    fun statusDatasources() : List<StatusDatasource> {
        val datasources = mutableListOf<StatusDatasource>(ConsoleDatasource())

        //TODO this block should be moved so it can be tested
        if (config.isPostActivated) {
            if (config.isAllChangesCommitted()) {
                findRemoteStatusDatasource(config)?.let {
                    datasources.add(it)
                    messageQueue.add(InfoMessage("${it.name().toUpperCase()} config was found"))
                } ?: messageQueue.add(ErrorMessage("No recognized post config was found"))
            } else {
                messageQueue.add(ErrorMessage("You must commit all changes to git before posting"))
            }
        }

        return datasources
    }


    fun statsDatasources(): List<StatsDatasource> {
        val datasources = mutableListOf<StatsDatasource>()
        if (config.statsBaseUrl.isNotBlank()) {
            val service = createRetrifotBuildStatsService(config.statsBaseUrl, "")
            datasources.add(RetrofitStatsDatasource(service))
        }
        return datasources
    }

    fun postStatusUseCase() : PostStatusUseCase {
        return PostStatusUseCase(statusDatasources(), messageQueue)
    }

    fun handleBuildSuccessUseCase() : HandleBuildSuccessUseCase {
        return HandleBuildSuccessUseCase(
                postStatusUseCase(),
                PostStatsUseCase(statsDatasources()),
                config,
                summariesUseCases()
        )
    }

    fun handleBuildFailedUseCase() : HandleBuildFailedUseCase {
        return HandleBuildFailedUseCase(postStatusUseCase(), summariesUseCases())
    }

    fun summariesUseCases(): List<GetSummaryUseCase> {
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
                        config.androidLintReports.toDocumentList() + config.checkstyleReports.toDocumentList(),
                        config.maxLintViolations
                )
        )
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase(), config, messageQueue)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase(), handleBuildSuccessUseCase(), config, messageQueue)
    }

    val messageQueue = mutableListOf<Message>()
}


