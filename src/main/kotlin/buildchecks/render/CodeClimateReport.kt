package buildchecks.render

import buildchecks.model.CheckSummary
import buildchecks.model.Severity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** GitLab code-quality artifact: declare it as a `codequality` artifact for the MR widget. */
class CodeClimateReport : Renderer {

    override val fileName = "codeclimate.json"

    @Serializable
    private data class Issue(
        val description: String,
        val check_name: String,
        val fingerprint: String,
        val severity: String,
        val location: IssueLocation,
    )

    @Serializable
    private data class IssueLocation(val path: String, val lines: Lines)

    @Serializable
    private data class Lines(val begin: Int)

    override fun render(summary: CheckSummary): String = json.encodeToString(summary.findings.map {
        Issue(
            description = it.finding.message,
            check_name = it.finding.ruleId,
            fingerprint = it.fingerprint,
            severity = when (it.finding.severity) {
                Severity.ERROR -> "major"
                Severity.WARNING -> "minor"
                Severity.INFO -> "info"
            },
            location = IssueLocation(
                path = it.finding.location?.path ?: "",
                lines = Lines(it.finding.location?.line ?: 1),
            ),
        )
    })

    private companion object {
        val json = Json { prettyPrint = true }
    }
}
