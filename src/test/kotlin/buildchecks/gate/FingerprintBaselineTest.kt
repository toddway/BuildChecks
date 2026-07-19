package buildchecks.gate

import buildchecks.model.Finding
import buildchecks.model.Location
import buildchecks.model.Severity
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FingerprintBaselineTest {

    @TempDir
    lateinit var dir: File

    private fun entry(fingerprint: String, rule: String = "MagicNumber") = FingerprintedFinding(
        Finding("detekt", rule, Severity.WARNING, "A magic number was found here in the code today", Location("src/A.kt", 7)),
        fingerprint,
    )

    @Test
    fun `round trips fingerprints and header totals`() {
        val file = File(dir, "buildchecks-baseline.txt")
        FingerprintBaseline(file).write(listOf(entry("ffff000011112222"), entry("aaaa000011112222")), 52.34)

        val baseline = FingerprintBaseline(file).read()!!
        assertEquals(setOf("ffff000011112222", "aaaa000011112222"), baseline.fingerprints)
        assertEquals(2, baseline.findingCount)
        assertEquals(52.34, baseline.coveragePercent)
    }

    @Test
    fun `file is sorted, human-readable, and capped at eight message words`() {
        val file = File(dir, "buildchecks-baseline.txt")
        FingerprintBaseline(file).write(listOf(entry("ffff000011112222"), entry("aaaa000011112222")), null)

        val lines = file.readLines().filterNot { it.startsWith("#") || it.isEmpty() }
        assertEquals(
            "aaaa000011112222  detekt  MagicNumber  src/A.kt:7  A magic number was found here in the",
            lines.first(),
        )
        assertTrue(lines == lines.sorted())
    }

    @Test
    fun `missing file reads as null`() {
        assertNull(FingerprintBaseline(File(dir, "absent.txt")).read())
    }

    @Test
    fun `empty snapshot round trips`() {
        val file = File(dir, "buildchecks-baseline.txt")
        FingerprintBaseline(file).write(emptyList(), null)
        val baseline = FingerprintBaseline(file).read()!!
        assertEquals(0, baseline.findingCount)
        assertTrue(baseline.fingerprints.isEmpty())
    }
}
