package buildchecks.model

enum class GateStatus { PASSED, FAILED, SKIPPED }

data class GateResult(
    val gate: String,
    val status: GateStatus,
    val detail: String,
)
