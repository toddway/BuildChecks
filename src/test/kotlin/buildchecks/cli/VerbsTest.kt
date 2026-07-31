package buildchecks.cli

import buildchecks.Fixtures
import buildchecks.gate.FingerprintBaseline
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** End-to-end: a temp project assembled from real Shelf reports, through both verbs. */
class VerbsTest {

    @TempDir
    lateinit var root: File

    private val out = mutableListOf<String>()
    private val text: String get() = out.joinToString("\n")

    private fun copy(fixture: String, dest: String) {
        Fixtures.file(fixture).copyTo(File(root, dest))
    }

    private fun copyPassingReports() {
        copy("shelf/detekt.xml", "build/reports/detekt.xml")
        copy("shelf/jvmTestCoverage.xml", "build/reports/jvmTestCoverage/jvmTestCoverage.xml")
        copy("shelf/TEST-com.toddway.shelf.ShelfTests.xml", "build/test-results/jvmTest/TEST-ShelfTests.xml")
    }

    @Test
    fun `baseline then check passes with exit code zero`() {
        copyPassingReports()
        assertEquals(0, runBaseline(root) { out += it })
        assertTrue(File(root, "buildchecks-baseline.txt").isFile)

        assertEquals(0, runCheck(root) { out += it })
        assertTrue(text.contains("PASS  findings: 0 new (max 0), 8 total (baseline max 8)"), text)
        assertTrue(text.contains("PASS  coverage:"), text)
        assertTrue(text.contains("PASS  test failures: 0 failed of 16"), text)
        // a clean run with fresh reports and no skipped gates is full confidence
        assertTrue(text.contains("confidence: HIGH"), text)
    }

    @Test
    fun `check writes every output file`() {
        copyPassingReports()
        copy("eslint.sarif", "build/reports/eslint.sarif")
        // a tool html report next to the ingested xml, as jacoco lays it out
        File(root, "build/reports/jvmTestCoverage/html").mkdirs()
        File(root, "build/reports/jvmTestCoverage/html/index.html").writeText("<html>jacoco report</html>")
        // Gradle's test html report, in its conventional location
        File(root, "build/reports/tests/jvmTest").mkdirs()
        File(root, "build/reports/tests/jvmTest/index.html").writeText("<html>test report</html>")
        runBaseline(root) { }
        assertEquals(0, runCheck(root) { out += it })

        val outputDir = File(root, ReportDiscovery.DEFAULT_OUTPUT_DIR)
        for (name in listOf("index.html", "summary.md", "summary.json", "findings.json", "codeclimate.json", "merged.sarif")) {
            assertTrue(File(outputDir, name).isFile, "$name missing")
        }
        assertTrue(File(outputDir, "summary.json").readText().contains("\"passed\": true"))
        assertTrue(File(outputDir, "merged.sarif").readText().contains("ESLint"))
        // jacoco html/ dir sits next to the ingested xml -> copied, linked, pinned to light mode
        val copied = File(outputDir, "tools/build-reports-jvmTestCoverage/html/index.html")
        assertTrue(copied.isFile)
        assertTrue(copied.readText().contains("color-scheme:only light"), copied.readText())
        val html = File(outputDir, "index.html").readText()
        assertTrue(html.contains("tools/build-reports-jvmTestCoverage/html/index.html"))
        // junit xml under test-results/jvmTest -> Gradle's reports/tests/jvmTest html
        assertTrue(File(outputDir, "tools/build-reports-tests-jvmTest/index.html").isFile)
        assertTrue(html.contains("<a href=\"tools/build-reports-tests-jvmTest/index.html\">jvmTest</a>"), html)
    }

    @Test
    fun `check fails against an emptied baseline`() {
        copyPassingReports()
        runBaseline(root) { }
        FingerprintBaseline(File(root, "buildchecks-baseline.txt")).write(emptyList(), null)

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(text.contains("FAIL  findings: 8 new (max 0), 8 total (baseline max 0)"), text)
        assertTrue(File(root, "${ReportDiscovery.DEFAULT_OUTPUT_DIR}/summary.json").readText()
            .contains("\"passed\": false"))
    }

