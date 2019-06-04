package data

import core.entity.BuildConfig
import core.usecase.PostStatusUseCase

fun BuildConfig.buildPostStatusDatasources() : List<PostStatusUseCase.Datasource> {
    val retrofit by lazy { retrofit(baseUrl, authorization) }
    return listOf(
            ConsoleDatasource(),
            GithubDatasource(retrofit, this),
            BitBucket1Datasource(retrofit, this),
            BitBucket2Datasource(retrofit, this)
    )
}

fun PostStatusUseCase.Datasource.Companion.buildList(config: BuildConfig) : List<PostStatusUseCase.Datasource> {
    val retrofit by lazy { retrofit(config.baseUrl, config.authorization) }
    return listOf(
            ConsoleDatasource(),
            GithubDatasource(retrofit, config),
            BitBucket1Datasource(retrofit, config),
            BitBucket2Datasource(retrofit, config)
    )
}