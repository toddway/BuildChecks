package core.post

import core.entity.Stats

class PostStatsUseCase(val datasources: List<StatsDatasource>) {
    fun invoke(stats: Stats) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }
}