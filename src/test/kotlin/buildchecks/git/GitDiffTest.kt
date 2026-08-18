package buildchecks.git

import buildchecks.Fixtures
import buildchecks.model.ChangedLines
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GitDiffTest {

    @TempDir
    lateinit var dir: File

    // Fixture generated with `git diff --unified=0 --no-color --find-renames` over a repo
    // exercising a new file, edits, a deletion, a binary change, and a rename with an edit.
    @Test
    fun `parses real diff output into changed line sets`() {
        val files = parseUnifiedDiff(Fixtures.text("git-diff.txt"))

        assertEquals(setOf(1, 2), files["docs/new.md"])
        assertEquals(setOf(3, 5), files["docs/notes.md"]) // pure-deletion hunk at line 2 yields nothing
        assertEquals(setOf(5, 6, 8, 9), files["src/main/kotlin/com/example/Greeter.kt"])
        assertEquals(setOf(6), files["src/main/kotlin/com/example/RenamedV2.kt"]) // tracked under the new path
        assertFalse("docs/old.md" in files, "deleted files have no new-side lines")
        assertFalse("logo.bin" in files, "binary files have no hunks")
        assertFalse(files.keys.any { it.startsWith("b/") }, "prefixes are stripped")
    }

    @Test
    fun `extracts changed lines from a real repository`() {
        git(dir, "init", "-q")
        File(dir, "app.txt").writeText((1..10).joinToString("\n") { "line $it" } + "\n")
        git(dir, "add", "-A")
        commit(dir, "base")
        git(dir, "branch", "base")
        File(dir, "app.txt").writeText((1..10).joinToString("\n") { if (it in 3..4) "line $it changed" else "line $it" } + "\n")
        git(dir, "add", "-A")
        commit(dir, "feature")

        val diff = GitDiff(dir).changedLines("base") as ChangedLines.Diff
        assertEquals("base", diff.baseRef)
        assertEquals(mapOf("app.txt" to setOf(3, 4)), diff.files)
    }

    @Test
    fun `a bare branch name falls back to the remote-tracking ref`() {
        val upstream = File(dir, "upstream")
        git(dir, "init", "-q", "upstream")
        File(upstream, "app.txt").writeText("one\ntwo\n")
        git(upstream, "add", "-A")
        commit(upstream, "base")
        git(upstream, "branch", "dev") // exists upstream only, like a PR's GITHUB_BASE_REF

        git(dir, "clone", "-q", upstream.absolutePath, "clone")
        val clone = File(dir, "clone")
        File(clone, "app.txt").writeText("one\ntwo changed\n")
        git(clone, "add", "-A")
        commit(clone, "feature")

        val diff = GitDiff(clone).changedLines("dev") as ChangedLines.Diff
        assertEquals("origin/dev", diff.baseRef)
        assertEquals(mapOf("app.txt" to setOf(2)), diff.files)
    }

    @Test
    fun `unknown refs and non-repositories are unavailable, never errors`() {
        git(dir, "init", "-q")
        commit(dir, "empty")
        val unknownRef = GitDiff(dir).changedLines("no-such-ref")
        assertTrue(unknownRef is ChangedLines.Unavailable, unknownRef.toString())
        assertTrue((unknownRef as ChangedLines.Unavailable).reason.contains("no-such-ref"), unknownRef.reason)

        val notARepo = GitDiff(File(dir, "elsewhere").apply { mkdirs() }).changedLines("main")
        assertTrue(notARepo is ChangedLines.Unavailable, notARepo.toString())
    }

    @Test
    fun `prefers the remote ref when the local one already contains HEAD`() {
        // Bitrise's PR builds merge the change into its target branch before building, so the local
        // `dev` sits at HEAD and `git diff dev...HEAD` exits 0 with no output. That read as "no
        // changed lines", silently skipping every gate built on the diff.
        val upstream = File(dir, "upstream")
        git(dir, "init", "-q", "upstream")
        File(upstream, "app.txt").writeText("one\ntwo\n")
        git(upstream, "add", "-A")
        commit(upstream, "base")
        git(upstream, "branch", "dev")

        git(dir, "clone", "-q", upstream.absolutePath, "clone")
        val clone = File(dir, "clone")
        File(clone, "app.txt").writeText("one\ntwo changed\n")
        git(clone, "add", "-A")
        commit(clone, "feature")
        git(clone, "branch", "dev", "HEAD") // the merged-in target ref, sitting at HEAD

        val diff = GitDiff(clone).changedLines("dev") as ChangedLines.Diff
        assertEquals("origin/dev", diff.baseRef, "must not settle for the vacuous local diff")
        assertEquals(mapOf("app.txt" to setOf(2)), diff.files)
    }

    @Test
    fun `a base ref containing HEAD with no remote fallback says so`() {
        // Same shape, but nothing better to fall back to: the result must name the cause rather than
        // claim nothing changed, since a benign skip is indistinguishable from a pass.
        git(dir, "init", "-q")
        File(dir, "app.txt").writeText("one\n")
        git(dir, "add", "-A")
        commit(dir, "base")
        git(dir, "branch", "merged-target", "HEAD")

        val result = GitDiff(dir).changedLines("merged-target")
        assertTrue(result is ChangedLines.Unavailable, result.toString())
        val reason = (result as ChangedLines.Unavailable).reason
        assertTrue(reason.contains("already contains HEAD"), reason)
        assertTrue(reason.contains("merged-target"), reason)
    }

    @Test
    fun `an empty diff that is not a contained base ref still reports no changed lines`() {
        // The retry must not turn a legitimately empty diff into an error. HEAD is ahead of the base
        // here (so it is not an ancestor of it) but carries no tree change, which is what a revert or
        // an empty commit looks like.
        git(dir, "init", "-q")
        File(dir, "app.txt").writeText("one\n")
        git(dir, "add", "-A")
        commit(dir, "base")
        git(dir, "branch", "base")
        commit(dir, "empty commit on top") // --allow-empty, so the tree is unchanged

        val result = GitDiff(dir).changedLines("base")
        assertTrue(result is ChangedLines.Diff, result.toString())
        assertEquals(emptyMap<String, Set<Int>>(), (result as ChangedLines.Diff).files)
        assertEquals("base", result.baseRef)
    }

    private fun git(workDir: File, vararg args: String) {
        val process = ProcessBuilder("git", *args).directory(workDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), "git ${args.first()}: $output")
    }

    private fun commit(workDir: File, message: String) =
        git(workDir, "-c", "user.email=test@test", "-c", "user.name=test", "commit", "-qm", message, "--allow-empty")
}
