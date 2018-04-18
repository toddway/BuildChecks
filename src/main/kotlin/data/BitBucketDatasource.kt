package data

import core.BuildStatusDatasource
import io.reactivex.Observable

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
        val body = BuildStatusBody(status, key, message, url, "")
        return service.postBuildStatus(hash, body).map { true }.onErrorReturn { true }
    }
}