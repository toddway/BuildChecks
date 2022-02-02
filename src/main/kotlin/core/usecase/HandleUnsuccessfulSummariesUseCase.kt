package core.usecase

import core.entity.BuildConfig
import org.gradle.api.GradleException

class HandleUnsuccessfulSummariesUseCase(
    val summaries: List<GetSummaryUseCase>,
    val config: BuildConfig
) {
    fun invoke() {
        summaries.forEach { summary ->
            config.log?.info("${summary.status()} for ${summary.key()} check, ${summary.value()}")
            if (!summary.isSuccessful())
                throw GradleException(summary.value() ?: "")
        }
    }
}