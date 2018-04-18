package data

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface BitbucketService {
    @POST("rest/build-status/1.0/commits/{hash}")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : BuildStatusBody) : Observable<ResponseBody>
}

fun createBitBucketService(baseUrl : String, authorization : String): BitbucketService {
    return retrofit(baseUrl, authorization).create(BitbucketService::class.java)
}