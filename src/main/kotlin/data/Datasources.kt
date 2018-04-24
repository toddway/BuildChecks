package data

import core.BuildStatusConfig
import core.BuildStatusDatasource

fun provideBuildStatusDatasources(config : BuildStatusConfig, postStatus : Boolean) : List<BuildStatusDatasource> {
    val datasources = mutableListOf<BuildStatusDatasource>(ConsoleDatasource(config.hash))
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