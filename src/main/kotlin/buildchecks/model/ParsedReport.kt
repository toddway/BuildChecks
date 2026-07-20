package buildchecks.model

data class ParsedReport(
    val findings: List<Finding> = emptyList(),
    val tests: List<TestResult> = emptyList(),
    val coverage: CoverageData? = null,
    // Producing tool of a findings report when it refines the format (SARIF's driver name);
    // null for coverage/test reports and formats that are their own tool. Feeds the origin
    // manifest kind (V4-PLAN.md §5.5) independent of how many findings a run happens to carry.
    val tool: String? = null,
)

fun List<ParsedReport>.merged(): ParsedReport = ParsedReport(
    findings = flatMap { it.findings },
    tests = flatMap { it.tests },
    coverage = mapNotNull { it.coverage }.flatMap { it.files }
        .ifEmpty { null }?.let { CoverageData(it) },
)
