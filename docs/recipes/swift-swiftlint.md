# Swift + SwiftLint (complete iOS)

The full iOS story, not lint-only. All three signals come from converter glue in the build —
no engine work, and deliberately **no `.xcresult` parser** (it's a proprietary, version-unstable
bundle readable only via `xcresulttool`, which would mean shelling out to a macOS/Xcode-specific
tool — against BuildChecks' no-running-tools rule, for no gain over these converters).

| Signal | Tool | Format |
|---|---|---|
| Findings | [SwiftLint](https://github.com/realm/SwiftLint) | SARIF |
| Coverage | [slather](https://github.com/SlatherOrg/slather) | Cobertura XML (or LCOV) |
| Tests | [xcbeautify](https://github.com/cpisciotta/xcbeautify) | JUnit XML |

## Install

```bash
brew install swiftlint xcbeautify
gem install slather

# BuildChecks itself — Homebrew pulls in Java for you, and puts `buildchecks` on your PATH
brew tap toddway/buildchecks https://github.com/toddway/BuildChecks
brew install buildchecks
```

## Run the tools (failures tolerated)

```bash
mkdir -p build/reports

# findings → SARIF
swiftlint lint --reporter sarif > build/reports/swiftlint.sarif || true

# build + test, piping the log to xcbeautify for JUnit XML
set -o pipefail
xcodebuild test \
  -scheme MyApp -destination 'platform=iOS Simulator,name=iPhone 15' \
  -enableCodeCoverage YES -resultBundlePath build/MyApp.xcresult \
  | xcbeautify --report junit --report-path build/reports || true

# coverage → Cobertura XML from the result bundle
slather coverage --cobertura-xml \
  --output-directory build/reports \
  --scheme MyApp MyApp.xcodeproj || true

# the single gate
buildchecks check
```

Wire this into whatever runs your tests — a Fastlane lane (`sh "buildchecks check"` after
`scan` + `slather`), this shell block in CI, or a `Makefile` target.

## Viewable report (drill-down)

BuildChecks links any tool HTML that sits **beside** the parsed file (see
[recipes/README.md](README.md)). Both signals here can supply it:

```bash
# SwiftLint HTML, same-basename sibling of the SARIF
swiftlint lint --reporter html > build/reports/swiftlint.html || true

# slather HTML into the same folder as its Cobertura XML, so index.html sits beside it
slather coverage --cobertura-xml --output-directory build/reports/coverage --scheme MyApp MyApp.xcodeproj || true
slather coverage --html         --output-directory build/reports/coverage --scheme MyApp MyApp.xcodeproj || true
```

BuildChecks links `swiftlint.html` next to the SARIF and `build/reports/coverage/index.html`
from the coverage row.

## Notes

- xcbeautify writes `build/reports/junit.xml`; slather writes `build/reports/cobertura.xml`;
  SwiftLint writes the SARIF. All three land under the default discovery locations.
- slather can emit LCOV instead (`--llvm-cov`) if you prefer — BuildChecks parses either.
- Because every format is already standard, iOS gets the **same** gated summary as the Android
  or backend projects in a portfolio — the point of a toolchain-agnostic gate.
- Run on a macOS runner (Xcode required for `xcodebuild`); BuildChecks itself only needs a JRE.
