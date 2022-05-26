package core.usecase

import core.entity.*
import core.toXmlDocuments
import org.gradle.api.GradleException
import java.io.File

interface GetSummaryUseCase {
    fun value() : String?
    fun key() : String
    fun isSuccessful() : Boolean
    fun status() = if (isSuccessful()) BuildStatus.SUCCESS else BuildStatus.FAILURE
    fun valueOrKey() = value() ?: key()
}

fun List<GetSummaryUseCase>.postStatuses(postStatusUseCase: PostStatusUseCase) {
    filter { it.value() != null }.forEach { postStatusUseCase.post(it.status(), it.valueOrKey(), it.key()) }
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
    val lintUseCase = filterIsInstance<GetLintSummaryUseCase>().firstOrNull()
    val coverageUseCase = filterIsInstance<GetCoverageSummaryUseCase>().firstOrNull()
    return Stats(
            coverageUseCase?.percent() ?: 0.0,
            lintUseCase?.asTotal() ?: 0,
            config.duration(),
            coverageUseCase?.linesPlusBranches() ?: 0,
            config.git.commitDate,
            config.git.commitHash,
            config.git.commitBranch
    )
}

fun List<GetSummaryUseCase>.postStats(config: BuildConfig, postStatsUseCase: PostStatsUseCase) {
    toStats(config).also { postStatsUseCase.post(it) }
}

fun List<GetSummaryUseCase>.toMessages(): List<Message?> = filter { it.value() != null }.map { s ->
    s.value()?.let {
        if (s.isSuccessful()) InfoMessage(it)
        else ErrorMessage(it)
    }
}

fun List<GetSummaryUseCase>.throwIfUnsuccessful(config: BuildConfig) = forEach { summary ->
    config.log?.info("${summary.status()} for ${summary.key()} check, ${summary.value()}")
    if (config.isSuccess && !summary.isSuccessful())
        throw GradleException(summary.value() ?: "")
}
