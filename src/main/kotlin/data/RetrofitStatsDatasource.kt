package data

import core.entity.BuildConfig
import core.entity.Stats
import core.usecase.PostStatsUseCase
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

class RetrofitStatsDatasource(val service : RetrofitBuildStatsService) : PostStatsUseCase.Datasource {
    override fun postStats(stats: Stats): Observable<Boolean> {
        return service.postStats(stats, stats.commitHash).map { true }.onErrorReturn { true }
    }
}

interface RetrofitBuildStatsService {
    @PUT("{slug}.json")
    fun postStats(@Body stats: Stats, @Path("slug") slug : String) : Observable<ResponseBody>
}


fun buildRetrofitStatsDatasource(baseUrl : String, authorization : String): RetrofitStatsDatasource {
    return RetrofitStatsDatasource(retrofit(baseUrl, authorization).create(RetrofitBuildStatsService::class.java))
}

fun BuildConfig.buildPostStatsDatasources() : List<PostStatsUseCase.Datasource> {
    val datasources = mutableListOf<PostStatsUseCase.Datasource>()
    if (statsBaseUrl.isNotBlank()) {
        datasources.add(buildRetrofitStatsDatasource(statsBaseUrl, ""))
    }
    return datasources
}

