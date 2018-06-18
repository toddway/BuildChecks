package data

import core.datasource.StatsDatasource
import core.datasource.StatusDatasource
import core.entity.StatsEntity
import io.reactivex.Observable

class ConsoleDatasource : StatusDatasource, StatsDatasource {
    override fun name(): String {
        return "Console"
    }

    override fun postStats(stats: StatsEntity): Observable<Boolean> {
        return Observable.just(true).doOnNext {  println("✔ $stats") }
    }

    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("✔ $message")
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("❌ $message")
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return postStatus("✔ $message")
    }

    fun postStatus(message: String): Observable<Boolean> {
        return Observable.just(true).doOnNext { println("$message") }
    }
}
