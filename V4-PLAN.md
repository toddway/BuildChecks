# BuildChecks v4 — Implementation Plan

**Status:** approved plan, pre-implementation
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
| Gradle plugin (`com.toddway.buildchecks` plugin ID) | 8-line `JavaExec` snippet (§7); jar resolved from Maven Central |
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

**Dependencies (complete list):** kotlin-stdlib, kotlinx-serialization-json, ktoml, clikt.
XML parsing uses the JDK's built-in SAX/DOM — no dependency. Anything beyond this list needs
a written justification in the PR.

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
  Sherwin-Williams project build and canonical samples per ecosystem).

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
  by the ratchets (finding count, coverage %). Re-baselining in a PR is the escape hatch and
  must be reviewable at a glance.
- **Known imperfection (documented):** heavily rewritten code changes its hash, so an old
  violation can resurface as "new." Acceptable — the code was rewritten — and the fix is a
  visible re-baseline in the same PR.
- **False positives** do not belong in the baseline: use in-code suppressions
  (`@Suppress`, ktlint-disable, etc.). Baseline = legacy debt only.

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
- **`summary.json`** — small, stable, versioned (`schemaVersion`): overall pass/fail,
  per-gate results, coverage %, finding counts. The scripting contract.
- **`findings.json`** — full finding/test/coverage model for power users.
- **`codeclimate.json`** — GitLab MR widget format.
- **`merged.sarif`** — all ingested SARIF combined, for GitHub code-scanning upload.
- **Console** — compact summary table (picnic-style, hand-rolled) + gate lines, always printed.

## 8. Gradle integration (no plugin — documented snippet)

```kotlin
// root build.gradle.kts
val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }

tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
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
- **Recipe pages (5):** Gradle+detekt+JaCoCo, npm+ESLint+Jest, Python+Ruff+pytest-cov,
  .NET+coverlet, Swift+SwiftLint. Each proves the platform-agnostic claim end to end.

## 10. Validation testbed

The Sherwin-Williams Android project is the continuous acceptance test. Each phase ends with a
run against real SW build reports; phase 6 runs v4 side-by-side with v3.3 and reconciles
coverage %, violation counts, and report content. SW migration notes (written during phase 6):

- Replace the `buildChecksPlugin` dependency and `com.toddway.buildchecks` application in
  `gradle-plugins` with the JavaExec snippet inside the existing `checks` lifecycle wiring.
- **Remove the hardcoded GitHub token fallback in `ChecksPlugin.kt` (and revoke it).** v4 needs
  no token; the Bitrise status post moves to the curl recipe with a secret-stored token.
- Migrate per-tool baselines (detekt/checkstyle/CPD) to one `buildchecks baseline` snapshot;
  delete the per-tool files and the custom CPD-baseline code; set tools to emit SARIF where
  supported (detekt, Android Lint) and keep `ignoreFailures = true` everywhere.
- Test-duration reporting stays in SW's convention plugins (project-side, out of scope here).

## 11. Phases

| # | Phase | Deliverable / acceptance | Est. |
|---|---|---|---|
| 0 | Repo reset | Tag `3.3.2`; clear v3 build/CI/source from main (history preserved); fresh Kotlin/Gradle scaffold incl. composition-root skeleton, CI, golden-test harness, this plan committed | ½ session |
| 1 | Model + parsers | 7 parsers → unified model; content sniffing; golden tests green against SW + per-ecosystem fixtures | 2 sessions |
| 2 | Baseline + gates | Fingerprinting, baseline read/write (`baseline` verb), new-finding gate, ratchets, absolute floors; exit codes | 1 session |
| 3 | Renderers | HTML (self-contained, filterable, drill-down), Markdown, summary.json, findings.json, CodeClimate, merged SARIF, console table, freshness warnings | 1–2 sessions |
| 4 | CLI | clikt commands, TOML config + env interpolation, zero-config discovery, `--open`; runnable jar + fat jar | 1 session |
| 5 | Changed-line coverage | Diff-hunk parsing, line mapping vs JaCoCo/Cobertura/LCOV line data, graceful skip w/o git; unit tests incl. renames/no-data files | 1–2 sessions |
| 6 | SW validation | Side-by-side vs v3.3 on the SW repo; migration notes; fix discrepancies; Gradle/Maven/npm/Make snippets in docs | 1–2 sessions |
| 7 | Release 4.0.0 | Maven Central publishing, GitHub Releases fat jar, first-party GitHub Action shim, README rewrite, 5 recipe pages, CI recipes | 1–2 sessions |

**Total: ~8–12 focused sessions.** User checkpoints: end of each phase (~20–30 min);
credentials/infra only you can do: Maven Central (Sonatype) keys, repo settings, Action
marketplace listing.

### Post-v1 roadmap (explicitly deferred)

1. GraalVM native binaries (fallback if fiddly: keep fat jar + jbang) → then Homebrew/mise.
2. `buildchecks export --format detekt-baseline` (IDE quiet) — generalizable per-tool later.
3. `core`/`cli` module split if embedding demand appears.

## 12. Risks

| Risk | Mitigation |
|---|---|
| Fingerprint instability on heavy refactors | Documented behavior; reviewable one-line re-baseline; ratchets as backstop |
| Changed-line mapping edge cases (renames, generated code, files w/o coverage data) | Severable gate (skip-with-notice); dedicated test matrix in phase 5 |
| SARIF producer variance (tools emit spec subsets) | Golden fixtures per real producer, not just spec samples |
| Scope creep (per-CI clients, DSLs, tool-running) | §1 identity sentence is the review gate; dependency allowlist in §2 |
| v3 users surprised by direction change | 3.3.2 stays published & tagged; README migration section |
