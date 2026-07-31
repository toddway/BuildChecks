package buildchecks.parse

import buildchecks.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Golden file: a real PIT run on this repo (`./gradlew pitest`) over three branch-heavy classes. */
class PitParserTest {

    private val mutation = PitParser().parse(Fixtures.text("mutations.xml")).mutation!!

    @Test
    fun `aggregates mutants across all classes`() {
        assertEquals(93, mutation.total)
        assertEquals(55, mutation.killed)
        assertEquals(3, mutation.files.size)
        assertEquals(59.14, mutation.score!!, 0.01) // 55 / 93
    }

    @Test
    fun `derives package paths from the mutated class`() {
        val file = mutation.files.single { it.path == "buildchecks/gate/CoverageGate.kt" }
        assertEquals(15, file.mutations.size)
        assertEquals(12, file.killed)
        assertEquals(3, file.survived)
    }

    @Test
    fun `reads status and PIT's detected flag per mutant`() {
        val all = mutation.files.flatMap { it.mutations }
        // In this fixture the only detected status is KILLED; SURVIVED and NO_COVERAGE are undetected.
        assertTrue(all.all { (it.status == "KILLED") == it.detected })
        assertTrue(all.any { it.status == "SURVIVED" && !it.detected })
        assertTrue(all.any { it.status == "NO_COVERAGE" && !it.detected })
        assertTrue(all.all { it.line > 0 })
    }
}
