package di

import core.BuildStatsDatasource
import core.BuildStatusConfig
import core.BuildStatusDatasource
import data.*

fun provideBuildStatusDatasources(config : BuildStatusConfig, postStatus : Boolean) : List<BuildStatusDatasource> {
    val datasources = mutableListOf<BuildStatusDatasource>(ConsoleDatasource())
    if (postStatus) {
        if (config.statusBaseUrl.contains("bitbucket")) {
            val service = createBitBucketService(config.statusBaseUrl, config.statusAuthorization)
            datasources.add(BitBucketDatasource(service, config.hash, config.buildUrl))
        } else if (config.statusBaseUrl.contains("github")) {
            val service = createGithubService(config.statusBaseUrl, config.statusAuthorization)
            datasources.add(GithubDatasource(service, config.hash, config.buildUrl))
        }
    }
    return datasources
}

fun provideBuildStatsDatasources(config : BuildStatusConfig, postStatus : Boolean): List<BuildStatsDatasource> {
    val datasources = mutableListOf<BuildStatsDatasource>()
    if (postStatus) {
        if (config.statsBaseUrl.isNotBlank()) {
            val service = createRetrifotBuildStatsService(config.statsBaseUrl, "")
            datasources.add(RetrofitBuildStatsDatasource(service))
        }
    }
    return datasources
}