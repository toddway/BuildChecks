package data

import core.BuildStatusDatasource
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

class BitBucketDatasource(
        val service: BitbucketService,
        val hash : String,
        val url : String
) : BuildStatusDatasource {

    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("INPROGRESS", message, key)
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("FAILED", message, key)
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("SUCCESSFUL", message, key)
    }

    fun postStatus(status: String, message: String, key: String) : Observable<Boolean> {
        val body = BitbucketBuildStatusBody(status, key, message, url, "")
        return service.postBuildStatus(hash, body).map { true }.onErrorReturn { true }
    }
}

interface BitbucketService {
    @POST("rest/build-status/1.0/commits/{hash}")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody) : Observable<ResponseBody>
}

fun createBitBucketService(baseUrl : String, authorization : String): BitbucketService {
    return retrofit(baseUrl, authorization).create(BitbucketService::class.java)
}

data class BitbucketBuildStatusBody(
        val state : String = "",
        val key : String = "",
        val name : String = "",
        var url : String? = null,
        var description : String? = null
)