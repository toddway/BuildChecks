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

        //TODO this block should be moved so it can be tested
        if (config.isPostActivated) {
            if (config.baseUrl.contains("bitbucket")) {
                datasources.add(bitbucketDatasource())
            } else if (config.baseUrl.contains("github")) {
                datasources.add(githubDatasource())
            }
        }

        return datasources
    }

    fun bitbucketDatasource() : BitBucketDatasource {
        val service = createBitBucketService(config.baseUrl, config.authorization)
        return BitBucketDatasource(service, config.hash(), config.buildUrl)
    }

    fun githubDatasource() : GithubDatasource {
        val service = createGithubService(config.baseUrl, config.authorization)
        return GithubDatasource(service, config.hash(), config.buildUrl)
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
        return PostStatusUseCase(statusDatasources(), ShowMessageUseCase())
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
                GetCoberturaSummaryUseCase(config.coberturaReports.toDocumentList()),
                GetJacocoSummaryUseCase(config.jacocoReports.toDocumentList()),
                GetLintSummaryUseCase(
                        config.androidLintReports.toDocumentList()
                                + config.checkstyleReports.toDocumentList()
                )
        )
    }

    fun handleBuildStartedUseCase() : HandleBuildStartedUseCase {
        return HandleBuildStartedUseCase(postStatusUseCase(), config, ShowMessageUseCase())
    }

    fun handleBuildFinishedUseCase() : HandleBuildFinishedUseCase {
        return HandleBuildFinishedUseCase(handleBuildFailedUseCase(), handleBuildSuccessUseCase(), config)
    }
}



