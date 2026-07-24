# BuildChecks

A toolchain-agnostic CLI that reads the report files your analyzers and test runners already
produce — SARIF, JUnit XML, JaCoCo/Cobertura/LCOV, Checkstyle, CPD — and turns them into **one
gated summary**: a browsable HTML report plus standard machine formats, with a single exit code
that passes or fails the build.

It runs analysis tools? No. It aggregates and **gates** their output, the same way on every
toolchain and every CI. There is no server, no account, no history database, and no code that
talks to a platform API — the whole gate is one file in your repo that runs in the same build
you run locally.

> **Upgrading from the v3 Gradle plugin?** v4 is a rewrite from a Gradle plugin into a CLI.
> The plugin is preserved at the [`3.3.2` tag](https://github.com/toddway/BuildChecks/tree/3.3.2)
> and on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.toddway.buildchecks).
> See [docs/migrating-from-v3.md](docs/migrating-from-v3.md).

## How it works

```
your tools write reports  ─►  buildchecks check  ─►  gated summary + exit code
 (SARIF, JUnit, JaCoCo…)      ingest → gate → render     (0 pass, 1 gate fail, 2 config/IO)
```

1. **Ingest** — discover report files by location, identify them by *content* (never by
   filename), parse into one unified model. Unrecognized files are listed, never silently
   skipped.
2. **Gate** — evaluate against absolute *and* baseline/diff-aware thresholds (below).
3. **Render** — write `index.html` and the machine formats to the output dir, print a console
   summary, and exit non-zero if any gate failed.

## Install & run

BuildChecks is a single self-contained jar. Pick whichever entry point fits your CI.

### GitHub Actions

```yaml
- uses: toddway/BuildChecks@v4.0.0
  with:
    args: check          # optional; this is the default
- run: cat build/reports/buildchecks/summary.md >> "$GITHUB_STEP_SUMMARY"
  if: always()
```

### Local or non-JVM (iOS/Swift, JS, Python) — install the command

Put a `buildchecks` command on your PATH. Java is the only prerequisite (Homebrew pulls it in
for you):

```bash
# Homebrew (macOS/Linux) — installs Java as a dependency
brew tap toddway/buildchecks https://github.com/toddway/BuildChecks
brew install buildchecks

# or the install script (needs a JRE already present)
curl -fsSL https://raw.githubusercontent.com/toddway/BuildChecks/main/install.sh | sh
```

Then call `buildchecks check` from your existing test command (a Fastlane lane, an npm script,
a Makefile, …).

### Any CI, or locally — the fat jar

