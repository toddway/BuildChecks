package core

class PostBuildStatsUseCase(val datasources: List<BuildStatsDatasource>) {
    fun invoke(stats: BuildStats) {
        datasources.forEach {
            it.postStats(stats).subscribe()
        }
    }
}