package di

import core.*
import data.*

class DI(val config : BuildStatusProperties = BuildStatusExtension()) {

    fun buildStatusDatasources() : List<BuildStatusDatasource> {
        val datasources = mutableListOf<BuildStatusDatasource>(ConsoleDatasource())
        if (config.isPostStatus) {
            if (config.statusBaseUrl.contains("bitbucket")) {
                val service = createBitBucketService(config.statusBaseUrl, config.statusAuthorization)
                datasources.add(BitBucketDatasource(service, config.hash(), config.buildUrl))
            } else if (config.statusBaseUrl.contains("github")) {
                val service = createGithubService(config.statusBaseUrl, config.statusAuthorization)
                datasources.add(GithubDatasource(service, config.hash(), config.buildUrl))
            }
        }
        return datasources
    }

    fun buildStatsDatasources(): List<BuildStatsDatasource> {
        val datasources = mutableListOf<BuildStatsDatasource>()
        if (config.isPostStatus) {
            if (config.statsBaseUrl.isNotBlank()) {
                val service = createRetrifotBuildStatsService(config.statsBaseUrl, "")
                datasources.add(RetrofitBuildStatsDatasource(service))
            }
        }
        return datasources
    }

    fun setBuildStatusUseCase() : SetBuildStatusUseCase {
        return SetBuildStatusUseCase(buildStatusDatasources())
    }

    fun handleBuildSuccessUseCase() : HandleBuildSuccessUseCase {
        val summaries = listOf(
                GetJacocoSummaryUseCase(config.jacocoReports.toDocumentList()),
                GetLintSummaryUseCase(config.lintReports.toDocumentList()),
                GetDetektSummaryUseCase(config.detektReports.toDocumentList()),
                GetCheckstyleSummaryUseCase(config.checkStyleReports.toDocumentList()),
                GetCoberturaSummaryUseCase(config.coberturaReports.toDocumentList())
        )

        return HandleBuildSuccessUseCase(
                setBuildStatusUseCase(),
                PostBuildStatsUseCase(buildStatsDatasources()),
                config,
                summaries
        )
    }
}



