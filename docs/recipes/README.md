# Recipes

Each recipe is a complete, end-to-end setup for one ecosystem: run the analyzers and tests with
failures **tolerated**, emit the standard report formats, then let `buildchecks check` be the
single gate. They prove the platform-agnostic claim — the engine never changes, only the glue
that produces reports does.

| Recipe | Findings | Coverage | Tests |
|---|---|---|---|
| [Gradle & Android (detekt / Lint + JaCoCo)](gradle-detekt-jacoco.md) | detekt / Android Lint → SARIF | JaCoCo XML | JUnit XML |
| [npm + ESLint + Jest](npm-eslint-jest.md) | ESLint → SARIF | LCOV | Jest → JUnit XML |
| [Python + Ruff + pytest-cov](python-ruff-pytest.md) | Ruff → SARIF | Cobertura XML | pytest → JUnit XML |
| [.NET + coverlet](dotnet-coverlet.md) | analyzers → SARIF | Cobertura XML | JUnit XML |
| [Swift + SwiftLint (iOS)](swift-swiftlint.md) | SwiftLint → SARIF | slather → Cobertura/LCOV | xcbeautify → JUnit XML |

The golden rule in every recipe: **tools must not fail the build themselves** (`ignoreFailures`,
`|| true`, `-`, `continue-on-error`). BuildChecks reads what they wrote and makes the one
pass/fail decision, so a lint warning and a coverage drop are gated by the same policy.

A second habit worth adopting everywhere: **also emit each tool's own HTML report** next to the
machine format BuildChecks parses. BuildChecks copies that HTML into its output dir and links it
from `index.html`, so the gate summary drills down into each tool's detail (a detekt finding,
JaCoCo's line-by-line coverage). It links any HTML that sits beside the parsed file — a
same-basename `.html`, or an `html/`/`index.html` dir in the same folder. The gate works without
it; you just lose the click-through.

See also [../integration.md](../integration.md) for the bare build-tool wiring and
[../ci-recipes.md](../ci-recipes.md) for posting status from any CI.
