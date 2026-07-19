package buildchecks.model

data class ParsedReport(
    val findings: List<Finding> = emptyList(),
    val tests: List<TestResult> = emptyList(),
    val coverage: CoverageData? = null,
)

fun List<ParsedReport>.merged(): ParsedReport = ParsedReport(
    findings = flatMap { it.findings },
    tests = flatMap { it.tests },
    coverage = mapNotNull { it.coverage }.flatMap { it.files }
        .ifEmpty { null }?.let { CoverageData(it) },
)
