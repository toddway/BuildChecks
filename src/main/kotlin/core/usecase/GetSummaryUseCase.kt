package core.usecase

import core.entity.*
import core.toXmlDocuments
import java.io.File

interface GetSummaryUseCase {
    fun value() : String?
    fun key() : String
    fun isSuccessful() : Boolean
    companion object
}

fun List<GetSummaryUseCase>.postStatuses(postStatusUseCase: PostStatusUseCase) {
    forEach { summary ->
        summary.value()?.let {
            val result = if (summary.isSuccessful()) BuildStatus.SUCCESS else BuildStatus.FAILURE
            postStatusUseCase.post(result, it, summary.key())
        }
    }
}

fun List<File>.toSummaries(config : BuildConfig) : List<GetSummaryUseCase> {
    val xml = toXmlDocuments()
    return listOf(
            GetDurationSummaryUseCase(config),
            xml.buildCoverage(config.minCoveragePercent),
            xml.buildLint(config.maxLintViolations)
    )
}

fun List<GetSummaryUseCase>.toStats(config: BuildConfig) : Stats {
    val lintUseCase = find { it is GetLintSummaryUseCase } as GetLintSummaryUseCase
    val coverageUseCase = find { it is GetCoverageSummaryUseCase } as GetCoverageSummaryUseCase
    return Stats(
            coverageUseCase.percent() ?: 0.0,
            lintUseCase.asTotal() ?: 0,
            config.duration(),
            coverageUseCase.linesPlusBranches() ?: 0,
            config.git.commitDate,
            config.git.commitHash,
            config.git.commitBranch
    )
}


fun List<GetSummaryUseCase>.toMessages() = filter { it.value() != null }.map { s ->
    s.value()?.let {
        if (s.isSuccessful()) InfoMessage(it)
        else ErrorMessage(it)
    }
}
