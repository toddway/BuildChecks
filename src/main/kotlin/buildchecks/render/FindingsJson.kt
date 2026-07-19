package buildchecks.render

import buildchecks.model.CheckSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The full finding/test/coverage model for power users. */
class FindingsJson : Renderer {

    override val fileName = "findings.json"

    @Serializable
    private data class Schema(
        val schemaVersion: Int,
        val findings: List<FindingSchema>,
        val tests: List<TestSchema>,
        val coverage: CoverageSchema?,
    )

    @Serializable
    private data class FindingSchema(
        val tool: String,
        val ruleId: String,
        val severity: String,
        val message: String,
        val path: String?,
        val line: Int?,
        val column: Int?,
        val fingerprint: String,
        val new: Boolean,
    )

    @Serializable
    private data class TestSchema(
        val suite: String,
        val name: String,
        val status: String,
        val message: String?,
        val durationSeconds: Double?,
    )

    @Serializable
    private data class CoverageSchema(
        val linePercent: Double?,
        val files: List<FileSchema>,
    )

    @Serializable
    private data class FileSchema(
        val path: String,
        val linesCovered: Int,
        val linesTotal: Int,
        val lines: List<List<Int>>, // [line, hits, coveredBranches, totalBranches]
    )

    override fun render(summary: CheckSummary): String = json.encodeToString(Schema(
        schemaVersion = 1,
        findings = summary.findings.map {
            FindingSchema(
                tool = it.finding.tool,
                ruleId = it.finding.ruleId,
                severity = it.finding.severity.name,
                message = it.finding.message,
                path = it.finding.location?.path,
                line = it.finding.location?.line,
                column = it.finding.location?.column,
                fingerprint = it.fingerprint,
                new = it.isNew,
            )
        },
        tests = summary.tests.map { TestSchema(it.suite, it.name, it.status.name, it.message, it.durationSeconds) },
        coverage = summary.coverage?.let { coverage ->
            CoverageSchema(
                linePercent = coverage.linePercent,
                files = coverage.files.map { file ->
                    FileSchema(
                        path = file.path,
                        linesCovered = file.linesCovered,
                        linesTotal = file.lines.size,
                        lines = file.lines.map { listOf(it.line, it.hits, it.coveredBranches, it.totalBranches) },
                    )
                },
            )
        },
    ))

    private companion object {
        val json = Json { prettyPrint = true }
    }
}
