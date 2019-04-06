package data


import core.entity.BuildStatus
import core.entity.ErrorMessage
import core.entity.InfoMessage
import core.entity.Stats
import core.usecase.PostStatsUseCase
import core.usecase.PostStatusUseCase
import io.reactivex.Observable

class ConsoleDatasource : PostStatusUseCase.Datasource, PostStatsUseCase.Datasource {
    override fun isRemote() = false

    override fun post(status: BuildStatus, message: String, key: String): Observable<Boolean> {
        return Observable.just(true).doOnNext { println(status.consoleFormat(message)) }
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun name() = "Console"

    override fun postStats(stats: Stats): Observable<Boolean> {
        return Observable.just(true).doOnNext {  println(InfoMessage(stats.toString()).toString()) }
    }
}

fun BuildStatus.consoleFormat(message: String) = when(this) {
    BuildStatus.FAILURE -> ErrorMessage(message).toString()
    else -> InfoMessage(message).toString()
}
