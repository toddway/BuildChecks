package buildchecks.model

enum class TestStatus { PASSED, FAILED, ERROR, SKIPPED }

data class TestResult(
    val suite: String,
    val name: String,
    val status: TestStatus,
    val message: String? = null,
    val durationSeconds: Double? = null,
)
