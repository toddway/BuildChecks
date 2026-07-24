package buildchecks.git

import buildchecks.model.ChangedLines
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Changed-line extraction — the only code in the tool that shells out (V4-PLAN.md §2.5).
 * Every outcome is plain data; nothing here can fail a build on its own.
 */
class GitDiff(private val root: File) {

    /**
     * Diffs the merge base (`<ref>...HEAD`). A bare branch name that doesn't resolve retries
     * as `origin/<ref>`, because GITHUB_BASE_REF carries names like `main` while CI checkouts
     * usually only have the remote-tracking ref.
     */
    fun changedLines(baseRef: String): ChangedLines {
        val direct = diff(baseRef)
        if (direct is ChangedLines.Diff || "/" in baseRef) return direct
        return diff("origin/$baseRef").takeIf { it is ChangedLines.Diff } ?: direct
    }

    /**
     * The remote's default branch as `origin/<name>` (e.g. `origin/main`), read from the
     * `origin/HEAD` symbolic ref that `git clone` sets. This is the local convenience base ref: a
     * plain `check` with no CI env and no configured ref still diffs against the default branch,
     * the same common heuristic other "changed since main" tools use. null when there is no
     * origin, no `origin/HEAD`, or git is unavailable — the caller then skips the gate.
     */
    fun defaultBranch(): String? =
        capture("git", "symbolic-ref", "--short", "refs/remotes/origin/HEAD")?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * The contents of [path] at [ref] (`git show <ref>:<path>`), or null when the file didn't exist
     * at that ref, the ref doesn't resolve, or git is unavailable. Retries `origin/<ref>` for a bare
     * branch name, mirroring [changedLines], so a base ref like `main` still reads on a CI checkout
     * that only has the remote-tracking ref. Feeds the 4.2 baseline- and config-diff signals.
     */
    fun show(ref: String, path: String): String? =
        capture("git", "show", "$ref:$path")
            ?: if ("/" in ref) null else capture("git", "show", "origin/$ref:$path")

    private fun diff(ref: String): ChangedLines {
        val process = try {
            ProcessBuilder("git", "diff", "--unified=0", "--no-color", "--find-renames", "$ref...HEAD")
                .directory(root)
                .start()
        } catch (e: IOException) {
            return ChangedLines.Unavailable("git not available: ${e.message}")
        }
        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return ChangedLines.Unavailable("git diff timed out")
        }
        if (process.exitValue() != 0) {
            val firstLine = error.lineSequence().firstOrNull { it.isNotBlank() } ?: "exit ${process.exitValue()}"
            return ChangedLines.Unavailable("git diff $ref...HEAD failed: ${firstLine.trim()}")
        }
        return ChangedLines.Diff(ref, parseUnifiedDiff(output))
    }

    // Run a read-only git command, returning stdout on success or null on any failure (no git,
    // non-zero exit, timeout). Like diff(), nothing here can fail a build on its own.
    private fun capture(vararg command: String): String? {
        val process = try {
            ProcessBuilder(*command).directory(root).start()
        } catch (e: IOException) {
            return null
        }
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        return if (process.exitValue() == 0) output else null
    }
}

/**
 * New-side paths (`+++ b/...`) to added/modified line numbers from `@@ -a,b +c,d @@` headers.
 * Deletions target `/dev/null` and binary files have no hunks, so both drop out; renames are
 * tracked under the new path.
 */
fun parseUnifiedDiff(diff: String): Map<String, Set<Int>> {
    val files = linkedMapOf<String, MutableSet<Int>>()
    var current: MutableSet<Int>? = null
    for (line in diff.lineSequence()) {
        when {
            line.startsWith("+++ ") -> {
                val path = line.removePrefix("+++ ").substringBefore('\t')
                current = if (path == "/dev/null") null
                else files.getOrPut(path.removePrefix("b/")) { linkedSetOf() }
            }
            line.startsWith("@@ ") -> {
                val hunk = HUNK.find(line) ?: continue
                val start = hunk.groupValues[1].toInt()
                val count = hunk.groupValues[2].ifEmpty { "1" }.toInt()
                current?.addAll(start until start + count)
            }
        }
    }
    return files
}

private val HUNK = Regex("""^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@""")
