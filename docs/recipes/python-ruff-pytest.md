# Python + Ruff + pytest-cov

Findings from [Ruff](https://docs.astral.sh/ruff/) as SARIF, coverage as Cobertura XML, and
tests as JUnit XML. BuildChecks runs from the fat jar.

## Install

```bash
pip install ruff pytest pytest-cov
```

## Run the tools (failures tolerated)

```bash
mkdir -p build/reports

# findings → SARIF
ruff check --output-format sarif -o build/reports/ruff.sarif . || true

# tests → JUnit XML, coverage → Cobertura XML
pytest \
  --junitxml=build/reports/junit.xml \
  --cov=. --cov-report=xml:build/reports/coverage.xml \
  || true

# the single gate
java -jar tools/buildchecks-4.0.0-all.jar check
```

## Viewable report (drill-down)

BuildChecks links any tool HTML that sits **beside** the parsed file (see
[recipes/README.md](README.md)). For coverage drill-down, emit pytest-cov's HTML into the same
folder as its XML so the folder's `index.html` is picked up:

```bash
pytest \
  --junitxml=build/reports/junit.xml \
  --cov=. \
  --cov-report=xml:build/reports/coverage/coverage.xml \
  --cov-report=html:build/reports/coverage \
  || true
```

BuildChecks then links `build/reports/coverage/index.html` from the coverage row. Ruff has no
HTML report, so findings stay text-only in the summary.

## Notes

- `pytest-cov --cov-report=xml` writes Cobertura-format XML — the same parser used for
  coverage.py and coverlet.
- `|| true` keeps a lint finding or a failing test from stopping the script before BuildChecks
  runs; BuildChecks decides pass/fail.
- **import-linter** contracts run under pytest emit JUnit XML, so layering rules gate alongside
  the unit tests with no extra config.
- Point `paths`/`output_dir` in `buildchecks.toml` if your reports don't land under the default
  discovery locations (`build/reports/**`, `coverage/**`).
