package buildchecks.model

data class ParsedReport(
    val findings: List<Finding> = emptyList(),
    val tests: List<TestResult> = emptyList(),
    val coverage: CoverageData? = null,
)
