package data

import core.entity.BuildConfig
import core.usecase.PostStatusUseCase
import retrofit2.Retrofit

fun BuildConfig.buildPostStatusDatasources(retrofit: Retrofit) : List<PostStatusUseCase.Datasource> {
    return listOf(
            ConsoleDatasource(),
            GithubDatasource(retrofit, this),
            BitBucket1Datasource(retrofit, this),
            BitBucket2Datasource(retrofit, this)
    )
}
