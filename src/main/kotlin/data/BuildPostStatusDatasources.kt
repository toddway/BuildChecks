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
