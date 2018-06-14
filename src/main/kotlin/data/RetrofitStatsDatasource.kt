package data

import core.datasource.StatsDatasource
import core.entity.StatsEntity
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

class RetrofitStatsDatasource(val service : RetrofitBuildStatsService) : StatsDatasource {
    override fun postStats(stats: StatsEntity): Observable<Boolean> {
        return service.postStats(stats, stats.commitHash).map { true }.onErrorReturn { true }
    }
}

interface RetrofitBuildStatsService {
    @PUT("{slug}.json")
    fun postStats(@Body stats: StatsEntity, @Path("slug") slug : String) : Observable<ResponseBody>
}

fun createRetrifotBuildStatsService(baseUrl : String, authorization : String): RetrofitBuildStatsService {
    return retrofit(baseUrl, authorization).create(RetrofitBuildStatsService::class.java)
}
