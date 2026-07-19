package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.Severity
import buildchecks.model.TestStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The small, stable scripting contract (V4-PLAN.md §7); breaking changes bump schemaVersion. */
class SummaryJson : Renderer {

    override val fileName = "summary.json"

    @Serializable
    private data class Schema(
        val schemaVersion: Int,
        val passed: Boolean,
        val gates: List<GateSchema>,
        val findings: FindingCounts,
        val tests: TestCounts?,
        val coveragePercent: Double?,
    )

    @Serializable
    private data class GateSchema(val gate: String, val status: String, val detail: String)

    @Serializable
    private data class FindingCounts(val total: Int, val new: Int, val errors: Int, val warnings: Int, val info: Int)

    @Serializable
    private data class TestCounts(val total: Int, val failed: Int, val skipped: Int)

    override fun render(summary: CheckSummary): String = json.encodeToString(Schema(
        schemaVersion = 1,
        passed = summary.passed,
        gates = summary.gates.map { GateSchema(it.gate, it.status.name, it.detail) },
        findings = FindingCounts(
            total = summary.findings.size,
            new = summary.findings.count { it.isNew },
            errors = summary.findings.count { it.finding.severity == Severity.ERROR },
            warnings = summary.findings.count { it.finding.severity == Severity.WARNING },
            info = summary.findings.count { it.finding.severity == Severity.INFO },
        ),
        tests = if (summary.tests.isEmpty()) null else TestCounts(
            total = summary.tests.size,
            failed = summary.tests.count { it.status == TestStatus.FAILED || it.status == TestStatus.ERROR },
            skipped = summary.tests.count { it.status == TestStatus.SKIPPED },
        ),
        coveragePercent = summary.coverage?.linePercent,
    ))

    private companion object {
        val json = Json { prettyPrint = true }
    }
}
