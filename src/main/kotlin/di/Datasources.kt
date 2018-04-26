package di

import core.BuildStatsDatasource
import core.BuildStatusConfig
import core.BuildStatusDatasource
import data.*

fun provideBuildStatusDatasources(config : BuildStatusConfig, postStatus : Boolean) : List<BuildStatusDatasource> {
    val datasources = mutableListOf<BuildStatusDatasource>(ConsoleDatasource())
    if (postStatus) {
        if (config.postBaseUrl.contains("bitbucket")) {
            val service = createBitBucketService(config.postBaseUrl, config.postAuthorization)
            datasources.add(BitBucketDatasource(service, config.hash, config.buildUrl))
        } else if (config.postBaseUrl.contains("github")) {
            val service = createGithubService(config.postBaseUrl, config.postAuthorization)
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