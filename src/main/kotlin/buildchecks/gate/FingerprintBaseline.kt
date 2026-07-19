package buildchecks.gate

import java.io.File

data class Baseline(
    val fingerprints: Set<String>,
    val findingCount: Int,
    val coveragePercent: Double?,
)

/**
 * The committed baseline file (V4-PLAN.md §5): sorted, one finding per line, diffable.
 * Header totals feed the ratchets; only the leading fingerprint token is parsed back.
 */
class FingerprintBaseline(private val file: File) {

    fun read(): Baseline? {
        if (!file.isFile) return null
        var findingCount: Int? = null
        var coveragePercent: Double? = null
        val fingerprints = mutableSetOf<String>()
        file.readLines().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("# findings:") -> findingCount = line.substringAfter(':').trim().toIntOrNull()
                line.startsWith("# coverage:") -> coveragePercent = line.substringAfter(':').trim().toDoubleOrNull()
                line.startsWith("#") || line.isEmpty() -> Unit
                else -> fingerprints += line.substringBefore(' ')
            }
        }
        return Baseline(fingerprints, findingCount ?: fingerprints.size, coveragePercent)
    }

    fun write(findings: List<FingerprintedFinding>, coveragePercent: Double?) {
        val header = buildString {
            appendLine("# buildchecks baseline v1")
            appendLine("# findings: ${findings.size}")
            if (coveragePercent != null) appendLine("# coverage: ${"%.2f".format(coveragePercent)}")
        }
        val lines = findings.map { entry(it) }.sorted()
        file.parentFile?.mkdirs()
        file.writeText(header + lines.joinToString("\n") + if (lines.isEmpty()) "" else "\n")
    }

    private fun entry(fingerprinted: FingerprintedFinding): String {
        val finding = fingerprinted.finding
        val place = finding.location?.let { "${it.path}:${it.line ?: 0}" } ?: "-"
        val words = finding.message.split(Regex("\\s+")).filter { it.isNotEmpty() }.take(8).joinToString(" ")
        return "${fingerprinted.fingerprint}  ${finding.tool}  ${finding.ruleId}  $place  $words"
    }
}
