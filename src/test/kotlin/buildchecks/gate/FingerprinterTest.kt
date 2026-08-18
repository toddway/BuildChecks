package buildchecks.gate

import buildchecks.model.Finding
import buildchecks.model.Location
import buildchecks.model.Severity
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class FingerprinterTest {

    // The violation sits mid-block so its two-line context window travels with the block.
    private val block = listOf(
        "fun total(items: List<Int>): Int {",
        "    // sums with an offset",
        "    val magic = 42",
        "    return items.sum() + magic",
        "}",
    )

    private fun finding(
        path: String = "src/Total.kt",
        line: Int? = 3,
        rule: String = "MagicNumber",
        message: String = "This expression contains a magic number.",
    ) = Finding("detekt", rule, Severity.WARNING, message, Location(path, line))

    private fun fingerprint(finding: Finding, source: Map<String, List<String>>): String =
        Fingerprinter { source[it] }.fingerprint(listOf(finding)).single().fingerprint

    // -- occurrence index --

    // A finding whose source can't be read falls back to hashing the message (see baseFingerprint).
    // Formatting rules phrase every violation identically, so these collide by design and are told
    // apart only by the occurrence index.
    private fun sourceless(path: String, line: Int) =
        Finding("detekt", "NoConsecutiveBlankLines", Severity.WARNING, "Needless blank line(s)", Location(path, line))

    @Test
    fun `colliding findings always get distinct fingerprints`() {
        // The invariant the occurrence index exists to hold. Scoping it per file would give each of
        // these the same bare hash, collapsing them into one baseline entry so a genuinely new
        // identical finding would match an existing fingerprint and never be reported.
        val findings = listOf(
            sourceless("src/A.kt", 10),
            sourceless("src/A.kt", 20),
            sourceless("src/B.kt", 30),
            sourceless("src/C.kt", 40),
        )
        val prints = Fingerprinter { null }.fingerprint(findings).map { it.fingerprint }
        assertEquals(findings.size, prints.toSet().size, "every colliding finding needs its own fingerprint")
    }

    @Test
    fun `resolving the source keeps distinct code out of one collision group`() {
        // The real defence against index churn: when the violating source is readable, findings hash
        // by content, so unrelated files land in different groups and fixing one cannot re-index the
        // other. Same rule and message, different code.
        val source = mapOf(
            "a/src/Only.kt" to listOf("fun a() {", "    val x = 1", "}"),
            "b/src/Only.kt" to listOf("fun b() {", "    val y = 2", "}"),
        )
        val printer = Fingerprinter { source[it] }
        val a = printer.fingerprint(listOf(finding(path = "a/src/Only.kt", line = 2))).single().fingerprint
        val b = printer.fingerprint(listOf(finding(path = "b/src/Only.kt", line = 2))).single().fingerprint
        assertNotEquals(a, b)
        assertEquals(false, a.contains('-'), "a content-hashed finding needs no occurrence index: $a")
    }

    @Test
    fun `survives the block shifting to a different line`() {
        val original = fingerprint(finding(line = 3), mapOf("src/Total.kt" to block))
        val shifted = fingerprint(
            finding(line = 13),
            mapOf("src/Total.kt" to List(10) { "// header $it" } + block),
        )
        assertEquals(original, shifted)
    }

    @Test
    fun `survives a file rename`() {
        val original = fingerprint(finding(path = "src/Total.kt"), mapOf("src/Total.kt" to block))
        val renamed = fingerprint(finding(path = "src/Sum.kt"), mapOf("src/Sum.kt" to block))
        assertEquals(original, renamed)
    }

    @Test
    fun `survives reindentation`() {
        val original = fingerprint(finding(), mapOf("src/Total.kt" to block))
        val reindented = fingerprint(finding(), mapOf("src/Total.kt" to block.map { "        $it" }))
        assertEquals(original, reindented)
    }

    @Test
    fun `changes when the violating code is rewritten`() {
        val original = fingerprint(finding(), mapOf("src/Total.kt" to block))
        val rewritten = block.toMutableList().also { it[2] = "    val magic = 43" }
        assertNotEquals(original, fingerprint(finding(), mapOf("src/Total.kt" to rewritten)))
    }

    @Test
    fun `changes when the rule differs`() {
        val source = mapOf("src/Total.kt" to block)
        assertNotEquals(
            fingerprint(finding(rule = "MagicNumber"), source),
            fingerprint(finding(rule = "ForbiddenComment"), source),
        )
    }

    @Test
    fun `falls back to the message when source is unavailable`() {
        val none = emptyMap<String, List<String>>()
        assertEquals(fingerprint(finding(), none), fingerprint(finding(line = 99), none))
        assertNotEquals(fingerprint(finding(), none), fingerprint(finding(message = "Other message."), none))
    }

    @Test
    fun `message fingerprint ignores the checkout root so a local baseline gates CI`() {
        // detekt EmptyKtFile embeds the absolute path in its message; an empty file has no source
        // line, so the message is what gets hashed. The fingerprint must be identical whether the
        // repo is at /Users/dev/proj (local) or /bitrise/src (CI).
        val none = emptyMap<String, List<String>>()
        fun emptyFileFinding(root: String) = Finding(
            "detekt", "EmptyKtFile", Severity.WARNING,
            "The empty Kotlin file $root/common/base-ui/src/main/Foo.kt can be removed.",
            Location("common/base-ui/src/main/Foo.kt", 1),
        )
        val local = Fingerprinter(File("/Users/dev/proj")) { none[it] }
            .fingerprint(listOf(emptyFileFinding("/Users/dev/proj"))).single().fingerprint
        val ci = Fingerprinter(File("/bitrise/src")) { none[it] }
            .fingerprint(listOf(emptyFileFinding("/bitrise/src"))).single().fingerprint
        assertEquals(local, ci)
    }

    @Test
    fun `identical findings get occurrence indexes`() {
        val twins = listOf(finding(line = 10), finding(line = 20))
        val fingerprints = Fingerprinter { null }.fingerprint(twins).map { it.fingerprint }
        assertEquals(2, fingerprints.distinct().size)
        assertEquals("${fingerprints[0]}-1", fingerprints[1])
    }

    @Test
    fun `cpd clones hash the fragment and token count, not the location`() {
        fun clone(snippet: String, tokens: Int = 48, path: String = "A.swift") = Finding(
            "cpd", "duplicated-code", Severity.WARNING, "dup",
            Location(path, 5), snippet = snippet, duplicatedTokens = tokens,
        )
        val a = fingerprint(clone("class Round {\n    val r = 1\n}"), emptyMap())
        val b = fingerprint(clone("class Round {  val r = 1 }", path = "B.swift"), emptyMap())
        assertEquals(a, b)
        assertNotEquals(a, fingerprint(clone("class Round {\n    val r = 1\n}", tokens = 50), emptyMap()))
    }
}