    @Test
    fun `a report that stops being emitted fails until re-baselined`() {
        copyPassingReports()
        runBaseline(root) { out += it }
        assertTrue(text.contains("origins: . (3 report(s))"), text)

        // the detekt check is silently disabled -> its report vanishes this run
        File(root, "build/reports/detekt.xml").delete()
        out.clear()
        assertEquals(1, runCheck(root) { out += it })
        assertTrue(text.contains("FAIL  expected reports: 1 expected report(s) missing: checkstyle in ."), text)

        // intentional removal is accepted the same way new findings are: a visible re-baseline
        out.clear()
        runBaseline(root) { out += it }
        assertEquals(0, runCheck(root) { out += it })
        assertTrue(text.contains("PASS  expected reports:"), text)
    }

    @Test
    fun `check skips the presence gate against a pre-v2 baseline`() {
        copyPassingReports()
        // a baseline written before the manifest existed (v1, no origin lines)
        File(root, "buildchecks-baseline.txt").writeText("# buildchecks baseline v1\n# findings: 8\n")

        runCheck(root) { out += it }
        assertTrue(text.contains("SKIP  expected reports: baseline predates the origin manifest"), text)
    }

    @Test
    fun `failing tests exit one even without a baseline`() {
        copy("shelf/TEST-com.toddway.shelf.JvmTests.xml", "build/test-results/jvmTest/TEST-JvmTests.xml")

        assertEquals(1, runCheck(root) { out += it })
        assertTrue(text.contains("FAIL  test failures: 1 failed of 4 tests (max 0)"), text)
        assertTrue(text.contains("SKIP  findings:"), text)
    }

    @Test
    fun `a project without reports passes with skips`() {
        assertEquals(0, runCheck(root) { out += it })
        assertTrue(text.contains("no report files found"), text)
    }

    @Test
    fun `unrecognized candidate files are listed, not silently skipped`() {
        File(root, "build/reports").mkdirs()
        File(root, "build/reports/lint-results.xml").writeText(Fixtures.text("lint-results-prodRelease.xml"))

        runCheck(root) { out += it }
        assertTrue(text.contains("not understood: build/reports/lint-results.xml"), text)
    }

