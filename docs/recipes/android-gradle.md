# Android (Gradle)

Android emits the same standard formats as any Gradle build — Android Lint (and/or detekt) as
SARIF, JaCoCo XML for coverage, JUnit XML for unit tests — so it gates exactly like the
[JVM recipe](gradle-detekt-jacoco.md). The only Android-specific work is that AGP gives you no
`jacocoTestReport` task, so you register one. BuildChecks resolves from its
[Pages Maven repo](https://toddway.github.io/BuildChecks) — nothing is committed to your repo,
and the JDK Gradle already runs on is the only prerequisite.

## Build wiring

```kotlin
// module build.gradle.kts (app or library)
plugins {
    id("com.android.application")            // or com.android.library
    id("org.jetbrains.kotlin.android")
    jacoco                                   // Android doesn't wire coverage for you — see below
    // id("io.gitlab.arturbosch.detekt") version "1.23.6"   // optional extra findings
}

android {
    testOptions {
        unitTests.all { it.extensions.configure<JacocoTaskExtension> { isIncludeNoLocationClasses = true } }
    }
    lint { sarifReport = true }              // → build/reports/lint-results-debug.sarif, ingested like detekt
}

// Resolve BuildChecks from its Pages Maven repo (keep google()/mavenCentral() for everything else).
repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }
val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }

// The Android wrinkle: unlike kotlin("jvm"), AGP registers no jacocoTestReport. Add one.
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports { xml.required = true; html.required = false }   // XML is what BuildChecks reads
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("**/testDebugUnitTest.exec") })
}

// The single gate — runs on the JDK Gradle already uses.
tasks.register<JavaExec>("buildchecks") {
    dependsOn("testDebugUnitTest", "jacocoTestReport", "lintDebug")   // produce the reports first
    classpath = buildchecks
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

## Run

```bash
./gradlew check      # unit tests + coverage + lint, then the single gate (non-zero on failure)
```

`testDebugUnitTest` writes JUnit XML to `build/test-results/…`, the report task writes JaCoCo XML
to `build/reports/…`, and Lint writes its SARIF to `build/reports/…` — all default discovery
locations, so **no `paths` config is needed**. Gate thresholds (coverage %, which findings gate)
go in a root `buildchecks.toml`.

## Notes

- **First run:** snapshot the baseline once and commit it — a sibling task with `args("baseline")`,
  or `./gradlew buildchecks` after temporarily swapping `check` for `baseline`. Commit
  `buildchecks-baseline.txt`.
- **Multi-module:** apply the coverage/lint config per module (a `buildSrc` convention plugin is
  the tidy way), but register the `buildchecks` **JavaExec at the root** so it aggregates every
  module's reports — each module's `build/…` is discovered and grouped by origin automatically:

  ```kotlin
  // root build.gradle.kts
  repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }
  val buildchecks by configurations.creating
  dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }
  tasks.register<JavaExec>("buildchecks") {
      dependsOn(subprojects.map { "${it.path}:testDebugUnitTest" })   // + jacoco/lint tasks
      classpath = buildchecks
      args("check")
  }
  ```

- **Findings format:** enable Android Lint's SARIF (`lint { sarifReport = true }`) and/or detekt's
  SARIF; ingest one format per tool so findings aren't double-counted.
- **Architecture rules:** [Konsist](https://docs.konsist.lemonappdev.com/) run as tests emit JUnit
  XML, so a layering violation gates today with no extra config.
