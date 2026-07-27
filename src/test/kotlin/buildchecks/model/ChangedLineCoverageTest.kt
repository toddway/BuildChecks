package buildchecks.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangedLineCoverageTest {

    // JaCoCo-style package path; git sees the full repo-relative path.
    private fun coverage(vararg lines: Pair<Int, Int>) = CoverageData(listOf(
        FileCoverage("com/example/Greeter.kt", lines.map { LineCoverage(it.first, it.second) }),
    ))

    private fun diff(vararg lines: Int) = ChangedLines.Diff(
        "origin/dev",
        mapOf("src/main/kotlin/com/example/Greeter.kt" to lines.toSet()),
    )

    @Test
    fun `no base ref is unavailable with the flag hint`() {
        val result = changedLineCoverage(null, coverage(1 to 1))
        assertTrue(result is ChangedLineCoverage.Unavailable)
        assertTrue((result as ChangedLineCoverage.Unavailable).reason.contains("default branch"))
    }

    @Test
    fun `git unavailability passes through verbatim`() {
        val reason = "git not available: No such file or directory"
        val result = changedLineCoverage(ChangedLines.Unavailable(reason), null)
        assertEquals(ChangedLineCoverage.Unavailable(reason), result)
    }

    @Test
    fun `empty diff, missing coverage, and no executable lines are distinct skips`() {
        assertEquals(
            ChangedLineCoverage.Unavailable("no changed lines vs main"),
            changedLineCoverage(ChangedLines.Diff("main", emptyMap()), coverage(1 to 1)),
        )
        assertEquals(
            ChangedLineCoverage.Unavailable("no coverage data"),
            changedLineCoverage(diff(5), null),
        )
        // line 99 changed but the report has no data for it (e.g. generated code)
        assertEquals(
            ChangedLineCoverage.Unavailable("no executable changed lines vs origin/dev"),
            changedLineCoverage(diff(99), coverage(1 to 1)),
        )
    }

    @Test
    fun `all changed files lacking coverage names the likely cause`() {
        // e.g. a build-logic-only diff: paths no ingested coverage report covers -> no data at all.
        val changed = ChangedLines.Diff("origin/dev", mapOf(
            "gradle-plugins/build.gradle.kts" to setOf(1, 2),
            "docs/README.md" to setOf(3),
        ))
        val result = changedLineCoverage(changed, coverage(1 to 1)) as ChangedLineCoverage.Unavailable
        assertTrue(result.reason.startsWith("no executable changed lines vs origin/dev"))
        assertTrue(result.reason.contains("2 changed files had no matching coverage data"))
        assertTrue(result.reason.contains("build logic"))
    }

    @Test
    fun `measured splits covered from uncovered and ignores non-executable lines`() {
        // lines 5,6 covered; 7 uncovered; 8 non-executable (absent from the report)
        val result = changedLineCoverage(diff(5, 6, 7, 8), coverage(5 to 1, 6 to 2, 7 to 0))
        val measured = result as ChangedLineCoverage.Measured
        assertEquals("origin/dev", measured.baseRef)
        val file = measured.files.single()
        assertEquals(listOf(5, 6), file.covered)
        assertEquals(listOf(7), file.uncovered)
        assertEquals(3, measured.executableCount)
        assertEquals(2, measured.coveredCount)
        assertEquals(66.66, measured.percent, 0.01)
        assertEquals(listOf(file), measured.uncoveredFiles)
    }

    @Test
    fun `changed files without matching coverage are counted, not measured`() {
        val changed = ChangedLines.Diff("main", mapOf(
            "src/main/kotlin/com/example/Greeter.kt" to setOf(5),
            "README.md" to setOf(1, 2),
        ))
        val measured = changedLineCoverage(changed, coverage(5 to 1)) as ChangedLineCoverage.Measured
        assertEquals(1, measured.filesWithoutData)
        assertEquals(1, measured.files.size)
        assertTrue(measured.uncoveredFiles.isEmpty()) // the one measured file is fully covered
    }

    @Test
    fun `matching bridges absolute report paths to repo-relative git paths`() {
        val absolute = CoverageData(listOf(
            FileCoverage("/ci/workspace/src/main/kotlin/com/example/Greeter.kt", listOf(LineCoverage(5, 1))),
        ))
        val matches = absolute.matching("src/main/kotlin/com/example/Greeter.kt")
        assertEquals(1, matches.size)
    }
}
