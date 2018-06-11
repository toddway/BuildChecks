package gradle

import core.datasource.StatsDatasource
import core.datasource.StatusDatasource
import core.entity.ConfigEntity
import core.entity.ConfigEntityDefault
import core.toDocumentList
import core.usecase.*
import data.*


class DI(val config : ConfigEntity = ConfigEntityDefault()) {

    fun statusDatasources() : List<StatusDatasource> {
        val datasources = mutableListOf<StatusDatasource>(ConsoleDatasource())
        if (config.statusBaseUrl.contains("bitbucket")) {
            val service = createBitBucketService(config.statusBaseUrl, config.statusAuthorization)
            datasources.add(BitBucketDatasource(service, config.hash(), config.buildUrl))
        } else if (config.statusBaseUrl.contains("github")) {
            val service = createGithubService(config.statusBaseUrl, config.statusAuthorization)
            datasources.add(GithubDatasource(service, config.hash(), config.buildUrl))
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

    fun setStatusUseCase() : SetStatusUseCase {
        return SetStatusUseCase(statusDatasources())
    }

    fun handleBuildSuccessUseCase() : HandleBuildSuccessUseCase {
        return HandleBuildSuccessUseCase(
                setStatusUseCase(),
                PostStatsUseCase(statsDatasources()),
                config,
                summariesList()
        )
    }

    fun summariesList(): List<GetSummaryUseCase> {
        return listOf(
                GetJacocoSummaryUseCase(config.jacocoReports.toDocumentList()),
                GetLintSummaryUseCase(config.lintReports.toDocumentList()),
                GetDetektSummaryUseCase(config.detektReports.toDocumentList()),
                GetCheckstyleSummaryUseCase(config.checkStyleReports.toDocumentList()),
                GetCoberturaSummaryUseCase(config.coberturaReports.toDocumentList())
        )
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(setStatusUseCase(), config)
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(setStatusUseCase(), handleBuildSuccessUseCase(), config)
    }
}



