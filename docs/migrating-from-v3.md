# Migrating from v3 (Gradle plugin) to v4 (CLI)

v3 was a Gradle plugin (`com.toddway.buildchecks`, preserved at tag `3.3.2`); v4 is a
standalone CLI that reads report files your tools already write. Migration is: delete the
plugin application, wire the [JavaExec snippet](integration.md#gradle) into your existing
check lifecycle, and move settings into `buildchecks.toml`.

## Setting map

| v3 (`BuildConfig`) | v4 |
|---|---|
| `reports = "<dirs>"` | `[reports] paths = [<globs>]`, or omit for zero-config discovery |
| `artifactsPath` | `[reports] output_dir` (default `build/reports/buildchecks`) |
| `minCoveragePercent` | `[gates] min_coverage_percent` |
| `maxLintViolations` | `[gates] max_errors` / `max_warnings`, plus the findings gate's new-vs-baseline check |
| `baseUrl`, `authorization`, `buildUrl` (status posting) | removed — post from CI with `curl` + `jq` over `summary.json` (see CI recipes) |
| per-tool baselines (detekt/checkstyle/CPD) | one `buildchecks baseline` snapshot; delete the per-tool files |
| `postChecks` / `printChecks` tasks | `buildchecks check` (console summary always prints) |

Keep `ignoreFailures = true` (or the equivalent) on every analyzer and test task: tools
report, BuildChecks gates.

## Behavior changes to expect

- **Coverage reads higher, ~3–4 points on Kotlin projects.** v3 summed JaCoCo *method-level*
  `LINE` + `BRANCH` counters, which double-counts source lines shared by compiler-generated
  methods (inlined lambdas, default arguments) and folds branch counts into the same
  percentage. v4 counts unique source lines, matching JaCoCo's own report totals. Re-check
  your floor against the new number before carrying it over.
- **Tests are counted.** v3 never read `build/test-results`; v4 ingests JUnit XML and gates
  on `max_test_failures`.
- **One report format per tool.** v4 sniffs content and ingests every report it understands.
  A tool configured to emit the same findings twice (detekt writes both a checkstyle XML and
  a SARIF file by default) gets counted twice. Disable one format or scope `[reports] paths`
  to the other.
- **Android Lint's proprietary XML is not parsed.** Enable lint's SARIF output
  (`android.lint.sarifReport = true`); SARIF is the preferred interchange format everywhere
  it exists (detekt, SwiftLint, ESLint via formatter, Android Lint).
- **Orphaned reports are flagged, not silently mixed in.** v3 only saw declared subprojects.
  v4's zero-config walk sees whatever is on disk — a module removed from the build but whose
  `build/` directory survives skews totals. The freshness warning ("reports differ in age…")
  is the tell; delete the stale directory or scope `paths` globs to live modules.

## Validation: side-by-side on a real Android project

Both versions were run against the same on-disk reports (45 modules, 34 JaCoCo XML, 92
checkstyle-format XML, 644 JUnit XML, 44 SARIF, 1 CPD) on the same day:

| | v3.3 `printChecks` | v4 `check` |
|---|---|---|
| Coverage | 57.53% (47,648 method-level lines+branches) | 61.06% (31,380 unique lines) |
| Violations / findings | 0 (max 0) | 0 errors, 0 warnings, 0 info |
| Tests | not counted | 4,978 (0 failed, 20 skipped) |
| Coverage floor 52% | pass | pass |

The coverage delta is fully accounted for by the counting-semantics change above: applying
v3's method-level lines+branches arithmetic to the same 34 files independently reproduces
57.53%/47,648 exactly, and unique-line arithmetic reproduces 61.06% exactly. Zero-config
discovery additionally surfaced an orphaned module directory (deleted from
`settings.gradle.kts`, stale reports still on disk) that v3 silently ignored — excluded via
`paths` globs after v4's freshness warning flagged it.

### Project-specific steps

- Replace the `buildChecksPlugin` dependency and `com.toddway.buildchecks` application in
  `gradle-plugins` with the JavaExec snippet inside the existing `checks` lifecycle wiring.
- **Remove the hardcoded GitHub token fallback in `ChecksPlugin.kt` and revoke that token.**
  v4 needs no token; the Bitrise commit-status post moves to the curl recipe with a
  secret-stored token.
- Migrate per-tool baselines (detekt/checkstyle/CPD) to one `buildchecks baseline` snapshot;
  delete the per-tool baseline files and the custom CPD-baseline code.
- Detekt already emits SARIF alongside its checkstyle XML — keep the SARIF, drop the XML
  report (or exclude it via `paths`) to avoid double counting.
- Enable Android Lint's SARIF report; today `lint-results-debug.xml` is empty so nothing is
  lost, but v4 will not read the proprietary XML when findings appear.
- Delete the orphaned top-level `registration/` build outputs (module now lives at
  `feature/registration`).
- Test-duration reporting stays in the project's convention plugins (project-side, out of scope).

Working config used for the validation run:

```toml
[reports]
paths = [
    "app/build/reports/**",
    "app/build/test-results/**",
    "{common,data,feature,legacy}/*/build/reports/**",
    "{common,data,feature,legacy}/*/build/test-results/**",
]
output_dir = "build/reports/buildChecks"

[gates]
min_coverage_percent = 52.0
```
