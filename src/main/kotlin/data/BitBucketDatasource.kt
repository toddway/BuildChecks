package data

import core.datasource.StatusDatasource
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

class BitBucketDatasource(
        val service: BitbucketService,
        val hash : String,
        val url : String
) : StatusDatasource {
    override fun name(): String {
        return "Bitbucket"
    }

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
        return service.postBuildStatus(hash, body).map { true }.onErrorReturn { false }
    }
}

interface BitbucketService {
    @POST("rest/build-status/1.0/commits/{hash}")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
            : Observable<ResponseBody>
}

interface BitbucketService2 : BitbucketService {
    @POST("commit/{hash}/statuses/build")
    override fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
            : Observable<ResponseBody>
}

fun createBitBucketService(baseUrl : String, authorization : String): BitbucketService {
    val clazz =
            if (baseUrl.contains("2.0/")) BitbucketService2::class.java
            else BitbucketService::class.java
    return retrofit(baseUrl, authorization).create(clazz)
}

data class BitbucketBuildStatusBody(
        val state : String = "",
        val key : String = "",
        val name : String = "",
        var url : String? = null,
        var description : String? = null
)
