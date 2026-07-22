package buildchecks.model

enum class TestStatus { PASSED, FAILED, ERROR, SKIPPED }

data class TestResult(
    val suite: String,
    val name: String,
    val status: TestStatus,
    val message: String? = null,
    val durationSeconds: Double? = null,
    // Repo-relative path of the ingested report this result came from, so the renderer can flag
    // results whose source report is a stale age-outlier (see Freshness.outlier). null if unknown.
    val reportPath: String? = null,
)
