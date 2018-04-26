package core

import io.reactivex.Observable

interface BuildStatsDatasource {
    fun postStats(stats: BuildStats): Observable<Boolean>
}