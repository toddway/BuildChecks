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

class BitBucket2Datasource(retrofit: Retrofit, val config : BuildConfig) : PostStatusUseCase.Datasource {
    override fun isRemote() = true
    override fun post(status: BuildStatus, message: String, key: String): Observable<Boolean> {
        val body = BitbucketBuildStatusBody(status.bitbucketFormat(), key, message, config.buildUrl, "")
        return service.postBuildStatus(config.git.commitHash, body).map { true }.onErrorReturn { false }
    }

    override fun isActive(): Boolean {
        return config.isPostActivated && config.baseUrl.contains("bitbucket") && config.baseUrl.contains("2.0/")
    }

    private val service by lazy { retrofit.create(Service::class.java) }

    override fun name() = "Bitbucket"

    interface Service {
        @POST("commit/{hash}/statuses/build")
        fun postBuildStatus(@Path("hash") hash : String, @Body body : BitbucketBuildStatusBody)
                : Observable<ResponseBody>
    }
}

