# npm + ESLint + Jest

Findings from [ESLint](https://eslint.org/) as SARIF, coverage as LCOV, and Jest tests as JUnit
XML. BuildChecks runs from the fat jar (`java -jar`), so the only prerequisite is a JRE 17+ on
the runner.

## Install the report formatters

```bash
npm i -D @microsoft/eslint-formatter-sarif jest-junit
```

## package.json

```json
{
  "scripts": {
    "lint": "eslint . --format @microsoft/eslint-formatter-sarif --output-file build/reports/eslint.sarif || true",
    "test": "jest --coverage --coverageReporters=lcov || true",
    "check": "npm run lint && npm run test && java -jar tools/buildchecks-4.0.0-all.jar check"
  },
  "jest": {
    "reporters": ["default", ["jest-junit", { "outputDirectory": "build/reports", "outputName": "junit.xml" }]],
    "coverageDirectory": "coverage"
  }
}
```

## Run

```bash
npm run check
```

Jest writes `coverage/lcov.info` (discovered by default) and `build/reports/junit.xml`; ESLint
writes `build/reports/eslint.sarif`. `buildchecks check` ingests all three and gates.

## Viewable report (drill-down)

BuildChecks links any tool HTML that sits **beside** the parsed file (see
[recipes/README.md](README.md)). ESLint gives you that for free with a second, HTML-format run
whose output is the same-basename sibling of the SARIF:

```json
"lint:html": "eslint . --format html --output-file build/reports/eslint.html || true"
```

Add `npm run lint:html` to the `check` script; BuildChecks links `eslint.html` next to
`eslint.sarif`. Jest's `lcov` reporter also writes an HTML tree, but under
`coverage/lcov-report/` — not beside `coverage/lcov.info` — so it isn't linked automatically;
point Jest's HTML output into a folder alongside `lcov.info` if you want coverage drill-down.

## Notes

- The `|| true` after each tool keeps a lint error or a failing test from short-circuiting the
  pipeline before BuildChecks can aggregate them — BuildChecks makes the pass/fail call.
- **TypeScript** is identical: ESLint with `@typescript-eslint` still emits SARIF; `ts-jest`
  still emits LCOV.
- **dependency-cruiser** rules run as tests emit JUnit XML, so import-boundary violations gate
  with no extra wiring.
- Keep the jar under version control or download it in CI (`gh release download`); or use the
  GitHub Action (`toddway/BuildChecks@v4.0.0`) instead of `java -jar` on GitHub-hosted runners.
