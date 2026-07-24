package buildchecks.model

/**
 * How much a green verdict is worth (V4-PLAN.md §11 item 7). `passed` answers "did the tracked
 * metrics hold"; confidence answers "how completely were they actually checked". Every way a pass
 * can quietly mean less than it looks — a gate that `SKIPPED`, a report we couldn't read, a stale
 * report, a source not yet baselined — reduces to one property: *effective checks < intended
 * checks*. Rather than enumerate each as its own gate, they collapse into one orthogonal axis
 * rendered next to `passed`.
 *
 * Informational only: confidence never sets the exit code. A signal that should *block* is promoted
 * to a real gate via an off-by-default `[gates]` knob (see GateConfig), which produces an ordinary
 * FAILED [GateResult] — the exit code stays entirely a function of the gates.
 *
 * Delta signals that need a git base ref (baseline diff, config diff, change-scoped freshness;
 * V4-PLAN §11 4.2) are additional [ConfidenceReason]s on this same axis — they widen the builder's
 * inputs, not this type.
 */
data class Confidence(val reasons: List<ConfidenceReason>) {
    /** Derived, single-sourced: no reasons is full confidence; any MAJOR reason is the low tier. */
    val level: ConfidenceLevel
        get() = when {
            reasons.isEmpty() -> ConfidenceLevel.HIGH
            reasons.any { it.weight == ConfidenceWeight.MAJOR } -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.MEDIUM
        }
}

enum class ConfidenceLevel { HIGH, MEDIUM, LOW }

/**
 * MAJOR = an intended check did not run at all (drops straight to LOW); MINOR = the check ran but
 * something makes it worth a little less (at most MEDIUM). Absolute-age staleness is deliberately
 * MINOR: an incremental build legitimately leaves an unchanged module's report old, so it is a
 * gentle nudge, not proof a check fell. 4.2's change-scoped freshness is the MAJOR version.
 */
enum class ConfidenceWeight { MINOR, MAJOR }

data class ConfidenceReason(
    /** Stable identifier for scripting/tests: "skipped-gates", "stale-reports", etc. */
    val signal: String,
    /** Human one-liner, count folded in — the whole reasons list stays short enough to always show. */
    val summary: String,
    val weight: ConfidenceWeight,
)

/**
 * Assembles the confidence axis from the point-in-time signals available without a base ref
 * (V4-PLAN §11 4.1). One reason per signal-type, with counts folded into the summary, so the list
 * reads at a glance. Wording and weights live here so the renderers only present, never decide.
 *
 * [newReportLabels] are the (origin, kind) sources ingested this run but absent from the baseline
 * manifest — computed by the caller (which owns the gate-package OriginKind type) and passed in as
 * plain labels, keeping this model function free of any outward dependency.
 */
fun confidence(
    gates: List<GateResult>,
    freshness: Freshness?,
    notUnderstood: List<String>,
    newReportLabels: List<String>,
): Confidence {
    val reasons = mutableListOf<ConfidenceReason>()

    val skipped = gates.filter { it.status == GateStatus.SKIPPED }
    if (skipped.isNotEmpty()) {
        reasons += ConfidenceReason(
            "skipped-gates",
            "${skipped.size} ${plural(skipped.size, "gate")} did not run " +
                "(${skipped.joinToString(", ") { it.gate }}) — those checks were not enforced this run",
            ConfidenceWeight.MAJOR,
        )
    }

    if (freshness?.stale == true) {
        reasons += ConfidenceReason(
            "stale-reports",
            "ingested reports span ${freshness.spreadMinutes} min in age " +
                "(tolerance ${freshness.toleranceMinutes}) — some numbers may predate the latest build",
            ConfidenceWeight.MINOR,
        )
    }

    if (notUnderstood.isNotEmpty()) {
        reasons += ConfidenceReason(
            "not-understood",
            "${notUnderstood.size} report ${plural(notUnderstood.size, "file")} found but not understood — " +
                "any signal they carry is missing from the totals above",
            ConfidenceWeight.MINOR,
        )
    }

    if (newReportLabels.isNotEmpty()) {
        reasons += ConfidenceReason(
            "new-report",
            "${newReportLabels.size} report ${plural(newReportLabels.size, "source")} not yet in the " +
                "baseline (${newReportLabels.joinToString(", ")}) — re-baseline to vouch for them",
            ConfidenceWeight.MINOR,
        )
    }

    return Confidence(reasons)
}

private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"
