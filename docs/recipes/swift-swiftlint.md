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
java -jar tools/buildchecks-4.0.0-all.jar check
```

## Notes

- xcbeautify writes `build/reports/junit.xml`; slather writes `build/reports/cobertura.xml`;
  SwiftLint writes the SARIF. All three land under the default discovery locations.
- slather can emit LCOV instead (`--llvm-cov`) if you prefer — BuildChecks parses either.
- Because every format is already standard, iOS gets the **same** gated summary as the Android
  or backend projects in a portfolio — the point of a toolchain-agnostic gate.
- Run on a macOS runner (Xcode required for `xcodebuild`); BuildChecks itself only needs a JRE.
