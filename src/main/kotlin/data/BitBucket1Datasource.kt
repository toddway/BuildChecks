package data

import core.entity.BuildConfig
import core.entity.BuildStatus
import core.usecase.PostStatusUseCase
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

class BitBucket1Datasource(retrofit: Retrofit, val config : BuildConfig) : PostStatusUseCase.Datasource {
    override fun isRemote() = true

    override fun post(status: BuildStatus, message: String, key: String): Observable<Boolean> {
        val body = BitbucketBuildStatusBody(status.bitbucketFormat(), key, message, config.buildUrl, "")
        return service.postBuildStatus(config.git.commitHash, body).map { true }.onErrorReturn { false }
    }

    override fun isActive(): Boolean {
        return config.isPostActivated && config.baseUrl.contains("bitbucket") && !config.baseUrl.contains("2.0/")
    }

    private val service by lazy { retrofit.create(Service::class.java) }

    override fun name() = "Bitbucket"

    interface Service {
        @POST("rest/build-status/1.0/commits/{hash}")
        fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
                : Observable<ResponseBody>
    }
}

data class BitbucketBuildStatusBody(
        val state : String = "",
        val key : String = "",
        val name : String = "",
        var url : String? = null,
        var description : String? = null
)

fun BuildStatus.bitbucketFormat() = when(this) {
    BuildStatus.FAILURE -> "FAILED"
    BuildStatus.PENDING -> "INPROGRESS"
    BuildStatus.SUCCESS -> "SUCCESSFUL"
}
