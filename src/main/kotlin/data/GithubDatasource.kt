package data

import core.entity.BuildConfig
import core.entity.BuildStatus
import core.usecase.PostStatusUseCase
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.*

class GithubDatasource(val config : BuildConfig) : PostStatusUseCase.Datasource {
    private val retrofit by lazy { retrofit(config.baseUrl, config.authorization) }

    override fun isRemote() = true

    override fun post(status: BuildStatus, message: String, key: String): Observable<Boolean> {
        val body = GithubBuildStatusBody(status.githubFormat(), key, message, config.buildUrl)
        return service.postBuildStatus(config.git.commitHash, body).map { true }.onErrorReturn { false }
    }

    private val service by lazy { retrofit.create(Service::class.java) }

    override fun isActive(): Boolean {
        return config.isPostActivated && config.baseUrl.contains("github")
    }

    override fun name() = "Github"

    interface Service {
        @Headers("Accept: application/json", "User-Agent: gradle build")
        @POST("statuses/{hash}")
        fun postBuildStatus(@Path("hash") hash : String, @Body body : GithubBuildStatusBody) : Observable<ResponseBody>

        @Headers("Accept: application/json", "User-Agent: gradle build")
        @GET("statuses/{hash}")
        fun getBuildStatus(@Path("hash") hash : String) : Observable<List<GithubBuildStatusBody>>
    }

    @SuppressWarnings("ConstructorParameterNaming")
    data class GithubBuildStatusBody(
            val state : String = "",
            val context : String = "",
            var description : String = "",
            var target_url : String? = null,
            var created_at : String? = null
    )

    data class GetBody (
        var description : String? = null
    )
}

fun BuildStatus.githubFormat() = when(this) {
    BuildStatus.FAILURE -> "failure"
    BuildStatus.PENDING -> "pending"
    BuildStatus.SUCCESS -> "success"
}
