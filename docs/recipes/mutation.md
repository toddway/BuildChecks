# Mutation testing on changed lines (PIT), kept fast

Coverage tells you a line *ran* under a test; it can't tell you the test would *notice* if the line
broke. Mutation testing answers that: it changes the line (a "mutant") and re-runs the covering
tests — a mutant that survives is a line your tests execute but don't actually check. BuildChecks
ingests [PIT](https://pitest.org/)'s `mutations.xml`, gates the kill rate on the lines a change
touched, and — when changed lines are well covered yet poorly killed — surfaces the
**covered-but-not-verified** contradiction as a named finding.

BuildChecks never runs PIT (it only reads the report), so **speed is entirely about how you invoke
PIT.** A full-project mutation run is minutes to hours; the trick that makes it a practical PR gate
is to mutate only the classes the change touched.

## Why it's slow, and the one lever that matters

PIT's cost is `mutants × covering-tests-per-mutant`. On a PR you don't care about the whole project —
you care about the diff. So scope PIT to the changed classes:

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

    // The lever: only mutate classes this change touched. Compute them from the diff and pass them
    // in; an empty set means "nothing changed" — skip the run entirely.
    val base = System.getenv("GITHUB_BASE_REF")?.let { "origin/$it" } ?: "origin/main"
    val changed = providers.exec {
        commandLine("git", "diff", "--name-only", "$base...HEAD")
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

Two more levers, both optional:

- **Incremental history** — `withHistory = true` (or `historyInputLocation`/`historyOutputLocation`
  persisted across CI runs). PIT then skips mutants whose class + coverage + mutator are unchanged.
- **Fewer, higher-signal mutators** — the `DEFAULTS` group, not `ALL`; raise `threads`.

Kotlin note: PIT mutates Kotlin bytecode fine; for cleaner results the
[Arcmutate Kotlin plugin](https://docs.arcmutate.com/docs/kotlin.html) filters compiler-generated
mutants (commercial). It isn't required.

## Gate it with BuildChecks

BuildChecks intersects PIT's per-line mutants with the same git diff, so it gates the kill rate on
**changed lines only** — fair even if PIT happened to mutate more:

```toml
# buildchecks.toml
[gates]
min_changed_line_coverage = 70   # changed lines must be run by a test …
min_changed_line_mutation = 60   # … and the tests must actually catch mutations of them
```

Both gates are **severable**: no diff, no base ref, or no `mutations.xml` → they skip with a notice
and never fail the build (and never lower confidence). So you can adopt gradually — wire PIT on one
module first. Setting a minimum to `0` keeps the report section and the contradiction while never
blocking.

## Run

```bash
./gradlew test pitest              # test writes JUnit XML; pitest writes mutations.xml
./gradlew buildchecks              # or `check`, if finalized with it
```

On a PR, pass the target branch so both changed-line gates measure against it — on GitHub Actions
`GITHUB_BASE_REF` is picked up automatically; elsewhere use `--base-ref` or `git.base_ref`.

## What you'll see

When a change's tests raise coverage without actually asserting on the new behaviour, the summary
leads with:

> 🕳️ **Covered but not verified:** changed lines are 92% covered yet only 45% of their mutants are
> killed — the tests run this change without asserting on it.

and the HTML report lists the surviving mutants per file, worst-first, so you can jump straight to
the lines whose changed behaviour no test pins down.

## What a surviving mutant does — and doesn't — tell you

Read a surviving mutant as *"no test that PIT ran would notice if this line's behaviour changed"* — a
prompt to look, not a proven defect. Three things keep it honest:

- **Mutation runs on a green suite.** PIT requires all tests to pass before it starts, so it never
  runs on a broken build. If your change actually breaks a test, that's an ordinary failure caught by
  the `test failures` gate, *upstream* of mutation. Mutation doesn't tell you whether the change is
  correct — it tells you how *sensitive* the passing tests are around the lines you touched.
- **A mutant is only exposed to the tests PIT ran.** PIT runs the tests that *cover* the mutated line,
  drawn from `targetTests`. If a line's behaviour is only asserted by an integration test, a test in
  another module, or a suite you excluded, PIT won't run it against the mutant — and the mutant
  **survives even though something does check it**. Point `targetTests` as broadly as your runtime
  budget allows, so "survived" means *unverified*, not merely *unseen*.
- **Some survivors are unkillable.** A mutant that doesn't change observable behaviour (an *equivalent*
  mutant) survives no matter how good the tests are. Expect a small irreducible floor of survivors;
  don't chase 100%.

Test code itself is never measured: coverage instruments `src/main`, and PIT only mutates
`targetClasses`, so changing or adding a test never moves the changed-line numbers *directly*. What
moves them is whether that test **kills mutants on the production lines it exercises** — which is the
behaviour you actually want to reward.

## Notes

- **Full/nightly runs:** you can still run PIT across the whole project on a schedule for a broad
  view; BuildChecks will read whatever `mutations.xml` it finds. There is currently no whole-project
  mutation *ratchet* gate — the changed-line gate is the PR signal.
- **Baseline:** the changed-line gates need no baseline entry; they measure the diff each run.
