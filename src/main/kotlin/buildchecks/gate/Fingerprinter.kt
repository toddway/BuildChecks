package buildchecks.gate

import buildchecks.model.Finding
import java.io.File
import java.security.MessageDigest

data class FingerprintedFinding(val finding: Finding, val fingerprint: String)

/**
 * Content-based finding identity (V4-PLAN.md §5): survives line shifts and file renames,
 * changes when the violating code itself is rewritten.
 *
 * fingerprint = hash(tool + rule + normalizedViolatingSource + contextHash). The file path is
 * deliberately not hashed (rename tolerance); identical snippets are disambiguated by an
 * occurrence index. Clone findings (CPD) hash their duplicated fragment plus token count
 * instead, so a new clone of old code still fingerprints as new.
 */
class Fingerprinter(
    root: File? = null,
    private val sourceLines: (String) -> List<String>?,
) {

    // Some tools embed the absolute checkout path in a finding's message (e.g. detekt's EmptyKtFile:
    // "The empty Kotlin file /bitrise/src/…/Foo.kt can be removed."). When such a message is what
    // gets hashed (empty/sourceless findings), the fingerprint would otherwise differ between a
    // local checkout and CI's /bitrise/src, so a locally-captured baseline can't gate CI. Strip the
    // root prefix first so the fingerprint is identical wherever the repo is checked out.
    private val rootPrefixes: List<String> = root
        ?.let { listOfNotNull(it.absolutePath, runCatching { it.canonicalPath }.getOrNull()) }
        ?.distinct()
        ?.map { it.removeSuffix("/") + "/" }
        ?: emptyList()

    fun fingerprint(findings: List<Finding>): List<FingerprintedFinding> {
        val base = findings.map { it to baseFingerprint(it) }
        val occurrence = mutableMapOf<String, Int>()
        return base
            .sortedWith(compareBy(
                { it.second },
                { it.first.location?.path ?: "" },
                { it.first.location?.line ?: 0 },
                { it.first.location?.column ?: 0 },
            ))
            .map { (finding, hash) ->
                val index = occurrence.merge(hash, 1, Int::plus)!! - 1
                FingerprintedFinding(finding, if (index == 0) hash else "$hash-$index")
            }
    }

    private fun baseFingerprint(finding: Finding): String {
        val content = if (finding.snippet != null) {
            normalize(finding.snippet!!) + ":" + (finding.duplicatedTokens ?: 0)
        } else {
            val line = finding.location?.line
            val lines = finding.location?.path?.let(sourceLines)
            if (line != null && lines != null && line in 1..lines.size) {
                normalize(lines[line - 1]) + ":" + contextHash(lines, line)
            } else {
                // Source unavailable (deleted file, no line info): the message is the
                // most stable content left to hash.
                normalize(finding.message)
            }
        }
        return sha256("${finding.tool}:${finding.ruleId}:$content").take(16)
    }

    // Two lines each side, so a violation keeps its identity when the whole block moves but
    // not when it is pasted into different surroundings.
    private fun contextHash(lines: List<String>, line: Int): String {
        val text = ((line - 3)..(line + 1))
            .filter { it in lines.indices && it != line - 1 }
            .joinToString("|") { normalize(lines[it]) }
        return sha256(text).take(8)
    }

    // Collapse (not strip) whitespace: keeps token boundaries so "val x" != "valx". Also strip any
    // checkout-root prefix so embedded absolute paths don't make the fingerprint machine-specific.
    private fun normalize(text: String): String {
        var t = text
        for (prefix in rootPrefixes) t = t.replace(prefix, "")
        return t.trim().replace(whitespace, " ")
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val whitespace = Regex("\\s+")
    }
}
