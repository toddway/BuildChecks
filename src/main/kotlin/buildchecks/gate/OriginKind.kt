package buildchecks.gate

/**
 * One entry in the baseline's presence manifest (V4-PLAN.md §5.5): a report of `kind` was
 * present under `origin` at snapshot time. Sortable so the baseline file stays diffable.
 */
data class OriginKind(val origin: String, val kind: String) : Comparable<OriginKind> {
    override fun compareTo(other: OriginKind) =
        compareValuesBy(this, other, { it.origin }, { it.kind })
}
