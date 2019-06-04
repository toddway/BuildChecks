package core.usecase

import core.entity.Stats
import io.reactivex.Observable

class PostStatsUseCase(val datasources: List<PostStatsUseCase.Datasource>) {
    fun invoke(stats: Stats) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }

    interface Datasource {
        fun postStats(stats: Stats): Observable<Boolean>
        companion object
    }
}
