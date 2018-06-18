package core.usecase

import core.datasource.StatsDatasource
import core.entity.StatsEntity

class PostStatsUseCase(val datasources: List<StatsDatasource>) {
    fun invoke(stats: StatsEntity) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }
}
