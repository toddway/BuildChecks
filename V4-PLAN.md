# BuildChecks v4 — Implementation Plan

**Status:** shipped — all v1 phases (0–7) landed and released (latest v4.0.2); owner-only infra complete. Post-v1: the **4.1 run-confidence axis** (§11 item 7) has landed in code (unreleased) — a `Confidence` axis on `CheckSummary`, the new-report notice, and the `fail_on_skipped_gates`/`require_base_ref` promotions. Remaining items are the rest of the deferred post-v1 roadmap in §11 (4.2 next: the base-ref delta signals).
**Repo:** `toddway/BuildChecks` (v4 developed in place; `3.3.2` tagged as the final Gradle-plugin release)
**License:** Apache 2.0 (unchanged)

## 1. Identity

One sentence that defines the tool and gates every scope decision:

> A single command that reads standard report files (SARIF, JUnit, LCOV/Cobertura/JaCoCo),
> gates on absolute *and* baseline/diff-aware thresholds, and emits one browsable HTML report
> plus standard summary formats for whatever CI you're on.

v4 inverts the v3 architecture: the product is a **CLI**, not a Gradle plugin. Gradle becomes
one of many toolchains that invoke it. There is no server, no database, no account, no history
storage, and no HTTP code anywhere in the project.

### Explicitly out of scope (v3 features dropped)

| Dropped | Replacement |
|---|---|
| Gradle plugin (`com.toddway.buildchecks` plugin ID) | 8-line `JavaExec` snippet (§7); jar resolved from the GitHub Pages Maven repo |
| GitHub/Bitbucket status posting (Retrofit/OkHttp/RxJava/Gson stack) | `summary.json` + documented curl/CI recipes (§9) |
| History chart, `pushArtifacts`, orphan-branch git plumbing | none (deliberately cut) |
| Remote stats endpoint (`RetrofitStatsDatasource`) | none |
| `maxDuration` build-time gate | none (belongs in build-scan tooling) |
| CDN-loaded Chart.js/Moment in the report | fully self-contained HTML |
| Per-tool baseline files as the gating mechanism | central fingerprint baseline (§5) |

Also out of scope, permanently: running the analysis tools themselves, per-CI API clients,
IDE plugins, quality-gate expression DSLs.

## 2. Architecture

Single Gradle module, single published artifact `com.toddway:buildchecks` (a runnable jar with
`Main-Class`, also published as a fat jar on GitHub Releases). Package layout keeps a
library/CLI seam so a `core` split is possible later without commitment now:

```
src/main/kotlin/
  model/     Finding, TestResult, CoverageData, GateResult, IngestedFile — the domain
  parse/     one parser per format, all -> model types; format sniffing by content
  gate/      threshold evaluation, fingerprinting, baseline read/write, ratchets
  git/       changed-line extraction (the only code that shells out; fully optional at runtime)
  render/    html, markdown, json, codeclimate, sarif-merge, console table
  cli/       clikt commands, TOML config, discovery, exit codes
```

**Dependencies (complete list):** kotlin-stdlib, kotlinx-serialization-json, ktoml, clikt,
kotlinx-html. XML parsing uses the JDK's built-in SAX/DOM — no dependency. Anything beyond
this list needs a written justification in the PR.

