package buildchecks.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangedLineMutationTest {

    // PIT-style package path; git sees the full repo-relative path.
    private fun mutation(vararg m: Triple<Int, String, Boolean>) = MutationData(listOf(
        FileMutations("com/example/Greeter.kt", m.map { Mutation(it.first, it.second, it.third) }),
    ))

    private fun killed(line: Int) = Triple(line, "KILLED", true)
    private fun survived(line: Int) = Triple(line, "SURVIVED", false)

    private fun diff(vararg lines: Int) = ChangedLines.Diff(
        "origin/dev",
        mapOf("src/main/kotlin/com/example/Greeter.kt" to lines.toSet()),
    )

    @Test
    fun `no base ref is unavailable with the flag hint`() {
        val result = changedLineMutation(null, mutation(killed(1)))
        assertTrue(result is ChangedLineMutation.Unavailable)
        assertTrue((result as ChangedLineMutation.Unavailable).reason.contains("default branch"))
    }

    @Test
    fun `git unavailability passes through verbatim`() {
        val reason = "git not available: No such file or directory"
        val result = changedLineMutation(ChangedLines.Unavailable(reason), null)
        assertEquals(ChangedLineMutation.Unavailable(reason), result)
    }

    @Test
    fun `empty diff, missing mutation data, and no mutants are distinct skips`() {
        assertEquals(
            ChangedLineMutation.Unavailable("no changed lines vs main"),
            changedLineMutation(ChangedLines.Diff("main", emptyMap()), mutation(killed(1))),
        )
        assertEquals(
            ChangedLineMutation.Unavailable("no mutation data"),
            changedLineMutation(diff(5), null),
        )
        // line 99 changed but no mutant fell on it (declaration, blank, or nothing mutable there)
        assertEquals(
            ChangedLineMutation.Unavailable("no changed lines carry mutants vs origin/dev"),
            changedLineMutation(diff(99), mutation(killed(1))),
        )
    }

    @Test
    fun `all changed files lacking mutation data names the likely cause`() {
        val changed = ChangedLines.Diff("origin/dev", mapOf(
            "gradle-plugins/build.gradle.kts" to setOf(1, 2),
            "docs/README.md" to setOf(3),
        ))
        val result = changedLineMutation(changed, mutation(killed(1))) as ChangedLineMutation.Unavailable
        assertTrue(result.reason.startsWith("no changed lines carry mutants vs origin/dev"))
        assertTrue(result.reason.contains("2 changed files had no matching mutation data"))
        assertTrue(result.reason.contains("build logic"))
    }

    @Test
    fun `measured counts mutants on changed lines and lists the surviving ones`() {
        // line 5 has one killed + one surviving mutant; line 6 one killed; line 7 changed but no mutant
        val data = mutation(killed(5), survived(5), killed(6), survived(8))
        val measured = changedLineMutation(diff(5, 6, 7), data) as ChangedLineMutation.Measured
        assertEquals("origin/dev", measured.baseRef)
        val file = measured.files.single()
        assertEquals(3, measured.mutantCount) // two on line 5, one on line 6; line 8's mutant isn't changed
        assertEquals(2, measured.killedCount)
        assertEquals(66.66, measured.percent, 0.01)
        assertEquals(listOf(5), file.survivedLines)
        assertEquals(listOf(file), measured.survivedFiles)
    }

    @Test
    fun `changed files without matching mutation data are counted, not measured`() {
        val changed = ChangedLines.Diff("main", mapOf(
            "src/main/kotlin/com/example/Greeter.kt" to setOf(5),
            "README.md" to setOf(1, 2),
        ))
        val measured = changedLineMutation(changed, mutation(killed(5))) as ChangedLineMutation.Measured
        assertEquals(1, measured.filesWithoutData)
        assertEquals(1, measured.files.size)
        assertTrue(measured.survivedFiles.isEmpty()) // the one measured file's mutant was killed
    }

    @Test
    fun `matching bridges package-derived and absolute paths to repo-relative git paths`() {
        // PIT's normal package-derived path is a suffix of the git path.
        val pit = mutation(killed(5))
        assertEquals(1, pit.matching("src/main/kotlin/com/example/Greeter.kt").size)
        // and a fully-qualified absolute report path shares the git path as its tail.
        val absolute = MutationData(listOf(
            FileMutations("/ci/workspace/src/main/kotlin/com/example/Greeter.kt", listOf(Mutation(5, "KILLED", true))),
        ))
        assertEquals(1, absolute.matching("src/main/kotlin/com/example/Greeter.kt").size)
    }
}
