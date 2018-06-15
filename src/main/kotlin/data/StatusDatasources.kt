package data

import core.datasource.StatusDatasource
import core.entity.ConfigEntity

fun findRemoteStatusDatasource(config : ConfigEntity): StatusDatasource? {
    return when {
        config.baseUrl.contains("bitbucket") -> bitbucketDatasource(config)
        config.baseUrl.contains("github") -> githubDatasource(config)
        else -> null
    }
}

fun bitbucketDatasource(config : ConfigEntity) : BitBucketDatasource {
    val service = createBitBucketService(config.baseUrl, config.authorization)
    return BitBucketDatasource(service, config.hash(), config.buildUrl)
}

fun githubDatasource(config : ConfigEntity) : GithubDatasource {
    val service = createGithubService(config.baseUrl, config.authorization)
    return GithubDatasource(service, config.hash(), config.buildUrl)
}
