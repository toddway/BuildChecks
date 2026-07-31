# BuildChecks — active plan

**Target:** 4.1 · **Status:** 4.0 shipped (latest v4.0.12); 4.1/4.2 confidence axis landed, unreleased.
**Convention:** this file holds only what is *open*. When a cycle closes it moves to
`docs/archive/` and a fresh `PLAN.md` replaces it — so the plan never accumulates.
**History:** [docs/archive/v4-plan.md](docs/archive/v4-plan.md) (v4.0 → confidence axis; shipped, with the full design reasoning).

## Standing constraints

Unchanged and not up for renegotiation in this cycle. Summary in [CLAUDE.md](CLAUDE.md);
long-form in the archive §1, §2, §2.5.

- Closed dependency list: kotlin-stdlib, kotlinx-serialization-json, ktoml, clikt, kotlinx-html. Additions need written justification.
- No servers, no accounts, no history storage, no per-CI API clients, no running the analysis tools themselves.
- Constructor injection, one composition root in `buildchecks.cli`. Domain names only.
- `model` is the shared vocabulary; `parse` produces it; `gate` and `render` consume it; only `git` and `cli` touch the outside world.

## The completion bar

v4 is **done** when these three hold. Rationale in [docs/mission.md](docs/mission.md); everything
else is explicitly past the bar.

| # | Bar | State |
|---|---|---|
| 1 | **Mutation signal gates.** Without it, a change whose tests cover every line and assert nothing goes green with `Confidence` HIGH — the exact failure the tool claims to catch. | **met in code (unreleased)** — changed-line mutation gate + the covered-but-not-verified contradiction; PIT parser dogfooded on this repo (`./gradlew pitest`). Whole-project ratchet deliberately descoped (see below). |
| 2 | **Proven outside the repo that authored it.** Real projects, not demos, with report shapes and CI wiring this repo never produces. | **met** — the validation project (a production Android app, anonymized; Gradle/Android, several engineers); Shelf (multi-module KMP, GitHub Actions, base-ref diffing + commit status) |
| 3 | **Legible to a non-author.** Someone who did not write it can read the summary, see why a gate fired, and steer it. | **met** — in routine use by engineers on the validation project |

So: **one item open.** Mutation is the whole remaining cycle.

**On iOS.** A Swift consumer would add the most remaining evidence for the agnostic claim —
SwiftLint SARIF, slather LCOV, xcodebuild JUnit are producers nothing here emits. It is an
opportunity, tracked, worth pursuing. It is deliberately **not** a bar item: adoption by a team you
are not on is not yours to force, and a bar that does not move when you work is not a bar, it is a
nag. Items 2 and 3 are already satisfied by real use; iOS strengthens the evidence rather than
deciding whether the thing succeeded.

**Working rule.** Releases are a flattering proxy — cheap to move, weakly correlated with the bar.
Prefer work that moves item 1 over refinement of what already works. When the bar is met, stop and
decide deliberately; "complete" is an allowed outcome.

## 4.1 — mutation signal (bar item 1) — LANDED (changed-line only, unreleased)

Scoped deliberately to **changed lines only** (the whole-project ratchet was dropped): mutation
testing's real adoption blocker is runtime, and the only way it's practical on a PR is a diff-scoped
run. A whole-project mutation *ratchet* also doesn't compose with diff-scoped PIT runs — a PR run
that mutates three classes can't be compared to a whole-project baseline score — so building it would
have been a gate no one could keep green. What shipped:

1. **Parser** — `PitParser`, sniffs the `<mutations>` root; JDK SAX, no new dependency. `mutations.xml`
   golden fixture is a real `./gradlew pitest` run on this repo.
2. **Model** — `MutationData` (file → per-line `Mutation`) through `ParsedReport`/`merged()`, and
   `ChangedLineMutation` mirroring `ChangedLineCoverage` (diff intersection via the same suffix match).
3. **Gate** — `ChangedLineMutationGate`, the twin of `ChangedLineCoverageGate`: an absolute floor on the
   kill rate of mutants on changed lines (`gates.min_changed_line_mutation`), severable, skips with a
   notice, and (like changed-line coverage) does not lower confidence when it skips.
4. **Render** — the **covered-but-not-verified `Contradiction`** as a named finding: high changed-line
   coverage + low changed-line mutation kill rate on the same diff, called out in the console, the
   Markdown PR comment, and a red HTML callout that leads the report. Plus a "surviving mutants on
   changed lines" section (worst-first) mirroring the changed-coverage section.
5. **Build** — `info.solidsoft.pitest` wired as a build-time (opt-in `pitest` task) dogfool, not on
   the `test`/`check` path.

Done alongside the code: `docs/recipes/mutation.md` — the practical PR-scoped PIT wiring (git diff →
`targetClasses`, incremental history) plus a "what a surviving mutant does/doesn't tell you" note.

**`buildchecks changed-files` helper verb — LANDED (unreleased).** The read-only verb that hands a
consumer the exact diff BuildChecks gates, so a diff-scoped PIT run mutates the same set BuildChecks
measures (same base-ref resolution order — otherwise a consumer's own `git diff` could resolve a
different base and mutate one set while gating another). `runChangedFiles` in `cli/Verbs.kt` reuses
the existing private `resolveBaseRef`; the `changed-files` clikt subcommand in `Main.kt` prints
repo-relative new-side paths (sorted) to **stdout** and every diagnostic to **stderr**, so the list
pipes cleanly. Exit 0 on a diff (empty diff → nothing printed, still 0, so a build step no-ops when
nothing changed); exit 2 when no base ref resolves or git can't diff (a loud targeting failure, not
a silent wrong-set mutation). Tool-agnostic — emits files, leaves files→classes→targeting to the
consumer. Wiring documented in `docs/recipes/mutation.md`. Inherits the committed-only diff
limitation from `GitDiff`.

**Still open / deferred out of this scope:**
- Whole-project mutation ratchet gate + a baseline `# mutation:` score, *if* a consumer wants to gate
  full/nightly runs. Requires the baseline-loosened delta to learn about mutation too.
- **Recommended next mutation parser — the `mutation-testing-elements` JSON schema** (Stryker's
  cross-language standard). One parser unlocks StrykerJS (JS/TS), Stryker.NET (C#), Stryker4s (Scala),
  and Mull (C/C++) at once — the SARIF-of-mutation, higher leverage than any single-tool parser. When
  adding it, formalize the status→`detected` policy: Killed/Timeout → detected, and exclude
  non-viable/compile-error mutants from the denominator (and fix `PitParser` counting PIT's
  `NON_VIABLE` as an undetected survivor — the same policy).
- Swift mutation (`muter`, separate per-tool parser) — wait for an iOS team to ask.

## Deferred (past the bar)

Not scheduled. Each needs its own justification before it starts.

- Per-gate enforcement level (advisory vs required) via a `[gates] advisory = [...]` list.
- Tag architecture rules as their own category so layering drift headlines instead of landing as generic `TestResult` failures.
- Agent-facing output format — the gate as a step inside an authoring loop rather than a post-hoc report. Fast local startup (GraalVM) is a precondition.
- GraalVM native binaries; would let the Homebrew formula drop `openjdk`.
- `buildchecks export --format detekt-baseline`.
- `core`/`cli` module split, if embedding demand appears.
- Additional parsers beyond the seven; additional renderers; history or longitudinal views.
