package data

import core.entity.BuildConfig
import core.usecase.PostStatusUseCase

fun PostStatusUseCase.Datasource.Companion.buildList(config: BuildConfig) : List<PostStatusUseCase.Datasource> {
    return listOf(
        ConsoleDatasource(),
        GithubDatasource(config),
        BitBucket1Datasource(config),
        BitBucket2Datasource(config)
    )
}