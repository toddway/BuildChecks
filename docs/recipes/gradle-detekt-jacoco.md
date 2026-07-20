# Gradle + detekt + JaCoCo

Findings from [detekt](https://detekt.dev/) as SARIF, coverage from JaCoCo XML, tests as JUnit
XML — all already produced by a standard Gradle build. BuildChecks resolves from Maven Central
and gates without a plugin.

## Build wiring

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    jacoco
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

detekt {
    ignoreFailures = true          // let BuildChecks gate, not detekt
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports { sarif.required = true }   // build/reports/detekt/detekt.sarif
}

tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport {
    reports { xml.required = true }     // build/reports/jacoco/test/jacocoTestReportXml/...
}

// Resolve BuildChecks from Maven Central and run it as the single gate.
val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }
tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

## Run

```bash
./gradlew test detekt   # produce reports (neither fails the build)
./gradlew buildchecks   # or just `./gradlew check`, which finalizes with it
```

## Notes

- **Emit SARIF, not the Checkstyle XML, from detekt.** detekt can produce both; ingesting both
  double-counts findings. Pick one format per tool.
- **Android:** enable `lintOptions { sarifReport = true }` (AGP) or the Android Lint SARIF
  output; it's ingested exactly like detekt's. JaCoCo per-variant reports live under each
  module's `build/` and are grouped by origin automatically.
- **Architecture rules:** [Konsist](https://docs.konsist.lemonappdev.com/) run as tests emit
  JUnit XML, so a layering violation gates today with no extra config.
- **First run:** snapshot the baseline once with a sibling task (the same `JavaExec` block with
  `args("baseline")`), then commit `buildchecks-baseline.txt`.
