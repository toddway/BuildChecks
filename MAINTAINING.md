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
`summary.md`, `summary.txt`, `summary.json`, `findings.json`, `codeclimate.json`, and `merged.sarif`.
An optional `buildchecks.toml` at the project root tunes paths, gates, and the baseline
file (documented in V4-PLAN.md §6). The repo's own `./gradlew run --args="check"` writes a
(findings-free) report for this codebase.

## Release

A release is two commands, and everything authenticates with the built-in `GITHUB_TOKEN` — there
are no signing keys, Sonatype tokens, or other secrets to manage:

    ./release.sh 4.0.1              # prep: version bump + jar + formula pin + commit + tag
    git push origin main v4.0.1     # release: triggers the workflow

The push is deliberately separate — it's the irreversible, public step, so the default stops just
before it and you can inspect the commit/diff first. Pass `--push` to do both at once
(`./release.sh 4.0.1 --push`) once you're confident.

[`release.sh`](release.sh) refuses to run unless the tree is clean and on `main`, then: sets
`version` in `build.gradle.kts`, builds the **reproducible** fat jar, pins `Formula/buildchecks.rb`
to that jar's `url` + `sha256`, commits, and tags. Because the jar is reproducible, the checksum it
pins matches the jar the workflow rebuilds — so the formula is correct *before* the tag exists, with
no bot pushing back to the protected `main` branch.

The push then triggers [`.github/workflows/release.yml`](.github/workflows/release.yml), which:
builds the fat jar and attaches it to a GitHub Release; runs `publishMavenPublicationToPagesRepository`
and copies `build/maven-repo/` onto the `gh-pages` branch (previous versions accumulate; a `.nojekyll`
marker keeps Pages from mangling the Maven metadata); and **verifies** the formula's `sha256` matches
the built jar, failing the release loudly if they ever drift. Nothing needs credentials, so a plain
`./gradlew build` on any machine works with no setup.

The `main` branch is protected (the `build` status check is required for PRs). `release.sh`'s commit
lands via your normal push to `main`; the formula is never bumped by a bot afterward, so no
branch-protection bypass is needed.

### One-time owner setup

> Already completed for `toddway/BuildChecks` — kept here as reference for a fork or new maintainer.

- **Enable GitHub Pages** for the repo, serving the **`gh-pages` branch** (Settings → Pages).
  The first tagged release creates that branch; after Pages is enabled the Maven repo is live at
  `https://toddway.github.io/BuildChecks`.
- **Branch protection:** the workflow pushes a formula-bump commit to `main`. If `main` requires
  PRs, either allow the `github-actions[bot]` actor to bypass, or move the formula-bump to a PR.
- **Action Marketplace listing** (optional): publish `action.yml` from a tagged release through
  the repo's *Releases → Publish this Action to the Marketplace* flow.
