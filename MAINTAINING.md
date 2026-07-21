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
    java -jar /path/to/BuildChecks/build/libs/buildchecks-4.0.0-all.jar check --open

`--open` launches `build/reports/buildchecks/index.html`; the same directory holds
`summary.md`, `summary.json`, `findings.json`, `codeclimate.json`, and `merged.sarif`.
An optional `buildchecks.toml` at the project root tunes paths, gates, and the baseline
file (documented in V4-PLAN.md §6). The repo's own `./gradlew run --args="check"` writes a
(findings-free) report for this codebase.

## Release

A release produces three things, all from one tag and all authenticated with the built-in
`GITHUB_TOKEN` — there are no signing keys, Sonatype tokens, or other secrets to manage:

- the **fat jar on GitHub Releases** (for `java -jar`, the first-party Action, Homebrew, and the
  install script),
- the **thin jar + POM in the `gh-pages` Maven repo** (so Gradle resolves
  `com.toddway:buildchecks:<version>` from `https://toddway.github.io/BuildChecks`),
- an updated **Homebrew formula** (`Formula/buildchecks.rb`, `url` + `sha256` bumped on `main`).

Set `version` in `build.gradle.kts` to the release number (no `-SNAPSHOT`), then tag and push:

    git tag v4.0.0 && git push origin v4.0.0

[`.github/workflows/release.yml`](.github/workflows/release.yml) does the rest: builds the fat
jar and attaches it to a GitHub Release; runs `publishMavenPublicationToPagesRepository` and
copies `build/maven-repo/` onto the `gh-pages` branch (previous versions accumulate; a `.nojekyll`
marker keeps Pages from mangling the Maven metadata); and rewrites the formula's `url`/`sha256`
to the new release. Nothing here needs credentials, so a plain `./gradlew build` on any machine
works with no setup.

To sanity-check the Maven layout locally before tagging:

    ./gradlew publishMavenPublicationToPagesRepository
    ls build/maven-repo/com/toddway/buildchecks/4.0.0/   # jar, .pom, .module

### One-time owner setup

- **Enable GitHub Pages** for the repo, serving the **`gh-pages` branch** (Settings → Pages).
  The first tagged release creates that branch; after Pages is enabled the Maven repo is live at
  `https://toddway.github.io/BuildChecks`.
- **Branch protection:** the workflow pushes a formula-bump commit to `main`. If `main` requires
  PRs, either allow the `github-actions[bot]` actor to bypass, or move the formula-bump to a PR.
- **Action Marketplace listing** (optional): publish `action.yml` from a tagged release through
  the repo's *Releases → Publish this Action to the Marketplace* flow.
