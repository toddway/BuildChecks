# .NET + coverlet

Coverage from [coverlet](https://github.com/coverlet-coverage/coverlet) as Cobertura XML, tests
as JUnit XML, and Roslyn analyzer findings as SARIF. BuildChecks runs from the fat jar.

## Install the JUnit test logger

```bash
dotnet add package JunitXml.TestLogger
```

## Run the tools (failures tolerated)

```bash
mkdir -p build/reports

# tests → JUnit XML, coverage → Cobertura XML (coverlet's default format)
dotnet test \
  --logger "junit;LogFilePath=build/reports/junit.xml" \
  --collect:"XPlat Code Coverage" \
  --results-directory build/reports \
  || true
# coverlet writes build/reports/<guid>/coverage.cobertura.xml — discovered under build/reports/**

# Roslyn analyzers → SARIF
dotnet build -p:ErrorLog="build/reports/analyzers.sarif%3Bversion=2.1" || true

# the single gate
java -jar tools/buildchecks-4.0.0-all.jar check
```

## Notes

- `--collect:"XPlat Code Coverage"` uses coverlet's data collector; its Cobertura XML is
  ingested by the same parser as coverage.py and pytest-cov.
- `-p:ErrorLog=…%3Bversion=2.1` requests SARIF **2.1** from the compiler (`%3B` is the escaped
  `;` separating the filename from the version argument).
- `|| true` keeps a failing test or analyzer error from stopping the script before BuildChecks
  aggregates and gates.
- The coverage file lands in a GUID subfolder under `build/reports/`; the default discovery
  globs already reach it, so no `buildchecks.toml` is needed for the standard layout.
