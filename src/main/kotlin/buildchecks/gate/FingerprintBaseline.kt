package buildchecks.gate

import java.io.File

data class Baseline(
    val fingerprints: Set<String>,
    val findingCount: Int,
    val coveragePercent: Double?,
    val manifest: Set<OriginKind>? = null, // null = pre-v2 baseline; the origin-presence gate skips
)

/**
 * The committed baseline file (V4-PLAN.md §5): sorted, one finding per line, diffable.
 * Header totals feed the ratchets; only the leading fingerprint token is parsed back.
 * From format v2 the header also records the origin presence manifest (§5.5) as `# origin`
 * lines — comment-prefixed, so fingerprint parsing ignores them and pre-v2 readers still work.
 */
class FingerprintBaseline(private val file: File) {

    fun read(): Baseline? {
        if (!file.isFile) return null
        var version = 1
        var findingCount: Int? = null
        var coveragePercent: Double? = null
        val fingerprints = mutableSetOf<String>()
        val manifest = mutableSetOf<OriginKind>()
        file.readLines().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith(VERSION_PREFIX) ->
                    version = line.removePrefix(VERSION_PREFIX).trim().toIntOrNull() ?: 1
                line.startsWith("# findings:") -> findingCount = line.substringAfter(':').trim().toIntOrNull()
                line.startsWith("# coverage:") -> coveragePercent = line.substringAfter(':').trim().toDoubleOrNull()
                line.startsWith(ORIGIN_PREFIX) -> originKind(line)?.let { manifest += it }
                line.startsWith("#") || line.isEmpty() -> Unit
                else -> fingerprints += line.substringBefore(' ')
            }
        }
        // Manifest is authoritative only from v2; a v1 baseline predates it, so the gate skips.
        return Baseline(fingerprints, findingCount ?: fingerprints.size, coveragePercent, manifest.takeIf { version >= 2 })
    }

    fun write(findings: List<FingerprintedFinding>, coveragePercent: Double?, manifest: Set<OriginKind> = emptySet()) {
        val header = buildString {
            appendLine("# buildchecks baseline v2")
            appendLine("# findings: ${findings.size}")
            if (coveragePercent != null) appendLine("# coverage: ${"%.2f".format(coveragePercent)}")
            manifest.sorted().forEach { appendLine("$ORIGIN_PREFIX${it.origin}  ${it.kind}") }
        }
        val lines = findings.map { entry(it) }.sorted()
        file.parentFile?.mkdirs()
        file.writeText(header + lines.joinToString("\n") + if (lines.isEmpty()) "" else "\n")
    }

    private fun originKind(line: String): OriginKind? {
        val tokens = line.removePrefix(ORIGIN_PREFIX).trim().split(Regex("\\s+"))
        return if (tokens.size >= 2) OriginKind(tokens[0], tokens[1]) else null
    }

    private fun entry(fingerprinted: FingerprintedFinding): String {
        val finding = fingerprinted.finding
        val place = finding.location?.let { "${it.path}:${it.line ?: 0}" } ?: "-"
        val words = finding.message.split(Regex("\\s+")).filter { it.isNotEmpty() }.take(8).joinToString(" ")
        return "${fingerprinted.fingerprint}  ${finding.tool}  ${finding.ruleId}  $place  $words"
    }

    private companion object {
        const val VERSION_PREFIX = "# buildchecks baseline v"
        const val ORIGIN_PREFIX = "# origin  "
    }
}
