package buildchecks.model

/** Changed lines from `git diff <base>...HEAD`, or the reason no diff could be produced. */
sealed interface ChangedLines {
    /** New-side path -> line numbers added or modified relative to the base ref. */
    data class Diff(val baseRef: String, val files: Map<String, Set<Int>>) : ChangedLines
    data class Unavailable(val reason: String) : ChangedLines
}
