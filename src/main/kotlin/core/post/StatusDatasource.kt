package core.post

import io.reactivex.Observable

interface StatusDatasource {
    fun postPendingStatus(message: String, key: String) : Observable<Boolean>
    fun postFailureStatus(message: String, key: String) : Observable<Boolean>
    fun postSuccessStatus(message: String, key: String) : Observable<Boolean>
    fun name() : String
}
