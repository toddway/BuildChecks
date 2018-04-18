package core

import io.reactivex.Observable

interface BuildStatusDatasource {
    fun postPendingStatus(message: String, key: String) : Observable<Boolean>
    fun postFailureStatus(message: String, key: String) : Observable<Boolean>
    fun postSuccessStatus(message: String, key: String) : Observable<Boolean>

}