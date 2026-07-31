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
 * V4-PLAN §11 4.2) are additional [ConfidenceReason]s on this same axis, fed by a [ChangeDelta] —
 * they widened the builder's inputs, not this type.
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
 *
 * [delta] carries the base-ref delta facts (V4-PLAN §11 4.2), likewise pre-computed at the cli/git
 * boundary; null when no base ref resolved, in which case no delta signal contributes.
 */
fun confidence(
    gates: List<GateResult>,
    freshness: Freshness?,
    notUnderstood: List<String>,
    newReportLabels: List<String>,
    delta: ChangeDelta? = null,
): Confidence {
    val reasons = mutableListOf<ConfidenceReason>()

    // A skipped gate normally means an intended check didn't run — except the changed-line coverage
    // and mutation gates, which are severable/informational by design and skip whenever there is
    // simply nothing to measure (no changed source, no base ref). That's not a check failing to run,
    // so it must not drop confidence; enforce those via min_changed_line_* / requireBaseRef instead.
    // Genuine gaps in the enforcing gates (coverage with no data, findings with no baseline) still count.
    val severable = setOf(CHANGED_LINE_COVERAGE_GATE, CHANGED_LINE_MUTATION_GATE)
    val skipped = gates.filter { it.status == GateStatus.SKIPPED && it.gate !in severable }
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
                "(tolerance ${freshness.toleranceMinutes}) — some numbers may predate the latest build " +
                "(expected on an incremental build where unchanged modules weren't rebuilt)",
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

    // Base-ref delta signals (4.2). Each is MAJOR: it means either the change wasn't re-measured, or
    // the ruler moved in this same change — both drop a green verdict to LOW until a reviewer looks.
    if (delta != null) deltaReasons(delta, reasons)

    return Confidence(reasons)
}

private fun deltaReasons(delta: ChangeDelta, reasons: MutableList<ConfidenceReason>) {
    val stale = delta.staleChangedOrigins
    if (stale.isNotEmpty()) {
        reasons += ConfidenceReason(
            "changed-origins-stale",
            "${stale.size} changed ${plural(stale.size, "origin")} produced only a stale report this run " +
                "(${stale.sorted().joinToString(", ") { displayOrigin(it) }}) — may not reflect this change",
            ConfidenceWeight.MAJOR,
        )
    }

    if (delta.baselineLoosened) {
        val parts = buildList {
            if (delta.baselineFindingsAccepted > 0)
                add("${delta.baselineFindingsAccepted} ${plural(delta.baselineFindingsAccepted, "finding")} newly accepted")
            delta.baselineCoverageLowered?.let { add("coverage floor down %.2f%%".format(it)) }
            if (delta.baselineReportsDropped.isNotEmpty())
                add("${delta.baselineReportsDropped.size} expected " +
                    "${plural(delta.baselineReportsDropped.size, "report")} dropped " +
                    "(${delta.baselineReportsDropped.joinToString(", ")})")
        }
        reasons += ConfidenceReason(
            "baseline-loosened",
            "baseline loosened vs the base ref: ${parts.joinToString(", ")} — " +
                "checks that would have failed at the base ref now pass",
            ConfidenceWeight.MAJOR,
        )
    }

    if (delta.configLoosened.isNotEmpty()) {
        reasons += ConfidenceReason(
            "config-loosened",
            "${delta.configLoosened.size} gate ${plural(delta.configLoosened.size, "setting")} loosened vs the " +
                "base ref (${delta.configLoosened.joinToString(", ")}) — the ruler moved in this same change",
            ConfidenceWeight.MAJOR,
        )
    }
}

// Mirror the *Gate.NAME constants (kept as literals so `model` stays off the `gate` package).
private const val CHANGED_LINE_COVERAGE_GATE = "changed-line coverage"
private const val CHANGED_LINE_MUTATION_GATE = "changed-line mutation"

private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"

/**
 * A bare "." (the root origin, cli.ROOT_ORIGIN — the catch-all for changed files that map to no
 * measured module: build scripts, docs, other non-source) is meaningless to a reader, so name it.
 * The literal is the documented root marker; this is display-only and adds no dependency on `cli`.
 */
private fun displayOrigin(origin: String) = if (origin == ".") "repository root" else origin
