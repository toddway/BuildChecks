package buildchecks.model

/**
 * Age spread of the ingested files (V4-PLAN.md §3). A large spread is the signature of
 * orphaned reports from deleted modules or a partially-run build. Warning only; never a gate.
 */
data class Freshness(
    val ageMinutes: Map<String, Long>, // ingested path -> minutes since last modified
    val toleranceMinutes: Long,
) {
    val spreadMinutes: Long get() = (ageMinutes.values.maxOrNull() ?: 0) - (ageMinutes.values.minOrNull() ?: 0)
    val stale: Boolean get() = spreadMinutes > toleranceMinutes
}

fun freshness(files: List<IngestedFile>, nowMillis: Long, toleranceMinutes: Long = 15): Freshness? {
    if (files.isEmpty()) return null
    val ages = files.associate { it.path to ((nowMillis - it.lastModified) / 60_000).coerceAtLeast(0) }
    return Freshness(ages, toleranceMinutes)
}
