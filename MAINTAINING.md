# Maintaining BuildChecks

Everything below uses the standard Kotlin/Gradle toolchain (JDK 17+) and the tool's own
command line — nothing else is required. Architecture, scope rules, and the phase plan live
in [V4-PLAN.md](V4-PLAN.md); read it before changing anything structural.

## Build

    ./gradlew build        # compile + run all tests
    ./gradlew assemble     # jars in build/libs/: buildchecks-<version>.jar
                           # and buildchecks-<version>-all.jar (fat, self-contained)

## Run the tests

    ./gradlew test

JUnit 5, with per-test output enabled. Every parser has golden-file tests against real tool
output committed under `src/test/resources/fixtures/` — fixtures are never hand-written; how
each was generated is recorded in its commit message. `ParserContractTest` fails when a
fixture is claimed by no parser, or by the wrong one.

## Self-check (dogfood)

The repo gates itself with its own CLI; this is the standard validation loop:

    ./gradlew test                    # writes JUnit XML + JaCoCo XML (core jacoco plugin)
    ./gradlew run --args="check"      # gates against the committed buildchecks-baseline.txt
    ./gradlew run --args="baseline"   # re-snapshot after intentional changes

## Generate an example report

Point the fat jar at any project that has supported reports on disk (SARIF, JUnit XML,
JaCoCo, Cobertura, LCOV, Checkstyle XML, CPD XML):

    ./gradlew assemble
    cd /path/to/any/project
    java -jar /path/to/BuildChecks/build/libs/buildchecks-4.0.0-SNAPSHOT-all.jar check --open

`--open` launches `build/reports/buildchecks/index.html`; the same directory holds
`summary.md`, `summary.json`, `findings.json`, `codeclimate.json`, and `merged.sarif`.
An optional `buildchecks.toml` at the project root tunes paths, gates, and the baseline
file (documented in V4-PLAN.md §6). The repo's own `./gradlew run --args="check"` writes a
(findings-free) report for this codebase.

## Publish

Not wired up yet. Maven Central publishing, the GitHub Releases fat jar, and the GitHub
Action shim are phase 7 (V4-PLAN.md §11); this section gets the real steps then. Until
that lands, the shippable artifact is the fat jar from `./gradlew assemble`.