    @Test
    fun `config drives gates, output dir, and baseline file`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [reports]
            output_dir = "out/quality"
            [gates]
            max_errors = 0
            [git]
            baseline_file = "quality-baseline.txt"
        """.trimIndent())
        val config = loadConfig(null, root)

        runBaseline(root, config) { }
        assertTrue(File(root, "quality-baseline.txt").isFile)

        assertEquals(1, runCheck(root, config) { out += it })
        assertTrue(text.contains("FAIL  errors: 6 errors (max 0)"), text) // shelf detekt has 6 errors
        assertTrue(File(root, "out/quality/summary.json").isFile)
    }

    @Test
    fun `configured report globs narrow what is ingested`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [reports]
            paths = ["**/test-results/**"]
        """.trimIndent())

        runCheck(root, loadConfig(null, root)) { out += it }
        assertTrue(text.contains("ingested: build/test-results/jvmTest/TEST-ShelfTests.xml (junit)"), text)
        assertTrue(!text.contains("detekt.xml"), text)
    }

    @Test
    fun `changed-line coverage skips visibly without a base ref`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_changed_line_coverage = 80
        """.trimIndent())
        runBaseline(root) { }

        assertEquals(0, runCheck(root, loadConfig(null, root), env = { null }) { out += it })
        assertTrue(text.contains("SKIP  changed-line coverage: no base ref"), text)
        // the changed-line coverage gate is severable by design, so its skip is shown but does NOT
        // lower confidence — there was nothing to measure, not an intended check that failed to run
        assertTrue(text.contains("confidence: HIGH"), text)
    }

    @Test
    fun `failOnSkippedGates promotes a skip to a failure`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_changed_line_coverage = 80
            fail_on_skipped_gates = true
        """.trimIndent())
        runBaseline(root) { }

        // same run as the skip test above, but the knob turns the skip into a hard failure
        assertEquals(1, runCheck(root, loadConfig(null, root), env = { null }) { out += it })
        assertTrue(text.contains("SKIP  changed-line coverage: no base ref"), text)
        assertTrue(text.contains("FAIL  no skipped gates:"), text)
    }

    @Test
    fun `requireBaseRef promotes a missing base ref to a failure`() {
        copyPassingReports()
        File(root, "buildchecks.toml").writeText("""
            [gates]
            require_base_ref = true
        """.trimIndent())
        runBaseline(root) { }

        assertEquals(1, runCheck(root, loadConfig(null, root), env = { null }) { out += it })
        assertTrue(text.contains("FAIL  base ref required:"), text)
    }

    @Test
    fun `changed-line coverage gates a real diff against lcov line data`() {
        // calculator.js per lcov.info: lines 1-10 hit, 11-12 missed, 13-15 hit, 16-18 missed
        copy("lcov.info", "coverage/lcov.info")
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_changed_line_coverage = 80
            [git]
            base_ref = "base"
        """.trimIndent())
        git("init", "-q")
        File(root, "src").mkdirs()
        val calculator = File(root, "src/calculator.js")
        calculator.writeText((1..20).joinToString("\n") { "// line $it" } + "\n")
        git("add", "src")
        git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "base")
        git("branch", "base")
        // change one covered line (4) and one uncovered line (11) -> 50% < 80
        calculator.writeText((1..20).joinToString("\n") { if (it == 4 || it == 11) "// line $it changed" else "// line $it" } + "\n")
        git("add", "src")
        git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "change")
        runBaseline(root, loadConfig(null, root)) { }

        assertEquals(1, runCheck(root, loadConfig(null, root)) { out += it })
        assertTrue(text.contains("FAIL  changed-line coverage: 50.00% of 2 changed lines vs base (min 80%)"), text)
    }

    // A repo whose only diff vs the `base` branch touches one covered line (4) and one uncovered
    // line (11) of calculator.js -> 50% changed-line coverage, below the 80% gate. The base ref is
    // left to the caller (env or flag), so these tests exercise resolution, not just the diff.
    private fun setUpChangedLineRepo() {
        copy("lcov.info", "coverage/lcov.info")
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_changed_line_coverage = 80
        """.trimIndent())
        git("init", "-q")
        File(root, "src").mkdirs()
        val calculator = File(root, "src/calculator.js")
        calculator.writeText((1..20).joinToString("\n") { "// line $it" } + "\n")
        git("add", "src")
        git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "base")
        git("branch", "base")
        calculator.writeText((1..20).joinToString("\n") { if (it == 4 || it == 11) "// line $it changed" else "// line $it" } + "\n")
        git("add", "src")
        git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "change")
        runBaseline(root, loadConfig(null, root)) { }
    }

    @Test
    fun `changed-line coverage resolves the base ref from a CI env var`() {
        setUpChangedLineRepo()
        val env = mapOf("BITRISEIO_GIT_BRANCH_DEST" to "base")
        assertEquals(1, runCheck(root, loadConfig(null, root), env = { env[it] }) { out += it })
        assertTrue(text.contains("FAIL  changed-line coverage: 50.00% of 2 changed lines vs base (min 80%)"), text)
    }

    @Test
    fun `a refs-heads-prefixed CI base ref is normalized`() {
        setUpChangedLineRepo()
        val env = mapOf("SYSTEM_PULLREQUEST_TARGETBRANCH" to "refs/heads/base")
        assertEquals(1, runCheck(root, loadConfig(null, root), env = { env[it] }) { out += it })
        assertTrue(text.contains("FAIL  changed-line coverage: 50.00% of 2 changed lines vs base (min 80%)"), text)
    }

    @Test
    fun `changed-line coverage falls back to the remote default branch, and says so`() {
        setUpChangedLineRepo()
        // Stand in for a clone: a remote-tracking ref at the base commit + origin/HEAD pointing at it.
        git("update-ref", "refs/remotes/origin/base", "base")
        git("symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/base")
        // No flag, no config base_ref, no CI env -> the default-branch fallback resolves the ref.
        assertEquals(1, runCheck(root, loadConfig(null, root), env = { null }) { out += it })
        assertTrue(text.contains("diffing against origin/base (default branch)"), text)
        assertTrue(text.contains("FAIL  changed-line coverage: 50.00% of 2 changed lines vs origin/base (min 80%)"), text)
    }

    @Test
    fun `base-ref delta signals lower confidence when the ruler moved in this same change`() {
        copyPassingReports()
        git("init", "-q")
        // the base ref: a strict coverage floor and a baseline of exactly the findings present
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_coverage_percent = 90.0
        """.trimIndent())
        runBaseline(root, loadConfig(null, root)) { }
        git("add", "buildchecks.toml", "buildchecks-baseline.txt")
        git("-c", "user.email=t@t", "-c", "user.name=t", "commit", "-qm", "base")
        git("branch", "base")

        // this change loosens the coverage floor and grows the accepted baseline (working tree only —
        // the delta compares committed base against the on-disk files this run gated with)
        File(root, "buildchecks.toml").writeText("""
            [gates]
            min_coverage_percent = 50.0
        """.trimIndent())
        File(root, "buildchecks-baseline.txt").appendText("deadbeef  detekt  Extra  A.kt:1  fabricated finding\n")

        val vars = mapOf("GITHUB_BASE_REF" to "base")
        // still green (every tracked metric holds) but confidence drops: the run says so out loud
        assertEquals(0, runCheck(root, loadConfig(null, root), env = { vars[it] }) { out += it })
        assertTrue(text.contains("confidence: LOW"), text)
        assertTrue(text.contains("baseline loosened vs the base ref"), text)
        assertTrue(text.contains("1 finding newly accepted"), text)
        assertTrue(text.contains("min_coverage_percent 90.0 → 50.0"), text)
    }

    private val err = mutableListOf<String>()

    @Test
    fun `changed-files prints the changed paths to stdout against a resolved base ref`() {
        setUpChangedLineRepo() // one file changed vs the `base` branch: src/calculator.js
        val env = mapOf("GITHUB_BASE_REF" to "base")

        assertEquals(0, runChangedFiles(root, loadConfig(null, root), env = { env[it] },
            emit = { out += it }, echo = { err += it }))
        assertEquals(listOf("src/calculator.js"), out) // only the diff, nothing else, on stdout
    }

    @Test
    fun `changed-files keeps the base-ref fallback notice off stdout`() {
        setUpChangedLineRepo()
        // Stand in for a clone so the default-branch fallback resolves (no flag/config/CI env).
        git("update-ref", "refs/remotes/origin/base", "base")
        git("symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/base")

        assertEquals(0, runChangedFiles(root, loadConfig(null, root), env = { null },
            emit = { out += it }, echo = { err += it }))
        assertEquals(listOf("src/calculator.js"), out) // stdout is only paths — pipe-clean
        assertTrue(err.joinToString("\n").contains("diffing against origin/base (default branch)"), err.toString())
    }

    @Test
    fun `changed-files exits two with no base ref, printing nothing to stdout`() {
        setUpChangedLineRepo() // a git repo, but nothing resolves a base ref

        assertEquals(2, runChangedFiles(root, loadConfig(null, root), env = { null },
            emit = { out += it }, echo = { err += it }))
        assertTrue(out.isEmpty(), out.toString())
        assertTrue(err.joinToString("\n").contains("no base ref resolved"), err.toString())
    }

    @Test
    fun `changed-files exits two when git cannot diff, e g outside a repo`() {
        // No `git init` under root -> git diff fails; the reason surfaces on stderr, stdout stays empty.
        assertEquals(2, runChangedFiles(root, Config(), baseRefFlag = "base",
            emit = { out += it }, echo = { err += it }))
        assertTrue(out.isEmpty(), out.toString())
        assertTrue(err.joinToString("\n").contains("changed files unavailable"), err.toString())
    }

    private fun git(vararg args: String) {
        val process = ProcessBuilder("git", *args).directory(root).redirectErrorStream(true).start()
        assertEquals(0, process.waitFor(), "git failed: ${process.inputStream.bufferedReader().readText()}")
    }

    @Test
    fun `stale sibling reports produce a freshness warning`() {
        copyPassingReports()
        File(root, "build/reports/detekt.xml").setLastModified(System.currentTimeMillis() - 60 * 60_000)

        runCheck(root) { out += it }
        assertTrue(text.contains("WARNING: ingested reports differ in age by"), text)
    }
}
