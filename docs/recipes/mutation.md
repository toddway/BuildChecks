# Mutation testing on changed lines (PIT), kept fast

Coverage tells you a line *ran* under a test; it can't tell you the test would *notice* if the line
broke. Mutation testing answers that: it changes the line (a "mutant") and re-runs the covering
tests — a surviving mutant is a line your tests execute but don't actually check. BuildChecks ingests
[PIT](https://pitest.org/)'s `mutations.xml`, gates the kill rate on the lines a change touched, and
— when those lines are well covered yet poorly killed — surfaces the **covered-but-not-verified**
contradiction as a named finding.

BuildChecks never runs PIT; it only reads the report. A full-project run is minutes to hours, so the
one thing that makes it a practical PR gate is mutating **only the classes the change touched**.

## Scope PIT to the changed set

Let BuildChecks tell you what changed. `buildchecks changed-files` prints the exact paths it gates —
base ref resolved through `--base-ref` → `git.base_ref` → CI env → remote default branch. Using it
(instead of a second `git diff`) is the whole point: PIT mutates the *same* set BuildChecks measures,
against the *same* base ref, for *both* the coverage and mutation gates. One diff, one base, no drift.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    jacoco
    id("info.solidsoft.pitest") version "1.15.0"
}

pitest {
    junit5PluginVersion = "1.2.1"
    outputFormats = setOf("XML", "HTML")   // XML is gated; HTML is linked from the report
    timestampedReports = false             // stable path: build/reports/pitest/mutations.xml

    // Ask BuildChecks for the changed paths, map them to class names. Empty = nothing changed → skip.
    val changed = providers.exec {
        commandLine("buildchecks", "changed-files")   // inherits GITHUB_BASE_REF etc.; no base-ref wiring here
    }.standardOutput.asText.get().lineSequence()
        .filter { it.endsWith(".kt") && it.startsWith("src/main/") }
        .map { it.removePrefix("src/main/kotlin/").removeSuffix(".kt").replace('/', '.') }
        .toSet()

    if (changed.isNotEmpty()) {
        targetClasses = changed
        targetTests = setOf("your.root.package.*")   // let the whole suite run against the mutants
    }
}
```

`changed-files` prints paths to stdout and diagnostics to stderr, so it pipes cleanly. An empty diff
prints nothing and exits 0 (skip the run); an unresolvable base ref or git failure exits 2 with the
reason on stderr, so targeting fails loudly rather than silently mutating the wrong set. The verb is
tool-agnostic — it emits files and leaves the files→classes mapping (above) to you.

Two optional speedups: **incremental history** (`withHistory = true`, or persist
`historyInputLocation`/`historyOutputLocation` across CI runs — PIT then skips unchanged mutants) and
**fewer mutators** (the `DEFAULTS` group, not `ALL`; raise `threads`). Kotlin bytecode mutates fine;
the commercial [Arcmutate Kotlin plugin](https://docs.arcmutate.com/docs/kotlin.html) filters
compiler-generated mutants for cleaner results but isn't required.

## Gate it

```toml
# buildchecks.toml
[gates]
min_changed_line_coverage = 70   # changed lines must be run by a test …
min_changed_line_mutation = 60   # … and the tests must actually catch mutations of them
```

BuildChecks intersects PIT's per-line mutants with the same diff it handed PIT, so it gates the kill
rate on **changed lines only** — fair even if PIT mutated more. Both gates are **severable**: no
diff, no base ref, or no `mutations.xml` → they skip with a notice, never failing the build or
lowering confidence. So adopt gradually (wire PIT on one module first); a minimum of `0` keeps the
report section and the contradiction while never blocking.

```bash
./gradlew test pitest    # test writes JUnit XML; pitest writes mutations.xml
./gradlew buildchecks    # ingest → gate → render
```

On a PR, pass the target branch so both gates (and `changed-files`) measure against it — GitHub
Actions' `GITHUB_BASE_REF` is picked up automatically; elsewhere use `--base-ref` or `git.base_ref`.

## What you'll see

When a change's tests raise coverage without asserting on the new behaviour, the summary leads with:

> 🕳️ **Covered but not verified:** changed lines are 92% covered yet only 45% of their mutants are
> killed — the tests run this change without asserting on it.

and the HTML report lists surviving mutants per file, worst-first, so you can jump straight to the
lines whose changed behaviour no test pins down.

## What a surviving mutant does — and doesn't — tell you

Read a survivor as *"no test PIT ran would notice if this line changed"* — a prompt to look, not a
proven defect. Three caveats keep it honest:

- **It runs on a green suite.** PIT requires all tests to pass first, so a change that breaks a test
  is caught *upstream* by the `test failures` gate. Mutation measures how *sensitive* the passing
  tests are around your lines, not whether the change is correct.
- **A mutant only sees the tests PIT ran** (those covering the line, from `targetTests`). If the real
  assertion lives in an excluded suite or another module, the mutant survives though something *does*
  check it. Point `targetTests` as broadly as your budget allows, so "survived" means *unverified*.
- **Some survivors are unkillable** — an *equivalent* mutant changes no observable behaviour. Expect a
  small irreducible floor; don't chase 100%.

## Notes

- **Full/nightly runs:** you can still run PIT project-wide on a schedule; BuildChecks reads whatever
  `mutations.xml` it finds. There is no whole-project mutation *ratchet* gate — changed-line is the PR signal.
- **Baseline:** the changed-line gates need none; they measure the diff each run.
