package data

import core.datasource.StatusDatasource
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

class GithubDatasource(
        val service: GithubService,
        val hash : String,
        val url : String
) : StatusDatasource {
    override fun name(): String {
        return "Github"
    }

    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("pending", message, key)
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("failure", message, key)
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("success", message, key)
    }

    fun postStatus(status: String, message: String, key: String) : Observable<Boolean> {
        val body = GithubBuildStatusBody(status, key, message, url)
        return service.postBuildStatus(hash, body).map { true }.onErrorReturn { false }
    }
}

interface GithubService {
    @Headers(
            "Accept: application/json",
            "User-Agent: gradle build"
    )
    @POST("statuses/{hash}")
    fun postBuildStatus(@Path("hash") hash : String, @Body body : GithubBuildStatusBody) : Observable<ResponseBody>
}

fun createGithubService(baseUrl : String, authorization : String): GithubService {
    return retrofit(baseUrl, authorization).create(GithubService::class.java)
}


data class GithubBuildStatusBody(
        val state : String = "",
        val context : String = "",
        var description : String = "",
        var target_url : String? = null
)