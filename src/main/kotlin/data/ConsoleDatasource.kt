package data

import core.BuildStats
import core.BuildStatsDatasource
import core.BuildStatusDatasource
import io.reactivex.Observable

class ConsoleDatasource : BuildStatusDatasource, BuildStatsDatasource {
    override fun postStats(stats: BuildStats): Observable<Boolean> {
        return Observable.just(true).doOnNext {  println("✔ $stats") }
    }

    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("· $message")
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("! $message")
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("✔ $message")
    }

    fun postStatus(message: String): Observable<Boolean> {
        return Observable.just(true).doOnNext { println("$message") }
    }
}