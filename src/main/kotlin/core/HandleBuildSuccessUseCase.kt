package core

class HandleBuildSuccessUseCase(
        val setBuildStatusUseCase: SetBuildStatusUseCase? = null,
        val postBuildStatsUseCase: PostBuildStatsUseCase,
        val buildStatusConfig: BuildStatusConfig,
        val getJacocoSummaryUseCase: GetJacocoSummaryUseCase,
        val getLintSummaryUseCase: GetLintSummaryUseCase,
        val getDetektSummaryUseCase: GetDetektSummaryUseCase,
        val getCheckstyleSummaryUseCase: GetCheckstyleSummaryUseCase
) {
    fun invoke() {
        getJacocoSummaryUseCase.percentAsString()?.let { setBuildStatusUseCase?.success(it, "j") }
        getLintSummaryUseCase.asString()?.let { setBuildStatusUseCase?.success(it, "l")}
        getDetektSummaryUseCase.asString()?.let { setBuildStatusUseCase?.success(it, "d") }
        getCheckstyleSummaryUseCase.asString()?.let { setBuildStatusUseCase?.success(it, "c") }

        val issueCount = (getLintSummaryUseCase.asTotal() ?: 0)
                + (getDetektSummaryUseCase.asTotal() ?: 0)
                + (getCheckstyleSummaryUseCase.asTotal() ?: 0)

        postBuildStatsUseCase.invoke(BuildStats(
                getJacocoSummaryUseCase.percent() ?: 0.0,
                issueCount,
                buildStatusConfig.duration(),
                getJacocoSummaryUseCase.lines() ?: 0,
                buildStatusConfig.commitDate,
                buildStatusConfig.hash,
                buildStatusConfig.commitBranch
        ))
    }
}