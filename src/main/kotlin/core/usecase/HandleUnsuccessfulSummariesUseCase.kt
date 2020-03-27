package core.usecase

import org.gradle.api.GradleException

class HandleUnsuccessfulSummariesUseCase(val summaries: List<GetSummaryUseCase>) {
    fun invoke() {
        summaries.forEach { summary ->
            if (!summary.isSuccessful())
                throw GradleException(summary.value() ?: "")
        }
    }
}