Download `buildchecks-<version>-all.jar` from the
[latest release](https://github.com/toddway/BuildChecks/releases) and run it:

```bash
java -jar buildchecks-4.0.0-all.jar check
```

### Gradle (Android/Java/Kotlin) — resolve it, no plugin, no committed jar

```kotlin
// root build.gradle.kts
repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }  // the BuildChecks Maven repo

val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }                 // keep mavenCentral() too, for transitives

tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
    mainClass.set("buildchecks.cli.MainKt")   // JavaExec is a classpath launch; name the entry point
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

Gradle downloads and caches the jar like any other dependency — nothing lands in your repo.
Maven, npm, and Make snippets are in [docs/integration.md](docs/integration.md); complete
end-to-end recipes per ecosystem (including [Android](docs/recipes/gradle-detekt-jacoco.md#android))
are in [docs/recipes/](docs/recipes/).

## Supported report formats

Identified by content, so any tool that emits one of these works:

| Format | Common producers |
|---|---|
| SARIF 2.1.0 | detekt, ktlint, Android Lint, ESLint, SwiftLint, Semgrep, … |
| JUnit XML | any JVM/JS/Swift test runner; also Konsist/ArchUnit layering rules |
| JaCoCo XML | JVM coverage |
| Cobertura XML | coverage.py, coverlet/.NET, many others |
| LCOV | JS/TS (Istanbul/nyc), Swift (slather) |
| Checkstyle XML | kotlinter and many linters |
| CPD XML | copy-paste / duplication detectors |

## The gates

Evaluated in order; any failure exits `1` (config/IO errors exit `2`). All are optional and
off by default unless a threshold or baseline gives them something to check.

| Gate | What it checks |
|---|---|
| **changed-line coverage** | Lines changed vs a git base ref must be covered at ≥ the minimum. The base ref is auto-detected from the common CI providers (GitHub Actions, Bitrise, Bitbucket, GitLab, Jenkins, Azure) on PR/MR builds, and falls back to the remote default branch (`origin/HEAD`) locally — both noted in the output; `--base-ref`/`base_ref` override. Skips with a notice when git or a base ref isn't available. |
| **findings** | No finding absent from the committed baseline, and the total must not rise above the baseline's total. Content-fingerprinted, so line shifts and renames don't churn. |
| **coverage** | Overall line coverage stays at or above the higher of (baseline − tolerance) and a configured floor. |
| **caps** | Optional absolute maxima for errors, warnings, and test failures (test failures default to 0 when JUnit reports are present). |
| **expected reports** | Every report present at baseline time is still ingested — catches a check silently disabled or a source that stopped emitting. |

The **baseline** (`buildchecks-baseline.txt`, committed) is a single human-readable snapshot
that replaces per-tool suppression files. Accept the current state — or an intentional removal —
with `buildchecks baseline`, reviewable as a one-file diff in the same PR.

## Confidence

Passing tells you the tracked metrics held; **confidence** tells you how completely they were
actually checked. It's a second axis — `HIGH` / `MEDIUM` / `LOW` — shown next to the verdict in
every output, and it's **informational: it never changes the exit code.** It drops when a pass is
worth less than it looks:

| Signal | Effect |
|---|---|
| a gate **skipped** (e.g. no base ref for changed-line coverage) | → `LOW` |
| ingested reports **span a wide age** (a report may predate the latest build) | → `MEDIUM` |
| a report file was **found but not understood** (its signal is missing) | → `MEDIUM` |
| a report source is **not yet in the baseline** (`buildchecks baseline` to vouch for it) | → `MEDIUM` |

If you'd rather have a signal *block* rather than just inform, promote it to a hard gate with an
off-by-default knob: `fail_on_skipped_gates` turns any skipped gate into a failure, and
`require_base_ref` fails when no git base ref could be resolved.

## Outputs

Written to `build/reports/buildchecks/` every run:

- **`index.html`** — self-contained (no CDN), self-explaining: gate results, filterable
  findings, test failures, coverage, links into each tool's own copied HTML report.
- **`summary.md`** — for `$GITHUB_STEP_SUMMARY` / PR comments.
- **`summary.json`** — small, stable, versioned; the scripting contract for CI status.
- **`findings.json`** — the full model for power users.
- **`codeclimate.json`** — GitLab MR code-quality widget.
- **`merged.sarif`** — all ingested SARIF combined, for GitHub code-scanning upload.
- **Console** — a compact table plus one line per gate, always printed.

## Configuration

Zero config works: with no file, BuildChecks scans standard report locations from the working
directory. An optional `buildchecks.toml` at the repo root tunes it (all keys optional):

```toml
[reports]
paths = ["**/build/reports/**", "coverage/lcov.info"]   # default = discovery set
output_dir = "build/reports/buildchecks"
freshness_tolerance_minutes = 15

[gates]
min_changed_line_coverage = 80
max_new_findings = 0
ratchet = true
coverage_tolerance = 0.1
min_coverage_percent = 52.0
max_errors = 0
max_warnings = 1000
fail_on_skipped_gates = false           # promote any skipped gate to a failure (default off)
require_base_ref = false                # fail if no git base ref resolves (default off)

[git]
base_ref = "origin/main"                # optional; CI PR/MR builds auto-detect this
baseline_file = "buildchecks-baseline.txt"
```

String values support `${VAR}` environment interpolation. No tokens or secrets ever belong in
config — nothing in BuildChecks needs one.

## CLI

```
buildchecks check      # ingest → gate → render → exit code   (default command)
buildchecks baseline   # ingest → snapshot the baseline file
  --config <path>  --base-ref <ref>  --output-dir <path>  --open  --verbose
```

## Documentation

- [docs/integration.md](docs/integration.md) — Gradle, Maven, npm, Make snippets
- [docs/recipes/](docs/recipes/) — complete end-to-end recipes (Gradle, npm, Python, .NET, Swift)
- [docs/ci-recipes.md](docs/ci-recipes.md) — posting status from any CI; GitLab/Bitbucket
- [docs/migrating-from-v3.md](docs/migrating-from-v3.md) — upgrading from the Gradle plugin
- [MAINTAINING.md](MAINTAINING.md) — build, test, dogfood, release
- [V4-PLAN.md](V4-PLAN.md) — scope, architecture, and design rules (source of truth)

## License

Apache 2.0 — see [LICENSE](LICENSE). Copyright 2018-Present Todd Way.