*kotlinx-html justification:* the HTML report was the one renderer maintained as bare string
concatenation — manual escaping at every call site and tag balancing by eyeball, both silent
failure modes. kotlinx-html removes both by construction (text/attributes escaped always,
unclosed tags don't compile). It is JetBrains-maintained, pure Kotlin, and its only transitive
dependency is kotlin-stdlib, which we already ship — consistent with the "depend only on
frozen things" bet.

**Toolchain:** current stable Kotlin, JVM 17 floor, Gradle (current) with version catalog for
building the tool itself, GitHub Actions CI.

### Dependency injection (manual composition root — no framework)

- **Constructor injection everywhere.** Classes declare what they need as constructor
  parameters; nothing reaches out to a locator or a global. (v3's `Registry` pattern is
  retired.) This discipline is where all the DI benefits live, and it needs no framework.
- **One composition root.** A single plain function in `cli/` builds the entire graph —
  ~30–50 lines of constructor calls. It is the one place that knows how the system assembles,
  and it doubles as living documentation: the whole structure on one screen, in
  initialization order, greppable, debuggable with an ordinary stack trace.
- **Explicit sets, not multibinding.** Parsers, gates, and renderers are declared as literal
  ordered lists in the root (`listOf(SarifParser(), JunitParser(), …)`). Ordering is visible
  (parser sniff order can matter), "what formats exist" is answerable by reading, and a
  forgotten registration is caught immediately by the golden-file harness ("fixture claimed
  by no parser").
- **Why no framework:** the graph is ~20 short-lived, stateless objects wired once at startup
  — below the size where a framework removes more than it adds. A compiler-plugin DI
  dependency would also be the most Kotlin-upgrade-blocking item in the build, inverting the
  project's "depend only on frozen things" maintenance bet.
- **Optionality preserved:** constructor-injected code with a manual root adopts a framework
  (e.g. Metro) almost mechanically later, if the binding count grows or a second regular
  contributor arrives. Revisit then, not before.

## 2.5 Design principles & code style

These are review criteria, not aspirations — a PR that violates them gets changed.

**Coupling & cohesion (the dependency rules):**
- `model/` is the only shared vocabulary. `parse/` produces it, `gate/` and `render/` consume
  it; parsers know nothing about gates, renderers know nothing about parsing or git.
- `git/` is the only package that shells out, and everything downstream treats its output
  (changed-line sets) as plain data — the rest of the tool must work when git is absent.
- `cli/` depends on everything; nothing depends on `cli/`. Config types cross the boundary as
  plain values, not as clikt/ktoml types.
- A package earns its existence by having one reason to change (formats change → `parse/`;
  gate policy changes → `gate/`; presentation changes → `render/`).

**SOLID, applied pragmatically:**
- Interfaces exist only where variation actually exists today: `ReportParser` (7 variants),
  `Renderer` (6 variants), `Gate` (4 variants). A concept with one implementation is a class,
  not an interface-plus-class pair — that's where `Impl` suffixes come from, and both are
  banned. Extension points appear when the second implementation does, not before.
- Open/closed with explicit registration: new format/output/gate = one new class plus one
  line in the composition root's list. Existing code never changes.
- Liskov kept honest by the shared golden-test harness: every `ReportParser` runs through the
  same contract suite.

**Naming — say what it is in the domain:**
- `JacocoParser`, `LcovParser`, `FingerprintBaseline`, `ChangedLineCoverageGate`,
  `RatchetGate`, `HtmlReport`, `MarkdownSummary`, `ConsoleSummary`, `ReportDiscovery`,
  `FreshnessCheck`.
- Banned suffixes/words: `Impl`, `UseCase`, `Manager`, `Helper`, `Util(s)`, `Service`,
  `Handler`, `Processor` (v3's `GetLintSummaryUseCase` style is explicitly retired). If a
  class is hard to name without them, the abstraction is wrong — fix the design, not the name.

**Clarity & simplicity:**
- Prefer pure functions for transforms (parse → model, model → rendered text); confine side
  effects (filesystem, process exec, exit codes) to the edges (`cli/`, `git/`).
- Small files, one concept per file; data as immutable `data class`/sealed hierarchies.
- No speculative configuration, flags, or abstraction layers. Optionality is preserved by
  keeping seams clean (model as contract, DI containment rule, severable git package) — not
  by building for hypothetical futures.
- Comments only for constraints the code can't express (spec quirks in parsers, fingerprint
  stability rationale). Code that needs narration gets rewritten instead.

## 3. Ingestion

Five modern formats plus two legacy carry-overs. Each parser is a frozen spec — this is the
low-maintenance guarantee.

| Format | Detect by | Yields |
|---|---|---|
| SARIF (2.1.0) | JSON w/ `$schema`/`runs` | findings (primary lint path: detekt, ktlint, Android Lint, ESLint, SwiftLint, Semgrep…) |
| JUnit XML | `<testsuite(s)>` root | test pass/fail/skip counts, failure messages |
| JaCoCo XML | `<report>` + jacoco DTD | line/branch coverage, per-line data |
| Cobertura XML | `<coverage>` root | coverage (also covers coverage.py, coverlet/.NET) |
| LCOV | `TN:`/`SF:` records | coverage (JS/TS, Swift) |
| Checkstyle XML (legacy) | `<checkstyle>` root | findings (also emitted by many tools incl. kotlinter) |
| CPD XML (legacy) | `<pmd-cpd>` root | duplication findings (clone pairs) |

Rules:
- **Sniff content, never trust filenames.** Discovery feeds candidate files; parsers claim them.
- Unrecognized files are listed in output as "found but not understood" — no silent skips.
- Every parser ships with golden-file tests using real reports (fixtures harvested from the
  a real Android project build and canonical samples per ecosystem).

### Discovery (zero-config)

With no config file, scan from CWD: `**/build/reports/**`, `**/build/test-results/**`,
`**/target/site/jacoco/**`, `coverage/**`, `**/lcov.info`, excluding `**/node_modules/**` and
the tool's own output dir. Print what was found, what was ingested, what was skipped and why.
Config only overrides defaults.

### Freshness (replaces v3's BuildEventService concern)

The summary lists every ingested file with its modification age. Emit a prominent warning when
ingested files disagree significantly in age (e.g., newest and oldest differ by more than
`freshness_tolerance`, default 15 min) — the signature of orphaned reports from deleted
modules/variants or a partially-run build. Warning only; never a gate.

## 4. Gating stack

Evaluated in this order; any failure → exit code 1 (config/IO errors → exit code 2):

1. **Changed-line coverage** (diff-aware, coverage only): changed lines in
   `git diff <base>...HEAD` must be covered ≥ `min_changed_line_coverage`.
   Base ref resolution: `--base-ref` flag → `base_ref` config → `GITHUB_BASE_REF` env →
   skip gate with a visible notice (never fail when git/base is unavailable — the tool must
   work in non-git contexts).
2. **No new findings** (fingerprint baseline, all tools): every finding absent from the
   committed baseline fails the gate. See §5.
3. **Aggregate ratchets** (attribution-free backstop): total finding count must not exceed
   the baseline's recorded total; total coverage % must not fall below the baseline's recorded
   % minus `coverage_tolerance` (default 0.1). Catches deleted tests and any fingerprint miss.
4. **Absolute floors** (optional): `min_coverage_percent`, `max_errors`, `max_warnings`,
   `max_test_failures` (default 0 when JUnit reports are present).
5. **Missing expected reports** (origin-presence, severable): every `(origin, kind)` recorded
   in the baseline manifest must have a matching ingested report this run; absentees fail the
   gate. Skips with a notice when the baseline predates the manifest. See §5.5.

Thresholds are severity-aware where applicable. No expression language — named keys only.

## 5. Fingerprint baseline

The single mechanism that replaces detekt-baseline.xml, checkstyle suppressions, CPD baseline
XML, etc. Content-based, so it survives line shifts and unrelated edits.

- **Fingerprint:** `hash(toolId + ruleId + normalizedViolatingSource + contextHash)` where
  normalization strips whitespace/indentation and contextHash is a small hash of the
  surrounding lines. File path is recorded for display but **not** part of the hash
  (rename tolerance); an occurrence index disambiguates identical snippets.
- **Clone findings (CPD):** fingerprint = hash of the duplicated fragment's normalized token
  stream + token count. A new clone of old code produces a new fingerprint → correctly gated.
- **Baseline file:** committed, sorted, one finding per line, human-readable and diffable:
  `fingerprint  tool  rule  path:line  first-8-words-of-message`. Header records totals used
  by the ratchets (finding count, coverage %) and, from format v2, the origin manifest (§5.5).
  Re-baselining in a PR is the escape hatch and must be reviewable at a glance.
- **Known imperfection (documented):** heavily rewritten code changes its hash, so an old
  violation can resurface as "new." Acceptable — the code was rewritten — and the fix is a
  visible re-baseline in the same PR.
- **False positives** do not belong in the baseline: use in-code suppressions
  (`@Suppress`, ktlint-disable, etc.). Baseline = legacy debt only.

## 5.5 Origins & missing-report detection

The baseline also answers *"is every report that used to be here still here?"* — catching a
check that was silently disabled or a source that stopped emitting its report. That regression
otherwise reads as an *improvement*: fewer findings, ratchet satisfied, no warning. Freshness
(§3) can't see it — a report that stopped being generated leaves no stale file to flag; it is
simply absent.

- **Origin (derived, no config, no domain entity):** each ingested report's source group,
  computed from its path as the prefix before the build-output marker discovery already keys
  on (`build/`, `target/`, `coverage/`, `lcov.info`). `services/auth/build/reports/…` → origin
  `services/auth`; root or aggregated reports → the root origin. A single-module repo collapses
  to one origin, so the feature is inert where the pattern is absent. The term is toolchain-
  agnostic on purpose — "origin" spans module / project / package / workspace / crate. It is a
  computed property of the ingested file, not a new package, config key, or model entity.
- **Presence manifest:** the baseline gains a sorted `(origin, kind)` manifest recording every
  report present at snapshot time, where `kind` is the producing tool when the report carries
  findings (detekt, eslint, checkstyle, cpd…) and otherwise the coverage/test format (jacoco,
  cobertura, lcov, junit). Baseline format bumps `v1` → `v2`; readers accept both.
- **Origin-presence gate (severable):** any `(origin, kind)` in the manifest with no matching
  ingested report this run fails the gate, naming exactly what is missing and where. Severable
  like changed-line coverage — starts warn-only, promoted to failing once the manifest
  stabilizes; skips with a notice against a pre-v2 baseline that has no manifest. Intentional
  removal is accepted exactly as new findings are: a visible re-baseline in the same PR.
- **Relationship to freshness:** freshness catches *present-but-stale*; this catches
  *expected-but-absent*. Together they close the orphaned/partial-build family §3 names.
- **Known limitation (documented, not chased):** two reports sharing both an origin *and* a
  kind (e.g. two JaCoCo files under one origin) are not individually distinguished — losing one
  while the other remains is invisible. Same redundancy blind spot as the ratchets; full-path
  keying would catch it but churns on every file rename. The report `log`s per-origin source
  counts so a 2→1 drop is at least visible.
- **Deferred polish:** per-origin grouping in the HTML/console report (v3 had this, likewise
  auto-derived by path prefix) reuses the same origin key. Now secondary — the gate automates
  the manual scan that made it valuable — so it lands only if the flat report grows unwieldy.

## 6. CLI surface & config

```
buildchecks check      # ingest -> gate -> render -> exit code   (default command)
buildchecks baseline   # ingest -> snapshot baseline file
Flags: --config <path>  --base-ref <ref>  --output-dir <path>  --open  --verbose
```

`buildchecks.toml` at repo root (all keys optional; empty file ≡ no file):

```toml
[reports]
paths = ["**/build/reports/**", "coverage/lcov.info"]   # globs; default = discovery set
output_dir = "build/reports/buildchecks"
freshness_tolerance_minutes = 15

[gates]
min_changed_line_coverage = 80
max_new_findings = 0
ratchet = true                    # totals must not regress vs baseline
coverage_tolerance = 0.1
min_coverage_percent = 52.0       # optional absolute floor
max_errors = 0
max_warnings = 1000

[git]
base_ref = "origin/dev"
baseline_file = "buildchecks-baseline.txt"
```

Env-var interpolation: `"${VAR}"` in string values. Tokens/secrets never appear in config —
v4 has no feature that needs one.

## 7. Outputs

All written to `output_dir` every run:

- **`index.html`** — self-contained (inline CSS/JS, no CDN). Header: gate results + totals.
  Body: filterable/sortable findings table (severity, tool, file, new-vs-baselined),
  test failures section, coverage summary, per-directory drill-down links into each tool's
  own copied HTML report (v3's best feature, kept: tool report dirs are copied under
  `output_dir` and linked). Footer: ingested-files list with freshness ages.
- **`summary.md`** — Markdown gate summary for `$GITHUB_STEP_SUMMARY` / PR comments.
- **`summary.txt`** — single-line gate headline BuildChecks composes for a commit-status
  description (posted verbatim, no jq); required/advisory-aware once roadmap #6 lands.
- **`summary.json`** — small, stable, versioned (`schemaVersion`): overall pass/fail,
  per-gate results, coverage %, finding counts. The scripting contract.
- **`findings.json`** — full finding/test/coverage model for power users.
- **`codeclimate.json`** — GitLab MR widget format.
- **`merged.sarif`** — all ingested SARIF combined, for GitHub code-scanning upload.
- **Console** — compact summary table (picnic-style, hand-rolled) + gate lines, always printed.

## 8. Gradle integration (no plugin — documented snippet)

```kotlin
// root build.gradle.kts
repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }  // Pages Maven repo
val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }

tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
    mainClass.set("buildchecks.cli.MainKt")   // classpath launch; JavaExec never reads Main-Class
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

Why this is cache-safe (README-level explanation, replacing BuildEventService):
- The task declares no outputs → Gradle never marks it UP-TO-DATE; it runs every invocation.
- Upstream UP-TO-DATE/FROM-CACHE tasks are harmless by Gradle's contract: skipped means the
  report files on disk already match current sources; restored-from-cache means they were
  just materialized. Either way the files read are correct for the current code.
- Orphaned files from removed modules are the real residual risk → freshness warnings (§3).

Equivalent snippets documented for Maven (`exec-maven-plugin`), npm scripts, Makefile.

## 9. CI recipes (docs, not code)

- **GitHub commit status from any CI** (Bitrise etc.): ~6-line `curl` to
  `POST /repos/{owner}/{repo}/statuses/{sha}` reading state/description from `summary.json`
  via `jq`. Token lives in CI secrets.
- **GitHub Actions:** exit code → job status; `cat summary.md >> $GITHUB_STEP_SUMMARY`;
  stock sticky-comment action for PR comments; stock `upload-sarif` action for code scanning.
  Plus the first-party action shim (§11).
- **GitLab:** declare `codeclimate.json` as a `codequality` artifact → native MR widget.
- **Bitbucket:** curl to the build-status endpoint.
- **Architecture rules (any ecosystem):** Konsist/ArchUnit (JVM), dependency-cruiser (JS),
  import-linter (Python) run as tests and emit JUnit XML — already parsed, so layering rules
  gate today with no new code. Document as a supported pattern; distinguishing it as its own
  category is a 4.1 enhancement (post-v1 roadmap #2).
- **Recipe pages (5):** Gradle+detekt+JaCoCo, npm+ESLint+Jest, Python+Ruff+pytest-cov,
  .NET+coverlet, Swift+SwiftLint. Each proves the platform-agnostic claim end to end.
  The Swift page must be the *complete* iOS story, not lint-only: SwiftLint → SARIF (findings),
  slather → Cobertura or lcov (coverage), and xcbeautify → JUnit (tests). All three formats are
  already parsed, so iOS integration is converter glue in the build — no engine work. (This is
  why a native `.xcresult` parser is deliberately *not* pursued: `.xcresult` is a proprietary,
  version-unstable directory bundle readable only via `xcresulttool`, which would violate the
  no-running-tools and outside-world rules and add a macOS/Xcode-version dependency — for no gain
  over the converter recipe.)

## 10. Validation testbed

A real Android project (anonymized here) is the continuous acceptance test. Each phase ends
with a run against its real build reports; phase 6 runs v4 side-by-side with v3.3 and reconciles
coverage %, violation counts, and report content. Migration notes (written during phase 6):

- Replace the `buildChecksPlugin` dependency and `com.toddway.buildchecks` application in
  `gradle-plugins` with the JavaExec snippet inside the existing `checks` lifecycle wiring.
- **Remove the hardcoded GitHub token fallback in `ChecksPlugin.kt` (and revoke it).** v4 needs
  no token; the Bitrise status post moves to the curl recipe with a secret-stored token.
- Migrate per-tool baselines (detekt/checkstyle/CPD) to one `buildchecks baseline` snapshot;
  delete the per-tool files and the custom CPD-baseline code; set tools to emit SARIF where
  supported (detekt, Android Lint) and keep `ignoreFailures = true` everywhere.
- Test-duration reporting stays in the project's convention plugins (project-side, out of scope here).

## 11. Phases

| # | Phase | Deliverable / acceptance | Est. |
|---|---|---|---|
| 0 | Repo reset | Tag `3.3.2`; clear v3 build/CI/source from main (history preserved); fresh Kotlin/Gradle scaffold incl. composition-root skeleton, CI, golden-test harness, this plan committed | ½ session |
| 1 | Model + parsers | 7 parsers → unified model; content sniffing; golden tests green against the real project + per-ecosystem fixtures | 2 sessions |
| 2 | Baseline + gates | Fingerprinting, baseline read/write (`baseline` verb), new-finding gate, ratchets, absolute floors; exit codes | 1 session |
| 3 | Renderers | HTML (self-contained, filterable, drill-down), Markdown, summary.json, findings.json, CodeClimate, merged SARIF, console table, freshness warnings | 1–2 sessions |
| 4 | CLI | clikt commands, TOML config + env interpolation, zero-config discovery, `--open`; runnable jar + fat jar | 1 session |
| 5 | Changed-line coverage | Diff-hunk parsing, line mapping vs JaCoCo/Cobertura/LCOV line data, graceful skip w/o git; unit tests incl. renames/no-data files | 1–2 sessions |
| 6 | Project validation | Side-by-side vs v3.3 on the real project repo; migration notes; fix discrepancies; Gradle/Maven/npm/Make snippets in docs | 1–2 sessions |
| 6.5 | Origins & missing-report gate | Origin derived from path; baseline v2 `(origin, kind)` manifest (`baseline` writes it, reader accepts v1+v2); severable origin-presence gate with graceful skip on pre-v2 baselines; per-origin source counts in report; tests incl. single-origin collapse, multi-origin drop, intentional-removal re-baseline | 1 session |
| 7 | Release 4.0.0 | GitHub Pages Maven repo + Homebrew formula + install script, GitHub Releases fat jar, first-party GitHub Action shim, README rewrite, recipe pages, CI recipes | 1–2 sessions |

**All phases complete and released** (latest v4.0.2). The one-time owner-only infra is
done: GitHub Pages serves the `gh-pages` branch and release tags have been cut. (Action
Marketplace listing is optional and not required for distribution; the pipeline is
`GITHUB_TOKEN`-only — no Sonatype/PGP.)

### Post-v1 roadmap (explicitly deferred)

> **Immediate next focus (agreed 2026-07-24): item 7, the run-confidence axis.** Chosen over
> the other deferred items because it is the least-explored and highest-leverage direction: it
> reframes the green/red verdict around *how much a pass is worth*, and it unifies a scatter of
> would-be one-off gates (skipped gates, stale/unparsed reports, loosened baselines, moved
> thresholds) under one orthogonal property. Start with the 4.1 (no-base-ref) slice below.

1. **Mutation-testing signal (PIT), targeted for 4.1.** New parser for PIT `mutations.xml`
   (flat, stable XML; JDK SAX/DOM, no new dependency; sniff `<mutations>` root) plus a
   `MutationResult` model type wired through `ParsedReport`/`CheckSummary`, a severable mutation
   gate (`CoverageGate` as template), and render — including surfacing the coverage-up /
   mutation-down contradiction. Golden fixture by dogfooding PIT on this repo. ~3–5 days.
   Deferred from 4.0 on purpose: it is net-new feature work, additive/non-breaking, and demand
   is better proven by real adoption (agents gaming coverage) than built speculatively.
   Follow-up within the item: scope mutation to changed files (builds on phase 5) for runtime
   sanity on large multi-module projects. Swift mutation (`muter`, JSON) is a separate parser —
   defer until an iOS team asks.
2. **Distinguish the architecture signal.** Konsist/ArchUnit layering rules already gate *today*
   because they run as tests and emit JUnit XML (ingested by `JunitParser`) — but they land as
   generic `TestResult` failures. Enhancement: tag architecture rules as their own category so
   the report can headline layering drift instead of burying it among test failures. (No parser
   needed; a 4.0 recipe/doc note should already state that layering-as-JUnit gates now.)
3. GraalVM native binaries (fallback if fiddly: keep fat jar + jbang). (Homebrew delivered in
   4.0 as a jar-wrapping formula; a native binary would later let the formula drop `openjdk`.)
4. `buildchecks export --format detekt-baseline` (IDE quiet) — generalizable per-tool later.
5. `core`/`cli` module split if embedding demand appears.
6. **Per-gate enforcement level (advisory vs required), targeted for 4.1.** Today the exit code is
   all-or-nothing: any `FAILED` gate fails the run. Teams want some gates to block a merge and
   others to be advisory (visible but non-blocking) — v3 did this by posting a separate commit-status
   context per gate, which is un-portable (each host configures branch protection differently) and
   forces granular per-gate status-posting glue. The correct home for this policy is `buildchecks.toml`,
   not per-platform branch-protection UI, so the CI side stays **one required check** everywhere.
   Design: add an `advisory` list to the `[gates]` config (referencing the stable gate names —
   `findings`, `coverage`, `test failures`, `changed-line coverage`, `expected reports`); e.g.
   `advisory = ["findings"]`. Semantics: the process exits non-zero iff an *enforced* gate is
   `FAILED`; an advisory gate still evaluates and reports but never affects the exit code. Surface it
   as an additive `enforced: bool` field on each `summary.json` gate entry (purely additive — likely
   no `schemaVersion` bump, but decide explicitly) and in `findings.json`. Keep gate classes
   unchanged/pure — they only produce `GateResult`s; enforcement is applied at aggregation in the
   composition root (`cli`) from config, and the console/HTML render advisory failures distinctly
   (a WARN marker, not red) with an in-place explanation that advisory = shown, not blocking
   ([[self-explanatory-output]]). Pairs with a small, opt-in commit-status convenience in the
   first-party Action (`action.yml with: commit-status: true`) that posts a single `buildchecks`
   status from `summary.json` — platform glue in the platform shim, engine stays agnostic (§1 holds:
   no per-CI client in the engine). Net effect: required/optional is versioned config in the repo,
   portable across GitHub/GitLab/Bitbucket/local, gated by one check — strictly simpler and more
   portable than v3's per-context model. Additive/non-breaking; deferred from 4.0 as net-new feature
   work whose exact shape is better proven by real team adoption.
7. **Run-confidence axis + change-integrity signals, targeted for 4.1–4.2.** Motivation: the
   green/red verdict answers "did the tracked metrics hold" but not "how much is that green worth."
   Every way a pass can quietly mean less than it looks — a severable gate that `SKIPPED`, a stale
   or unparsed report, a loosened baseline, a lowered threshold — reduces to one property:
   *effective checks < intended checks*, or *the ruler moved in this same PR*. Rather than enumerate
   each evasion as its own gate, add one orthogonal, always-shown **confidence** axis (a tier +
   contributing reasons on `CheckSummary`, rendered next to `passed`) that these signals feed.
   Confidence is informational and never sets the exit code by itself; individual signals may be
   *promoted* to hard gates via off-by-default `[gates]` knobs (same gate mechanism; the inverse
   direction of roadmap #6's advisory lever — enforced-vs-not). Split into two releases along the
   base-ref line, because that is the real dependency boundary (delta signals need a "before";
   point-in-time signals don't), and each release makes exactly one pass through the ~4 human-facing
   renderers + their golden fixtures — the actual cost here:

   - **4.1 (local-friendly, no base ref) — LANDED (2026-07-24):** the confidence axis itself; a
     **new-report notice** (`presentOrigins − manifest`, the inverse of `MissingReportGate` — a report
     source ingested this run but not baselined, surfaced as a notice not a failure, since adding
     coverage is legitimate); and rolling the signals that *already exist* — skipped gates, freshness
     (`Freshness.stale`), `notUnderstood` — into the axis. Add `failOnSkippedGates` and
     `requireBaseRef` here (off by default): their signals are already present, so promotion is
     ~1h each and there's no reason to hold them. All of this works on a bare
     `./gradlew run --args="check"` with no PR context.
     - **As built:** `model.Confidence` = a list of `ConfidenceReason(signal, summary, weight)` with a
       derived `ConfidenceLevel` (HIGH none / MEDIUM any MINOR / LOW any MAJOR); on `CheckSummary` as a
       defaulted, additive field, orthogonal to `passed` and never touching the exit code. The
       `confidence(gates, freshness, notUnderstood, newReportLabels)` builder owns wording+weights; the
       type is delta-signal-ready so 4.2 only widens the builder's inputs, not the type. **Weights:**
       skipped gates MAJOR; stale-reports, not-understood, new-report all MINOR — absolute-age
       staleness is legitimate on an unchanged module in an incremental build, so LOW is reserved for a
       gate that genuinely did not run (4.2's change-scoped freshness is the MAJOR staleness signal).
       Promotions (`fail_on_skipped_gates`, `require_base_ref`) are a post-evaluation `promotedGates()`
       step appending ordinary FAILED `GateResult`s. Rendered in all five human-facing renderers +
       `summary.json` (additive, `schemaVersion` unchanged).
   - **4.2 (needs a base ref → CI PR dogfooding):** a **baseline diff** (`git show <ref>:baseline`
     vs the on-disk baseline we gated with — the git-backed companion to changed-line coverage,
     reusing the same base-ref resolution) surfacing findings added/removed, coverage-threshold
     delta, and manifest changes; a **config diff** (`git show <ref>:buildchecks.toml`) catching a
     threshold lowered or a gate disabled in the same PR; both feeding the same axis, plus
     `failOnBaselineLoosened`. Compare against the on-disk file we *used* (HEAD-vs-working-tree
     decided deliberately). The base ref is auto-detected across CI providers as of 4.0.5.
     - **change-scoped freshness** (the strongest confidence signal, added 2026-07-24): today
       freshness is absolute age; the higher-value question is whether the *change* was actually
       measured. Map each changed file (already computed for changed-line coverage) to its origin,
       then check whether that origin produced a *fresh* report this run. "This PR touched 5
       origins; 3 produced fresh reports" is far more actionable than a per-file age chip — a pass
       on a PR whose touched modules didn't re-run is exactly the low-confidence case the axis
       exists to surface. Reuses the changed-file set (phase 5), origin derivation (§5.5), and
       `Freshness` (§3) — no new inputs. Optional promotion knob `requireChangedOriginsFresh`.

   Out of scope (holds §1): no in-report re-implementation of the git diff of the committed baseline
   file — a reviewer already sees that in the PR; confidence surfaces the *summary* of the change,
   not a second diff view. Additive/non-breaking; enforcement stays opt-in.

## 12. Risks

| Risk | Mitigation |
|---|---|
| Fingerprint instability on heavy refactors | Documented behavior; reviewable one-line re-baseline; ratchets as backstop |
| Changed-line mapping edge cases (renames, generated code, files w/o coverage data) | Severable gate (skip-with-notice); dedicated test matrix in phase 5 |
| SARIF producer variance (tools emit spec subsets) | Golden fixtures per real producer, not just spec samples |
| Origin derivation misgroups reports on non-standard layouts | Prefix rule reuses discovery's own build-output markers; degrades to the root origin; gate is severable and re-baselineable |
| Scope creep (per-CI clients, DSLs, tool-running) | §1 identity sentence is the review gate; dependency allowlist in §2 |
| v3 users surprised by direction change | 3.3.2 stays published & tagged; README migration section |
