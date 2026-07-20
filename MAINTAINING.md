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

A release has two artifacts: the **thin jar + POM on Maven Central** (so the Gradle `JavaExec`
snippet resolves `com.toddway:buildchecks:<version>` and its deps) and the **fat jar on GitHub
Releases** (for `java -jar` and the first-party Action). Set `version` in `build.gradle.kts` to
the release number (no `-SNAPSHOT`) before tagging.

### 1. GitHub Release + fat jar (automated)

Tag and push; [`.github/workflows/release.yml`](.github/workflows/release.yml) builds the fat
jar and attaches it to a GitHub Release named for the tag:

    git tag v4.0.0 && git push origin v4.0.0

The first-party Action (`action.yml`, used as `toddway/BuildChecks@v4.0.0`) and the `java -jar`
recipes download `buildchecks-<version>-all.jar` from that release.

### 2. Maven Central (manual — needs credentials only the owner has)

Publishing signs the artifacts and uploads a bundle to the [Central Portal](https://central.sonatype.com).
It is deliberately manual: it needs a PGP signing key and a Central Portal token, which never
live in the repo. Only core Gradle plugins are used (`maven-publish` + `signing`), so a plain
`./gradlew build` needs none of this.

    # PGP key (ASCII-armored) and its passphrase; the public key must be on a keyserver
    export SIGNING_KEY="$(gpg --armor --export-secret-keys <KEY_ID>)"
    export SIGNING_PASSWORD="…"

    # Alternatively, the classic gradle-signing properties in the gitignored repo-root
    # gradle.properties work too: signing.keyId / signing.password / signing.secretKeyRingFile.
    # The in-memory env form above is preferred — it needs no keyring file on disk.

    ./gradlew publishMavenPublicationToStagingRepository   # signed artifacts → build/staging-deploy

    # zip the bundle and upload to the Central Portal Publisher API
    (cd build/staging-deploy && zip -qr ../central-bundle.zip .)
    curl --fail -H "Authorization: Bearer $CENTRAL_TOKEN" \
      -F bundle=@build/central-bundle.zip \
      https://central.sonatype.com/api/v1/publisher/upload

Then approve the deployment in the Central Portal UI (or pass `?publishingType=AUTOMATIC`).
`CENTRAL_TOKEN` is the base64 user-token from your Central Portal account settings.

### One-time owner setup

- **Central Portal namespace** `com.toddway` verified (via the GitHub-account verification flow).
- **PGP key** generated and its public half published to a keyserver.
- **Action Marketplace listing** (optional): publish `action.yml` from a tagged release through
  the repo's *Releases → Publish this Action to the Marketplace* flow.
