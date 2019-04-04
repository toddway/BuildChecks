package data

import core.datasource.StatusDatasource
import core.entity.BuildConfig

fun findRemoteStatusDatasource(config : BuildConfig): StatusDatasource? {
    return when {
        config.baseUrl.contains("bitbucket") -> bitbucketDatasource(config)
        config.baseUrl.contains("github") -> githubDatasource(config)
        else -> null
    }
}

fun bitbucketDatasource(config : BuildConfig) : BitBucketDatasource {
    val service = BitbucketService(config.baseUrl, config.authorization)
    return BitBucketDatasource(service, config.git.commitHash, config.buildUrl)
}

fun githubDatasource(config : BuildConfig) : GithubDatasource {
    val service = createGithubService(config.baseUrl, config.authorization)
    return GithubDatasource(service, config.git.commitHash, config.buildUrl)
}
