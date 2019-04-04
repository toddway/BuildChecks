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

interface BitbucketService1 {
    @POST("rest/build-status/1.0/commits/{hash}")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
            : Observable<ResponseBody>
}

interface BitbucketService2 {
    @POST("commit/{hash}/statuses/build")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
            : Observable<ResponseBody>
}

class BitbucketService(baseUrl : String, authorization : String) : BitbucketService1, BitbucketService2 {
    private val retrofit = retrofit(baseUrl, authorization)
    private val service1 = retrofit.create(BitbucketService1::class.java)
    private val service2 = retrofit.create(BitbucketService2::class.java)
    private fun isVersion2() = retrofit.baseUrl().toString().contains("2.0/")

    override fun postBuildStatus(hash: String, body: BitbucketBuildStatusBody): Observable<ResponseBody> {
        return if (isVersion2()) service2.postBuildStatus(hash, body) else service1.postBuildStatus(hash, body)
    }
}

data class BitbucketBuildStatusBody(
        val state : String = "",
        val key : String = "",
        val name : String = "",
        var url : String? = null,
        var description : String? = null
)
