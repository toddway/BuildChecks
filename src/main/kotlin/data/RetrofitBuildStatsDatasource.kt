package data

import core.BuildStats
import core.BuildStatsDatasource
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

class RetrofitBuildStatsDatasource(val service : RetrofitBuildStatsService) : BuildStatsDatasource {
    override fun postStats(stats: BuildStats): Observable<Boolean> {
        return service.postStats(stats, stats.commitHash).map { true }.onErrorReturn { true }
    }
}

interface RetrofitBuildStatsService {
    @PUT("{slug}.json")
    fun postStats(@Body stats: BuildStats, @Path("slug") slug : String) : Observable<ResponseBody>
}

fun createRetrifotBuildStatsService(baseUrl : String, authorization : String): RetrofitBuildStatsService {
    return retrofit(baseUrl, authorization).create(RetrofitBuildStatsService::class.java)
}