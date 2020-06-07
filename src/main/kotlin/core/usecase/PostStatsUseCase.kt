package core.usecase

import core.entity.Stats
import io.reactivex.Observable

open class PostStatsUseCase(val datasources: List<Datasource>) {
    fun post(stats: Stats) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }

    interface Datasource {
        fun postStats(stats: Stats): Observable<Boolean>
        companion object
    }
}
