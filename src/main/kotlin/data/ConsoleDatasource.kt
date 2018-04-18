package data

import core.BuildStatusDatasource
import io.reactivex.Observable

class ConsoleDatasource(val hash : String) : BuildStatusDatasource {
    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return post("· $message", hash)
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return post("! $message", hash)
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return post("✔ $message", hash)
    }

    fun post(message: String, hash : String): Observable<Boolean> {
        return Observable.just(true).doOnNext { println("$message") }
    }
}