package data

import core.post.StatsDatasource
import core.post.StatusDatasource
import core.entity.ErrorMessage
import core.entity.InfoMessage
import core.entity.Stats
import io.reactivex.Observable

class ConsoleDatasource : StatusDatasource, StatsDatasource {
    override fun name(): String {
        return "Console"
    }

    override fun postStats(stats: Stats): Observable<Boolean> {
        return Observable.just(true).doOnNext {  println(InfoMessage(stats.toString()).toString()) }
    }

    override fun postPendingStatus(message: String, key: String): Observable<Boolean> {
        return postStatus(InfoMessage(message).toString())
    }

    override fun postFailureStatus(message: String, key: String): Observable<Boolean> {
        return postStatus(ErrorMessage(message).toString())
    }

    override fun postSuccessStatus(message: String, key: String): Observable<Boolean> {
        return postStatus(InfoMessage(message).toString())
    }

    fun postStatus(message: String): Observable<Boolean> {
        return Observable.just(true).doOnNext { println("$message") }
    }
}
