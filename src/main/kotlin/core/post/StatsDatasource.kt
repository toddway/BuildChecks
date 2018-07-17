package core.post

import core.entity.Stats
import io.reactivex.Observable

interface StatsDatasource {
    fun postStats(stats: Stats): Observable<Boolean>
}