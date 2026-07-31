package buildchecks.model

/**
 * Mutation-testing results (PIT `mutations.xml`), the shared vocabulary the PIT parser produces and
 * the changed-line mutation machinery consumes. Coverage answers "was this line executed by a
 * test"; mutation answers the harder question "would a test have *noticed* if this line's behaviour
 * changed" — a line can be fully covered yet have every mutant survive, meaning the tests run it but
 * assert nothing about it. That gap is the whole point of the signal (see [Contradiction]).
 *
 * Structured file -> mutant to mirror [CoverageData] and, more importantly, so the results can be
 * intersected with a diff by line (see [changedLineMutation]).
 */
data class MutationData(val files: List<FileMutations>) {
    val total: Int get() = files.sumOf { it.mutations.size }
    val killed: Int get() = files.sumOf { it.killed }
    /** PIT's mutation score: detected mutants / total, or null when nothing was mutated. */
    val score: Double? get() = if (total == 0) null else 100.0 * killed / total
}

data class FileMutations(
    val path: String,
    val mutations: List<Mutation>,
    // Repo-relative path of the ingested PIT report this file came from, so the renderer can flag
    // rows whose source report is a stale age-outlier (see Freshness.outlier). null if unknown.
    val reportPath: String? = null,
) {
    val killed: Int get() = mutations.count { it.detected }
    val survived: Int get() = mutations.count { !it.detected }
}

data class Mutation(
    val line: Int,
    // PIT status verbatim: KILLED, SURVIVED, NO_COVERAGE, TIMED_OUT, MEMORY_ERROR, RUN_ERROR, NON_VIABLE.
    val status: String,
    // PIT's authoritative "was this mutant caught" flag — true for KILLED/TIMED_OUT/MEMORY_ERROR,
    // false for SURVIVED/NO_COVERAGE. Used directly so the score matches PIT's own headline number.
    val detected: Boolean,
)

/**
 * Git paths are repo-relative; PIT paths are package-derived (`com/example/Foo.kt`). Segment-aligned
 * suffix matching bridges them — the same bridge [CoverageData.matching] applies to coverage reports.
 */
fun MutationData.matching(gitPath: String): List<FileMutations> {
    val gitSegments = gitPath.split('/')
    return files.filter { file ->
        val segments = file.path.split('/').filter { it.isNotEmpty() && it != "." }
        if (segments.size <= gitSegments.size) gitSegments.takeLast(segments.size) == segments
        else segments.takeLast(gitSegments.size) == gitSegments
    }
}
