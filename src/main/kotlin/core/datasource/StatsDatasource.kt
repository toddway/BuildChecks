package core.datasource

import core.entity.StatsEntity
import io.reactivex.Observable

interface StatsDatasource {
    fun postStats(stats: StatsEntity): Observable<Boolean>
}
