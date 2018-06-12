package core.usecase

import core.entity.StatsEntity
import core.datasource.StatsDatasource

class PostStatsUseCase(val datasources: List<StatsDatasource>) {
    fun invoke(stats: StatsEntity) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }
}